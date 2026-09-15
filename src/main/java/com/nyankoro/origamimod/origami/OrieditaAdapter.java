package com.nyankoro.origamimod.origami;

import origami.crease_pattern.FoldingException;
import origami.crease_pattern.LineSegmentSet;
import origami.crease_pattern.PointSet;
import origami.crease_pattern.element.Point;

import origami.folding.FoldedFigure;
import origami.folding.element.SubFace;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OrieditaAdapter {

    private OrieditaAdapter() {
    }

    public static OrigamiFoldResult fold(
            InputStream cpInput
    ) throws IOException,
            InterruptedException,
            FoldingException {

        return foldForView(
                cpInput,
                -1,
                null,
                false
        );
    }

    public static OrigamiFoldResult fold(
            InputStream cpInput,
            Path debugDirectory
    ) throws IOException,
            InterruptedException,
            FoldingException {

        return foldForView(
                cpInput,
                -1,
                debugDirectory,
                false
        );
    }

    public static OrigamiFoldResult fold(
            InputStream cpInput,
            int startingFaceId,
            Path debugDirectory
    ) throws IOException,
            InterruptedException,
            FoldingException {

        return foldForView(
                cpInput,
                startingFaceId,
                debugDirectory,
                false
        );
    }

    /*
     * 裏側から見た折り紙を取得する。
     */
    public static OrigamiFoldResult foldBack(
            InputStream cpInput
    ) throws IOException,
            InterruptedException,
            FoldingException {

        return foldForView(
                cpInput,
                -1,
                null,
                true
        );
    }

    public static OrigamiFoldResult foldBack(
            InputStream cpInput,
            Path debugDirectory
    ) throws IOException,
            InterruptedException,
            FoldingException {

        return foldForView(
                cpInput,
                -1,
                debugDirectory,
                true
        );
    }

    /*
     * front / back 共通の実処理。
     */
    private static OrigamiFoldResult foldForView(
            InputStream cpInput,
            int startingFaceId,
            Path debugDirectory,
            boolean rearView
    ) throws IOException,
            InterruptedException,
            FoldingException {
        // =========================================================
        // Stage 1
        // .cpをOriedita方式で読み込む
        // =========================================================

        CpLoader.LoadResult loaded =
                CpLoader.loadForFolding(
                        cpInput
                );

        LineSegmentSet rawInput =
                loaded.rawLineSegmentSet();

        LineSegmentSet foldingInput =
                loaded.foldingLineSegmentSet();

        if (debugDirectory != null) {

            Files.createDirectories(
                    debugDirectory
            );

            /*
             * 元の.cpをSVGとして確認。
             */
            OrigamiDebugSvgExporter
                    .exportLineSegmentSet(
                            rawInput,
                            debugDirectory.resolve(
                                    "01-raw-cp.svg"
                            )
                    );

            /*
             * 元の.cpを再出力。
             * 478本の全線を含む。
             */
            OrigamiDebugCpExporter.export(
                    rawInput,
                    debugDirectory.resolve(
                            "01-raw-cp.cp"
                    )
            );

            /*
             * 実際にfolding_estimated()へ渡す
             * 476本の線をSVGとして確認。
             */
            OrigamiDebugSvgExporter
                    .exportLineSegmentSet(
                            foldingInput,
                            debugDirectory.resolve(
                                    "02-folding-input.svg"
                            )
                    );

            /*
             * 重要:
             *
             * Modが実際にOriedita coreへ渡すデータを
             * .cpとしてそのまま保存する。
             *
             * このファイルをデスクトップ版Orieditaで
             * 開いて比較する。
             */
            OrigamiDebugCpExporter.export(
                    foldingInput,
                    debugDirectory.resolve(
                            "02-folding-input.cp"
                    )
            );
        }

        // =========================================================
        // Stage 2
        // Oriedita core
        // =========================================================

        FoldedFigure foldedFigure =
                new FoldedFigure(
                        new NoOpBulletinBoard()
                );

        foldedFigure.estimationOrder =
                FoldedFigure
                        .EstimationOrder
                        .ORDER_5;

        foldedFigure.folding_estimated(
                foldingInput,
                startingFaceId
        );

        PointSet foldedWire =
                foldedFigure
                        .wireFrameWorker_foldedNotSubdivided
                        .get();

        PointSet subdivided =
                foldedFigure
                        .wireFrameWorker_foldedSubdivided
                        .get();

        PointSet flat =
                foldedFigure
                        .wireFrameWorker_flatCp
                        .get();

        if (debugDirectory != null) {

            OrigamiDebugSvgExporter
                    .exportPointSetWireframe(
                            foldedWire,
                            debugDirectory.resolve(
                                    "03-folded-wire.svg"
                            )
                    );

            OrigamiDebugSvgExporter
                    .exportPointSetWireframe(
                            subdivided,
                            debugDirectory.resolve(
                                    "04-subdivided-wire.svg"
                            )
                    );
        }

        // =========================================================
        // Stage 3
        // SubFaceの上下関係を計算
        // =========================================================

        SubFace[] subFaces =
                foldedFigure
                        .foldedFigure_worker
                        .s0;

        int regionCount =
                subdivided.getNumFaces();

        /*
         * 本家FoldedFigure_Worker_Drawerの
         * calculateFromTopCountedPosition()
         * と同じ処理。
         */
        for (int regionId = 1;
             regionId <= regionCount;
             regionId++) {

            if (regionId >= subFaces.length) {
                break;
            }

            SubFace subFace =
                    subFaces[regionId];

            if (subFace == null) {
                continue;
            }

            subFace
                    .set_FaceId2fromTop_counted_position(
                            foldedFigure
                                    .foldedFigure_worker
                                    .hierarchyList
                    );
        }

        // =========================================================
        // Stage 4
        // 正面から見える面を取得
        // =========================================================

        List<OrigamiFace> visibleRegions =
                new ArrayList<>();

        for (int regionId = 1;
             regionId <= regionCount;
             regionId++) {

            if (regionId >= subFaces.length) {
                break;
            }

            SubFace subFace =
                    subFaces[regionId];

            if (subFace == null
                    || subFace.getFaceIdCount() == 0) {
                continue;
            }

            /*
             * 正面表示。
             *
             * 上から1番目の元Face。
             */
            /*
             * 正面では上から1番目、
             * 裏面では上から最後のFaceを使用する。
             */
            int faceOrder =
                    rearView
                            ? subFace.getFaceIdCount()
                            : 1;

            int visibleFaceId =
                    subFace.fromTop_count_FaceId(faceOrder);

            if (visibleFaceId <= 0) {
                throw new IllegalStateException(
                        "Invalid visible Face ID. "
                                + "regionId=" + regionId
                                + ", faceId=" + visibleFaceId
                                + ", faceCount="
                                + subFace.getFaceIdCount()
                                + ", rearView=" + rearView
                );
            }

            int facePosition =
                    foldedFigure
                            .wireFrameWorker_flatCp
                            .getIFacePosition(visibleFaceId);

            /*
             * Oriedita GUI と同じ表裏判定。
             *
             * 正面：
             *   position が奇数 → 表
             *
             * 裏面：
             *   position が偶数 → 表
             */
            boolean frontSideUp =
                    rearView
                            ? (facePosition % 2) == 0
                            : (facePosition % 2) == 1;

            int vertexCount =
                    subdivided.getPointsCount(
                            regionId
                    );

            if (vertexCount < 3) {
                continue;
            }

            List<OrigamiVertex> vertices =
                    new ArrayList<>();

            for (int vertexIndex = 1;
                 vertexIndex <= vertexCount;
                 vertexIndex++) {

                int pointId =
                        subdivided.getPointId(
                                regionId,
                                vertexIndex
                        );

                vertices.add(
                        new OrigamiVertex(
                                subdivided
                                        .getPointX(
                                                pointId
                                        ),
                                subdivided
                                        .getPointY(
                                                pointId
                                        )
                        )
                );
            }

            visibleRegions.add(
                    new OrigamiFace(
                            visibleFaceId,
                            vertices,
                            frontSideUp,
                            0
                    )
            );
        }

        // =========================================================
// Stage 5
// Oriedita本家と同じ条件で可視境界線を抽出
// =========================================================

        List<OrigamiEdge> visibleEdges =
                collectVisibleEdges(
                        subdivided,
                        subFaces,
                        rearView
                );

        OrigamiFoldResult result =
                new OrigamiFoldResult(
                        visibleRegions,
                        visibleEdges
                );

        // =========================================================
        // Debug
        // =========================================================

        if (debugDirectory != null) {

            OrigamiDebugSvgExporter.export(
                    result,
                    debugDirectory.resolve(
                            "05-visible-faces.svg"
                    )
            );

            writeFaceInfo(
                    debugDirectory,
                    foldedFigure,
                    flat,
                    subdivided,
                    subFaces
            );

            writeDebugStatus(
                    debugDirectory,
                    loaded,
                    foldedFigure,
                    flat,
                    foldedWire,
                    subdivided,
                    subFaces,
                    startingFaceId
            );
        }

        return result;
    }

    // =========================================================
    // Face詳細
    // =========================================================

    /*
     * Oriedita本家の
     * FoldedFigure_Worker_Drawer
     * と同じ考え方で、実際に表示する線だけ抽出する。
     */
    private static List<OrigamiEdge> collectVisibleEdges(
            PointSet subdivided,
            SubFace[] subFaces,
            boolean rearView
    ) {

        List<OrigamiEdge> edges =
                new ArrayList<>();

        for (int lineId = 1;
             lineId <= subdivided.getNumLines();
             lineId++) {

            /*
             * この線を境界として持つ
             * 2つのSubFaceを取得する。
             */
            int regionMin =
                    subdivided
                            .lineInFaceBorder_min_lookup(
                                    lineId
                            );

            int regionMax =
                    subdivided
                            .lineInFaceBorder_max_lookup(
                                    lineId
                            );

            boolean minHasPaper =
                    hasPaper(
                            subFaces,
                            regionMin
                    );

            boolean maxHasPaper =
                    hasPaper(
                            subFaces,
                            regionMax
                    );

            boolean shouldDraw = false;

            /*
             * Oriedita本家と同じ条件。
             *
             * 1. 片側が空領域
             * 2. 線の片側にしかSubFaceがない
             * 3. 左右で実際に見えている元Faceが異なる
             */
            if (!minHasPaper
                    || !maxHasPaper) {

                shouldDraw = true;

            } else if (regionMin == regionMax) {

                shouldDraw = true;

            } else {

                int visibleFaceMin =
                        getVisibleFaceId(
                                subFaces[regionMin],
                                rearView
                        );

                int visibleFaceMax =
                        getVisibleFaceId(
                                subFaces[regionMax],
                                rearView
                        );

                if (visibleFaceMin
                        != visibleFaceMax) {

                    shouldDraw = true;
                }
            }

            if (!shouldDraw) {
                continue;
            }

            OrigamiVertex a =
                    new OrigamiVertex(
                            subdivided.getBeginX(
                                    lineId
                            ),
                            subdivided.getBeginY(
                                    lineId
                            )
                    );

            OrigamiVertex b =
                    new OrigamiVertex(
                            subdivided.getEndX(
                                    lineId
                            ),
                            subdivided.getEndY(
                                    lineId
                            )
                    );

            edges.add(
                    new OrigamiEdge(
                            a,
                            b
                    )
            );
        }

        return edges;
    }

    private static boolean hasPaper(
            SubFace[] subFaces,
            int regionId
    ) {

        if (regionId <= 0
                || regionId >= subFaces.length) {
            return false;
        }

        SubFace subFace =
                subFaces[regionId];

        return subFace != null
                && subFace.getFaceIdCount() > 0;
    }

    private static int getVisibleFaceId(
            SubFace subFace,
            boolean rearView
    ) {

        if (subFace == null
                || subFace.getFaceIdCount() == 0) {
            return 0;
        }

        int faceOrder =
                rearView
                        ? subFace.getFaceIdCount()
                        : 1;

        return subFace
                .fromTop_count_FaceId(
                        faceOrder
                );
    }

    private static void writeFaceInfo(
            Path debugDirectory,
            FoldedFigure foldedFigure,
            PointSet flat,
            PointSet subdivided,
            SubFace[] subFaces
    ) throws IOException {

        StringBuilder text =
                new StringBuilder();

        text.append(
                "===== Original face positions =====\n\n"
        );

        int originalFront = 0;
        int originalBack = 0;
        int originalZero = 0;

        /*
         * 展開図に存在する全元Faceについて、
         * 基準面から何回反転した位置なのかを確認。
         */
        for (int faceId = 1;
             faceId <= flat.getNumFaces();
             faceId++) {

            int position =
                    foldedFigure
                            .wireFrameWorker_flatCp
                            .getIFacePosition(
                                    faceId
                            );

            String side;

            if (position == 0) {

                side = "ZERO";
                originalZero++;

            } else if ((position % 2) == 1) {

                side = "FRONT";
                originalFront++;

            } else {

                side = "BACK";
                originalBack++;
            }

            text.append(
                    String.format(
                            Locale.US,
                            "Face %d : position=%d  %s%n",
                            faceId,
                            position,
                            side
                    )
            );
        }

        text.append("\n");
        text.append(
                "original FRONT = "
        );
        text.append(
                originalFront
        );
        text.append("\n");

        text.append(
                "original BACK  = "
        );
        text.append(
                originalBack
        );
        text.append("\n");

        text.append(
                "original ZERO  = "
        );
        text.append(
                originalZero
        );
        text.append("\n\n");

        // ---------------------------------------------------------
        // SubFace
        // ---------------------------------------------------------

        text.append(
                "===== Visible SubFaces =====\n\n"
        );

        int visibleFront = 0;
        int visibleBack = 0;
        int empty = 0;

        int regionCount =
                subdivided.getNumFaces();

        for (int regionId = 1;
             regionId <= regionCount;
             regionId++) {

            if (regionId >= subFaces.length) {

                text.append(
                        "SubFace array ended before region "
                );

                text.append(
                        regionId
                );

                text.append("\n");

                break;
            }

            SubFace subFace =
                    subFaces[regionId];

            if (subFace == null) {

                text.append(
                        "SubFace "
                );

                text.append(
                        regionId
                );

                text.append(
                        " : null\n"
                );

                continue;
            }

            if (subFace.getFaceIdCount() == 0) {

                empty++;

                text.append(
                        String.format(
                                Locale.US,
                                "SubFace %d : EMPTY%n",
                                regionId
                        )
                );

                continue;
            }

            int topFaceId =
                    subFace
                            .fromTop_count_FaceId(
                                    1
                            );

            int position =
                    foldedFigure
                            .wireFrameWorker_flatCp
                            .getIFacePosition(
                                    topFaceId
                            );

            boolean front =
                    (position % 2) == 1;

            if (front) {
                visibleFront++;
            } else {
                visibleBack++;
            }

            text.append(
                    String.format(
                            Locale.US,
                            "SubFace %d : layers=%d  topFace=%d  position=%d  %s%n",
                            regionId,
                            subFace.getFaceIdCount(),
                            topFaceId,
                            position,
                            front
                                    ? "FRONT"
                                    : "BACK"
                    )
            );
        }

        text.append("\n");
        text.append(
                "visible FRONT = "
        );
        text.append(
                visibleFront
        );
        text.append("\n");

        text.append(
                "visible BACK  = "
        );
        text.append(
                visibleBack
        );
        text.append("\n");

        text.append(
                "empty SubFace = "
        );
        text.append(
                empty
        );
        text.append("\n");

        Files.writeString(
                debugDirectory.resolve(
                        "06-face-info.txt"
                ),
                text.toString()
        );
    }

    // =========================================================
    // 全体ステータス
    // =========================================================

    private static void writeDebugStatus(
            Path debugDirectory,
            CpLoader.LoadResult loaded,
            FoldedFigure foldedFigure,
            PointSet flat,
            PointSet foldedWire,
            PointSet subdivided,
            SubFace[] subFaces,
            int requestedStartingFaceId
    ) throws IOException {

        int actualStartingFaceId =
                foldedFigure.ip3;

        Point startingFacePoint;

        try {

            startingFacePoint =
                    flat.insidePoint_surface(
                            actualStartingFaceId
                    );

        } catch (Exception e) {

            startingFacePoint =
                    new Point(
                            Double.NaN,
                            Double.NaN
                    );
        }

        int originalOdd = 0;
        int originalEven = 0;
        int originalZero = 0;

        for (int faceId = 1;
             faceId <= flat.getNumFaces();
             faceId++) {

            int position =
                    foldedFigure
                            .wireFrameWorker_flatCp
                            .getIFacePosition(
                                    faceId
                            );

            if (position == 0) {

                originalZero++;

            } else if ((position % 2) == 1) {

                originalOdd++;

            } else {

                originalEven++;
            }
        }

        int visibleFront = 0;
        int visibleBack = 0;

        for (int regionId = 1;
             regionId <= subdivided.getNumFaces();
             regionId++) {

            if (regionId >= subFaces.length) {
                break;
            }

            SubFace subFace =
                    subFaces[regionId];

            if (subFace == null
                    || subFace.getFaceIdCount() == 0) {
                continue;
            }

            int topFace =
                    subFace
                            .fromTop_count_FaceId(
                                    1
                            );

            int position =
                    foldedFigure
                            .wireFrameWorker_flatCp
                            .getIFacePosition(
                                    topFace
                            );

            if ((position % 2) == 1) {
                visibleFront++;
            } else {
                visibleBack++;
            }
        }

        String coreSource = "unknown";

        try {

            if (FoldedFigure
                    .class
                    .getProtectionDomain()
                    .getCodeSource() != null) {

                coreSource =
                        FoldedFigure
                                .class
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toString();
            }

        } catch (Exception ignored) {
        }

        String implementationVersion =
                FoldedFigure
                        .class
                        .getPackage()
                        .getImplementationVersion();

        String text =
                String.format(
                        Locale.US,
                        """
                        ===== Origami fold debug =====

                        requestedStartingFaceId = %d
                        actualStartingFaceId    = %d

                        startingFacePoint       = (%.9f, %.9f)

                        estimationStep          = %s
                        displayStyle            = %s

                        discoveredFoldCases     = %d
                        findAnotherOverlapValid = %s

                        ----- CP input -----

                        rawCpLines              = %d
                        selectedConnectedLines  = %d
                        foldingInputLines       = %d

                        selectionSeed           = (%.9f, %.9f)

                        ----- Original flat faces -----

                        points                  = %d
                        lines                   = %d
                        faces                   = %d

                        odd/front positions     = %d
                        even/back positions     = %d
                        zero positions          = %d

                        ----- Folded wire -----

                        points                  = %d
                        lines                   = %d
                        faces                   = %d

                        ----- Subdivided -----

                        points                  = %d
                        lines                   = %d
                        faces                   = %d

                        ----- Visible -----

                        FRONT                   = %d
                        BACK                    = %d

                        ----- Oriedita core -----

                        implementationVersion   = %s
                        codeSource              = %s
                        """,

                        requestedStartingFaceId,
                        actualStartingFaceId,

                        startingFacePoint.getX(),
                        startingFacePoint.getY(),

                        foldedFigure.estimationStep,
                        foldedFigure.displayStyle,

                        foldedFigure
                                .discovered_fold_cases,

                        foldedFigure
                                .findAnotherOverlapValid,

                        loaded.rawLineCount(),

                        loaded
                                .selectedConnectedLineCount(),

                        loaded.foldingLineCount(),

                        loaded
                                .selectionSeed()
                                .getX(),

                        loaded
                                .selectionSeed()
                                .getY(),

                        flat.getNumPoints(),
                        flat.getNumLines(),
                        flat.getNumFaces(),

                        originalOdd,
                        originalEven,
                        originalZero,

                        foldedWire.getNumPoints(),
                        foldedWire.getNumLines(),
                        foldedWire.getNumFaces(),

                        subdivided.getNumPoints(),
                        subdivided.getNumLines(),
                        subdivided.getNumFaces(),

                        visibleFront,
                        visibleBack,

                        String.valueOf(
                                implementationVersion
                        ),

                        coreSource
                );

        Files.writeString(
                debugDirectory.resolve(
                        "00-status.txt"
                ),
                text
        );
    }
}