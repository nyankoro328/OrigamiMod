package com.nyankoro.origamimod.origami;

import fold.io.CreasePatternReader;
import fold.model.Edge;
import fold.model.FoldEdgeAssignment;
import fold.model.FoldFile;
import origami.crease_pattern.LineSegmentSet;
import origami.crease_pattern.element.LineColor;
import origami.crease_pattern.element.Point;

import java.io.IOException;
import java.io.InputStream;

public final class CpLoader {

    private CpLoader() {
    }

    public static LineSegmentSet load(InputStream input) throws IOException {
        CreasePatternReader reader = new CreasePatternReader(input);
        FoldFile foldFile = reader.read();

        LineSegmentSet lineSegmentSet = new LineSegmentSet();

        for (Edge edge : foldFile.getRootFrame().getEdges()) {
            Point start = new Point(
                    edge.getStart().getX(),
                    edge.getStart().getY()
            );

            Point end = new Point(
                    edge.getEnd().getX(),
                    edge.getEnd().getY()
            );

            lineSegmentSet.addLine(
                    start,
                    end,
                    toLineColor(edge.getAssignment())
            );
        }

        return lineSegmentSet;
    }

    private static LineColor toLineColor(FoldEdgeAssignment assignment) {
        return switch (assignment) {
            case MOUNTAIN_FOLD -> LineColor.RED_1;
            case VALLEY_FOLD -> LineColor.BLUE_2;
            case FLAT_FOLD -> LineColor.CYAN_3;
            default -> LineColor.BLACK_0;
        };
    }
}