package com.nyankoro.origamimod.origami;

import fold.io.CreasePatternReader;
import fold.model.Edge;
import fold.model.FoldEdgeAssignment;
import fold.model.FoldFile;

import origami.crease_pattern.FoldLineSet;
import origami.crease_pattern.LineSegmentSet;
import origami.crease_pattern.element.LineColor;
import origami.crease_pattern.element.LineSegment;
import origami.crease_pattern.element.Point;

import java.io.IOException;
import java.io.InputStream;

public final class CpLoader {

    private CpLoader() {
    }

    /**
     * Oriedita本体に近い形で読み込んだ結果。
     */
    public record LoadResult(
            FoldLineSet fullFoldLineSet,
            LineSegmentSet rawLineSegmentSet,
            LineSegmentSet foldingLineSegmentSet,
            Point selectionSeed,
            int rawLineCount,
            int selectedConnectedLineCount,
            int foldingLineCount
    ) {
    }

    /**
     * 互換用。
     *
     * 既存コードからCpLoader.load()を呼んでも、
     * 折り畳み用LineSegmentSetが返る。
     */
    public static LineSegmentSet load(
            InputStream input
    ) throws IOException {

        return loadForFolding(input)
                .foldingLineSegmentSet();
    }

    /**
     * .cpをOrieditaと同じ考え方で読み込む。
     */
    public static LoadResult loadForFolding(
            InputStream input
    ) throws IOException {

        CreasePatternReader reader =
                new CreasePatternReader(input);

        FoldFile foldFile =
                reader.read();

        /*
         * Orieditaの編集状態に相当。
         *
         * CYANを含め、
         * .cpの全線をここでは保持する。
         */
        FoldLineSet fullFoldLineSet =
                new FoldLineSet();

        /*
         * デバッグ用。
         * .cpの全線をSVG化するときに使う。
         */
        LineSegmentSet rawLineSegmentSet =
                new LineSegmentSet();

        for (Edge edge :
                foldFile
                        .getRootFrame()
                        .getEdges()) {

            Point start =
                    new Point(
                            edge.getStart().getX(),
                            edge.getStart().getY()
                    );

            Point end =
                    new Point(
                            edge.getEnd().getX(),
                            edge.getEnd().getY()
                    );

            LineColor color =
                    toLineColor(
                            edge.getAssignment()
                    );

            LineSegment segment =
                    new LineSegment(
                            start,
                            end,
                            color
                    );

            /*
             * 全線を保持。
             *
             * FLAT_FOLD = CYANもここでは消さない。
             */
            fullFoldLineSet.addLine(
                    segment
            );

            rawLineSegmentSet.addLine(
                    start,
                    end,
                    color
            );
        }

        int rawLineCount =
                fullFoldLineSet.getTotal();

        /*
         * Orieditaの通常の「全接続線を折る」に近い処理。
         *
         * 今回は原点に最も近い頂点を開始点にする。
         *
         * 今回のCPはほぼ全体が接続されているため、
         * 最終的には全体が選ばれる想定。
         */
        Point selectionSeed =
                fullFoldLineSet.closestPoint(
                        new Point(0.0, 0.0)
                );

        fullFoldLineSet.unselect_all();

        fullFoldLineSet.selectProbablyConnected(
                selectionSeed
        );

        /*
         * OrieditaのgetForSelectFolding()に相当する
         * LineSegmentSetを自前で作る。
         */
        LineSegmentSet foldingLineSegmentSet =
                new LineSegmentSet();

        int selectedConnectedLineCount = 0;
        int foldingLineCount = 0;

        for (LineSegment segment :
                fullFoldLineSet
                        .getLineSegmentsCollection()) {

            if (segment.getSelected() != 2) {
                continue;
            }

            selectedConnectedLineCount++;

            /*
             * OrieditaのisFoldingLine()は
             *
             * BLACK
             * RED
             * BLUE
             *
             * の3種類。
             *
             * CYAN等はここで初めて除外する。
             */
            if (!segment
                    .getColor()
                    .isFoldingLine()) {
                continue;
            }

            foldingLineSegmentSet.addLine(
                    segment.getA(),
                    segment.getB(),
                    segment.getColor()
            );

            foldingLineCount++;
        }

        return new LoadResult(
                fullFoldLineSet,
                rawLineSegmentSet,
                foldingLineSegmentSet,
                selectionSeed,
                rawLineCount,
                selectedConnectedLineCount,
                foldingLineCount
        );
    }

    /**
     * OrieditaのFoldImporter.getColor()と同じ対応。
     */
    private static LineColor toLineColor(
            FoldEdgeAssignment assignment
    ) {

        return switch (assignment) {

            case MOUNTAIN_FOLD ->
                    LineColor.RED_1;

            case VALLEY_FOLD ->
                    LineColor.BLUE_2;

            case FLAT_FOLD ->
                    LineColor.CYAN_3;

            /*
             * BORDER等。
             */
            default ->
                    LineColor.BLACK_0;
        };
    }
}