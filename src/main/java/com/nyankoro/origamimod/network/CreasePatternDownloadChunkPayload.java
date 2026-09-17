package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;


/*
 * Server -> Client
 *
 * 保存済みCP本体のチャンク。
 */
public record CreasePatternDownloadChunkPayload(
        String creasePatternId,
        String fileName,
        int totalLength,
        int chunkIndex,
        int chunkCount,
        byte[] chunkData
) implements CustomPacketPayload {

    public static final int CHUNK_SIZE =
            12 * 1024;

    public static final int MAX_CP_BYTES =
            1024 * 1024;


    public CreasePatternDownloadChunkPayload {

        chunkData =
                chunkData.clone();
    }


    @Override
    public byte[] chunkData() {

        return chunkData.clone();
    }


    public static final Type<
            CreasePatternDownloadChunkPayload
            >
            TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "crease_pattern_download_chunk"
                    )
            );


    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            CreasePatternDownloadChunkPayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    CreasePatternDownloadChunkPayload::encode,
                    CreasePatternDownloadChunkPayload::decode
            );


    private static void encode(
            RegistryFriendlyByteBuf buffer,
            CreasePatternDownloadChunkPayload payload
    ) {

        buffer.writeUtf(
                payload.creasePatternId()
        );

        buffer.writeUtf(
                payload.fileName()
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


    private static CreasePatternDownloadChunkPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {

        String creasePatternId =
                buffer.readUtf(
                        64
                );

        String fileName =
                buffer.readUtf(
                        128
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
                    "Invalid CP download chunk size"
            );
        }


        byte[] data =
                new byte[
                        chunkLength
                        ];


        buffer.readBytes(
                data
        );


        return new CreasePatternDownloadChunkPayload(
                creasePatternId,
                fileName,
                totalLength,
                chunkIndex,
                chunkCount,
                data
        );
    }


    @Override
    public Type<
            ? extends CustomPacketPayload
            > type() {

        return TYPE;
    }
}