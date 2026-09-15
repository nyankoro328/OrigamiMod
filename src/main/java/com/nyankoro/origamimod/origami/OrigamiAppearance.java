package com.nyankoro.origamimod.origami;

/*
 * 折り紙の見た目設定。
 *
 * ARGB形式:
 * 0xAARRGGBB
 */
public record OrigamiAppearance(
        int frontColor,
        int backColor,
        int edgeColor
) {

    public static final OrigamiAppearance DEFAULT =
            new OrigamiAppearance(
                    0xFFFFC857,
                    0xFF4EA5D9,
                    0xFF202020
            );
}