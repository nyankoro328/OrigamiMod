package com.nyankoro.origamimod.origami;

import origami.crease_pattern.LineSegmentSet;
import origami.crease_pattern.PointSet;
import origami.crease_pattern.element.LineColor;
import origami.crease_pattern.element.Point;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class OrigamiDebugSvgExporter {

    private OrigamiDebugSvgExporter() {
    }

    // =========================================================
    // Stage 1
    // 入力された展開図
    // =========================================================

    public static Path exportLineSegmentSet(
            LineSegmentSet lineSegmentSet,
            Path output
    ) throws IOException {

        if (lineSegmentSet.getNumLineSegments() == 0) {
            throw new IllegalArgumentException(
                    "LineSegmentSet is empty"
            );
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (int i = 0;
             i < lineSegmentSet.getNumLineSegments();
             i++) {

            Point a = lineSegmentSet.getA(i);
            Point b = lineSegmentSet.getB(i);

            minX = Math.min(
                    minX,
                    Math.min(a.getX(), b.getX())
            );

            minY = Math.min(
                    minY,
                    Math.min(a.getY(), b.getY())
            );

            maxX = Math.max(
                    maxX,
                    Math.max(a.getX(), b.getX())
            );

            maxY = Math.max(
                    maxY,
                    Math.max(a.getY(), b.getY())
            );
        }

        StringBuilder svg =
                beginSvg(minX, minY, maxX, maxY);

        for (int i = 0;
             i < lineSegmentSet.getNumLineSegments();
             i++) {

            Point a = lineSegmentSet.getA(i);
            Point b = lineSegmentSet.getB(i);

            appendLine(
                    svg,
                    a.getX(),
                    a.getY(),
                    b.getX(),
                    b.getY(),
                    colorForLine(
                            lineSegmentSet.getColor(i)
                    ),
                    0.8
            );
        }

        svg.append("</svg>\n");

        return write(output, svg);
    }

    // =========================================================
    // Stage 2 / 3
    // Oriedita内部のワイヤーフレーム
    // =========================================================

    public static Path exportPointSetWireframe(
            PointSet pointSet,
            Path output
    ) throws IOException {

        if (pointSet.getNumPoints() == 0) {
            throw new IllegalArgumentException(
                    "PointSet is empty"
            );
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (int pointId = 1;
             pointId <= pointSet.getNumPoints();
             pointId++) {

            double x =
                    pointSet.getPointX(pointId);

            double y =
                    pointSet.getPointY(pointId);

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);

            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        StringBuilder svg =
                beginSvg(minX, minY, maxX, maxY);

        for (int lineId = 1;
             lineId <= pointSet.getNumLines();
             lineId++) {

            appendLine(
                    svg,
                    pointSet.getBeginX(lineId),
                    pointSet.getBeginY(lineId),
                    pointSet.getEndX(lineId),
                    pointSet.getEndY(lineId),
                    colorForLine(
                            pointSet.getColor(lineId)
                    ),
                    0.6
            );
        }

        svg.append("</svg>\n");

        return write(output, svg);
    }

    // =========================================================
    // Stage 4
    // Minecraftへ渡す最終可視面
    // =========================================================

    public static Path export(
            OrigamiFoldResult result,
            Path output
    ) throws IOException {

        if (result.faces().isEmpty()) {
            throw new IllegalArgumentException(
                    "OrigamiFoldResult has no faces"
            );
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (OrigamiFace face : result.faces()) {

            for (OrigamiVertex vertex :
                    face.vertices()) {

                minX = Math.min(
                        minX,
                        vertex.x()
                );

                minY = Math.min(
                        minY,
                        vertex.y()
                );

                maxX = Math.max(
                        maxX,
                        vertex.x()
                );

                maxY = Math.max(
                        maxY,
                        vertex.y()
                );
            }
        }

        StringBuilder svg =
                beginSvg(minX, minY, maxX, maxY);

        for (OrigamiFace face :
                result.faces()) {

            String color =
                    face.frontSideUp()
                            ? "#ffcc55"
                            : "#55aadd";

            svg.append(
                    "<polygon points=\""
            );

            for (OrigamiVertex vertex :
                    face.vertices()) {

                svg.append(
                        String.format(
                                Locale.US,
                                "%.6f,%.6f ",
                                vertex.x(),
                                vertex.y()
                        )
                );
            }

            /*
             * 重要：
             *
             * SubFaceを全て黒線で囲わない。
             *
             * 以前はここでstroke=\"black\"にしていたため、
             * Orieditaの透明図のような骨組みに見えていた。
             */
            svg.append("\" fill=\"")
                    .append(color)
                    .append("\" stroke=\"none\"")
                    .append("/>\n");
        }

        svg.append("</svg>\n");

        return write(output, svg);
    }

    // =========================================================
    // 共通処理
    // =========================================================

    private static StringBuilder beginSvg(
            double minX,
            double minY,
            double maxX,
            double maxY
    ) {

        double sourceWidth =
                Math.max(1.0, maxX - minX);

        double sourceHeight =
                Math.max(1.0, maxY - minY);

        double margin =
                Math.max(
                        5.0,
                        Math.max(
                                sourceWidth,
                                sourceHeight
                        ) * 0.03
                );

        minX -= margin;
        minY -= margin;
        maxX += margin;
        maxY += margin;

        double width =
                maxX - minX;

        double height =
                maxY - minY;

        StringBuilder svg =
                new StringBuilder();

        svg.append(
                String.format(
                        Locale.US,
                        """
                        <svg xmlns="http://www.w3.org/2000/svg"
                             viewBox="%.6f %.6f %.6f %.6f"
                             preserveAspectRatio="xMidYMid meet">
                        <rect x="%.6f"
                              y="%.6f"
                              width="%.6f"
                              height="%.6f"
                              fill="white"/>
                        """,
                        minX,
                        minY,
                        width,
                        height,
                        minX,
                        minY,
                        width,
                        height
                )
        );

        return svg;
    }

    private static void appendLine(
            StringBuilder svg,
            double x1,
            double y1,
            double x2,
            double y2,
            String color,
            double width
    ) {

        svg.append(
                String.format(
                        Locale.US,
                        """
                        <line x1="%.6f"
                              y1="%.6f"
                              x2="%.6f"
                              y2="%.6f"
                              stroke="%s"
                              stroke-width="%.3f"
                              stroke-linecap="round"
                              vector-effect="non-scaling-stroke"/>
                        """,
                        x1,
                        y1,
                        x2,
                        y2,
                        color,
                        width
                )
        );
    }

    private static String colorForLine(
            LineColor color
    ) {

        if (color == LineColor.RED_1) {
            return "#ff2020";
        }

        if (color == LineColor.BLUE_2) {
            return "#2040ff";
        }

        if (color == LineColor.CYAN_3) {
            return "#00aaaa";
        }

        return "#202020";
    }

    private static Path write(
            Path output,
            StringBuilder svg
    ) throws IOException {

        Path absolute =
                output.toAbsolutePath();

        Path parent =
                absolute.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                absolute,
                svg.toString()
        );

        return absolute;
    }
}