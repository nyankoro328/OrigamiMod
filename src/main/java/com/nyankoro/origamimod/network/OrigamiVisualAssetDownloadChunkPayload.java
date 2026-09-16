package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;


/*
 * Server -> Client
 *
 * 保存済みPNGを分割して配信する。
 */
public record OrigamiVisualAssetDownloadChunkPayload(
        String visualAssetId,
        Side side,
        int totalLength,
        int chunkIndex,
        int chunkCount,
        byte[] chunkData
) implements CustomPacketPayload {

    public static final int CHUNK_SIZE =
            12 * 1024;

    public static final int MAX_IMAGE_BYTES =
            1024 * 1024;

    public static final int MAX_ASSET_ID_LENGTH =
            64;


    public enum Side {

        FRONT,
        BACK
    }


    public OrigamiVisualAssetDownloadChunkPayload {

        chunkData =
                chunkData.clone();
    }


    @Override
    public byte[] chunkData() {

        return chunkData.clone();
    }


    public static final
    CustomPacketPayload.Type<
            OrigamiVisualAssetDownloadChunkPayload
            >
            TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "origami_visual_asset_download_chunk"
                    )
            );


    public static final
    StreamCodec<
            RegistryFriendlyByteBuf,
            OrigamiVisualAssetDownloadChunkPayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    OrigamiVisualAssetDownloadChunkPayload::encode,
                    OrigamiVisualAssetDownloadChunkPayload::decode
            );


    private static void encode(
            RegistryFriendlyByteBuf buffer,
            OrigamiVisualAssetDownloadChunkPayload payload
    ) {

        buffer.writeUtf(
                payload.visualAssetId()
        );

        buffer.writeEnum(
                payload.side()
        );

        buffer.writeVarInt(
                payload.totalLength()
        );

        buffer.writeVarInt(
                payload.chunkIndex()
        );

        buffer.writeVarInt(
                payload.chunkCount()
        );


        byte[] data =
                payload.chunkData();


        buffer.writeVarInt(
                data.length
        );

        buffer.writeBytes(
                data
        );
    }


    private static OrigamiVisualAssetDownloadChunkPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {

        String visualAssetId =
                buffer.readUtf(
                        MAX_ASSET_ID_LENGTH
                );

        Side side =
                buffer.readEnum(
                        Side.class
                );

        int totalLength =
                buffer.readVarInt();

        int chunkIndex =
                buffer.readVarInt();

        int chunkCount =
                buffer.readVarInt();

        int chunkLength =
                buffer.readVarInt();


        if (chunkLength <= 0
                || chunkLength > CHUNK_SIZE) {

            throw new IllegalArgumentException(
                    "Invalid download chunk size: "
                            + chunkLength
            );
        }


        byte[] data =
                new byte[
                        chunkLength
                        ];


        buffer.readBytes(
                data
        );


        return new OrigamiVisualAssetDownloadChunkPayload(
                visualAssetId,
                side,
                totalLength,
                chunkIndex,
                chunkCount,
                data
        );
    }


    @Override
    public CustomPacketPayload.Type<
            ? extends CustomPacketPayload
            > type() {

        return TYPE;
    }
}