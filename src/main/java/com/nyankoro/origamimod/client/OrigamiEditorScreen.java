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

    private Button foldButton;

    public OrigamiEditorScreen() {

        super(
                Component.literal(
                        "Origami Editor"
                )
        );
    }

    @Override
    protected void init() {

        int centerX =
                this.width / 2;

        int buttonY =
                this.height - 50;

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
                                        centerX - 105,
                                        buttonY
                                )
                                .size(
                                        100,
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
                                button ->
                                        this.minecraft.gui
                                                .setScreen(
                                                        null
                                                )
                        )
                        .pos(
                                centerX + 5,
                                buttonY
                        )
                        .size(
                                100,
                                20
                        )
                        .build()
        );
    }

    /*
     * Minecraftのウィンドウへ
     * ファイルをドロップしたときに呼ばれる。
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

            cpData =
                    Files.readAllBytes(
                            file
                    );

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

            cpData =
                    null;

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
             * 次の設定画面へ進む。
             */
            this.minecraft.gui
                    .setScreen(
                            new OrigamiSettingsScreen(
                                    this,
                                    cpFileName,
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

        int left =
                this.width / 2 - 150;

        graphics.text(
                this.font,
                Component.literal(
                        "Origami Editor"
                ),
                left,
                30,
                0xFFFFFF
        );

        graphics.text(
                this.font,
                Component.literal(
                        "CP: " + cpFileName
                ),
                left,
                55,
                0xFFFFFF
        );

        graphics.text(
                this.font,
                Component.literal(
                        status
                ),
                left,
                75,
                0xAAAAAA
        );

        graphics.text(
                this.font,
                Component.literal(
                        "ここへ .cp ファイルをドラッグ＆ドロップ"
                ),
                left,
                110,
                0xFFFF55
        );
    }
}