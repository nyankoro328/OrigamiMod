package com.nyankoro.origamimod.origami;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class OrigamiDebugSvgExporter {

    private OrigamiDebugSvgExporter() {
    }

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
            for (OrigamiVertex vertex : face.vertices()) {
                minX = Math.min(minX, vertex.x());
                minY = Math.min(minY, vertex.y());
                maxX = Math.max(maxX, vertex.x());
                maxY = Math.max(maxY, vertex.y());
            }
        }

        double margin = 10.0;

        minX -= margin;
        minY -= margin;
        maxX += margin;
        maxY += margin;

        double width = maxX - minX;
        double height = maxY - minY;

        StringBuilder svg = new StringBuilder();

        svg.append(String.format(
                Locale.US,
                """
                <svg xmlns="http://www.w3.org/2000/svg"
                     viewBox="%.6f %.6f %.6f %.6f">
                <rect x="%.6f" y="%.6f"
                      width="%.6f" height="%.6f"
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
        ));

        for (OrigamiFace face : result.faces()) {

            String color = face.frontSideUp()
                    ? "#ffcc55"
                    : "#55aadd";

            svg.append("<polygon points=\"");

            for (OrigamiVertex vertex : face.vertices()) {
                svg.append(String.format(
                        Locale.US,
                        "%.6f,%.6f ",
                        vertex.x(),
                        vertex.y()
                ));
            }

            svg.append("\" fill=\"")
                    .append(color)
                    .append("\" stroke=\"black\"")
                    .append(" stroke-width=\"0.3\"")
                    .append("/>\n");
        }

        svg.append("</svg>\n");

        Files.writeString(output, svg.toString());

        return output.toAbsolutePath();
    }
}