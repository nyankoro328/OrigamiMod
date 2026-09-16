package com.nyankoro.origamimod.origami;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HexFormat;


/*
 * 折り紙の描画用画像を識別するID。
 *
 * 作品そのものを識別するorigamiIdとは別物。
 *
 * 同じ表裏画像なら、
 * 同じvisualAssetIdになる。
 */
public final class OrigamiVisualAssetId {

    private static final String VERSION =
            "origamimod-visual-asset-v1";


    private OrigamiVisualAssetId() {
    }


    public static String calculate(
            int width,
            int height,
            int[] frontPixels,
            int[] backPixels
    ) {

        if (width <= 0
                || height <= 0) {

            throw new IllegalArgumentException(
                    "Invalid image size"
            );
        }


        long expectedPixelCount =
                (long) width
                        * height;


        if (expectedPixelCount
                > Integer.MAX_VALUE) {

            throw new IllegalArgumentException(
                    "Image is too large"
            );
        }


        if (frontPixels == null
                || backPixels == null
                || frontPixels.length
                != expectedPixelCount
                || backPixels.length
                != expectedPixelCount) {

            throw new IllegalArgumentException(
                    "Invalid pixel array size"
            );
        }


        final MessageDigest digest;


        try {

            digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 is not available",
                    e
            );
        }


        digest.update(
                VERSION.getBytes(
                        StandardCharsets.UTF_8
                )
        );


        /*
         * 現在のvisualAssetId仕様と
         * 同じ並びを維持する。
         */
        updateInt(
                digest,
                width
        );

        updateInt(
                digest,
                height
        );


        digest.update(
                (byte) 1
        );

        updatePixels(
                digest,
                frontPixels
        );


        digest.update(
                (byte) 2
        );

        updatePixels(
                digest,
                backPixels
        );


        return HexFormat.of()
                .formatHex(
                        digest.digest()
                );
    }


    public static boolean isValidFormat(
            String visualAssetId
    ) {

        if (visualAssetId == null
                || visualAssetId.length()
                != 64) {

            return false;
        }


        for (int i = 0;
             i < visualAssetId.length();
             i++) {

            char c =
                    visualAssetId.charAt(
                            i
                    );


            boolean digit =
                    c >= '0'
                            && c <= '9';

            boolean lowerHex =
                    c >= 'a'
                            && c <= 'f';


            if (!digit
                    && !lowerHex) {

                return false;
            }
        }


        return true;
    }


    private static void updatePixels(
            MessageDigest digest,
            int[] pixels
    ) {

        updateInt(
                digest,
                pixels.length
        );


        for (int pixel :
                pixels) {

            updateInt(
                    digest,
                    pixel
            );
        }
    }


    private static void updateInt(
            MessageDigest digest,
            int value
    ) {

        digest.update(
                (byte) (
                        value >>> 24
                )
        );

        digest.update(
                (byte) (
                        value >>> 16
                )
        );

        digest.update(
                (byte) (
                        value >>> 8
                )
        );

        digest.update(
                (byte) value
        );
    }
}