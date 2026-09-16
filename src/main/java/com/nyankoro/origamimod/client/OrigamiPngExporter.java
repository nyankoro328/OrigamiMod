package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.origami.OrigamiAppearance;
import com.nyankoro.origamimod.origami.OrigamiFoldResult;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;


/*
 * OrigamiFoldResultを
 * 表裏PNGへ変換する。
 *
 * PNGファイルだけでなく、
 * マルチプレイ同期で使用するbyte[]と
 * visualAssetIdも生成する。
 */
public final class OrigamiPngExporter {

    /*
     * 現段階では512x512固定。
     */
    public static final int IMAGE_SIZE =
            512;


    /*
     * visualAssetIdの仕様バージョン。
     *
     * 将来、画像生成方式を大きく変更した場合は
     * この値を変更できる。
     */

    private OrigamiPngExporter() {
    }


    /*
     * PNG生成途中の内部データ。
     *
     * pixels:
     *   assetId計算用の正規化されたARGB。
     *
     * pngBytes:
     *   実際に保存・転送するPNG。
     */
    private record RenderedPng(
            int[] pixels,
            byte[] pngBytes
    ) {
    }


    /*
     * 外部へ返す生成結果。
     *
     * visualAssetId:
     *   折り紙作品そのもののIDではなく、
     *   表裏画像の見た目を識別するID。
     */
    public record ExportResult(
            String visualAssetId,
            byte[] frontPng,
            byte[] backPng,
            Path frontPath,
            Path backPath
    ) {

        public ExportResult {

            frontPng =
                    frontPng.clone();

            backPng =
                    backPng.clone();
        }


        /*
         * 呼び出し側から内部配列を
         * 書き換えられないようcloneを返す。
         */
        @Override
        public byte[] frontPng() {

            return frontPng.clone();
        }


        @Override
        public byte[] backPng() {

            return backPng.clone();
        }


        public int frontByteLength() {

            return frontPng.length;
        }


        public int backByteLength() {

            return backPng.length;
        }
    }


    /*
     * 表面・裏面をPNG化し、
     * visualAssetIdを生成する。
     *
     * デバッグ確認用として
     * PNGファイルもローカルへ保存する。
     */
    public static ExportResult export(
            OrigamiFoldResult front,
            OrigamiFoldResult back,
            OrigamiAppearance appearance
    ) throws IOException {

        RenderedPng frontRendered =
                renderPng(
                        front,
                        appearance
                );


        RenderedPng backRendered =
                renderPng(
                        back,
                        appearance
                );


        /*
         * PNG圧縮結果そのものではなく、
         * 正規化されたARGBピクセルから
         * assetIdを生成する。
         *
         * これによりPNGエンコーダの差ではなく、
         * 実際の見た目を基準に識別できる。
         */
        String visualAssetId =
                OrigamiVisualAssetId.calculate(
                        IMAGE_SIZE,
                        IMAGE_SIZE,
                        frontRendered.pixels(),
                        backRendered.pixels()
                );


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


        Files.write(
                frontPath,
                frontRendered.pngBytes()
        );


        Files.write(
                backPath,
                backRendered.pngBytes()
        );


        return new ExportResult(
                visualAssetId,
                frontRendered.pngBytes(),
                backRendered.pngBytes(),
                frontPath.toAbsolutePath(),
                backPath.toAbsolutePath()
        );
    }


    /*
     * OrigamiFoldResultから
     * 512x512 ARGB画像とPNG byte[]を生成する。
     */
    private static RenderedPng renderPng(
            OrigamiFoldResult result,
            OrigamiAppearance appearance
    ) throws IOException {

        /*
         * 現在GUIで使用しているRasterizerを
         * そのまま使用する。
         *
         * zoom = 1.0
         * angle = 0.0
         *
         * 回転・拡大率は画像へ焼き込まず、
         * 将来のDisplayEntity側で変更する。
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


        /*
         * 0x00000000 = 完全透明。
         */
        int[] pixels =
                new int[
                        IMAGE_SIZE
                                * IMAGE_SIZE
                        ];


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

                pixels[
                        y * IMAGE_SIZE
                                + x
                        ] =
                        span.color();
            }
        }


        BufferedImage image =
                new BufferedImage(
                        IMAGE_SIZE,
                        IMAGE_SIZE,
                        BufferedImage.TYPE_INT_ARGB
                );


        image.setRGB(
                0,
                0,
                IMAGE_SIZE,
                IMAGE_SIZE,
                pixels,
                0,
                IMAGE_SIZE
        );


        byte[] pngBytes;


        try (
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            boolean written =
                    ImageIO.write(
                            image,
                            "PNG",
                            output
                    );


            if (!written) {

                throw new IOException(
                        "PNG writer was not found"
                );
            }


            pngBytes =
                    output.toByteArray();
        }


        return new RenderedPng(
                pixels,
                pngBytes
        );
    }

}