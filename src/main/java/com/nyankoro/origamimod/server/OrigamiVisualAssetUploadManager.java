package com.nyankoro.origamimod.server;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.network.UploadOrigamiVisualAssetChunkPayload;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.Map;
import java.util.UUID;

import java.util.concurrent.ConcurrentHashMap;


/*
 * Clientから送られてきた
 * 表裏PNGを再構築する。
 *
 * 現段階では永続保存せず、
 * Serverメモリ上だけにキャッシュする。
 */
public final class OrigamiVisualAssetUploadManager {

    private static final int MAX_IMAGE_DIMENSION =
            1024;


    /*
     * 今は一時キャッシュなので
     * 無制限には保持しない。
     */
    private static final int MAX_CACHED_ASSETS =
            32;


    /*
     * Playerごとに同時アップロードは1作品。
     */
    private static final Map<
            UUID,
            PendingUpload
            >
            PENDING =
            new ConcurrentHashMap<>();


    private static final Map<
            String,
            CachedAsset
            >
            CACHE =
            new ConcurrentHashMap<>();


    private OrigamiVisualAssetUploadManager() {
    }


    public enum Status {

        ACCEPTED,
        COMPLETE,
        ALREADY_CACHED,
        REJECTED
    }


    public record UploadResult(
            Status status,
            String message
    ) {
    }


    private record PngInfo(
            int width,
            int height
    ) {
    }


    private record CachedAsset(
            int width,
            int height,
            byte[] frontPng,
            byte[] backPng
    ) {

        private CachedAsset {

            frontPng =
                    frontPng.clone();

            backPng =
                    backPng.clone();
        }
    }


    public static UploadResult acceptChunk(
            UUID playerId,
            UploadOrigamiVisualAssetChunkPayload payload
    ) {

        String metadataError =
                validateMetadata(
                        payload
                );


        if (metadataError != null) {

            PENDING.remove(
                    playerId
            );


            return rejected(
                    metadataError
            );
        }


        if (CACHE.containsKey(
                payload.visualAssetId()
        )) {

            return new UploadResult(
                    Status.ALREADY_CACHED,
                    "already cached"
            );
        }


        /*
         * 別assetの送信が始まった場合は
         * 以前の未完了データを破棄する。
         */
        PendingUpload pending =
                PENDING.compute(
                        playerId,
                        (uuid, current) -> {

                            if (current == null
                                    || !current.assetId.equals(
                                    payload.visualAssetId()
                            )) {

                                return new PendingUpload(
                                        payload.visualAssetId()
                                );
                            }


                            return current;
                        }
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
                        playerId,
                        pending
                );


                return rejected(
                        "Chunk metadata changed during upload"
                );
            }


            if (!assembly.accept(
                    payload.chunkIndex(),
                    payload.chunkData()
            )) {

                PENDING.remove(
                        playerId,
                        pending
                );


                return rejected(
                        "Invalid or duplicate chunk"
                );
            }


            if (!pending.isComplete()) {

                return new UploadResult(
                        Status.ACCEPTED,
                        "chunk accepted"
                );
            }


            PENDING.remove(
                    playerId,
                    pending
            );


            return validateAndCache(
                    pending
            );
        }
    }


    public static boolean contains(
            String visualAssetId
    ) {

        return CACHE.containsKey(
                visualAssetId
        );
    }


    public static int cachedAssetCount() {

        return CACHE.size();
    }


    private static String validateMetadata(
            UploadOrigamiVisualAssetChunkPayload payload
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
                > UploadOrigamiVisualAssetChunkPayload
                .MAX_IMAGE_BYTES) {

            return "Invalid total image size";
        }


        int expectedChunkCount =
                (
                        payload.totalLength()
                                + UploadOrigamiVisualAssetChunkPayload
                                .CHUNK_SIZE
                                - 1
                )
                        / UploadOrigamiVisualAssetChunkPayload
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
                        * UploadOrigamiVisualAssetChunkPayload
                        .CHUNK_SIZE;


        int expectedLength =
                Math.min(
                        UploadOrigamiVisualAssetChunkPayload
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


    private static UploadResult validateAndCache(
            PendingUpload pending
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


        /*
         * 表裏は同じキャンバスサイズとする。
         */
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


            if (frontImage.getWidth()
                    != frontInfo.width()
                    || frontImage.getHeight()
                    != frontInfo.height()
                    || backImage.getWidth()
                    != backInfo.width()
                    || backImage.getHeight()
                    != backInfo.height()) {

                return rejected(
                        "Decoded image dimensions differ"
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


            /*
             * Server側でもvisualAssetIdを
             * 独立して再計算する。
             */
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


        } catch (IOException e) {

            return rejected(
                    "PNG decode failed"
            );
        }


        if (!CACHE.containsKey(
                pending.assetId
        )
                && CACHE.size()
                >= MAX_CACHED_ASSETS) {

            return rejected(
                    "Temporary asset cache is full"
            );
        }


        CACHE.putIfAbsent(
                pending.assetId,
                new CachedAsset(
                        frontInfo.width(),
                        frontInfo.height(),
                        front,
                        back
                )
        );


        OrigamiMod.LOGGER.info(
                "Verified origami visual asset: "
                        + "assetId={}, "
                        + "size={}x{}, "
                        + "front={} bytes, "
                        + "back={} bytes, "
                        + "cachedAssets={}",
                pending.assetId,
                frontInfo.width(),
                frontInfo.height(),
                front.length,
                back.length,
                CACHE.size()
        );


        return new UploadResult(
                Status.COMPLETE,
                "verified"
        );
    }


    /*
     * PNG全体を展開する前に
     * IHDRから画像サイズを確認する。
     *
     * 巨大画像によるメモリ消費を防ぐ。
     */
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


        /*
         * 最初のchunkはIHDRで、
         * data長は13byte。
         */
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


    private static UploadResult rejected(
            String message
    ) {

        return new UploadResult(
                Status.REJECTED,
                message
        );
    }


    private static final class PendingUpload {

        private final String assetId;

        private ImageAssembly front;

        private ImageAssembly back;


        private PendingUpload(
                String assetId
        ) {

            this.assetId =
                    assetId;
        }


        private ImageAssembly getOrCreate(
                UploadOrigamiVisualAssetChunkPayload.Side side,
                int totalLength,
                int chunkCount
        ) {

            if (side
                    == UploadOrigamiVisualAssetChunkPayload
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
                            * UploadOrigamiVisualAssetChunkPayload
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