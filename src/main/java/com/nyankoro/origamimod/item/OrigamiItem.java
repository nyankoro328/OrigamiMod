package com.nyankoro.origamimod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;
import java.util.Locale;
import com.nyankoro.origamimod.origami.OrigamiItemData;

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

    private static final double WALL_RENDER_OFFSET =
            0.01;

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


        OrigamiItemData data =
                stack.getOrDefault(
                        OrigamiMod.ORIGAMI_DATA.get(),
                        OrigamiItemData.DEFAULT
                );


        tooltipAdder.accept(
                Component.literal(
                                "用途: "
                                        + data.useType()
                        )
                        .withStyle(
                                ChatFormatting.DARK_GRAY
                        )
        );


        if (data.hasVisualAsset()) {

            tooltipAdder.accept(
                    Component.literal(
                                    "画像ID: "
                                            + data
                                            .visualAssetId()
                                            .substring(
                                                    0,
                                                    12
                                            )
                            )
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            )
            );

        } else {

            tooltipAdder.accept(
                    Component.literal(
                                    "画像ID: 未設定"
                            )
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            )
            );
        }
    }

    /*
     * 折り紙アイテムをブロック面に使用したときの処理。
     *
     * 現段階では壁掛けそのものは生成しない。
     *
     * NORTH / SOUTH / EAST / WEST:
     *   壁掛け候補として受け付ける。
     *
     * UP / DOWN:
     *   床・天井なので拒否する。
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
         * 床と天井には壁掛けできない。
         */
        if (clickedFace == Direction.UP
                || clickedFace == Direction.DOWN) {

            if (context.getPlayer()
                    instanceof ServerPlayer player) {

                OrigamiMod.LOGGER.info(
                        "Rejected origami wall placement: "
                                + "player={}, "
                                + "block={}, "
                                + "face={}",
                        player.getName()
                                .getString(),
                        clickedPos,
                        clickedFace
                );


                player.sendSystemMessage(
                        Component.literal(
                                "折り紙は壁面にのみ設置できます"
                        )
                );
            }


            return InteractionResult.FAIL;
        }


        /*
         * 実際に折り紙を表示する予定位置。
         *
         * クリック位置そのものではなく、
         * 壁面から少しだけ外側へずらす。
         */
        Vec3 placementPosition =
                calculateWallPosition(
                        clickLocation,
                        clickedFace
                );


        /*
         * 壁の外側を向く水平角度。
         *
         * SOUTH =   0
         * WEST  =  90
         * NORTH = 180
         * EAST  = 270
         */
        float wallYaw =
                calculateWallYaw(
                        clickedFace
                );


        if (context.getPlayer()
                instanceof ServerPlayer player) {

            OrigamiMod.LOGGER.info(
                    "Origami wall placement calculated: "
                            + "player={}, "
                            + "block={}, "
                            + "face={}, "
                            + "position={}, "
                            + "yaw={}",
                    player.getName()
                            .getString(),
                    clickedPos,
                    clickedFace,
                    placementPosition,
                    wallYaw
            );


            player.sendSystemMessage(
                    Component.literal(
                            String.format(
                                    Locale.ROOT,
                                    "表示予定位置: %.3f, %.3f, %.3f / %s / yaw=%.0f",
                                    placementPosition.x(),
                                    placementPosition.y(),
                                    placementPosition.z(),
                                    clickedFace,
                                    wallYaw
                            )
                    )
            );
        }


        return InteractionResult.SUCCESS;
    }

    /*
     * クリック位置から、
     * 壁より少し外側の表示予定位置を求める。
     */
    private static Vec3 calculateWallPosition(
            Vec3 clickLocation,
            Direction face
    ) {

        return switch (face) {

            case NORTH ->
                    clickLocation.add(
                            0.0,
                            0.0,
                            -WALL_RENDER_OFFSET
                    );

            case SOUTH ->
                    clickLocation.add(
                            0.0,
                            0.0,
                            WALL_RENDER_OFFSET
                    );

            case EAST ->
                    clickLocation.add(
                            WALL_RENDER_OFFSET,
                            0.0,
                            0.0
                    );

            case WEST ->
                    clickLocation.add(
                            -WALL_RENDER_OFFSET,
                            0.0,
                            0.0
                    );

            default ->
                    clickLocation;
        };
    }


    /*
     * Minecraftの水平向きを
     * 0～360度のyawとして表す。
     */
    private static float calculateWallYaw(
            Direction face
    ) {

        return switch (face) {

            case SOUTH ->
                    0.0F;

            case WEST ->
                    90.0F;

            case NORTH ->
                    180.0F;

            case EAST ->
                    270.0F;

            default ->
                    0.0F;
        };
    }
}