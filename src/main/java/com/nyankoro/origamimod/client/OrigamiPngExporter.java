package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.origami.OrigamiAppearance;
import com.nyankoro.origamimod.origami.OrigamiFoldResult;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;


/*
 * OrigamiFoldResultを
 * PNG画像として書き出す。
 *
 * 現段階では動作確認用。
 *
 * 将来的には、
 *
 * ・PNG byte[]
 * ・SHA-256 assetId
 * ・サーバー保存
 * ・クライアントキャッシュ
 *
 * へ発展させる。
 */
public final class OrigamiPngExporter {

    /*
     * 最初は512x512固定。
     *
     * Rasterizer内部ではさらに
     * SUPERSAMPLEされるため、
     * 最終画像はアンチエイリアス済みになる。
     */
    private static final int IMAGE_SIZE =
            512;


    private OrigamiPngExporter() {
    }


    public record ExportResult(
            Path frontPath,
            Path backPath
    ) {
    }


    /*
     * 表面・裏面をそれぞれPNG化する。
     *
     * 重要:
     * 回転や拡大率は画像へ焼き込まない。
     *
     * それらは将来、
     * OrigamiDisplayEntity側のTransformで
     * 自由に変更できるようにする。
     */
    public static ExportResult export(
            OrigamiFoldResult front,
            OrigamiFoldResult back,
            OrigamiAppearance appearance
    ) throws IOException {

        Path directory =
                Path.of(
                        "origami-debug",
                        "png-export"
                );


        Files.createDirectories(
                directory
        );


        Path frontPath =
                directory.resolve(
                        "front.png"
                );

        Path backPath =
                directory.resolve(
                        "back.png"
                );


        writePng(
                frontPath,
                front,
                appearance
        );


        writePng(
                backPath,
                back,
                appearance
        );


        return new ExportResult(
                frontPath.toAbsolutePath(),
                backPath.toAbsolutePath()
        );
    }


    private static void writePng(
            Path path,
            OrigamiFoldResult result,
            OrigamiAppearance appearance
    ) throws IOException {

        /*
         * 現在GUIで使っているRasterizerを
         * そのまま利用する。
         *
         * zoom = 1
         * angle = 0
         *
         * として標準姿勢の画像を生成する。
         */
        List<OrigamiPreviewRasterizer.Span> spans =
                OrigamiPreviewRasterizer.rasterize(
                        result,
                        IMAGE_SIZE,
                        IMAGE_SIZE,
                        1.0,
                        0.0,
                        appearance
                );


        BufferedImage image =
                new BufferedImage(
                        IMAGE_SIZE,
                        IMAGE_SIZE,
                        BufferedImage.TYPE_INT_ARGB
                );


        /*
         * BufferedImageの初期値は透明。
         *
         * Rasterizerが返したSpan部分だけ
         * ARGBを書き込む。
         */
        for (OrigamiPreviewRasterizer.Span span :
                spans) {

            int y =
                    span.y();


            if (y < 0
                    || y >= IMAGE_SIZE) {

                continue;
            }


            int xStart =
                    Math.max(
                            0,
                            span.xStart()
                    );

            int xEnd =
                    Math.min(
                            IMAGE_SIZE - 1,
                            span.xEnd()
                    );


            if (xStart > xEnd) {
                continue;
            }


            for (int x = xStart;
                 x <= xEnd;
                 x++) {

                image.setRGB(
                        x,
                        y,
                        span.color()
                );
            }
        }


        boolean written =
                ImageIO.write(
                        image,
                        "PNG",
                        path.toFile()
                );


        if (!written) {

            throw new IOException(
                    "PNG writer was not found"
            );
        }
    }
}