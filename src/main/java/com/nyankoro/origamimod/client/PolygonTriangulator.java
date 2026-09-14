package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.origami.OrigamiVertex;

import java.util.ArrayList;
import java.util.List;

public final class PolygonTriangulator {

    private static final double EPSILON = 1.0e-8;

    private PolygonTriangulator() {
    }

    public record Triangle(
            OrigamiVertex a,
            OrigamiVertex b,
            OrigamiVertex c
    ) {
    }

    public static List<Triangle> triangulate(
            List<OrigamiVertex> polygon
    ) {
        List<OrigamiVertex> vertices =
                cleanPolygon(polygon);

        if (vertices.size() < 3) {
            return List.of();
        }

        if (vertices.size() == 3) {
            return List.of(
                    new Triangle(
                            vertices.get(0),
                            vertices.get(1),
                            vertices.get(2)
                    )
            );
        }

        double area = signedArea(vertices);

        if (Math.abs(area) < EPSILON) {
            return List.of();
        }

        boolean counterClockwise = area > 0.0;

        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < vertices.size(); i++) {
            indices.add(i);
        }

        List<Triangle> triangles =
                new ArrayList<>();

        int guard = 0;
        int maxIterations =
                vertices.size() * vertices.size();

        while (indices.size() > 3
                && guard < maxIterations) {

            boolean earFound = false;

            for (int i = 0; i < indices.size(); i++) {

                int previousIndex =
                        indices.get(
                                (i - 1 + indices.size())
                                        % indices.size()
                        );

                int currentIndex =
                        indices.get(i);

                int nextIndex =
                        indices.get(
                                (i + 1)
                                        % indices.size()
                        );

                OrigamiVertex a =
                        vertices.get(previousIndex);

                OrigamiVertex b =
                        vertices.get(currentIndex);

                OrigamiVertex c =
                        vertices.get(nextIndex);

                if (!isConvex(
                        a,
                        b,
                        c,
                        counterClockwise
                )) {
                    continue;
                }

                boolean containsOtherPoint = false;

                for (int index : indices) {

                    if (index == previousIndex
                            || index == currentIndex
                            || index == nextIndex) {
                        continue;
                    }

                    if (pointInTriangle(
                            vertices.get(index),
                            a,
                            b,
                            c
                    )) {
                        containsOtherPoint = true;
                        break;
                    }
                }

                if (containsOtherPoint) {
                    continue;
                }

                triangles.add(
                        new Triangle(a, b, c)
                );

                indices.remove(i);

                earFound = true;
                break;
            }

            if (!earFound) {
                throw new IllegalArgumentException(
                        "Could not triangulate polygon."
                );
            }

            guard++;
        }

        if (indices.size() == 3) {

            triangles.add(
                    new Triangle(
                            vertices.get(indices.get(0)),
                            vertices.get(indices.get(1)),
                            vertices.get(indices.get(2))
                    )
            );
        }

        return triangles;
    }

    private static List<OrigamiVertex> cleanPolygon(
            List<OrigamiVertex> polygon
    ) {
        List<OrigamiVertex> result =
                new ArrayList<>();

        for (OrigamiVertex vertex : polygon) {

            if (result.isEmpty()
                    || !samePoint(
                    result.get(result.size() - 1),
                    vertex
            )) {
                result.add(vertex);
            }
        }

        if (result.size() > 1
                && samePoint(
                result.get(0),
                result.get(result.size() - 1)
        )) {
            result.remove(result.size() - 1);
        }

        boolean changed = true;

        while (changed && result.size() > 3) {

            changed = false;

            for (int i = 0;
                 i < result.size();
                 i++) {

                OrigamiVertex previous =
                        result.get(
                                (i - 1 + result.size())
                                        % result.size()
                        );

                OrigamiVertex current =
                        result.get(i);

                OrigamiVertex next =
                        result.get(
                                (i + 1)
                                        % result.size()
                        );

                if (Math.abs(
                        cross(
                                previous,
                                current,
                                next
                        )
                ) < EPSILON) {

                    result.remove(i);
                    changed = true;
                    break;
                }
            }
        }

        return result;
    }

    private static double signedArea(
            List<OrigamiVertex> vertices
    ) {
        double area = 0.0;

        for (int i = 0;
             i < vertices.size();
             i++) {

            OrigamiVertex a =
                    vertices.get(i);

            OrigamiVertex b =
                    vertices.get(
                            (i + 1)
                                    % vertices.size()
                    );

            area +=
                    a.x() * b.y()
                            - b.x() * a.y();
        }

        return area * 0.5;
    }

    private static boolean isConvex(
            OrigamiVertex a,
            OrigamiVertex b,
            OrigamiVertex c,
            boolean counterClockwise
    ) {
        double value = cross(a, b, c);

        return counterClockwise
                ? value > EPSILON
                : value < -EPSILON;
    }

    private static double cross(
            OrigamiVertex a,
            OrigamiVertex b,
            OrigamiVertex c
    ) {
        return (b.x() - a.x())
                * (c.y() - a.y())
                - (b.y() - a.y())
                * (c.x() - a.x());
    }

    private static boolean pointInTriangle(
            OrigamiVertex p,
            OrigamiVertex a,
            OrigamiVertex b,
            OrigamiVertex c
    ) {
        double c1 = cross(a, b, p);
        double c2 = cross(b, c, p);
        double c3 = cross(c, a, p);

        boolean hasNegative =
                c1 < -EPSILON
                        || c2 < -EPSILON
                        || c3 < -EPSILON;

        boolean hasPositive =
                c1 > EPSILON
                        || c2 > EPSILON
                        || c3 > EPSILON;

        return !(hasNegative && hasPositive);
    }

    private static boolean samePoint(
            OrigamiVertex a,
            OrigamiVertex b
    ) {
        return Math.abs(a.x() - b.x())
                < EPSILON
                && Math.abs(a.y() - b.y())
                < EPSILON;
    }
}