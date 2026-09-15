package com.nyankoro.origamimod.origami;

/*
 * Minecraft上での折り紙の配置情報。
 *
 * translation:
 *   基準位置からの移動量。
 *
 * rotation:
 *   XYZ各軸の回転角度（degree）。
 *
 * scale:
 *   基本サイズに対する倍率。
 */
public record OrigamiTransform(
        double translationX,
        double translationY,
        double translationZ,

        float rotationX,
        float rotationY,
        float rotationZ,

        float scale
) {

    public OrigamiTransform {

        if (!Double.isFinite(translationX)
                || !Double.isFinite(translationY)
                || !Double.isFinite(translationZ)) {

            throw new IllegalArgumentException(
                    "Translation must be finite"
            );
        }

        if (!Float.isFinite(rotationX)
                || !Float.isFinite(rotationY)
                || !Float.isFinite(rotationZ)) {

            throw new IllegalArgumentException(
                    "Rotation must be finite"
            );
        }

        if (!Float.isFinite(scale)
                || scale <= 0.0F) {

            throw new IllegalArgumentException(
                    "Scale must be positive"
            );
        }
    }
}