package com.nyankoro.origamimod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import com.nyankoro.origamimod.OrigamiMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;


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

    /*
     * 折り紙アイテムをブロック面に使用したときの処理。
     *
     * 現段階では壁掛けそのものは生成せず、
     *
     * ・クリックしたブロック
     * ・クリックした面
     * ・正確なクリック位置
     *
     * を取得できることだけ確認する。
     */
    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {

        BlockPos clickedPos =
                context.getClickedPos();

        Direction clickedFace =
                context.getClickedFace();

        Vec3 clickLocation =
                context.getClickLocation();


        /*
         * メッセージとログはサーバー側だけで出す。
         *
         * クライアント側でもuseOnは呼ばれるため、
         * ServerPlayerで判定しないと
         * 二重処理の原因になり得る。
         */
        if (context.getPlayer()
                instanceof ServerPlayer player) {

            OrigamiMod.LOGGER.info(
                    "Origami item used on block: "
                            + "player={}, "
                            + "block={}, "
                            + "face={}, "
                            + "hit={}",
                    player.getName()
                            .getString(),
                    clickedPos,
                    clickedFace,
                    clickLocation
            );


            player.sendSystemMessage(
                    Component.literal(
                            "折り紙使用検出: "
                                    + clickedPos.getX()
                                    + ", "
                                    + clickedPos.getY()
                                    + ", "
                                    + clickedPos.getZ()
                                    + " / "
                                    + clickedFace
                    )
            );
        }


        return InteractionResult.SUCCESS;
    }
}