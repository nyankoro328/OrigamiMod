package com.nyankoro.origamimod.server;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.network
        .UploadCreasePatternChunkPayload;
import com.nyankoro.origamimod.origami
        .CreasePatternId;

import net.minecraft.server.MinecraftServer;

import java.io.IOException;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import java.util.concurrent.ConcurrentHashMap;


/*
 * Clientから届くCPチャンクを再構築して
 * CreasePatternStoreへ保存する。
 */
public final class CreasePatternUploadManager {

    private static final Map<
            UUID,
            PendingUpload
            >
            PENDING =
            new ConcurrentHashMap<>();


    private CreasePatternUploadManager() {
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


    public static UploadResult acceptChunk(
            MinecraftServer server,
            UUID playerId,
            UploadCreasePatternChunkPayload payload
    ) {

        String error =
                validateMetadata(
                        payload
                );


        if (error != null) {

            PENDING.remove(
                    playerId
            );

            return rejected(
                    error
            );
        }


        if (CreasePatternStore.exists(
                server,
                payload.creasePatternId()
        )) {

            return new UploadResult(
                    Status.ALREADY_CACHED,
                    "already persisted"
            );
        }


        PendingUpload pending =
                PENDING.compute(
                        playerId,
                        (uuid, current) -> {

                            if (current == null
                                    || !current.matches(
                                    payload
                            )) {

                                return new PendingUpload(
                                        payload.creasePatternId(),
                                        payload.fileName(),
                                        payload.totalLength(),
                                        payload.chunkCount()
                                );
                            }


                            return current;
                        }
                );


        synchronized (pending) {

            if (!pending.accept(
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


            byte[] cpData =
                    pending.copyBytes();


            String calculatedId =
                    CreasePatternId.calculate(
                            cpData
                    );


            if (!calculatedId.equals(
                    pending.creasePatternId
            )) {

                return rejected(
                        "creasePatternId mismatch"
                );
            }


            try {

                CreasePatternStore.SaveResult saveResult =
                        CreasePatternStore.save(
                                server,
                                pending.creasePatternId,
                                pending.fileName,
                                cpData
                        );


                if (!saveResult.created()) {

                    return new UploadResult(
                            Status.ALREADY_CACHED,
                            "already persisted"
                    );
                }


                OrigamiMod.LOGGER.info(
                        "Persisted crease pattern: "
                                + "id={}, file={}, bytes={}, directory={}",
                        pending.creasePatternId,
                        pending.fileName,
                        cpData.length,
                        saveResult.directory()
                );


                return new UploadResult(
                        Status.COMPLETE,
                        "saved"
                );


            } catch (IOException
                     | IllegalArgumentException e) {

                OrigamiMod.LOGGER.error(
                        "Failed to persist crease pattern",
                        e
                );


                return rejected(
                        "Could not persist crease pattern"
                );
            }
        }
    }


    private static String validateMetadata(
            UploadCreasePatternChunkPayload payload
    ) {

        if (!CreasePatternId.isValidFormat(
                payload.creasePatternId()
        )) {

            return "Invalid creasePatternId";
        }


        if (payload.fileName() == null
                || payload.fileName().isBlank()
                || payload.fileName().length()
                > UploadCreasePatternChunkPayload
                .MAX_FILE_NAME_LENGTH) {

            return "Invalid file name";
        }


        if (!payload.fileName()
                .toLowerCase(
                        Locale.ROOT
                )
                .endsWith(
                        ".cp"
                )) {

            return "Not a CP file";
        }


        if (payload.totalLength() <= 0
                || payload.totalLength()
                > UploadCreasePatternChunkPayload
                .MAX_CP_BYTES) {

            return "Invalid CP size";
        }


        int expectedChunkCount =
                (
                        payload.totalLength()
                                + UploadCreasePatternChunkPayload.CHUNK_SIZE
                                - 1
                )
                        / UploadCreasePatternChunkPayload.CHUNK_SIZE;


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
                        * UploadCreasePatternChunkPayload.CHUNK_SIZE;


        int expectedLength =
                Math.min(
                        UploadCreasePatternChunkPayload.CHUNK_SIZE,
                        payload.totalLength()
                                - offset
                );


        if (payload.chunkData().length
                != expectedLength) {

            return "Invalid chunk length";
        }


        return null;
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

        private final String creasePatternId;

        private final String fileName;

        private final int totalLength;

        private final int chunkCount;

        private final byte[] bytes;

        private final boolean[] received;

        private int receivedCount;


        private PendingUpload(
                String creasePatternId,
                String fileName,
                int totalLength,
                int chunkCount
        ) {

            this.creasePatternId =
                    creasePatternId;

            this.fileName =
                    fileName;

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
                UploadCreasePatternChunkPayload payload
        ) {

            return this.creasePatternId.equals(
                    payload.creasePatternId()
            )
                    && this.fileName.equals(
                    payload.fileName()
            )
                    && this.totalLength
                    == payload.totalLength()
                    && this.chunkCount
                    == payload.chunkCount();
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
                            * UploadCreasePatternChunkPayload.CHUNK_SIZE;


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