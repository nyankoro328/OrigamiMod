package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.network
        .UploadCreasePatternChunkPayload;
import com.nyankoro.origamimod.origami
        .CreasePatternId;

import net.neoforged.neoforge.client.network
        .ClientPacketDistributor;

import java.util.Arrays;


/*
 * CPをServerへチャンク送信する。
 */
public final class CreasePatternUploader {

    private CreasePatternUploader() {
    }


    public static String upload(
            String fileName,
            byte[] cpData
    ) {

        if (cpData == null
                || cpData.length == 0
                || cpData.length
                > UploadCreasePatternChunkPayload.MAX_CP_BYTES) {

            throw new IllegalArgumentException(
                    "Invalid CP size"
            );
        }


        String creasePatternId =
                CreasePatternId.calculate(
                        cpData
                );


        int chunkCount =
                (
                        cpData.length
                                + UploadCreasePatternChunkPayload.CHUNK_SIZE
                                - 1
                )
                        / UploadCreasePatternChunkPayload.CHUNK_SIZE;


        OrigamiMod.LOGGER.info(
                "Sending crease pattern: "
                        + "id={}, file={}, bytes={}, chunks={}",
                creasePatternId,
                fileName,
                cpData.length,
                chunkCount
        );


        for (int chunkIndex = 0;
             chunkIndex < chunkCount;
             chunkIndex++) {

            int start =
                    chunkIndex
                            * UploadCreasePatternChunkPayload.CHUNK_SIZE;


            int end =
                    Math.min(
                            start
                                    + UploadCreasePatternChunkPayload.CHUNK_SIZE,
                            cpData.length
                    );


            byte[] chunk =
                    Arrays.copyOfRange(
                            cpData,
                            start,
                            end
                    );


            ClientPacketDistributor.sendToServer(
                    new UploadCreasePatternChunkPayload(
                            creasePatternId,
                            fileName,
                            cpData.length,
                            chunkIndex,
                            chunkCount,
                            chunk
                    )
            );
        }


        return creasePatternId;
    }
}