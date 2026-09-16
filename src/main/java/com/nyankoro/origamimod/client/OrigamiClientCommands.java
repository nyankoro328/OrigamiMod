package com.nyankoro.origamimod.client;

import com.mojang.brigadier.arguments.StringArgumentType;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.network
        .RequestOrigamiVisualAssetPayload;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import java.io.IOException;


@EventBusSubscriber(
        modid = OrigamiMod.MODID,
        value = Dist.CLIENT
)
public final class OrigamiClientCommands {

    private OrigamiClientCommands() {
    }


    @SubscribeEvent
    public static void registerCommands(
            RegisterClientCommandsEvent event
    ) {

        /*
         * Origami Editor。
         */
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "origami_editor"
                                )
                                .executes(
                                        context -> {

                                            Minecraft minecraft =
                                                    Minecraft.getInstance();


                                            minecraft.execute(
                                                    () ->
                                                            minecraft.gui
                                                                    .setScreen(
                                                                            new OrigamiEditorScreen()
                                                                    )
                                            );


                                            return 1;
                                        }
                                )
                );


        /*
         * Server -> Client画像配信の
         * 開発用テストコマンド。
         *
         * 後でDisplayEntityが自動要求するようになれば
         * 削除可能。
         */
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "origami_asset_request"
                                )
                                .then(
                                        Commands.argument(
                                                        "assetId",
                                                        StringArgumentType.word()
                                                )
                                                .executes(
                                                        context -> {

                                                            String assetId =
                                                                    StringArgumentType
                                                                            .getString(
                                                                                    context,
                                                                                    "assetId"
                                                                            );


                                                            Minecraft minecraft =
                                                                    Minecraft.getInstance();


                                                            if (!OrigamiVisualAssetId
                                                                    .isValidFormat(
                                                                            assetId
                                                                    )) {

                                                                if (minecraft.player
                                                                        != null) {

                                                                    minecraft.player
                                                                            .sendSystemMessage(
                                                                                    Component.literal(
                                                                                            "visualAssetIdが不正です"
                                                                                    )
                                                                            );
                                                                }


                                                                return 0;
                                                            }


                                                            if (OrigamiVisualAssetClientCache
                                                                    .isCached(
                                                                            assetId
                                                                    )) {

                                                                try {

                                                                    boolean alreadyLoaded =
                                                                            OrigamiTextureCache.isLoaded(
                                                                                    assetId
                                                                            );


                                                                    OrigamiTextureCache.TexturePair textures =
                                                                            OrigamiTextureCache.getOrLoad(
                                                                                    assetId
                                                                            );


                                                                    if (textures == null) {

                                                                        if (minecraft.player != null) {

                                                                            minecraft.player
                                                                                    .sendSystemMessage(
                                                                                            Component.literal(
                                                                                                    "折り紙Textureを読み込めませんでした"
                                                                                            )
                                                                                    );
                                                                        }


                                                                        return 0;
                                                                    }


                                                                    if (minecraft.player != null) {

                                                                        minecraft.player
                                                                                .sendSystemMessage(
                                                                                        Component.literal(
                                                                                                alreadyLoaded
                                                                                                        ? "この折り紙Textureは既にGPUへ読み込み済みです"
                                                                                                        : "キャッシュから折り紙TextureをGPUへ読み込みました"
                                                                                        )
                                                                                );
                                                                    }


                                                                    OrigamiMod.LOGGER.info(
                                                                            "Origami texture test: "
                                                                                    + "assetId={}, "
                                                                                    + "front={}, "
                                                                                    + "back={}, "
                                                                                    + "loadedAssets={}, "
                                                                                    + "estimatedGpuBytes={}",
                                                                            assetId,
                                                                            textures.front(),
                                                                            textures.back(),
                                                                            OrigamiTextureCache
                                                                                    .loadedAssetCount(),
                                                                            OrigamiTextureCache
                                                                                    .estimatedGpuBytes()
                                                                    );


                                                                    return 1;


                                                                } catch (
                                                                        IOException
                                                                        | RuntimeException e
                                                                ) {

                                                                    OrigamiMod.LOGGER.error(
                                                                            "Failed to load cached origami texture",
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


                                                                    return 0;
                                                                }
                                                            }


                                                            ClientPacketDistributor
                                                                    .sendToServer(
                                                                            new RequestOrigamiVisualAssetPayload(
                                                                                    assetId
                                                                            )
                                                                    );


                                                            if (minecraft.player
                                                                    != null) {

                                                                minecraft.player
                                                                        .sendSystemMessage(
                                                                                Component.literal(
                                                                                        "折り紙画像をサーバーへ要求しました"
                                                                                )
                                                                        );
                                                            }


                                                            return 1;
                                                        }
                                                )
                                )
                );

        /*
         * PNG版World Preview。
         *
         * TextureがClientに無ければ
         * 自動でServerへ要求する。
         */
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "origami_image_preview"
                                )
                                .then(
                                        Commands.argument(
                                                        "assetId",
                                                        StringArgumentType.word()
                                                )
                                                .executes(
                                                        context -> {

                                                            String assetId =
                                                                    StringArgumentType
                                                                            .getString(
                                                                                    context,
                                                                                    "assetId"
                                                                            );


                                                            Minecraft minecraft =
                                                                    Minecraft.getInstance();


                                                            if (!OrigamiVisualAssetId
                                                                    .isValidFormat(
                                                                            assetId
                                                                    )) {

                                                                if (minecraft.player
                                                                        != null) {

                                                                    minecraft.player
                                                                            .sendSystemMessage(
                                                                                    Component.literal(
                                                                                            "visualAssetIdが不正です"
                                                                                    )
                                                                            );
                                                                }


                                                                return 0;
                                                            }


                                                            if (!OrigamiImageWorldRenderer
                                                                    .showPreview(
                                                                            assetId
                                                                    )) {

                                                                return 0;
                                                            }


                                                            /*
                                                             * Disk cache済みなら
                                                             * GPUへロード。
                                                             */
                                                            if (OrigamiVisualAssetClientCache
                                                                    .isCached(
                                                                            assetId
                                                                    )) {

                                                                try {

                                                                    OrigamiTextureCache
                                                                            .getOrLoad(
                                                                                    assetId
                                                                            );


                                                                    if (minecraft.player
                                                                            != null) {

                                                                        minecraft.player
                                                                                .sendSystemMessage(
                                                                                        Component.literal(
                                                                                                "PNG版折り紙Previewを表示しました"
                                                                                        )
                                                                                );
                                                                    }


                                                                    return 1;


                                                                } catch (IOException e) {

                                                                    OrigamiMod.LOGGER.error(
                                                                            "Failed to load "
                                                                                    + "origami preview texture",
                                                                            e
                                                                    );


                                                                    return 0;
                                                                }
                                                            }


                                                            /*
                                                             * Client cacheに無ければ
                                                             * Serverから取得する。
                                                             *
                                                             * Preview stateは既に設定済みなので、
                                                             * Texture受信後に自動で表示される。
                                                             */
                                                            ClientPacketDistributor
                                                                    .sendToServer(
                                                                            new RequestOrigamiVisualAssetPayload(
                                                                                    assetId
                                                                            )
                                                                    );


                                                            if (minecraft.player
                                                                    != null) {

                                                                minecraft.player
                                                                        .sendSystemMessage(
                                                                                Component.literal(
                                                                                        "折り紙画像を取得後、Previewを表示します"
                                                                                )
                                                                        );
                                                            }


                                                            return 1;
                                                        }
                                                )
                                )
                );


        /*
         * PNG版World Previewを消す。
         */
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "origami_image_preview_clear"
                                )
                                .executes(
                                        context -> {

                                            OrigamiImageWorldRenderer
                                                    .clearPreview();


                                            Minecraft minecraft =
                                                    Minecraft.getInstance();


                                            if (minecraft.player
                                                    != null) {

                                                minecraft.player
                                                        .sendSystemMessage(
                                                                Component.literal(
                                                                        "PNG版折り紙Previewを消しました"
                                                                )
                                                        );
                                            }


                                            return 1;
                                        }
                                )
                );
    }
}