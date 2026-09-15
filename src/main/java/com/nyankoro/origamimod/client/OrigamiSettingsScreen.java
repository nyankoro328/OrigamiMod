package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.origami.OrigamiFoldResult;
import com.nyankoro.origamimod.origami.OrigamiUseType;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public final class OrigamiSettingsScreen
        extends Screen {

    private final Screen parent;

    private final String cpFileName;

    private final OrigamiFoldResult front;

    private final OrigamiFoldResult back;

    /*
     * 折り紙の用途。
     */
    private OrigamiUseType useType =
            OrigamiUseType.WALL;

    /*
     * false = 表
     * true  = 裏
     */
    private boolean showBack =
            false;

    /*
     * UIプレビュー専用設定。
     *
     * 装備位置調整用の3D Transformとは
     * 完全に別物として扱う。
     */
    private double previewZoom =
            1.0;

    private int previewAngle =
            0;

    /*
     * Rasterizerが生成した
     * 横方向の描画Span。
     */
    private List<OrigamiPreviewRasterizer.Span>
            previewSpans =
            List.of();

    /*
     * プレビュー枠。
     */
    private int previewLeft;

    private int previewTop;

    private int previewWidth;

    private int previewHeight;

    /*
     * 右側操作パネル。
     */
    private int controlsLeft;

    private int controlsWidth;

    private Button useTypeButton;

    private Button sideButton;


    public OrigamiSettingsScreen(
            Screen parent,
            String cpFileName,
            OrigamiFoldResult front,
            OrigamiFoldResult back
    ) {

        super(
                Component.literal(
                        "Origami Settings"
                )
        );

        this.parent =
                parent;

        this.cpFileName =
                cpFileName;

        this.front =
                front;

        this.back =
                back;
    }


    @Override
    protected void init() {

        /*
         * 画面サイズから配置を決める。
         *
         * 左:
         * プレビュー
         *
         * 右:
         * 操作パネル
         */
        int margin =
                10;

        int gap =
                10;

        int top =
                32;

        int bottom =
                this.height - 40;

        controlsWidth =
                Math.min(
                        220,
                        Math.max(
                                140,
                                this.width / 3
                        )
                );

        previewLeft =
                margin;

        previewTop =
                top;

        previewWidth =
                this.width
                        - margin * 2
                        - gap
                        - controlsWidth;

        /*
         * 小さいウィンドウでも
         * 最低限プレビューを残す。
         */
        if (previewWidth < 120) {

            controlsWidth =
                    Math.max(
                            110,
                            this.width / 3
                    );

            previewWidth =
                    Math.max(
                            100,
                            this.width
                                    - margin * 2
                                    - gap
                                    - controlsWidth
                    );
        }

        previewHeight =
                Math.max(
                        100,
                        bottom - top
                );

        controlsLeft =
                previewLeft
                        + previewWidth
                        + gap;


        // =========================================================
        // 用途
        // =========================================================

        int y =
                previewTop;

        useTypeButton =
                this.addRenderableWidget(
                        Button.builder(
                                        getUseTypeText(),
                                        button -> {

                                            useType =
                                                    useType.next();

                                            button.setMessage(
                                                    getUseTypeText()
                                            );
                                        }
                                )
                                .pos(
                                        controlsLeft,
                                        y
                                )
                                .size(
                                        controlsWidth,
                                        20
                                )
                                .build()
                );


        // =========================================================
        // 表裏切り替え
        // =========================================================

        y +=
                28;

        sideButton =
                this.addRenderableWidget(
                        Button.builder(
                                        getSideText(),
                                        button -> {

                                            showBack =
                                                    !showBack;

                                            button.setMessage(
                                                    getSideText()
                                            );

                                            rebuildPreview();
                                        }
                                )
                                .pos(
                                        controlsLeft,
                                        y
                                )
                                .size(
                                        controlsWidth,
                                        20
                                )
                                .build()
                );


        // =========================================================
        // Zoom
        // =========================================================

        y +=
                48;

        int halfWidth =
                (controlsWidth - 4)
                        / 2;

        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Zoom -"
                                ),
                                button ->
                                        changeZoom(
                                                -0.1
                                        )
                        )
                        .pos(
                                controlsLeft,
                                y
                        )
                        .size(
                                halfWidth,
                                20
                        )
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Zoom +"
                                ),
                                button ->
                                        changeZoom(
                                                0.1
                                        )
                        )
                        .pos(
                                controlsLeft
                                        + halfWidth
                                        + 4,
                                y
                        )
                        .size(
                                halfWidth,
                                20
                        )
                        .build()
        );


        // =========================================================
        // 2D Angle
        // =========================================================

        y +=
                48;

        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Angle -15°"
                                ),
                                button ->
                                        changeAngle(
                                                -15
                                        )
                        )
                        .pos(
                                controlsLeft,
                                y
                        )
                        .size(
                                halfWidth,
                                20
                        )
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Angle +15°"
                                ),
                                button ->
                                        changeAngle(
                                                15
                                        )
                        )
                        .pos(
                                controlsLeft
                                        + halfWidth
                                        + 4,
                                y
                        )
                        .size(
                                halfWidth,
                                20
                        )
                        .build()
        );


        // =========================================================
        // 表示リセット
        // =========================================================

        y +=
                35;

        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "表示をリセット"
                                ),
                                button ->
                                        resetPreview()
                        )
                        .pos(
                                controlsLeft,
                                y
                        )
                        .size(
                                controlsWidth,
                                20
                        )
                        .build()
        );


        // =========================================================
        // 戻る
        // =========================================================

        int bottomButtonY =
                this.height - 30;

        int bottomButtonWidth =
                100;

        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "戻る"
                                ),
                                button ->
                                        this.minecraft.gui
                                                .setScreen(
                                                        parent
                                                )
                        )
                        .pos(
                                this.width / 2
                                        - bottomButtonWidth
                                        - 4,
                                bottomButtonY
                        )
                        .size(
                                bottomButtonWidth,
                                20
                        )
                        .build()
        );


        // =========================================================
        // アイテム化
        // =========================================================

        Button createItemButton =
                this.addRenderableWidget(
                        Button.builder(
                                        Component.literal(
                                                "アイテム化"
                                        ),
                                        button -> {

                                            /*
                                             * 後で実装。
                                             */
                                        }
                                )
                                .pos(
                                        this.width / 2
                                                + 4,
                                        bottomButtonY
                                )
                                .size(
                                        bottomButtonWidth,
                                        20
                                )
                                .build()
                );

        createItemButton.active =
                false;


        /*
         * 現在の画面サイズに合わせて
         * 最初のプレビューを作る。
         */
        rebuildPreview();
    }


    // =========================================================
    // Preview操作
    // =========================================================

    private void changeZoom(
            double amount
    ) {

        previewZoom =
                clamp(
                        previewZoom
                                + amount,
                        0.25,
                        3.0
                );

        rebuildPreview();
    }


    private void changeAngle(
            int amount
    ) {

        previewAngle =
                normalizeAngle(
                        previewAngle
                                + amount
                );

        rebuildPreview();
    }


    private void resetPreview() {

        showBack =
                false;

        previewZoom =
                1.0;

        previewAngle =
                0;

        if (sideButton != null) {

            sideButton.setMessage(
                    getSideText()
            );
        }

        rebuildPreview();
    }


    private void rebuildPreview() {

        OrigamiFoldResult result =
                showBack
                        ? back
                        : front;

        previewSpans =
                OrigamiPreviewRasterizer
                        .rasterize(
                                result,
                                previewWidth,
                                previewHeight,
                                previewZoom,
                                previewAngle
                        );
    }


    // =========================================================
    // 表示テキスト
    // =========================================================

    private Component getUseTypeText() {

        return Component.literal(
                "用途: "
                        + useType.displayName()
        );
    }


    private Component getSideText() {

        return Component.literal(
                showBack
                        ? "表示: 裏  ⇄"
                        : "表示: 表  ⇄"
        );
    }


    private String getZoomText() {

        return String.format(
                Locale.ROOT,
                "Zoom: %.0f%%",
                previewZoom
                        * 100.0
        );
    }


    private String getAngleText() {

        return String.format(
                Locale.ROOT,
                "Angle: %d°",
                previewAngle
        );
    }


    // =========================================================
    // Rendering
    // =========================================================

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {

        super.extractBackground(
                graphics,
                mouseX,
                mouseY,
                partialTicks
        );

        /*
         * タイトル。
         */
        graphics.text(
                font,
                Component.literal(
                        "折り紙設定"
                ),
                10,
                10,
                0xFFFFFF
        );


        // =========================================================
        // Preview panel
        // =========================================================

        /*
         * 黒い1pxの枠。
         */
        graphics.fill(
                previewLeft - 1,
                previewTop - 1,
                previewLeft
                        + previewWidth
                        + 1,
                previewTop
                        + previewHeight
                        + 1,
                0xFF202020
        );

        /*
         * DEFOXに近い白背景。
         */
        graphics.fill(
                previewLeft,
                previewTop,
                previewLeft
                        + previewWidth,
                previewTop
                        + previewHeight,
                0xFFF4F4F4
        );

        /*
         * Rasterizerで作った折り紙。
         */
        for (OrigamiPreviewRasterizer.Span span :
                previewSpans) {

            graphics.fill(
                    previewLeft
                            + span.xStart(),
                    previewTop
                            + span.y(),
                    previewLeft
                            + span.xEnd()
                            + 1,
                    previewTop
                            + span.y()
                            + 1,
                    span.color()
            );
        }


        // =========================================================
        // Right control panel text
        // =========================================================

        graphics.text(
                font,
                Component.literal(
                        getZoomText()
                ),
                controlsLeft,
                previewTop + 60,
                0xFFFFFF
        );

        graphics.text(
                font,
                Component.literal(
                        getAngleText()
                ),
                controlsLeft,
                previewTop + 108,
                0xFFFFFF
        );

        /*
         * CP名は長い場合があるため
         * 画面下部に簡易表示。
         */
        graphics.text(
                font,
                Component.literal(
                        "CP: "
                                + cpFileName
                ),
                controlsLeft,
                previewTop + 200,
                0xAAAAAA
        );
    }


    // =========================================================
    // Utilities
    // =========================================================

    private int normalizeAngle(
            int angle
    ) {

        int result =
                angle % 360;

        if (result < 0) {
            result +=
                    360;
        }

        return result;
    }


    private double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }
}