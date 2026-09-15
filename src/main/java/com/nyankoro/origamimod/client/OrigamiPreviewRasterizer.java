package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.origami.OrigamiEdge;
import com.nyankoro.origamimod.origami.OrigamiFace;
import com.nyankoro.origamimod.origami.OrigamiFoldResult;
import com.nyankoro.origamimod.origami.OrigamiVertex;
import com.nyankoro.origamimod.origami.OrigamiAppearance;

import java.util.ArrayList;
import java.util.List;

/*
 * OrigamiFoldResult を
 * GUI表示用の2D画像データへ変換する。
 *
 * MinecraftのTextureにはまだ変換せず、
 * 同色の横方向ピクセルをSpanとしてまとめる。
 *
 * 将来的には、この処理結果を
 * PNG / DynamicTexture生成へ流用できる。
 */
public final class OrigamiPreviewRasterizer {

    private static final int PADDING =
            12;

    /*
     * GUIの表示サイズは変えず、
     * 内部だけ2倍の解像度で描画する。
     */
    private static final int SUPERSAMPLE =
            2;

    private OrigamiPreviewRasterizer() {
    }

    /*
     * 1行分の同色ピクセル区間。
     *
     * xStart～xEndまでを
     * colorで塗ればよい。
     */
    public record Span(
            int y,
            int xStart,
            int xEnd,
            int color
    ) {
    }

    private record PreviewPoint(
            double x,
            double y
    ) {
    }

    private record TransformContext(
            double centerX,
            double centerY,
            double scale,
            double cos,
            double sin,
            int width,
            int height
    ) {
    }

    /*
     * SUPERSAMPLE倍で描いた画像を
     * 元のGUIサイズへ縮小する。
     *
     * 各ピクセルのRGBAを平均することで、
     * 斜線や輪郭に簡易アンチエイリアスを掛ける。
     */
    private static int[] downsample(
            int[] source,
            int sourceWidth,
            int sourceHeight,
            int targetWidth,
            int targetHeight
    ) {

        int[] target =
                new int[
                        targetWidth
                                * targetHeight
                        ];

        int sampleCount =
                SUPERSAMPLE
                        * SUPERSAMPLE;

        for (int y = 0;
             y < targetHeight;
             y++) {

            for (int x = 0;
                 x < targetWidth;
                 x++) {

                long alphaSum = 0L;

                long redPremultipliedSum = 0L;
                long greenPremultipliedSum = 0L;
                long bluePremultipliedSum = 0L;

                for (int sampleY = 0;
                     sampleY < SUPERSAMPLE;
                     sampleY++) {

                    for (int sampleX = 0;
                         sampleX < SUPERSAMPLE;
                         sampleX++) {

                        int sourceX =
                                x * SUPERSAMPLE
                                        + sampleX;

                        int sourceY =
                                y * SUPERSAMPLE
                                        + sampleY;

                        int color =
                                source[
                                        sourceY
                                                * sourceWidth
                                                + sourceX
                                        ];

                        int alpha =
                                color >>> 24
                                        & 0xFF;

                        int red =
                                color >>> 16
                                        & 0xFF;

                        int green =
                                color >>> 8
                                        & 0xFF;

                        int blue =
                                color
                                        & 0xFF;

                        alphaSum +=
                                alpha;

                        redPremultipliedSum +=
                                (long) red
                                        * alpha;

                        greenPremultipliedSum +=
                                (long) green
                                        * alpha;

                        bluePremultipliedSum +=
                                (long) blue
                                        * alpha;
                    }
                }

                if (alphaSum == 0L) {

                    target[
                            y * targetWidth
                                    + x
                            ] = 0;

                    continue;
                }

                int outputAlpha =
                        (int) (
                                alphaSum
                                        / sampleCount
                        );

                int outputRed =
                        (int) (
                                redPremultipliedSum
                                        / alphaSum
                        );

                int outputGreen =
                        (int) (
                                greenPremultipliedSum
                                        / alphaSum
                        );

                int outputBlue =
                        (int) (
                                bluePremultipliedSum
                                        / alphaSum
                        );

                target[
                        y * targetWidth
                                + x
                        ] =
                        outputAlpha << 24
                                | outputRed << 16
                                | outputGreen << 8
                                | outputBlue;
            }
        }

        return target;
    }



    public static List<Span> rasterize(
            OrigamiFoldResult result,
            int width,
            int height,
            double zoom,
            double angleDegrees
    ) {

        return rasterize(
                result,
                width,
                height,
                zoom,
                angleDegrees,
                OrigamiAppearance.DEFAULT
        );
    }


    /*
     * 実際のラスタライズ処理。
     * 色を外部から指定できる版。
     */
    public static List<Span> rasterize(
            OrigamiFoldResult result,
            int width,
            int height,
            double zoom,
            double angleDegrees,
            OrigamiAppearance appearance
    ) {

        if (width <= 0
                || height <= 0
                || result.faces().isEmpty()) {

            return List.of();
        }

        /*
         * 実際にラスタライズする内部解像度。
         *
         * 例:
         * GUI = 500×350
         * 内部 = 1000×700
         */
        int renderWidth =
                width * SUPERSAMPLE;

        int renderHeight =
                height * SUPERSAMPLE;

        double minX =
                Double.POSITIVE_INFINITY;

        double minY =
                Double.POSITIVE_INFINITY;

        double maxX =
                Double.NEGATIVE_INFINITY;

        double maxY =
                Double.NEGATIVE_INFINITY;

        /*
         * 折り上がり図全体のBounding Boxを取得。
         */
        for (OrigamiFace face :
                result.faces()) {

            for (OrigamiVertex vertex :
                    face.vertices()) {

                minX =
                        Math.min(
                                minX,
                                vertex.x()
                        );

                minY =
                        Math.min(
                                minY,
                                vertex.y()
                        );

                maxX =
                        Math.max(
                                maxX,
                                vertex.x()
                        );

                maxY =
                        Math.max(
                                maxY,
                                vertex.y()
                        );
            }
        }

        if (!Double.isFinite(minX)
                || !Double.isFinite(minY)
                || !Double.isFinite(maxX)
                || !Double.isFinite(maxY)) {

            return List.of();
        }

        double modelWidth =
                Math.max(
                        0.000001,
                        maxX - minX
                );

        double modelHeight =
                Math.max(
                        0.000001,
                        maxY - minY
                );

        double usableWidth =
                Math.max(
                        1.0,
                        renderWidth
                                - PADDING
                                * 2.0
                                * SUPERSAMPLE
                );

        double usableHeight =
                Math.max(
                        1.0,
                        renderHeight
                                - PADDING
                                * 2.0
                                * SUPERSAMPLE
                );

        /*
         * Zoom=1.0のとき、
         * プレビュー枠内へ自動フィット。
         */
        double fitScale =
                Math.min(
                        usableWidth
                                / modelWidth,

                        usableHeight
                                / modelHeight
                );

        double centerX =
                (minX + maxX)
                        / 2.0;

        double centerY =
                (minY + maxY)
                        / 2.0;

        double angleRadians =
                Math.toRadians(
                        angleDegrees
                );

        TransformContext context =
                new TransformContext(
                        centerX,
                        centerY,
                        fitScale
                                * zoom,
                        Math.cos(
                                angleRadians
                        ),
                        Math.sin(
                                angleRadians
                        ),
                        renderWidth,
                        renderHeight
                );

        /*
         * 0 は透明扱い。
         */
        int[] pixels =
                new int[
                        renderWidth
                                * renderHeight
                        ];

        /*
         * まず可視Faceを塗る。
         */
        for (OrigamiFace face :
                result.faces()) {

            int color =
                    face.frontSideUp()
                            ? appearance.frontColor()
                            : appearance.backColor();
            try {

                for (PolygonTriangulator.Triangle triangle :
                        PolygonTriangulator.triangulate(
                                face.vertices()
                        )) {

                    PreviewPoint a =
                            transform(
                                    triangle.a(),
                                    context
                            );

                    PreviewPoint b =
                            transform(
                                    triangle.b(),
                                    context
                            );

                    PreviewPoint c =
                            transform(
                                    triangle.c(),
                                    context
                            );

                    fillTriangle(
                            pixels,
                            renderWidth,
                            renderHeight,
                            a,
                            b,
                            c,
                            color
                    );
                }

            } catch (IllegalArgumentException ignored) {

                /*
                 * GUIプレビューでは、
                 * 1Faceの三角形化失敗で
                 * 画面全体を壊さない。
                 */
            }
        }

        /*
         * Faceの後に境界線を描き、
         * 必ず線が上に表示されるようにする。
         */
        for (OrigamiEdge edge :
                result.edges()) {

            PreviewPoint a =
                    transform(
                            edge.a(),
                            context
                    );

            PreviewPoint b =
                    transform(
                            edge.b(),
                            context
                    );

            drawLine(
                    pixels,
                    renderWidth,
                    renderHeight,
                    a,
                    b,
                    appearance.edgeColor()
            );
        }

        int[] downsampledPixels =
                downsample(
                        pixels,
                        renderWidth,
                        renderHeight,
                        width,
                        height
                );

        return createSpans(
                downsampledPixels,
                width,
                height
        );
    }

    private static PreviewPoint transform(
            OrigamiVertex vertex,
            TransformContext context
    ) {

        /*
         * 作品中央を原点にする。
         */
        double x =
                vertex.x()
                        - context.centerX();

        double y =
                vertex.y()
                        - context.centerY();

        /*
         * 2D回転。
         */
        double rotatedX =
                x
                        * context.cos()
                        - y
                        * context.sin();

        double rotatedY =
                x
                        * context.sin()
                        + y
                        * context.cos();

        /*
         * プレビュー枠中央へ配置。
         */
        double screenX =
                context.width()
                        / 2.0
                        + rotatedX
                        * context.scale();

        double screenY =
                context.height()
                        / 2.0
                        + rotatedY
                        * context.scale();

        return new PreviewPoint(
                screenX,
                screenY
        );
    }

    private static void fillTriangle(
            int[] pixels,
            int width,
            int height,
            PreviewPoint a,
            PreviewPoint b,
            PreviewPoint c,
            int color
    ) {

        int minX =
                clamp(
                        (int) Math.floor(
                                Math.min(
                                        a.x(),
                                        Math.min(
                                                b.x(),
                                                c.x()
                                        )
                                )
                        ),
                        0,
                        width - 1
                );

        int maxX =
                clamp(
                        (int) Math.ceil(
                                Math.max(
                                        a.x(),
                                        Math.max(
                                                b.x(),
                                                c.x()
                                        )
                                )
                        ),
                        0,
                        width - 1
                );

        int minY =
                clamp(
                        (int) Math.floor(
                                Math.min(
                                        a.y(),
                                        Math.min(
                                                b.y(),
                                                c.y()
                                        )
                                )
                        ),
                        0,
                        height - 1
                );

        int maxY =
                clamp(
                        (int) Math.ceil(
                                Math.max(
                                        a.y(),
                                        Math.max(
                                                b.y(),
                                                c.y()
                                        )
                                )
                        ),
                        0,
                        height - 1
                );

        for (int y = minY;
             y <= maxY;
             y++) {

            for (int x = minX;
                 x <= maxX;
                 x++) {

                double px =
                        x + 0.5;

                double py =
                        y + 0.5;

                if (pointInTriangle(
                        px,
                        py,
                        a,
                        b,
                        c
                )) {

                    pixels[
                            y * width
                                    + x
                            ] =
                            color;
                }
            }
        }
    }

    private static boolean pointInTriangle(
            double px,
            double py,
            PreviewPoint a,
            PreviewPoint b,
            PreviewPoint c
    ) {

        double d1 =
                edgeFunction(
                        a,
                        b,
                        px,
                        py
                );

        double d2 =
                edgeFunction(
                        b,
                        c,
                        px,
                        py
                );

        double d3 =
                edgeFunction(
                        c,
                        a,
                        px,
                        py
                );

        boolean hasNegative =
                d1 < 0.0
                        || d2 < 0.0
                        || d3 < 0.0;

        boolean hasPositive =
                d1 > 0.0
                        || d2 > 0.0
                        || d3 > 0.0;

        return !(hasNegative
                && hasPositive);
    }

    private static double edgeFunction(
            PreviewPoint a,
            PreviewPoint b,
            double px,
            double py
    ) {

        return (px - a.x())
                * (b.y() - a.y())
                - (py - a.y())
                * (b.x() - a.x());
    }

    private static void drawLine(
            int[] pixels,
            int width,
            int height,
            PreviewPoint a,
            PreviewPoint b,
            int color
    ) {

        int x0 =
                (int) Math.round(
                        a.x()
                );

        int y0 =
                (int) Math.round(
                        a.y()
                );

        int x1 =
                (int) Math.round(
                        b.x()
                );

        int y1 =
                (int) Math.round(
                        b.y()
                );

        int dx =
                Math.abs(
                        x1 - x0
                );

        int sx =
                x0 < x1
                        ? 1
                        : -1;

        int dy =
                -Math.abs(
                        y1 - y0
                );

        int sy =
                y0 < y1
                        ? 1
                        : -1;

        int error =
                dx + dy;

        while (true) {

            /*
             * 約2pxの輪郭にする。
             */
            putPixel(
                    pixels,
                    width,
                    height,
                    x0,
                    y0,
                    color
            );

            putPixel(
                    pixels,
                    width,
                    height,
                    x0 + 1,
                    y0,
                    color
            );

            putPixel(
                    pixels,
                    width,
                    height,
                    x0,
                    y0 + 1,
                    color
            );

            if (x0 == x1
                    && y0 == y1) {
                break;
            }

            int e2 =
                    2 * error;

            if (e2 >= dy) {

                error +=
                        dy;

                x0 +=
                        sx;
            }

            if (e2 <= dx) {

                error +=
                        dx;

                y0 +=
                        sy;
            }
        }
    }

    private static void putPixel(
            int[] pixels,
            int width,
            int height,
            int x,
            int y,
            int color
    ) {

        if (x < 0
                || y < 0
                || x >= width
                || y >= height) {

            return;
        }

        pixels[
                y * width
                        + x
                ] =
                color;
    }

    private static List<Span> createSpans(
            int[] pixels,
            int width,
            int height
    ) {

        List<Span> spans =
                new ArrayList<>();

        for (int y = 0;
             y < height;
             y++) {

            int x =
                    0;

            while (x < width) {

                int color =
                        pixels[
                                y * width
                                        + x
                                ];

                if (color == 0) {

                    x++;

                    continue;
                }

                int start =
                        x;

                x++;

                while (x < width
                        && pixels[
                        y * width
                                + x
                        ] == color) {

                    x++;
                }

                spans.add(
                        new Span(
                                y,
                                start,
                                x - 1,
                                color
                        )
                );
            }
        }

        return List.copyOf(
                spans
        );
    }

    private static int clamp(
            int value,
            int min,
            int max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }
}