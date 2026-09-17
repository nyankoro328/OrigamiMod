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
import java.io.IOException;
import com.nyankoro.origamimod.network
        .OpenOrigamiDisplayScaleEditorPayload;
import com.nyankoro.origamimod.network
        .CreasePatternListPayload;
import com.nyankoro.origamimod.network
        .CreasePatternDownloadChunkPayload;


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
                OpenOrigamiDisplayScaleEditorPayload.TYPE,
                HandlerThread.MAIN,
                OrigamiClientNetwork::handleOpenScaleEditor
        );

        event.register(
                OrigamiVisualAssetDownloadChunkPayload.TYPE,
                HandlerThread.NETWORK,
                OrigamiClientNetwork::handleVisualAssetChunk
        );

        event.register(
                CreasePatternListPayload.TYPE,
                HandlerThread.MAIN,
                OrigamiClientNetwork::handleCreasePatternList
        );

        event.register(
                CreasePatternDownloadChunkPayload.TYPE,
                HandlerThread.NETWORK,
                OrigamiClientNetwork::handleCreasePatternChunk
        );
    }

    private static void handleOpenScaleEditor(
            OpenOrigamiDisplayScaleEditorPayload payload,
            IPayloadContext context
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();


        minecraft.gui.setScreen(
                new OrigamiDisplayScaleScreen(
                        payload.entityId(),
                        payload.scale()
                )
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

                OrigamiVisualAssetAutoLoader
                        .markDownloadComplete(
                                payload.visualAssetId()
                        );

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


                            try {

                                OrigamiTextureCache.TexturePair textures =
                                        OrigamiTextureCache.getOrLoad(
                                                payload.visualAssetId()
                                        );


                                if (textures == null) {

                                    OrigamiMod.LOGGER.warn(
                                            "Downloaded origami asset "
                                                    + "could not be loaded into GPU: {}",
                                            payload.visualAssetId()
                                    );

                                    return;
                                }


                                OrigamiMod.LOGGER.info(
                                        "Origami GPU texture ready: "
                                                + "assetId={}, "
                                                + "front={}, "
                                                + "back={}",
                                        payload.visualAssetId(),
                                        textures.front(),
                                        textures.back()
                                );


                                if (minecraft.player != null) {

                                    minecraft.player
                                            .sendSystemMessage(
                                                    Component.literal(
                                                            "折り紙画像を受信・読み込みしました: "
                                                                    + payload
                                                                    .visualAssetId()
                                                                    .substring(
                                                                            0,
                                                                            12
                                                                    )
                                                    )
                                            );
                                }


                            } catch (
                                    IOException
                                    | RuntimeException e
                            ) {

                                OrigamiMod.LOGGER.error(
                                        "Failed to load downloaded origami "
                                                + "texture into GPU: "
                                                + "assetId={}",
                                        payload.visualAssetId(),
                                        e
                                );


                                if (minecraft.player != null) {

                                    minecraft.player
                                            .sendSystemMessage(
                                                    Component.literal(
                                                            "折り紙Textureの読み込みに失敗しました"
                                                    )
                                            );
                                }
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

    private static void handleCreasePatternList(
            CreasePatternListPayload payload,
            IPayloadContext context
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();


        if (minecraft.gui.screen()
                instanceof CreasePatternLibraryScreen screen) {

            screen.acceptList(
                    payload
            );

            return;
        }


        OrigamiMod.LOGGER.warn(
                "Received crease pattern list "
                        + "while library screen was not open"
        );
    }

    private static void handleCreasePatternChunk(
            CreasePatternDownloadChunkPayload payload,
            IPayloadContext context
    ) {

        CreasePatternDownloadManager.Result result =
                CreasePatternDownloadManager.acceptChunk(
                        payload
                );


        if (result.status()
                != CreasePatternDownloadManager.Status.COMPLETE) {

            return;
        }


        context.enqueueWork(
                () -> {

                    Minecraft minecraft =
                            Minecraft.getInstance();


                    if (minecraft.gui.screen()
                            instanceof CreasePatternLibraryScreen screen) {

                        screen.acceptDownloadedPattern(
                                result.creasePatternId(),
                                result.fileName(),
                                result.cpData()
                        );
                    }
                }
        );
    }
}