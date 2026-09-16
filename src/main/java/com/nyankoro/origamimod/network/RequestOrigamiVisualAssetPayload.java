package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;
import com.nyankoro.origamimod.server.OrigamiVisualAssetStore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;
import java.util.Arrays;


/*
 * Client -> Server
 *
 * 指定visualAssetIdの画像を要求する。
 */
public record RequestOrigamiVisualAssetPayload(
        String visualAssetId
) implements CustomPacketPayload {

    public static final int MAX_ASSET_ID_LENGTH =
            64;


    public static final
    CustomPacketPayload.Type<
            RequestOrigamiVisualAssetPayload
            >
            TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "request_origami_visual_asset"
                    )
            );


    public static final
    StreamCodec<
            RegistryFriendlyByteBuf,
            RequestOrigamiVisualAssetPayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    RequestOrigamiVisualAssetPayload::encode,
                    RequestOrigamiVisualAssetPayload::decode
            );


    private static void encode(
            RegistryFriendlyByteBuf buffer,
            RequestOrigamiVisualAssetPayload payload
    ) {

        buffer.writeUtf(
                payload.visualAssetId()
        );
    }


    private static RequestOrigamiVisualAssetPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {

        return new RequestOrigamiVisualAssetPayload(
                buffer.readUtf(
                        MAX_ASSET_ID_LENGTH
                )
        );
    }


    @Override
    public CustomPacketPayload.Type<
            ? extends CustomPacketPayload
            > type() {

        return TYPE;
    }


    public static void handle(
            RequestOrigamiVisualAssetPayload payload,
            IPayloadContext context
    ) {

        if (!(context.player()
                instanceof ServerPlayer player)) {

            return;
        }


        if (!OrigamiVisualAssetId.isValidFormat(
                payload.visualAssetId()
        )) {

            OrigamiMod.LOGGER.warn(
                    "Rejected invalid visual asset request from {}",
                    player.getUUID()
            );

            return;
        }


        try {

            OrigamiVisualAssetStore.AssetData asset =
                    OrigamiVisualAssetStore.read(
                            player.level()
                                    .getServer(),
                            payload.visualAssetId()
                    );


            if (asset == null) {

                OrigamiMod.LOGGER.warn(
                        "Requested visual asset does not exist: "
                                + "player={}, assetId={}",
                        player.getUUID(),
                        payload.visualAssetId()
                );

                return;
            }


            sendSide(
                    player,
                    payload.visualAssetId(),
                    OrigamiVisualAssetDownloadChunkPayload
                            .Side.FRONT,
                    asset.frontPng()
            );


            sendSide(
                    player,
                    payload.visualAssetId(),
                    OrigamiVisualAssetDownloadChunkPayload
                            .Side.BACK,
                    asset.backPng()
            );


            OrigamiMod.LOGGER.info(
                    "Sent origami visual asset: "
                            + "player={}, "
                            + "assetId={}",
                    player.getUUID(),
                    payload.visualAssetId()
            );


        } catch (IOException e) {

            OrigamiMod.LOGGER.error(
                    "Failed to read origami visual asset: "
                            + "assetId={}",
                    payload.visualAssetId(),
                    e
            );
        }
    }


    private static void sendSide(
            ServerPlayer player,
            String visualAssetId,
            OrigamiVisualAssetDownloadChunkPayload.Side side,
            byte[] png
    ) {

        int chunkCount =
                (
                        png.length
                                + OrigamiVisualAssetDownloadChunkPayload
                                .CHUNK_SIZE
                                - 1
                )
                        / OrigamiVisualAssetDownloadChunkPayload
                        .CHUNK_SIZE;


        for (int chunkIndex = 0;
             chunkIndex < chunkCount;
             chunkIndex++) {

            int start =
                    chunkIndex
                            * OrigamiVisualAssetDownloadChunkPayload
                            .CHUNK_SIZE;


            int end =
                    Math.min(
                            start
                                    + OrigamiVisualAssetDownloadChunkPayload
                                    .CHUNK_SIZE,
                            png.length
                    );


            byte[] chunk =
                    Arrays.copyOfRange(
                            png,
                            start,
                            end
                    );


            PacketDistributor.sendToPlayer(
                    player,
                    new OrigamiVisualAssetDownloadChunkPayload(
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