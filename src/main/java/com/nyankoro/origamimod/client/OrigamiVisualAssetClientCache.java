package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.network
        .OrigamiVisualAssetDownloadChunkPayload;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/*
 * Serverから受信したPNGを再構築し、
 * Client側へキャッシュする。
 *
 * visualAssetIdは画像内容のhashなので、
 * サーバーをまたいで同じ画像を再利用できる。
 */
public final class OrigamiVisualAssetClientCache {

    private static final int MAX_IMAGE_DIMENSION =
            1024;

    private static final String FRONT_FILE =
            "front.png";

    private static final String BACK_FILE =
            "back.png";


    private static final Map<
            String,
            PendingDownload
            >
            PENDING =
            new ConcurrentHashMap<>();


    private static final Object WRITE_LOCK =
            new Object();


    private OrigamiVisualAssetClientCache() {
    }


    public enum Status {

        ACCEPTED,
        COMPLETE,
        ALREADY_CACHED,
        REJECTED
    }


    public record Result(
            Status status,
            String message,
            Path directory
    ) {
    }


    private record PngInfo(
            int width,
            int height
    ) {
    }


    public static boolean isCached(
            String visualAssetId
    ) {

        if (!OrigamiVisualAssetId.isValidFormat(
                visualAssetId
        )) {

            return false;
        }


        Path directory =
                assetDirectory(
                        visualAssetId
                );


        return Files.isRegularFile(
                directory.resolve(
                        FRONT_FILE
                )
        )
                && Files.isRegularFile(
                directory.resolve(
                        BACK_FILE
                )
        );
    }


    public static Path assetDirectory(
            String visualAssetId
    ) {

        if (!OrigamiVisualAssetId.isValidFormat(
                visualAssetId
        )) {

            throw new IllegalArgumentException(
                    "Invalid visualAssetId"
            );
        }


        return cacheRoot()
                .resolve(
                        visualAssetId
                );
    }


    public static Result acceptChunk(
            OrigamiVisualAssetDownloadChunkPayload payload
    ) {

        String error =
                validateMetadata(
                        payload
                );


        if (error != null) {

            PENDING.remove(
                    payload.visualAssetId()
            );

            return rejected(
                    error
            );
        }


        if (isCached(
                payload.visualAssetId()
        )) {

            PENDING.remove(
                    payload.visualAssetId()
            );


            return new Result(
                    Status.ALREADY_CACHED,
                    "already cached",
                    assetDirectory(
                            payload.visualAssetId()
                    )
            );
        }


        PendingDownload pending =
                PENDING.computeIfAbsent(
                        payload.visualAssetId(),
                        PendingDownload::new
                );


        synchronized (pending) {

            ImageAssembly assembly =
                    pending.getOrCreate(
                            payload.side(),
                            payload.totalLength(),
                            payload.chunkCount()
                    );


            if (assembly == null) {

                PENDING.remove(
                        payload.visualAssetId(),
                        pending
                );


                return rejected(
                        "Chunk metadata changed"
                );
            }


            if (!assembly.accept(
                    payload.chunkIndex(),
                    payload.chunkData()
            )) {

                PENDING.remove(
                        payload.visualAssetId(),
                        pending
                );


                return rejected(
                        "Invalid or duplicate chunk"
                );
            }


            if (!pending.isComplete()) {

                return new Result(
                        Status.ACCEPTED,
                        "chunk accepted",
                        null
                );
            }


            PENDING.remove(
                    payload.visualAssetId(),
                    pending
            );


            return validateAndStore(
                    pending
            );
        }
    }


    private static Result validateAndStore(
            PendingDownload pending
    ) {

        byte[] front =
                pending.front.copyBytes();

        byte[] back =
                pending.back.copyBytes();


        PngInfo frontInfo =
                inspectPng(
                        front
                );

        PngInfo backInfo =
                inspectPng(
                        back
                );


        if (frontInfo == null
                || backInfo == null) {

            return rejected(
                    "Invalid PNG header"
            );
        }


        if (frontInfo.width()
                != backInfo.width()
                || frontInfo.height()
                != backInfo.height()) {

            return rejected(
                    "Front/back dimensions differ"
            );
        }


        try {

            BufferedImage frontImage =
                    ImageIO.read(
                            new ByteArrayInputStream(
                                    front
                            )
                    );

            BufferedImage backImage =
                    ImageIO.read(
                            new ByteArrayInputStream(
                                    back
                            )
                    );


            if (frontImage == null
                    || backImage == null) {

                return rejected(
                        "Could not decode PNG"
                );
            }


            int pixelCount =
                    frontInfo.width()
                            * frontInfo.height();


            int[] frontPixels =
                    new int[
                            pixelCount
                            ];

            int[] backPixels =
                    new int[
                            pixelCount
                            ];


            frontImage.getRGB(
                    0,
                    0,
                    frontInfo.width(),
                    frontInfo.height(),
                    frontPixels,
                    0,
                    frontInfo.width()
            );


            backImage.getRGB(
                    0,
                    0,
                    backInfo.width(),
                    backInfo.height(),
                    backPixels,
                    0,
                    backInfo.width()
            );


            String calculatedAssetId =
                    OrigamiVisualAssetId.calculate(
                            frontInfo.width(),
                            frontInfo.height(),
                            frontPixels,
                            backPixels
                    );


            if (!calculatedAssetId.equals(
                    pending.assetId
            )) {

                return rejected(
                        "visualAssetId mismatch"
                );
            }


            Path directory =
                    writeCache(
                            pending.assetId,
                            front,
                            back
                    );


            OrigamiMod.LOGGER.info(
                    "Cached origami visual asset: "
                            + "assetId={}, "
                            + "size={}x{}, "
                            + "front={} bytes, "
                            + "back={} bytes, "
                            + "directory={}",
                    pending.assetId,
                    frontInfo.width(),
                    frontInfo.height(),
                    front.length,
                    back.length,
                    directory
            );


            return new Result(
                    Status.COMPLETE,
                    "verified and cached",
                    directory
            );


        } catch (IOException e) {

            OrigamiMod.LOGGER.error(
                    "Failed to cache origami visual asset",
                    e
            );


            return rejected(
                    "PNG processing failed"
            );
        }
    }


    private static Path writeCache(
            String visualAssetId,
            byte[] front,
            byte[] back
    ) throws IOException {

        synchronized (WRITE_LOCK) {

            Path directory =
                    assetDirectory(
                            visualAssetId
                    );


            if (isCached(
                    visualAssetId
            )) {

                return directory;
            }


            Path root =
                    cacheRoot();


            Files.createDirectories(
                    root
            );


            /*
             * 過去の不完全なcacheがあれば掃除する。
             */
            if (Files.exists(
                    directory
            )) {

                cleanupDirectory(
                        directory
                );
            }


            Path temporaryDirectory =
                    Files.createTempDirectory(
                            root,
                            ".download-"
                    );


            try {

                Files.write(
                        temporaryDirectory.resolve(
                                FRONT_FILE
                        ),
                        front
                );


                Files.write(
                        temporaryDirectory.resolve(
                                BACK_FILE
                        ),
                        back
                );


                try {

                    Files.move(
                            temporaryDirectory,
                            directory,
                            StandardCopyOption.ATOMIC_MOVE
                    );

                } catch (
                        AtomicMoveNotSupportedException e
                ) {

                    Files.move(
                            temporaryDirectory,
                            directory
                    );
                }


                return directory;


            } catch (FileAlreadyExistsException e) {

                return directory;


            } finally {

                cleanupDirectory(
                        temporaryDirectory
                );
            }
        }
    }


    private static Path cacheRoot() {

        return Minecraft.getInstance()
                .gameDirectory
                .toPath()
                .resolve(
                        "origamimod-cache"
                )
                .resolve(
                        "visual_assets"
                );
    }


    private static String validateMetadata(
            OrigamiVisualAssetDownloadChunkPayload payload
    ) {

        if (!OrigamiVisualAssetId.isValidFormat(
                payload.visualAssetId()
        )) {

            return "Invalid visualAssetId";
        }


        if (payload.side() == null) {

            return "Missing side";
        }


        if (payload.totalLength() <= 0
                || payload.totalLength()
                > OrigamiVisualAssetDownloadChunkPayload
                .MAX_IMAGE_BYTES) {

            return "Invalid total length";
        }


        int expectedChunkCount =
                (
                        payload.totalLength()
                                + OrigamiVisualAssetDownloadChunkPayload
                                .CHUNK_SIZE
                                - 1
                )
                        / OrigamiVisualAssetDownloadChunkPayload
                        .CHUNK_SIZE;


        if (payload.chunkCount()
                != expectedChunkCount) {

            return "Invalid chunk count";
        }


        if (payload.chunkIndex() < 0
                || payload.chunkIndex()
                >= payload.chunkCount()) {

            return "Invalid chunk index";
        }


        int offset =
                payload.chunkIndex()
                        * OrigamiVisualAssetDownloadChunkPayload
                        .CHUNK_SIZE;


        int expectedLength =
                Math.min(
                        OrigamiVisualAssetDownloadChunkPayload
                                .CHUNK_SIZE,
                        payload.totalLength()
                                - offset
                );


        if (payload.chunkData().length
                != expectedLength) {

            return "Invalid chunk length";
        }


        return null;
    }


    private static PngInfo inspectPng(
            byte[] data
    ) {

        if (data == null
                || data.length < 33) {

            return null;
        }


        byte[] signature = {
                (byte) 0x89,
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A
        };


        for (int i = 0;
             i < signature.length;
             i++) {

            if (data[i]
                    != signature[i]) {

                return null;
            }
        }


        if (readInt(
                data,
                8
        ) != 13) {

            return null;
        }


        if (data[12] != 'I'
                || data[13] != 'H'
                || data[14] != 'D'
                || data[15] != 'R') {

            return null;
        }


        int width =
                readInt(
                        data,
                        16
                );

        int height =
                readInt(
                        data,
                        20
                );


        if (width <= 0
                || height <= 0
                || width > MAX_IMAGE_DIMENSION
                || height > MAX_IMAGE_DIMENSION) {

            return null;
        }


        return new PngInfo(
                width,
                height
        );
    }


    private static int readInt(
            byte[] data,
            int offset
    ) {

        return (
                data[offset]
                        & 0xFF
        ) << 24
                | (
                data[offset + 1]
                        & 0xFF
        ) << 16
                | (
                data[offset + 2]
                        & 0xFF
        ) << 8
                | (
                data[offset + 3]
                        & 0xFF
        );
    }


    private static void cleanupDirectory(
            Path directory
    ) {

        try {

            Files.deleteIfExists(
                    directory.resolve(
                            FRONT_FILE
                    )
            );

            Files.deleteIfExists(
                    directory.resolve(
                            BACK_FILE
                    )
            );

            Files.deleteIfExists(
                    directory
            );

        } catch (IOException ignored) {
        }
    }


    private static Result rejected(
            String message
    ) {

        return new Result(
                Status.REJECTED,
                message,
                null
        );
    }


    private static final class PendingDownload {

        private final String assetId;

        private ImageAssembly front;

        private ImageAssembly back;


        private PendingDownload(
                String assetId
        ) {

            this.assetId =
                    assetId;
        }


        private ImageAssembly getOrCreate(
                OrigamiVisualAssetDownloadChunkPayload.Side side,
                int totalLength,
                int chunkCount
        ) {

            if (side
                    == OrigamiVisualAssetDownloadChunkPayload
                    .Side.FRONT) {

                if (front == null) {

                    front =
                            new ImageAssembly(
                                    totalLength,
                                    chunkCount
                            );
                }


                if (!front.matches(
                        totalLength,
                        chunkCount
                )) {

                    return null;
                }


                return front;
            }


            if (back == null) {

                back =
                        new ImageAssembly(
                                totalLength,
                                chunkCount
                        );
            }


            if (!back.matches(
                    totalLength,
                    chunkCount
            )) {

                return null;
            }


            return back;
        }


        private boolean isComplete() {

            return front != null
                    && back != null
                    && front.isComplete()
                    && back.isComplete();
        }
    }


    private static final class ImageAssembly {

        private final int totalLength;

        private final int chunkCount;

        private final byte[] bytes;

        private final boolean[] received;

        private int receivedCount;


        private ImageAssembly(
                int totalLength,
                int chunkCount
        ) {

            this.totalLength =
                    totalLength;

            this.chunkCount =
                    chunkCount;

            this.bytes =
                    new byte[
                            totalLength
                            ];

            this.received =
                    new boolean[
                            chunkCount
                            ];
        }


        private boolean matches(
                int totalLength,
                int chunkCount
        ) {

            return this.totalLength
                    == totalLength
                    && this.chunkCount
                    == chunkCount;
        }


        private boolean accept(
                int chunkIndex,
                byte[] chunk
        ) {

            if (received[
                    chunkIndex
                    ]) {

                return false;
            }


            int offset =
                    chunkIndex
                            * OrigamiVisualAssetDownloadChunkPayload
                            .CHUNK_SIZE;


            if (offset < 0
                    || offset + chunk.length
                    > bytes.length) {

                return false;
            }


            System.arraycopy(
                    chunk,
                    0,
                    bytes,
                    offset,
                    chunk.length
            );


            received[
                    chunkIndex
                    ] =
                    true;


            receivedCount++;


            return true;
        }


        private boolean isComplete() {

            return receivedCount
                    == chunkCount;
        }


        private byte[] copyBytes() {

            return bytes.clone();
        }
    }
}