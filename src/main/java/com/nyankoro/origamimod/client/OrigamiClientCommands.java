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

                                                                if (minecraft.player
                                                                        != null) {

                                                                    minecraft.player
                                                                            .sendSystemMessage(
                                                                                    Component.literal(
                                                                                            "この折り紙画像は既にキャッシュされています"
                                                                                    )
                                                                            );
                                                                }


                                                                return 1;
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
    }
}