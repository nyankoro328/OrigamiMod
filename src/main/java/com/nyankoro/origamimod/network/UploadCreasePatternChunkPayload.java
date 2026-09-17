package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.server.CreasePatternUploadManager;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;


/*
 * Client -> Server
 *
 * CPファイルをチャンク分割して送る。
 */
public record UploadCreasePatternChunkPayload(
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

    public static final int MAX_ID_LENGTH =
            64;

    public static final int MAX_FILE_NAME_LENGTH =
            128;


    public UploadCreasePatternChunkPayload {

        chunkData =
                chunkData.clone();
    }


    @Override
    public byte[] chunkData() {

        return chunkData.clone();
    }


    public static final
    Type<UploadCreasePatternChunkPayload>
            TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "upload_crease_pattern_chunk"
                    )
            );


    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            UploadCreasePatternChunkPayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    UploadCreasePatternChunkPayload::encode,
                    UploadCreasePatternChunkPayload::decode
            );


    private static void encode(
            RegistryFriendlyByteBuf buffer,
            UploadCreasePatternChunkPayload payload
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


    private static UploadCreasePatternChunkPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {

        String creasePatternId =
                buffer.readUtf(
                        MAX_ID_LENGTH
                );

        String fileName =
                buffer.readUtf(
                        MAX_FILE_NAME_LENGTH
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
                    "Invalid CP chunk size"
            );
        }


        byte[] data =
                new byte[
                        chunkLength
                        ];


        buffer.readBytes(
                data
        );


        return new UploadCreasePatternChunkPayload(
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


    public static void handle(
            UploadCreasePatternChunkPayload payload,
            IPayloadContext context
    ) {

        if (!(context.player()
                instanceof ServerPlayer player)) {

            return;
        }


        CreasePatternUploadManager.UploadResult result =
                CreasePatternUploadManager.acceptChunk(
                        player.level()
                                .getServer(),
                        player.getUUID(),
                        payload
                );


        if (result.status()
                == CreasePatternUploadManager.Status.COMPLETE) {

            OrigamiMod.LOGGER.info(
                    "Crease pattern upload complete: "
                            + "player={}, id={}",
                    player.getUUID(),
                    payload.creasePatternId()
            );


            context.enqueueWork(
                    () ->
                            player.sendSystemMessage(
                                    Component.literal(
                                            "展開図を保存しました: "
                                                    + payload.fileName()
                                    )
                            )
            );
        }
    }
}