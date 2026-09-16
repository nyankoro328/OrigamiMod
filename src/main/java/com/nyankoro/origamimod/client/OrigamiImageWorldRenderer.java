package com.nyankoro.origamimod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;


/*
 * PNG化された折り紙を
 * 軽量な1Quadとしてワールドへ表示する。
 *
 * 現段階ではDebug Preview専用。
 *
 * 将来的には
 * OrigamiDisplayEntityRendererへ
 * この描画処理を移す。
 */
@EventBusSubscriber(
        value = Dist.CLIENT,
        modid = OrigamiMod.MODID
)
public final class OrigamiImageWorldRenderer {

    /*
     * Debug表示時の横幅。
     *
     * 512x512なら縦も1.5 block。
     */
    private static final float PREVIEW_WIDTH =
            1.5F;


    /*
     * 現在表示するvisual asset。
     */
    private static String previewAssetId;


    /*
     * 折り紙中央のWorld座標。
     */
    private static Vec3 anchor;


    /*
     * 表面が向いている方向。
     */
    private static Vec3 frontNormal;


    /*
     * 表面から見たときの
     * 画像の右方向。
     */
    private static Vec3 frontRight;


    private OrigamiImageWorldRenderer() {
    }


    /*
     * プレイヤー約4 block前へ
     * Debug Previewを配置する。
     *
     * TextureがまだServerから届いていなくても
     * 配置情報自体は保持する。
     *
     * Texture受信後、自動的に表示される。
     */
    public static boolean showPreview(
            String visualAssetId
    ) {

        if (!OrigamiVisualAssetId.isValidFormat(
                visualAssetId
        )) {

            return false;
        }


        Minecraft minecraft =
                Minecraft.getInstance();


        if (minecraft.player == null
                || minecraft.level == null) {

            return false;
        }


        Vec3 look =
                minecraft.player
                        .getLookAngle();


        /*
         * 上下方向を無視する。
         */
        Vec3 horizontalLook =
                new Vec3(
                        look.x,
                        0.0,
                        look.z
                );


        if (horizontalLook.lengthSqr()
                < 0.000001) {

            horizontalLook =
                    new Vec3(
                            0.0,
                            0.0,
                            1.0
                    );
        }


        horizontalLook =
                horizontalLook.normalize();


        /*
         * プレイヤーから4 block前、
         * おおよそ胸～顔くらいの高さ。
         */
        Vec3 forward =
                minecraft.player
                        .position()
                        .add(
                                horizontalLook
                                        .scale(
                                                4.0
                                        )
                        );


        anchor =
                new Vec3(
                        forward.x,
                        minecraft.player.getY()
                                + 1.5,
                        forward.z
                );


        /*
         * 表面は、
         * Preview作成時のプレイヤー側を向く。
         */
        frontNormal =
                horizontalLook
                        .scale(
                                -1.0
                        )
                        .normalize();


        /*
         * 表面から見た画像の右方向。
         *
         * Y-up × frontNormal。
         */
        frontRight =
                new Vec3(
                        0.0,
                        1.0,
                        0.0
                )
                        .cross(
                                frontNormal
                        )
                        .normalize();


        previewAssetId =
                visualAssetId;


        OrigamiMod.LOGGER.info(
                "Prepared origami image preview: "
                        + "assetId={}, "
                        + "anchor={}, "
                        + "normal={}",
                previewAssetId,
                anchor,
                frontNormal
        );


        return true;
    }


    public static void clearPreview() {

        previewAssetId =
                null;

        anchor =
                null;

        frontNormal =
                null;

        frontRight =
                null;
    }


    @SubscribeEvent
    public static void onSubmitCustomGeometry(
            SubmitCustomGeometryEvent event
    ) {

        if (previewAssetId == null
                || anchor == null
                || frontNormal == null
                || frontRight == null) {

            return;
        }


        /*
         * Renderer内ではDisk I/Oをしない。
         *
         * GPUへ既に登録されているTextureのみ
         * 使用する。
         */
        OrigamiTextureCache.TexturePair textures =
                OrigamiTextureCache.getIfLoaded(
                        previewAssetId
                );


        if (textures == null) {

            return;
        }


        Vec3 camera =
                event
                        .getLevelRenderState()
                        .cameraRenderState
                        .pos;


        /*
         * カメラが表側か裏側か判定。
         */
        Vec3 toCamera =
                camera.subtract(
                        anchor
                );


        boolean rearView =
                toCamera.dot(
                        frontNormal
                ) < 0.0;


        Identifier texture =
                rearView
                        ? textures.back()
                        : textures.front();


        /*
         * PNGのaspect ratioを維持。
         */
        float width =
                PREVIEW_WIDTH;

        float height =
                PREVIEW_WIDTH
                        * (
                        (float) textures.height()
                                / textures.width()
                );


        float halfWidth =
                width * 0.5F;

        float halfHeight =
                height * 0.5F;


        PoseStack poseStack =
                event.getPoseStack();


        poseStack.pushPose();


        /*
         * World座標からCamera相対座標へ。
         */
        poseStack.translate(
                anchor.x - camera.x,
                anchor.y - camera.y,
                anchor.z - camera.z
        );


        /*
         * この1回のsubmitにつき、
         * 描画する頂点は4個だけ。
         */
        event.getSubmitNodeCollector()
                .submitCustomGeometry(
                        poseStack,
                        RenderTypes.entityTranslucent(
                                texture
                        ),
                        (pose, consumer) ->
                                renderQuad(
                                        consumer,
                                        pose,
                                        halfWidth,
                                        halfHeight,
                                        rearView
                                )
                );


        poseStack.popPose();
    }


    /*
     * Cameraから見えている側だけを
     * Quad 1枚として描画する。
     *
     * 表側ではfront.png、
     * 裏側ではback.pngを使用する。
     *
     * 裏側から見たときの左右反転は
     * Quadを反対側から見ることで
     * 自然に発生するため、
     * UV自体は反転しない。
     */
    private static void renderQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float halfWidth,
            float halfHeight,
            boolean rearView
    ) {

        float normalSign =
                rearView
                        ? -1.0F
                        : 1.0F;


        float normalX =
                (float) (
                        frontNormal.x
                                * normalSign
                );

        float normalY =
                0.0F;

        float normalZ =
                (float) (
                        frontNormal.z
                                * normalSign
                );


        /*
         * 表裏とも同じUVを使用する。
         *
         * 裏側から見るとQuad自体が
         * 視点上で自然に左右反転するため、
         * ここで追加のU反転は行わない。
         */
        float leftU =
                0.0F;

        float rightU =
                1.0F;


        /*
         * 左下
         */
        addVertex(
                consumer,
                pose,
                -halfWidth,
                -halfHeight,
                leftU,
                1.0F,
                normalX,
                normalY,
                normalZ
        );


        /*
         * 右下
         */
        addVertex(
                consumer,
                pose,
                halfWidth,
                -halfHeight,
                rightU,
                1.0F,
                normalX,
                normalY,
                normalZ
        );


        /*
         * 右上
         */
        addVertex(
                consumer,
                pose,
                halfWidth,
                halfHeight,
                rightU,
                0.0F,
                normalX,
                normalY,
                normalZ
        );


        /*
         * 左上
         */
        addVertex(
                consumer,
                pose,
                -halfWidth,
                halfHeight,
                leftU,
                0.0F,
                normalX,
                normalY,
                normalZ
        );
    }


    /*
     * local XをfrontRightへ変換して
     * Quad頂点を作る。
     *
     * local YはMinecraft Yそのまま。
     */
    private static void addVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float localX,
            float localY,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ
    ) {

        float x =
                (float) (
                        frontRight.x
                                * localX
                );

        float z =
                (float) (
                        frontRight.z
                                * localX
                );


        consumer
                .addVertex(
                        pose,
                        x,
                        localY,
                        z
                )
                .setColor(
                        0xFFFFFFFF
                )
                .setUv(
                        u,
                        v
                )
                .setOverlay(
                        OverlayTexture.NO_OVERLAY
                )
                .setLight(
                        LightCoordsUtil.FULL_BRIGHT
                )
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }
}