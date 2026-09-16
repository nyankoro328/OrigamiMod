package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.server.OrigamiVisualAssetUploadManager;

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
 * PNGを小さなチャンクへ分割して送信する。
 */
public record UploadOrigamiVisualAssetChunkPayload(
        String visualAssetId,
        Side side,
        int totalLength,
        int chunkIndex,
        int chunkCount,
        byte[] chunkData
) implements CustomPacketPayload {

    /*
     * 12 KiB。
     *
     * serverboundの32 KiB制限に対して
     * 十分な余裕を残す。
     */
    public static final int CHUNK_SIZE =
            12 * 1024;


    /*
     * 現段階では1面あたり最大1 MiB。
     */
    public static final int MAX_IMAGE_BYTES =
            1024 * 1024;


    public static final int MAX_ASSET_ID_LENGTH =
            64;


    public enum Side {

        FRONT,
        BACK
    }


    public UploadOrigamiVisualAssetChunkPayload {

        chunkData =
                chunkData.clone();
    }


    @Override
    public byte[] chunkData() {

        return chunkData.clone();
    }


    public static final
    CustomPacketPayload.Type<
            UploadOrigamiVisualAssetChunkPayload
            >
            TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "upload_origami_visual_asset_chunk"
                    )
            );


    public static final
    StreamCodec<
            RegistryFriendlyByteBuf,
            UploadOrigamiVisualAssetChunkPayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    UploadOrigamiVisualAssetChunkPayload::encode,
                    UploadOrigamiVisualAssetChunkPayload::decode
            );


    private static void encode(
            RegistryFriendlyByteBuf buffer,
            UploadOrigamiVisualAssetChunkPayload payload
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


    private static UploadOrigamiVisualAssetChunkPayload decode(
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
                    "Invalid visual asset chunk size: "
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


        return new UploadOrigamiVisualAssetChunkPayload(
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


    /*
     * NETWORK threadで呼ばれる。
     */
    public static void handle(
            UploadOrigamiVisualAssetChunkPayload payload,
            IPayloadContext context
    ) {

        if (!(context.player()
                instanceof ServerPlayer player)) {

            return;
        }


        OrigamiVisualAssetUploadManager.UploadResult result =
                OrigamiVisualAssetUploadManager.acceptChunk(
                        player.level()
                                .getServer(),
                        player.getUUID(),
                        payload
                );


        switch (result.status()) {

            case ACCEPTED,
                 ALREADY_CACHED -> {
            }


            case COMPLETE -> {

                OrigamiMod.LOGGER.info(
                        "Origami visual asset upload complete: "
                                + "player={}, "
                                + "assetId={}",
                        player.getUUID(),
                        payload.visualAssetId()
                );


                context.enqueueWork(
                        () ->
                                player.sendSystemMessage(
                                        Component.literal(
                                                "折り紙画像をサーバーへ登録しました: "
                                                        + payload
                                                        .visualAssetId()
                                                        .substring(
                                                                0,
                                                                12
                                                        )
                                        )
                                )
                );
            }


            case REJECTED -> {

                OrigamiMod.LOGGER.warn(
                        "Rejected visual asset upload: "
                                + "player={}, "
                                + "assetId={}, "
                                + "reason={}",
                        player.getUUID(),
                        payload.visualAssetId(),
                        result.message()
                );


                context.enqueueWork(
                        () ->
                                player.sendSystemMessage(
                                        Component.literal(
                                                "折り紙画像の送信に失敗しました"
                                        )
                                )
                );
            }
        }
    }
}