package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.origami.OrigamiFoldResult;
import com.nyankoro.origamimod.origami.OrieditaAdapter;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayInputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;


public final class OrigamiEditorScreen
        extends Screen {

    private byte[] cpData;

    private String cpFileName =
            "未選択";

    private String status =
            ".cp ファイルをこの画面へドラッグ＆ドロップしてください";


    /*
     * 折る前の展開図プレビュー。
     */
    private CreasePatternPreviewTexture cpPreview;


    private Button foldButton;


    /*
     * init()時に画面サイズから決定する。
     */
    private int previewX;

    private int previewY;

    private int previewSize;

    private int controlsX;

    private int controlsWidth;


    public OrigamiEditorScreen() {

        super(
                Component.literal(
                        "Origami Editor"
                )
        );
    }


    @Override
    protected void init() {

        /*
         * 画面右側を操作欄、
         * 左側をプレビュー欄にする。
         */
        int margin =
                18;

        controlsWidth =
                Math.max(
                        190,
                        Math.min(
                                280,
                                width / 3
                        )
                );

        int previewAreaWidth =
                width
                        - controlsWidth
                        - margin * 3;

        int previewAreaHeight =
                height
                        - margin * 2
                        - 20;

        previewSize =
                Math.max(
                        96,
                        Math.min(
                                previewAreaWidth,
                                previewAreaHeight
                        )
                );

        previewX =
                margin
                        + Math.max(
                        0,
                        (
                                previewAreaWidth
                                        - previewSize
                        ) / 2
                );

        previewY =
                margin
                        + Math.max(
                        0,
                        (
                                previewAreaHeight
                                        - previewSize
                        ) / 2
                );


        controlsX =
                width
                        - margin
                        - controlsWidth;


        int buttonY =
                height - 60;

        /*
         * 保存済みCPライブラリ。
         */
        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "保存済みCP"
                                ),
                                button ->
                                        this.minecraft.gui
                                                .setScreen(
                                                        new CreasePatternLibraryScreen(
                                                                this
                                                        )
                                                )
                        )
                        .pos(
                                controlsX,
                                buttonY - 25
                        )
                        .size(
                                controlsWidth,
                                20
                        )
                        .build()
        );


        foldButton =
                this.addRenderableWidget(
                        Button.builder(
                                        Component.literal(
                                                "折りたたむ"
                                        ),
                                        button ->
                                                foldCp()
                                )
                                .pos(
                                        controlsX,
                                        buttonY
                                )
                                .size(
                                        controlsWidth,
                                        20
                                )
                                .build()
                );


        /*
         * CP未読込では押せない。
         */
        foldButton.active =
                cpData != null;


        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "閉じる"
                                ),
                                button -> {

                                    closeCpPreview();

                                    this.minecraft.gui
                                            .setScreen(
                                                    null
                                            );
                                }
                        )
                        .pos(
                                controlsX,
                                buttonY + 25
                        )
                        .size(
                                controlsWidth,
                                20
                        )
                        .build()
        );
    }


    /*
     * Minecraftウィンドウへ
     * .cpをドロップしたときに呼ばれる。
     */
    @Override
    public void onFilesDrop(
            List<Path> files
    ) {

        if (files.size() != 1) {

            status =
                    "1つの .cp ファイルだけをドロップしてください";

            return;
        }


        Path file =
                files.getFirst();


        String fileName =
                file.getFileName()
                        .toString();


        if (!fileName
                .toLowerCase()
                .endsWith(
                        ".cp"
                )) {

            status =
                    ".cp ファイルではありません";

            return;
        }


        try {

            byte[] newCpData =
                    Files.readAllBytes(
                            file
                    );

            loadCreasePattern(
                    fileName,
                    newCpData
            );


            /*
             * 新しいプレビューを先に生成する。
             *
             * 成功してから古いものを消すことで、
             * 壊れたCPをドロップしても
             * 直前の正常なプレビューを失わない。
             */
            CreasePatternPreviewTexture newPreview =
                    new CreasePatternPreviewTexture(
                            newCpData
                    );


            closeCpPreview();


            cpPreview =
                    newPreview;

            cpData =
                    newCpData;

            cpFileName =
                    fileName;


            status =
                    "読み込み完了: "
                            + fileName;


            if (foldButton != null) {

                foldButton.active =
                        true;
            }


            OrigamiMod.LOGGER.info(
                    "CP loaded from {}",
                    file
            );

        } catch (Exception e) {

            status =
                    "CPファイルの読み込みに失敗しました";

            OrigamiMod.LOGGER.error(
                    "Failed to load CP file",
                    e
            );
        }
    }


    private void foldCp() {

        if (cpData == null) {
            return;
        }


        status =
                "折りたたみ計算中...";


        try {

            OrigamiFoldResult front;

            OrigamiFoldResult back;


            try (
                    ByteArrayInputStream frontInput =
                            new ByteArrayInputStream(
                                    cpData
                            );

                    ByteArrayInputStream backInput =
                            new ByteArrayInputStream(
                                    cpData
                            )
            ) {

                front =
                        OrieditaAdapter.fold(
                                frontInput
                        );


                back =
                        OrieditaAdapter.foldBack(
                                backInput
                        );
            }


            /*
             * 折りたたみ成功。
             *
             * CPプレビューを保持したまま、
             * 設定画面へ遷移する。
             *
             * 「戻る」でこのScreenへ戻った際、
             * 同じCPプレビューを再利用できる。
             */
            this.minecraft.gui
                    .setScreen(
                            new OrigamiSettingsScreen(
                                    this,
                                    cpFileName,
                                    cpData,
                                    front,
                                    back
                            )
                    );


        } catch (Exception e) {

            status =
                    "折りたたみに失敗しました";


            OrigamiMod.LOGGER.error(
                    "Failed to fold imported CP",
                    e
            );
        }
    }


    /*
     * DynamicTexture解放。
     */
    private void closeCpPreview() {

        if (cpPreview != null) {

            cpPreview.close();

            cpPreview =
                    null;
        }
    }


    @Override
    public void onClose() {

        closeCpPreview();

        super.onClose();
    }


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


        // =========================================================
        // 左側：展開図プレビュー
        // =========================================================

        /*
         * 枠線。
         */
        graphics.fill(
                previewX - 3,
                previewY - 3,
                previewX + previewSize + 3,
                previewY + previewSize + 3,
                0xFF202020
        );


        /*
         * 背景。
         */
        graphics.fill(
                previewX,
                previewY,
                previewX + previewSize,
                previewY + previewSize,
                0xFFF5F5F5
        );


        if (cpPreview != null) {

            graphics.blit(
                    cpPreview.textureId(),

                    previewX,
                    previewY,

                    previewX + previewSize,
                    previewY + previewSize,

                    0.0F,
                    1.0F,
                    0.0F,
                    1.0F
            );

        } else {

            String message =
                    ".cp をここへドラッグ＆ドロップ";


            int textWidth =
                    font.width(
                            message
                    );


            graphics.text(
                    font,
                    Component.literal(
                            message
                    ),

                    previewX
                            + (
                            previewSize
                                    - textWidth
                    ) / 2,

                    previewY
                            + previewSize / 2,

                    0xFF777777
            );
        }


        // =========================================================
        // 右側：情報・操作
        // =========================================================

        graphics.text(
                font,
                Component.literal(
                        "Origami Editor"
                ),
                controlsX,
                25,
                0xFFFFFFFF
        );


        graphics.text(
                font,
                Component.literal(
                        "CP:"
                ),
                controlsX,
                55,
                0xFFAAAAAA
        );


        graphics.text(
                font,
                Component.literal(
                        cpFileName
                ),
                controlsX,
                70,
                0xFFFFFFFF
        );


        graphics.text(
                font,
                Component.literal(
                        status
                ),
                controlsX,
                100,
                0xFFAAAAAA
        );


        if (cpPreview != null) {

            graphics.text(
                    font,
                    Component.literal(
                            "線数: "
                                    + cpPreview.lineCount()
                    ),
                    controlsX,
                    125,
                    0xFFFFFFFF
            );


            graphics.text(
                    font,
                    Component.literal(
                            "赤: 山折り"
                    ),
                    controlsX,
                    155,
                    0xFFFF7777
            );


            graphics.text(
                    font,
                    Component.literal(
                            "青: 谷折り"
                    ),
                    controlsX,
                    170,
                    0xFF7799FF
            );


            graphics.text(
                    font,
                    Component.literal(
                            "黒: 外周"
                    ),
                    controlsX,
                    185,
                    0xFFFFFFFF
            );


            graphics.text(
                    font,
                    Component.literal(
                            "灰: FLAT"
                    ),
                    controlsX,
                    200,
                    0xFFBBBBBB
            );
        }
    }

    public boolean loadCreasePattern(
            String fileName,
            byte[] newCpData
    ) {

        if (fileName == null
                || fileName.isBlank()
                || !fileName
                .toLowerCase()
                .endsWith(
                        ".cp"
                )
                || newCpData == null
                || newCpData.length == 0) {

            status =
                    "CPデータが不正です";

            return false;
        }


        try {

            CreasePatternPreviewTexture newPreview =
                    new CreasePatternPreviewTexture(
                            newCpData
                    );


            closeCpPreview();


            cpPreview =
                    newPreview;

            cpData =
                    newCpData.clone();

            cpFileName =
                    fileName;


            status =
                    "読み込み完了: "
                            + fileName;


            if (foldButton != null) {

                foldButton.active =
                        true;
            }


            return true;


        } catch (Exception e) {

            status =
                    "CPファイルの読み込みに失敗しました";


            OrigamiMod.LOGGER.error(
                    "Failed to load crease pattern",
                    e
            );


            return false;
        }
    }
}