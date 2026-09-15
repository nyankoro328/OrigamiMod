package com.nyankoro.origamimod.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.origami.CpLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import origami.crease_pattern.LineSegmentSet;
import origami.crease_pattern.element.LineColor;
import origami.crease_pattern.element.Point;

import javax.imageio.ImageIO;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.concurrent.atomic.AtomicInteger;


/*
 * .cpファイルを折る前の状態で
 * UIプレビュー用テクスチャへ変換する。
 */
public final class CreasePatternPreviewTexture
        implements AutoCloseable {

    /*
     * UI表示サイズとは別。
     *
     * 内部画像を1024pxで作ることで、
     * 大きく表示しても線が崩れにくくする。
     */
    public static final int TEXTURE_SIZE =
            1024;

    private static final AtomicInteger NEXT_ID =
            new AtomicInteger();

    private final Identifier textureId;

    private final int lineCount;

    private boolean closed =
            false;


    public CreasePatternPreviewTexture(
            byte[] cpData
    ) throws IOException {

        CpLoader.LoadResult loaded;

        try (
                ByteArrayInputStream input =
                        new ByteArrayInputStream(
                                cpData
                        )
        ) {

            loaded =
                    CpLoader.loadForFolding(
                            input
                    );
        }

        LineSegmentSet lineSegmentSet =
                loaded.rawLineSegmentSet();

        lineCount =
                loaded.rawLineCount();

        BufferedImage preview =
                rasterize(
                        lineSegmentSet,
                        TEXTURE_SIZE
                );

        NativeImage nativeImage =
                toNativeImage(
                        preview
                );

        textureId =
                Identifier.fromNamespaceAndPath(
                        OrigamiMod.MODID,
                        "dynamic/cp_preview_"
                                + NEXT_ID.incrementAndGet()
                );

        Minecraft.getInstance()
                .getTextureManager()
                .register(
                        textureId,
                        new DynamicTexture(
                                textureId::toString,
                                nativeImage
                        )
                );
    }


    public Identifier textureId() {
        return textureId;
    }


    public int lineCount() {
        return lineCount;
    }


    /*
     * LineSegmentSetを
     * 1024×1024画像へ描画する。
     */
    private static BufferedImage rasterize(
            LineSegmentSet lineSegmentSet,
            int size
    ) {

        if (lineSegmentSet.getNumLineSegments()
                == 0) {

            throw new IllegalArgumentException(
                    "Crease pattern has no lines"
            );
        }

        double minX =
                Double.POSITIVE_INFINITY;

        double minY =
                Double.POSITIVE_INFINITY;

        double maxX =
                Double.NEGATIVE_INFINITY;

        double maxY =
                Double.NEGATIVE_INFINITY;


        for (int i = 0;
             i < lineSegmentSet
                     .getNumLineSegments();
             i++) {

            Point a =
                    lineSegmentSet.getA(i);

            Point b =
                    lineSegmentSet.getB(i);

            minX =
                    Math.min(
                            minX,
                            Math.min(
                                    a.getX(),
                                    b.getX()
                            )
                    );

            minY =
                    Math.min(
                            minY,
                            Math.min(
                                    a.getY(),
                                    b.getY()
                            )
                    );

            maxX =
                    Math.max(
                            maxX,
                            Math.max(
                                    a.getX(),
                                    b.getX()
                            )
                    );

            maxY =
                    Math.max(
                            maxY,
                            Math.max(
                                    a.getY(),
                                    b.getY()
                            )
                    );
        }


        double width =
                Math.max(
                        0.000001,
                        maxX - minX
                );

        double height =
                Math.max(
                        0.000001,
                        maxY - minY
                );


        /*
         * 画像端に少し余白を作る。
         */
        double padding =
                size * 0.06;

        double available =
                size
                        - padding * 2.0;

        double scale =
                Math.min(
                        available / width,
                        available / height
                );


        double drawnWidth =
                width * scale;

        double drawnHeight =
                height * scale;

        double offsetX =
                (size - drawnWidth)
                        / 2.0;

        double offsetY =
                (size - drawnHeight)
                        / 2.0;


        BufferedImage image =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D graphics =
                image.createGraphics();

        try {

            /*
             * 白背景。
             */
            graphics.setColor(
                    new Color(
                            245,
                            245,
                            245,
                            255
                    )
            );

            graphics.fillRect(
                    0,
                    0,
                    size,
                    size
            );


            /*
             * 線を高解像度で描画する。
             */
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );


            float lineWidth =
                    Math.max(
                            1.5F,
                            size / 512.0F
                    );

            graphics.setStroke(
                    new BasicStroke(
                            lineWidth,
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );


            for (int i = 0;
                 i < lineSegmentSet
                         .getNumLineSegments();
                 i++) {

                Point a =
                        lineSegmentSet.getA(i);

                Point b =
                        lineSegmentSet.getB(i);


                int x1 =
                        (int) Math.round(
                                offsetX
                                        + (a.getX() - minX)
                                        * scale
                        );

                int y1 =
                        (int) Math.round(
                                offsetY
                                        + (a.getY() - minY)
                                        * scale
                        );

                int x2 =
                        (int) Math.round(
                                offsetX
                                        + (b.getX() - minX)
                                        * scale
                        );

                int y2 =
                        (int) Math.round(
                                offsetY
                                        + (b.getY() - minY)
                                        * scale
                        );


                graphics.setColor(
                        colorForLine(
                                lineSegmentSet
                                        .getColor(i)
                        )
                );

                graphics.drawLine(
                        x1,
                        y1,
                        x2,
                        y2
                );
            }

        } finally {

            graphics.dispose();
        }

        return image;
    }


    /*
     * Orieditaの線色を
     * 展開図プレビュー色へ変換する。
     */
    private static Color colorForLine(
            LineColor color
    ) {

        return switch (color) {

            /*
             * 山折り。
             */
            case RED_1 ->
                    new Color(
                            220,
                            70,
                            70
                    );

            /*
             * 谷折り。
             */
            case BLUE_2 ->
                    new Color(
                            70,
                            95,
                            235
                    );

            /*
             * FLAT / 補助的な線。
             */
            case CYAN_3 ->
                    new Color(
                            165,
                            165,
                            165
                    );

            /*
             * 外周など。
             */
            default ->
                    new Color(
                            35,
                            35,
                            35
                    );
        };
    }


    /*
     * BufferedImage
     *      ↓
     * PNGメモリ
     *      ↓
     * Minecraft NativeImage
     *
     * と変換する。
     */
    private static NativeImage toNativeImage(
            BufferedImage image
    ) throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        if (!ImageIO.write(
                image,
                "png",
                output
        )) {

            throw new IOException(
                    "PNG writer was not found"
            );
        }

        try (
                ByteArrayInputStream input =
                        new ByteArrayInputStream(
                                output.toByteArray()
                        )
        ) {

            return NativeImage.read(
                    input
            );
        }
    }


    /*
     * 画面を閉じたときに
     * GPU側のDynamicTextureも解放する。
     */
    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed =
                true;

        Minecraft.getInstance()
                .getTextureManager()
                .release(
                        textureId
                );
    }
}