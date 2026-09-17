package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.network
        .UpdateOrigamiDisplayScalePayload;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.neoforged.neoforge.client.network
        .ClientPacketDistributor;

import java.util.Locale;


/*
 * 設置済み折り紙の
 * 大きさだけを変更する画面。
 */
public final class OrigamiDisplayScaleScreen
        extends Screen {

    private final int entityId;

    private float scale;

    private Button scaleDisplayButton;


    public OrigamiDisplayScaleScreen(
            int entityId,
            float scale
    ) {

        super(
                Component.literal(
                        "Origami Size"
                )
        );

        this.entityId =
                entityId;

        this.scale =
                clamp(
                        scale,
                        0.1F,
                        10.0F
                );
    }


    @Override
    protected void init() {

        int centerX =
                this.width / 2;

        int centerY =
                this.height / 2;


        int buttonWidth =
                80;

        int gap =
                4;


        /*
         * -0.1
         */
        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "- 0.1"
                                ),
                                button ->
                                        changeScale(
                                                -0.1F
                                        )
                        )
                        .pos(
                                centerX
                                        - buttonWidth
                                        - 44,
                                centerY - 20
                        )
                        .size(
                                buttonWidth,
                                20
                        )
                        .build()
        );


        /*
         * 現在値。
         *
         * 表示専用なのでactive=false。
         */
        scaleDisplayButton =
                this.addRenderableWidget(
                        Button.builder(
                                        getScaleText(),
                                        button -> {
                                        }
                                )
                                .pos(
                                        centerX - 40,
                                        centerY - 20
                                )
                                .size(
                                        80,
                                        20
                                )
                                .build()
                );

        scaleDisplayButton.active =
                false;


        /*
         * +0.1
         */
        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "+ 0.1"
                                ),
                                button ->
                                        changeScale(
                                                0.1F
                                        )
                        )
                        .pos(
                                centerX
                                        + 44,
                                centerY - 20
                        )
                        .size(
                                buttonWidth,
                                20
                        )
                        .build()
        );


        /*
         * 1.0倍へ戻す。
         */
        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "1.0にリセット"
                                ),
                                button ->
                                        setScale(
                                                1.0F
                                        )
                        )
                        .pos(
                                centerX - 84,
                                centerY + 12
                        )
                        .size(
                                168,
                                20
                        )
                        .build()
        );


        /*
         * 編集終了。
         */
        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "完了"
                                ),
                                button ->
                                        this.minecraft.gui
                                                .setScreen(
                                                        null
                                                )
                        )
                        .pos(
                                centerX - 84,
                                centerY + 44
                        )
                        .size(
                                168,
                                20
                        )
                        .build()
        );
    }


    private void changeScale(
            float amount
    ) {

        setScale(
                scale + amount
        );
    }


    private void setScale(
            float newScale
    ) {

        scale =
                clamp(
                        newScale,
                        0.1F,
                        10.0F
                );


        /*
         * 0.1刻みの計算誤差を抑える。
         */
        scale =
                Math.round(
                        scale * 10.0F
                ) / 10.0F;


        if (scaleDisplayButton != null) {

            scaleDisplayButton.setMessage(
                    getScaleText()
            );
        }


        /*
         * 押すたびにServerへ送る。
         *
         * Server側で値を検証し、
         * SynchedEntityData経由で全Clientへ反映される。
         */
        ClientPacketDistributor.sendToServer(
                new UpdateOrigamiDisplayScalePayload(
                        entityId,
                        scale
                )
        );
    }


    private Component getScaleText() {

        return Component.literal(
                String.format(
                        Locale.ROOT,
                        "%.1f 倍",
                        scale
                )
        );
    }


    private static float clamp(
            float value,
            float min,
            float max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }


    @Override
    public boolean isPauseScreen() {

        return false;
    }
}