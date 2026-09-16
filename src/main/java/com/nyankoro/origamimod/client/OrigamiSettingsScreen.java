package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.origami.OrigamiFoldResult;
import com.nyankoro.origamimod.origami.OrigamiUseType;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

import com.nyankoro.origamimod.origami.OrigamiAppearance;

import com.nyankoro.origamimod.network.CreateOrigamiItemPayload;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.nyankoro.origamimod.OrigamiMod;

import java.io.IOException;

public final class OrigamiSettingsScreen
        extends Screen {

    /*
     * 最初は簡単なプリセット方式。
     *
     * 将来的にはRGB入力や
     * カラーピッカーへ置き換えられる。
     */
    private record ColorPreset(
            String name,
            int color
    ) {
    }

    private static final ColorPreset[] COLOR_PRESETS = {

            new ColorPreset(
                    "白",
                    0xFFF5F5F5
            ),

            new ColorPreset(
                    "黒",
                    0xFF202020
            ),

            new ColorPreset(
                    "赤",
                    0xFFE05A5A
            ),

            new ColorPreset(
                    "青",
                    0xFF4EA5D9
            ),

            new ColorPreset(
                    "緑",
                    0xFF63B66C
            ),

            new ColorPreset(
                    "黄",
                    0xFFFFC857
            ),

            new ColorPreset(
                    "橙",
                    0xFFFF9F43
            ),

            new ColorPreset(
                    "紫",
                    0xFF9B72CF
            ),

            new ColorPreset(
                    "桃",
                    0xFFFF8FB1
            ),

            new ColorPreset(
                    "水色",
                    0xFF62D0E8
            )
    };

    private final Screen parent;

    private final String cpFileName;

    private final OrigamiFoldResult front;

    private final OrigamiFoldResult back;

    private final byte[] cpData;

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
     * 現在編集中の見た目設定。
     */
    private OrigamiAppearance appearance =
            OrigamiAppearance.DEFAULT;

    /*
     * COLOR_PRESETS内の現在位置。
     *
     * DEFAULTと同じになるよう、
     * 表 = 黄
     * 裏 = 青
     * 輪郭 = 黒
     */
    private int frontColorIndex =
            5;

    private int backColorIndex =
            3;

    private int edgeColorIndex =
            1;

    private int nextColorIndex(
            int current
    ) {

        return (current + 1)
                % COLOR_PRESETS.length;
    }

    private Button frontColorButton;

    private Button backColorButton;

    private Button edgeColorButton;

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
            byte[] cpData,
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

        this.cpData =
                cpData.clone();
    }

    private Component getFrontColorText() {

        return Component.literal(
                "表色: "
                        + COLOR_PRESETS[
                        frontColorIndex
                        ].name()
        );
    }


    private Component getBackColorText() {

        return Component.literal(
                "裏色: "
                        + COLOR_PRESETS[
                        backColorIndex
                        ].name()
        );
    }


    private Component getEdgeColorText() {

        return Component.literal(
                "輪郭色: "
                        + COLOR_PRESETS[
                        edgeColorIndex
                        ].name()
        );
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

        y += 35;

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
// 色設定
// =========================================================

        y += 35;

        /*
         * 3個のボタンを右側パネル内に横並びする。
         *
         * [ 表色 ][ 裏色 ][ 輪郭 ]
         */
        int colorButtonGap =
                3;

        int colorButtonWidth =
                (controlsWidth
                        - colorButtonGap * 2)
                        / 3;


        /*
         * 表色
         */
        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "表色"
                                ),
                                button ->
                                        changeFrontColor()
                        )
                        .pos(
                                controlsLeft,
                                y
                        )
                        .size(
                                colorButtonWidth,
                                20
                        )
                        .build()
        );


        /*
         * 裏色
         */
        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "裏色"
                                ),
                                button ->
                                        changeBackColor()
                        )
                        .pos(
                                controlsLeft
                                        + colorButtonWidth
                                        + colorButtonGap,
                                y
                        )
                        .size(
                                colorButtonWidth,
                                20
                        )
                        .build()
        );


        /*
         * 輪郭色
         */
        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "輪郭"
                                ),
                                button ->
                                        changeEdgeColor()
                        )
                        .pos(
                                controlsLeft
                                        + (colorButtonWidth
                                        + colorButtonGap)
                                        * 2,
                                y
                        )
                        .size(
                                colorButtonWidth,
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
                                        button ->
                                                createOrigamiItem()
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

    private void changeFrontColor() {

        frontColorIndex =
                nextColorIndex(
                        frontColorIndex
                );

        updateAppearance();
    }


    private void changeBackColor() {

        backColorIndex =
                nextColorIndex(
                        backColorIndex
                );

        updateAppearance();
    }


    private void changeEdgeColor() {

        edgeColorIndex =
                nextColorIndex(
                        edgeColorIndex
                );

        updateAppearance();
    }

    private void createOrigamiItem() {

        if (cpFileName == null
                || cpFileName.isBlank()) {

            showCreateItemError(
                    "CPファイル名が不正です"
            );

            return;
        }


        if (cpFileName.length()
                > CreateOrigamiItemPayload.MAX_FILE_NAME_LENGTH) {

            showCreateItemError(
                    "CPファイル名が長すぎるため"
                            + "アイテム化できません"
            );

            return;
        }


        if (!cpFileName
                .toLowerCase(
                        Locale.ROOT
                )
                .endsWith(
                        ".cp"
                )) {

            showCreateItemError(
                    "CPファイルではないため"
                            + "アイテム化できません"
            );

            return;
        }


        if (cpData.length == 0) {

            showCreateItemError(
                    "CPデータが空のため"
                            + "アイテム化できません"
            );

            return;
        }


        if (cpData.length
                > CreateOrigamiItemPayload.MAX_CP_BYTES) {

            showCreateItemError(
                    "CPデータが大きすぎるため"
                            + "アイテム化できません"
            );

            return;
        }

        /*
         * 現在の折り紙を
         * 表・裏PNGとして生成する。
         *
         * 現段階ではローカルへの
         * デバッグ出力だけ。
         */
        try {

            OrigamiPngExporter.ExportResult exportResult =
                    OrigamiPngExporter.export(
                            front,
                            back,
                            appearance
                    );


            OrigamiMod.LOGGER.info(
                    "Origami PNG exported: "
                            + "front={}, "
                            + "back={}",
                    exportResult.frontPath(),
                    exportResult.backPath()
            );

        } catch (IOException e) {

            OrigamiMod.LOGGER.error(
                    "Failed to export origami PNG",
                    e
            );


            showCreateItemError(
                    "折り紙画像の生成に失敗しました"
            );

            return;
        }


        ClientPacketDistributor.sendToServer(
                new CreateOrigamiItemPayload(
                        cpFileName,
                        cpData,
                        useType,
                        appearance.frontColor(),
                        appearance.backColor(),
                        appearance.edgeColor(),
                        previewAngle
                )
        );
    }


    private void showCreateItemError(
            String message
    ) {

        if (this.minecraft != null
                && this.minecraft.player != null) {

            this.minecraft.player
                    .sendSystemMessage(
                            Component.literal(
                                    message
                            )
                    );
        }
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
                                previewAngle,
                                appearance
                        );
    }

    private void updateAppearance() {

        appearance =
                new OrigamiAppearance(
                        COLOR_PRESETS[
                                frontColorIndex
                                ].color(),

                        COLOR_PRESETS[
                                backColorIndex
                                ].color(),

                        COLOR_PRESETS[
                                edgeColorIndex
                                ].color()
                );

        rebuildPreview();
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