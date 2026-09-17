package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.network
        .CreasePatternDownloadChunkPayload;
import com.nyankoro.origamimod.origami
        .CreasePatternId;


/*
 * Serverから届いたCPチャンクを
 * Client側で再構築する。
 */
public final class CreasePatternDownloadManager {

    private static PendingDownload pending;


    private CreasePatternDownloadManager() {
    }


    public enum Status {

        ACCEPTED,
        COMPLETE,
        REJECTED
    }


    public record Result(
            Status status,
            String creasePatternId,
            String fileName,
            byte[] cpData
    ) {

        public Result {

            if (cpData != null) {

                cpData =
                        cpData.clone();
            }
        }


        @Override
        public byte[] cpData() {

            return cpData == null
                    ? null
                    : cpData.clone();
        }
    }


    public static synchronized Result acceptChunk(
            CreasePatternDownloadChunkPayload payload
    ) {

        if (!CreasePatternId.isValidFormat(
                payload.creasePatternId()
        )) {

            pending =
                    null;

            return rejected();
        }


        if (payload.totalLength() <= 0
                || payload.totalLength()
                > CreasePatternDownloadChunkPayload.MAX_CP_BYTES) {

            pending =
                    null;

            return rejected();
        }


        int expectedChunkCount =
                (
                        payload.totalLength()
                                + CreasePatternDownloadChunkPayload.CHUNK_SIZE
                                - 1
                )
                        / CreasePatternDownloadChunkPayload.CHUNK_SIZE;


        if (payload.chunkCount()
                != expectedChunkCount
                || payload.chunkIndex() < 0
                || payload.chunkIndex()
                >= payload.chunkCount()) {

            pending =
                    null;

            return rejected();
        }


        if (pending == null
                || !pending.matches(
                payload
        )) {

            pending =
                    new PendingDownload(
                            payload.creasePatternId(),
                            payload.fileName(),
                            payload.totalLength(),
                            payload.chunkCount()
                    );
        }


        if (!pending.accept(
                payload.chunkIndex(),
                payload.chunkData()
        )) {

            pending =
                    null;

            return rejected();
        }


        if (!pending.isComplete()) {

            return new Result(
                    Status.ACCEPTED,
                    payload.creasePatternId(),
                    payload.fileName(),
                    null
            );
        }


        byte[] cpData =
                pending.bytes.clone();

        String id =
                pending.creasePatternId;

        String fileName =
                pending.fileName;


        pending =
                null;


        /*
         * Client側でも内容からIDを再計算。
         */
        if (!CreasePatternId.calculate(
                cpData
        ).equals(
                id
        )) {

            return rejected();
        }


        return new Result(
                Status.COMPLETE,
                id,
                fileName,
                cpData
        );
    }


    private static Result rejected() {

        return new Result(
                Status.REJECTED,
                "",
                "",
                null
        );
    }


    private static final class PendingDownload {

        private final String creasePatternId;

        private final String fileName;

        private final int totalLength;

        private final int chunkCount;

        private final byte[] bytes;

        private final boolean[] received;

        private int receivedCount;


        private PendingDownload(
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
                CreasePatternDownloadChunkPayload payload
        ) {

            return creasePatternId.equals(
                    payload.creasePatternId()
            )
                    && fileName.equals(
                    payload.fileName()
            )
                    && totalLength
                    == payload.totalLength()
                    && chunkCount
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
                            * CreasePatternDownloadChunkPayload.CHUNK_SIZE;


            if (offset + chunk.length
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
    }
}