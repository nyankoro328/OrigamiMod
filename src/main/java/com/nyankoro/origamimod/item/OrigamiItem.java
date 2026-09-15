package com.nyankoro.origamimod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;


/*
 * ユーザーが作成した折り紙を表す汎用アイテム。
 *
 * 作品ごとに別Itemを登録するのではなく、
 * すべての折り紙作品でこのItemクラスを共有する。
 *
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


    /*
     * アイテムにマウスカーソルを合わせたときの説明。
     *
     * 現段階では作品固有データは表示せず、
     * OrigamiItem専用クラスが使用されていることを
     * 確認するための固定説明だけを表示する。
     */
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag
    ) {

        tooltipAdder.accept(
                Component.literal(
                                "Origami Modで作成した折り紙"
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );
    }
}