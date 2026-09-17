package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.network
        .CreasePatternListPayload;
import com.nyankoro.origamimod.network
        .RequestCreasePatternListPayload;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.neoforged.neoforge.client.network
        .ClientPacketDistributor;

import java.util.List;

import com.nyankoro.origamimod.network
        .RequestCreasePatternPayload;

import net.minecraft.client.input.MouseButtonEvent;


/*
 * 保存済みCP一覧画面。
 *
 * 現段階では一覧表示のみ。
 * CP本体のダウンロードは次段階で実装する。
 */
public final class CreasePatternLibraryScreen
        extends Screen {

    private static final int ROWS_PER_PAGE =
            8;


    private final Screen parent;


    private List<CreasePatternListPayload.Entry>
            entries =
            List.of();


    private boolean loading =
            true;

    private String status =
            "保存済み展開図を取得中...";

    private int page =
            0;


    private Button previousButton;

    private Button nextButton;

    private String loadingPatternId;

    public CreasePatternLibraryScreen(
            Screen parent
    ) {

        super(
                Component.literal(
                        "Crease Pattern Library"
                )
        );

        this.parent =
                parent;
    }


    @Override
    protected void init() {

        int centerX =
                this.width / 2;

        int bottomY =
                this.height - 32;


        int smallWidth =
                80;


        previousButton =
                this.addRenderableWidget(
                        Button.builder(
                                        Component.literal(
                                                "前へ"
                                        ),
                                        button -> {
                                            page--;

                                            updatePageButtons();
                                        }
                                )
                                .pos(
                                        centerX - 170,
                                        bottomY
                                )
                                .size(
                                        smallWidth,
                                        20
                                )
                                .build()
                );


        nextButton =
                this.addRenderableWidget(
                        Button.builder(
                                        Component.literal(
                                                "次へ"
                                        ),
                                        button -> {
                                            page++;

                                            updatePageButtons();
                                        }
                                )
                                .pos(
                                        centerX - 85,
                                        bottomY
                                )
                                .size(
                                        smallWidth,
                                        20
                                )
                                .build()
                );


        this.addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "再読込"
                                ),
                                button ->
                                        requestList()
                        )
                        .pos(
                                centerX,
                                bottomY
                        )
                        .size(
                                smallWidth,
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
                                        returnToParent()
                        )
                        .pos(
                                centerX + 85,
                                bottomY
                        )
                        .size(
                                smallWidth,
                                20
                        )
                        .build()
        );


        updatePageButtons();


        /*
         * 最初に画面を開いたときだけ
         * Serverへ一覧を要求する。
         */
        if (loading) {

            requestList();
        }
    }


    private void requestList() {

        loading =
                true;

        status =
                "保存済み展開図を取得中...";


        ClientPacketDistributor.sendToServer(
                RequestCreasePatternListPayload.INSTANCE
        );
    }


    public void acceptList(
            CreasePatternListPayload payload
    ) {

        loading =
                false;


        if (!payload.success()) {

            entries =
                    List.of();

            status =
                    payload.message()
                            .isBlank()
                            ? "一覧の取得に失敗しました"
                            : payload.message();

            page =
                    0;

            updatePageButtons();

            return;
        }


        entries =
                List.copyOf(
                        payload.entries()
                );


        status =
                "保存済み展開図: "
                        + entries.size()
                        + "件";


        int maxPage =
                getMaxPage();


        if (page > maxPage) {

            page =
                    maxPage;
        }


        updatePageButtons();
    }

    public void acceptDownloadedPattern(
            String creasePatternId,
            String fileName,
            byte[] cpData
    ) {

        if (loadingPatternId == null
                || !loadingPatternId.equals(
                creasePatternId
        )) {

            return;
        }


        loadingPatternId =
                null;


        if (!(parent
                instanceof OrigamiEditorScreen editor)) {

            status =
                    "Origami Editorへ戻れません";

            return;
        }


        if (!editor.loadCreasePattern(
                fileName,
                cpData
        )) {

            status =
                    "展開図の読み込みに失敗しました";

            return;
        }


        this.minecraft.gui
                .setScreen(
                        editor
                );
    }


    private int getMaxPage() {

        if (entries.isEmpty()) {

            return 0;
        }


        return (
                entries.size()
                        - 1
        ) / ROWS_PER_PAGE;
    }


    private void updatePageButtons() {

        if (previousButton != null) {

            previousButton.active =
                    !loading
                            && page > 0;
        }


        if (nextButton != null) {

            nextButton.active =
                    !loading
                            && page < getMaxPage();
        }
    }


    private void returnToParent() {

        this.minecraft.gui
                .setScreen(
                        parent
                );
    }

    private void selectEntry(
            CreasePatternListPayload.Entry entry
    ) {

        if (loadingPatternId != null) {

            return;
        }


        loadingPatternId =
                entry.creasePatternId();


        status =
                "読み込み中: "
                        + entry.fileName();


        ClientPacketDistributor.sendToServer(
                new RequestCreasePatternPayload(
                        entry.creasePatternId()
                )
        );
    }

    @Override
    public void onClose() {

        returnToParent();
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


        int centerX =
                this.width / 2;


        graphics.text(
                font,
                Component.literal(
                        "保存済み展開図"
                ),
                centerX - 50,
                20,
                0xFFFFFFFF
        );


        graphics.text(
                font,
                Component.literal(
                        status
                ),
                30,
                45,
                0xFFBBBBBB
        );


        if (loading) {

            return;
        }


        if (entries.isEmpty()) {

            graphics.text(
                    font,
                    Component.literal(
                            "保存済みの展開図はありません"
                    ),
                    30,
                    80,
                    0xFFAAAAAA
            );

            return;
        }


        int start =
                page
                        * ROWS_PER_PAGE;

        int end =
                Math.min(
                        start
                                + ROWS_PER_PAGE,
                        entries.size()
                );


        int y =
                70;


        for (int i = start;
             i < end;
             i++) {

            CreasePatternListPayload.Entry entry =
                    entries.get(
                            i
                    );


            graphics.fill(
                    25,
                    y - 4,
                    this.width - 25,
                    y + 17,
                    0x66000000
            );


            graphics.text(
                    font,
                    Component.literal(
                            entry.fileName()
                    ),
                    35,
                    y,
                    0xFFFFFFFF
            );


            graphics.text(
                    font,
                    Component.literal(
                            "ID: "
                                    + entry
                                    .creasePatternId()
                                    .substring(
                                            0,
                                            12
                                    )
                    ),
                    35,
                    y + 10,
                    0xFF888888
            );


            y +=
                    30;
        }


        graphics.text(
                font,
                Component.literal(
                        "ページ "
                                + (
                                page + 1
                        )
                                + " / "
                                + (
                                getMaxPage()
                                        + 1
                        )
                ),
                centerX - 30,
                this.height - 52,
                0xFFAAAAAA
        );
    }

    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick
    ) {

        if (super.mouseClicked(
                event,
                doubleClick
        )) {

            return true;
        }


        if (event.button() != 0
                || loading
                || loadingPatternId != null) {

            return false;
        }


        int start =
                page
                        * ROWS_PER_PAGE;

        int end =
                Math.min(
                        start
                                + ROWS_PER_PAGE,
                        entries.size()
                );


        int y =
                70;


        for (int i = start;
             i < end;
             i++) {

            int top =
                    y - 4;

            int bottom =
                    y + 17;


            if (event.x() >= 25
                    && event.x() <= this.width - 25
                    && event.y() >= top
                    && event.y() <= bottom) {

                selectEntry(
                        entries.get(
                                i
                        )
                );

                return true;
            }


            y +=
                    30;
        }


        return false;
    }
}