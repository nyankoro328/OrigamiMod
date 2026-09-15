package com.nyankoro.origamimod.origami;

import origami.crease_pattern.LineSegmentSet;
import origami.crease_pattern.element.LineColor;
import origami.crease_pattern.element.Point;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class OrigamiDebugCpExporter {

    private OrigamiDebugCpExporter() {
    }

    /**
     * LineSegmentSetをOriedita互換の.cpとして保存する。
     *
     * .cpの色番号:
     *
     * 1 = BORDER / BLACK
     * 2 = MOUNTAIN / RED
     * 3 = VALLEY / BLUE
     * 4 = FLAT / AUX等
     */
    public static Path export(
            LineSegmentSet lineSegmentSet,
            Path output
    ) throws IOException {

        Path absolute =
                output.toAbsolutePath();

        Path parent =
                absolute.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer =
                     Files.newBufferedWriter(
                             absolute,
                             StandardCharsets.UTF_8
                     )) {

            int lineCount =
                    lineSegmentSet
                            .getNumLineSegments();

            for (int i = 0;
                 i < lineCount;
                 i++) {

                Point a =
                        lineSegmentSet.getA(i);

                Point b =
                        lineSegmentSet.getB(i);

                LineColor color =
                        lineSegmentSet.getColor(i);

                int cpColor =
                        toCpColor(color);

                /*
                 * Double.toString()を使って、
                 * できるだけ元座標を丸めず保存する。
                 */
                writer.write(
                        Integer.toString(cpColor)
                );

                writer.write(' ');

                writer.write(
                        Double.toString(
                                a.getX()
                        )
                );

                writer.write(' ');

                writer.write(
                        Double.toString(
                                a.getY()
                        )
                );

                writer.write(' ');

                writer.write(
                        Double.toString(
                                b.getX()
                        )
                );

                writer.write(' ');

                writer.write(
                        Double.toString(
                                b.getY()
                        )
                );

                writer.newLine();
            }
        }

        return absolute;
    }

    private static int toCpColor(
            LineColor color
    ) {

        if (color == LineColor.BLACK_0) {
            return 1;
        }

        if (color == LineColor.RED_1) {
            return 2;
        }

        if (color == LineColor.BLUE_2) {
            return 3;
        }

        /*
         * CYAN等。
         */
        return 4;
    }
}