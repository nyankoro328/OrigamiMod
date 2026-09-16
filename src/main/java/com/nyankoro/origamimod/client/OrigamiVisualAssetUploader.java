package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.network.UploadOrigamiVisualAssetChunkPayload;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Arrays;


/*
 * 表裏PNGをチャンク分割して
 * Serverへ送信する。
 */
public final class OrigamiVisualAssetUploader {

    private OrigamiVisualAssetUploader() {
    }


    public static void upload(
            OrigamiPngExporter.ExportResult exportResult
    ) {

        String visualAssetId =
                exportResult.visualAssetId();


        if (!OrigamiVisualAssetId.isValidFormat(
                visualAssetId
        )) {

            throw new IllegalArgumentException(
                    "Invalid visualAssetId"
            );
        }


        uploadSide(
                visualAssetId,
                UploadOrigamiVisualAssetChunkPayload
                        .Side.FRONT,
                exportResult.frontPng()
        );


        uploadSide(
                visualAssetId,
                UploadOrigamiVisualAssetChunkPayload
                        .Side.BACK,
                exportResult.backPng()
        );
    }


    private static void uploadSide(
            String visualAssetId,
            UploadOrigamiVisualAssetChunkPayload.Side side,
            byte[] png
    ) {

        if (png.length <= 0
                || png.length
                > UploadOrigamiVisualAssetChunkPayload
                .MAX_IMAGE_BYTES) {

            throw new IllegalArgumentException(
                    "PNG size is invalid: "
                            + png.length
            );
        }


        int chunkCount =
                (
                        png.length
                                + UploadOrigamiVisualAssetChunkPayload
                                .CHUNK_SIZE
                                - 1
                )
                        / UploadOrigamiVisualAssetChunkPayload
                        .CHUNK_SIZE;


        OrigamiMod.LOGGER.info(
                "Sending origami visual asset: "
                        + "assetId={}, "
                        + "side={}, "
                        + "bytes={}, "
                        + "chunks={}",
                visualAssetId,
                side,
                png.length,
                chunkCount
        );


        for (int chunkIndex = 0;
             chunkIndex < chunkCount;
             chunkIndex++) {

            int start =
                    chunkIndex
                            * UploadOrigamiVisualAssetChunkPayload
                            .CHUNK_SIZE;


            int end =
                    Math.min(
                            start
                                    + UploadOrigamiVisualAssetChunkPayload
                                    .CHUNK_SIZE,
                            png.length
                    );


            byte[] chunk =
                    Arrays.copyOfRange(
                            png,
                            start,
                            end
                    );


            ClientPacketDistributor.sendToServer(
                    new UploadOrigamiVisualAssetChunkPayload(
                            visualAssetId,
                            side,
                            png.length,
                            chunkIndex,
                            chunkCount,
                            chunk
                    )
            );
        }
    }
}