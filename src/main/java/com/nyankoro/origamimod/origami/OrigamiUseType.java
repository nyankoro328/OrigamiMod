package com.nyankoro.origamimod.origami;

/*
 * 完成した折り紙アイテムの用途。
 *
 * 1つの折り紙につき、基本的に1つだけ設定する。
 *
 * HEAD / CHEST / LEGS / FEET:
 * Minecraft標準装備に対応。
 *
 * LEFT_ARM / RIGHT_ARM / LEFT_LEG / RIGHT_LEG:
 * 将来的なCurios連携用。
 *
 * WALL:
 * 壁掛け専用。装備対象にはしない。
 */
public enum OrigamiUseType {

    WALL("壁掛け"),

    HEAD("頭"),
    CHEST("胴"),
    LEGS("脚"),
    FEET("足"),

    LEFT_ARM("左腕"),
    RIGHT_ARM("右腕"),

    LEFT_LEG("左脚"),
    RIGHT_LEG("右脚");

    private final String displayName;

    OrigamiUseType(
            String displayName
    ) {
        this.displayName =
                displayName;
    }

    public String displayName() {
        return displayName;
    }

    public OrigamiUseType next() {

        OrigamiUseType[] values =
                values();

        return values[
                (ordinal() + 1)
                        % values.length
                ];
    }
}