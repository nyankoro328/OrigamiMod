package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.origami.OrigamiFoldResult;
import com.nyankoro.origamimod.origami.OrigamiUseType;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OrigamiSettingsScreen
        extends Screen {

    private final Screen parent;

    private final String cpFileName;

    private final OrigamiFoldResult front;

    private final OrigamiFoldResult back;

    private OrigamiUseType useType =
            OrigamiUseType.WALL;

    private Button useTypeButton;

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

        int centerX =
                width / 2;

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
                                        centerX - 100,
                                        110
                                )
                                .size(
                                        200,
                                        20
                                )
                                .build()
                );

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
                                centerX - 100,
                                this.height - 40
                        )
                        .size(
                                95,
                                20
                        )
                        .build()
        );

        Button createItemButton =
                this.addRenderableWidget(
                        Button.builder(
                                        Component.literal(
                                                "アイテム化"
                                        ),
                                        button -> {
                                            /*
                                             * 次の段階で実装。
                                             */
                                        }
                                )
                                .pos(
                                        centerX + 5,
                                        this.height - 40
                                )
                                .size(
                                        95,
                                        20
                                )
                                .build()
                );

        /*
         * まだ実装しない。
         */
        createItemButton.active =
                false;
    }

    private Component getUseTypeText() {

        return Component.literal(
                "用途: "
                        + useType.displayName()
        );
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
                width / 2 - 150;

        graphics.text(
                font,
                Component.literal(
                        "折り紙設定"
                ),
                left,
                25,
                0xFFFFFF
        );

        graphics.text(
                font,
                Component.literal(
                        "CP: " + cpFileName
                ),
                left,
                50,
                0xFFFFFF
        );

        graphics.text(
                font,
                Component.literal(
                        "FRONT Faces: "
                                + front.faces().size()
                ),
                left,
                70,
                0xAAAAAA
        );

        graphics.text(
                font,
                Component.literal(
                        "BACK Faces: "
                                + back.faces().size()
                ),
                left,
                85,
                0xAAAAAA
        );
    }
}