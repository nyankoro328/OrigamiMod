package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.network
        .OrigamiVisualAssetDownloadChunkPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event
        .RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;


/*
 * Client専用のnetwork handler。
 *
 * Dedicated Serverから
 * Clientクラスを完全に隔離する。
 */
@EventBusSubscriber(
        value = Dist.CLIENT,
        modid = OrigamiMod.MODID
)
public final class OrigamiClientNetwork {

    private OrigamiClientNetwork() {
    }


    @SubscribeEvent
    public static void register(
            RegisterClientPayloadHandlersEvent event
    ) {

        event.register(
                OrigamiVisualAssetDownloadChunkPayload.TYPE,
                HandlerThread.NETWORK,
                OrigamiClientNetwork::handleVisualAssetChunk
        );
    }


    private static void handleVisualAssetChunk(
            OrigamiVisualAssetDownloadChunkPayload payload,
            IPayloadContext context
    ) {

        OrigamiVisualAssetClientCache.Result result =
                OrigamiVisualAssetClientCache.acceptChunk(
                        payload
                );


        switch (result.status()) {

            case ACCEPTED,
                 ALREADY_CACHED -> {
            }


            case COMPLETE -> {

                OrigamiMod.LOGGER.info(
                        "Origami visual asset download complete: "
                                + "assetId={}, "
                                + "directory={}",
                        payload.visualAssetId(),
                        result.directory()
                );


                context.enqueueWork(
                        () -> {

                            Minecraft minecraft =
                                    Minecraft.getInstance();


                            if (minecraft.player != null) {

                                minecraft.player
                                        .sendSystemMessage(
                                                Component.literal(
                                                        "折り紙画像を受信しました: "
                                                                + payload
                                                                .visualAssetId()
                                                                .substring(
                                                                        0,
                                                                        12
                                                                )
                                                )
                                        );
                            }
                        }
                );
            }


            case REJECTED -> {

                OrigamiMod.LOGGER.warn(
                        "Rejected downloaded origami visual asset: "
                                + "assetId={}, "
                                + "reason={}",
                        payload.visualAssetId(),
                        result.message()
                );
            }
        }
    }
}