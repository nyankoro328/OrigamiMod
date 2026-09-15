package com.nyankoro.origamimod.item;

import net.minecraft.world.item.Item;


/*
 * ユーザーが作成した折り紙を表す汎用アイテム。
 *
 * 作品ごとに別Itemを登録するのではなく、
 * すべての折り紙作品でこのItemクラスを共有する。
 *
 * 現段階では専用クラス化のみ行い、
 * 壁掛け・装備・作品データ保存などは
 * 後の段階で追加する。
 */
public final class OrigamiItem
        extends Item {

    public OrigamiItem(
            Properties properties
    ) {

        super(
                properties
        );
    }
}