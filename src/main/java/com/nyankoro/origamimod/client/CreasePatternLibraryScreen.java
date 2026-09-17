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

import net.minecraft.client.gui.components.EditBox;

import java.util.Locale;


/*
 * 保存済みCP一覧画面。
 *
 * 現段階では一覧表示のみ。
 * CP本体のダウンロードは次段階で実装する。
 */
public final class CreasePatternLibraryScreen
        extends Screen {
    /*
     * 一覧レイアウト。
     */
    private static final int LIST_TOP =
            100;

    private static final int ROW_HEIGHT =
            30;

    private static final int BOTTOM_RESERVED =
            82;
    private final Screen parent;


    private List<CreasePatternListPayload.Entry>
            entries =
            List.of();

    /*
     * 検索後に実際に画面へ表示する一覧。
     */
    private List<CreasePatternListPayload.Entry>
            filteredEntries =
            List.of();


    private EditBox searchBox;


    private String searchText =
            "";


    private boolean loading =
            true;

    private String status =
            "保存済み展開図を取得中...";

    private int page =
            0;

    private int getRowsPerPage() {

        int listBottom =
                this.height
                        - BOTTOM_RESERVED;


        int availableHeight =
                listBottom
                        - LIST_TOP;


        return Math.max(
                1,
                availableHeight
                        / ROW_HEIGHT
        );
    }


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

        /*
         * 保存済みCP検索欄。
         *
         * Serverへ再問い合わせせず、
         * Clientが既に持っている一覧を絞り込む。
         */
        searchBox =
                new EditBox(
                        this.font,
                        30,
                        44,
                        this.width - 60,
                        20,
                        Component.literal(
                                "展開図を検索"
                        )
                );


        searchBox.setMaxLength(
                128
        );


        searchBox.setHint(
                Component.literal(
                        "ファイル名で検索..."
                )
        );


        /*
         * 画面サイズ変更などでinit()が再実行されても
         * 入力中の検索文字を維持する。
         */
        searchBox.setValue(
                searchText
        );


        searchBox.setResponder(
                this::applySearch
        );


        this.addRenderableWidget(
                searchBox
        );


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

            filteredEntries =
                    List.of();

            page =
                    0;

            updatePageButtons();

            return;
        }


        entries =
                List.copyOf(
                        payload.entries()
                );


        entries =
                List.copyOf(
                        payload.entries()
                );


        applySearch(
                searchText
        );


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

        if (filteredEntries.isEmpty()) {

            return 0;
        }


        return (
                filteredEntries.size()
                        - 1
        ) / getRowsPerPage();
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

    private void applySearch(
            String text
    ) {

        searchText =
                text == null
                        ? ""
                        : text;


        String query =
                searchText
                        .strip()
                        .toLowerCase(
                                Locale.ROOT
                        );


        /*
         * 空欄なら全件表示。
         */
        if (query.isEmpty()) {

            filteredEntries =
                    entries;

        } else {

            filteredEntries =
                    entries
                            .stream()
                            .filter(
                                    entry ->
                                            entry.fileName()
                                                    .toLowerCase(
                                                            Locale.ROOT
                                                    )
                                                    .contains(
                                                            query
                                                    )
                            )
                            .toList();
        }


        /*
         * 検索条件が変わったら
         * 必ず1ページ目へ戻す。
         */
        page =
                0;


        updateStatus();
        updatePageButtons();
    }

    private void updateStatus() {

        if (loading) {

            status =
                    "保存済み展開図を取得中...";

            return;
        }


        if (searchText
                .strip()
                .isEmpty()) {

            status =
                    "保存済み展開図: "
                            + filteredEntries.size()
                            + "件";

        } else {

            status =
                    "検索結果: "
                            + filteredEntries.size()
                            + " / "
                            + filteredEntries.size()
                            + "件";
        }
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


        if (filteredEntries.isEmpty()) {

            graphics.text(
                    font,
                    Component.literal(
                            searchText
                                    .strip()
                                    .isEmpty()
                                    ? "保存済みの展開図はありません"
                                    : "検索条件に一致する展開図はありません"
                    ),
                    30,
                    80,
                    0xFFAAAAAA
            );

            return;
        }


        int rowsPerPage =
                getRowsPerPage();


        int start =
                page
                        * rowsPerPage;

        int end =
                Math.min(
                        start
                                + rowsPerPage,
                        filteredEntries.size()
                );


        int y =
                LIST_TOP;


        for (int i = start;
             i < end;
             i++) {

            CreasePatternListPayload.Entry entry =
                    filteredEntries.get(
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
                    ROW_HEIGHT;
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


        int rowsPerPage =
                getRowsPerPage();


        int start =
                page
                        * rowsPerPage;

        int end =
                Math.min(
                        start
                                + rowsPerPage,
                        filteredEntries.size()
                );


        int y =
                LIST_TOP;


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
                        filteredEntries.get(
                                i
                        )
                );

                return true;
            }


            y +=
                    ROW_HEIGHT;
        }


        return false;
    }
}