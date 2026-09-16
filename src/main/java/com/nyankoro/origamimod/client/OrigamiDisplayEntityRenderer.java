package com.nyankoro.origamimod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.nyankoro.origamimod.entity.OrigamiDisplayEntity;
import com.nyankoro.origamimod.origami.OrigamiItemData;

import net.minecraft.client.renderer.SubmitNodeCollector;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import net.minecraft.client.renderer.rendertype.RenderTypes;

import net.minecraft.client.renderer.state.level.CameraRenderState;

import net.minecraft.client.renderer.texture.OverlayTexture;

import net.minecraft.core.Direction;

import net.minecraft.resources.Identifier;

import net.minecraft.world.phys.Vec3;


/*
 * OrigamiDisplayEntityを
 * front/back PNGの1Quadとして描画する。
 *
 * 折り畳みPolygonは描画しない。
 */
public final class OrigamiDisplayEntityRenderer
        extends EntityRenderer<
        OrigamiDisplayEntity,
        OrigamiDisplayEntityRenderer.RenderState
        > {

    /*
     * 現在の初期表示幅。
     *
     * 後でEntityごとのscaleへ変更する。
     */
    private static final float DEFAULT_WIDTH =
            1.5F;


    public OrigamiDisplayEntityRenderer(
            EntityRendererProvider.Context context
    ) {

        super(
                context
        );


        this.shadowRadius =
                0.0F;
    }


    /*
     * Entityから描画threadへ渡すための
     * RenderState。
     */
    public static final class RenderState
            extends EntityRenderState {

        public String visualAssetId =
                OrigamiItemData.UNASSIGNED;


        public Direction wallFace =
                Direction.SOUTH;


        public Identifier frontTexture;

        public Identifier backTexture;


        public int textureWidth =
                1;

        public int textureHeight =
                1;

        public float displayScale =
                1.0F;


        public float horizontalOffset =
                0.0F;


        public float verticalOffset =
                0.0F;


        public float depthOffset =
                0.0F;

        public boolean backSideOutward =
                false;


        public int displayAngle =
                0;
    }


    @Override
    public RenderState createRenderState() {

        return new RenderState();
    }


    /*
     * Entityの同期データを
     * RenderStateへコピーする。
     */
    @Override
    public void extractRenderState(
            OrigamiDisplayEntity entity,
            RenderState state,
            float partialTicks
    ) {

        /*
         * x / y / z / lightCoordsなど、
         * EntityRenderStateの標準情報を設定する。
         */
        super.extractRenderState(
                entity,
                state,
                partialTicks
        );


        state.visualAssetId =
                entity.getVisualAssetId();


        state.wallFace =
                entity.getWallFace();

        state.displayScale =
                entity.getDisplayScale();


        state.horizontalOffset =
                entity.getHorizontalOffset();


        state.verticalOffset =
                entity.getVerticalOffset();


        state.depthOffset =
                entity.getDepthOffset();


        /*
         * 前フレームのTexture情報を
         * 必ずリセットする。
         */
        state.frontTexture =
                null;

        state.backTexture =
                null;

        state.textureWidth =
                1;

        state.textureHeight =
                1;

        state.backSideOutward =
                entity.isBackSideOutward();


        state.displayAngle =
                entity.getDisplayAngle();


        if (!entity.hasVisualAsset()) {

            return;
        }


        OrigamiTextureCache.TexturePair textures =
                OrigamiTextureCache.getIfLoaded(
                        state.visualAssetId
                );


        /*
         * Textureがまだ無い場合、
         * Client cacheまたはServerから
         * 自動準備する。
         */
        if (textures == null) {

            OrigamiVisualAssetAutoLoader
                    .ensureAvailable(
                            state.visualAssetId
                    );

            return;
        }


        state.frontTexture =
                textures.front();

        state.backTexture =
                textures.back();

        state.textureWidth =
                textures.width();

        state.textureHeight =
                textures.height();
    }


    /*
     * 実際の描画。
     *
     * EntityRendererへ渡されるPoseStackは
     * Entity位置を基準にしているため、
     * World座標へのtranslateは不要。
     */
    @Override
    public void submit(
            RenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {

        if (state.frontTexture == null
                || state.backTexture == null) {

            return;
        }


        /*
         * 壁の外側方向 =
         * 折り紙の表面Normal。
         */
        Vec3 frontNormal =
                new Vec3(
                        state.wallFace
                                .getStepX(),
                        0.0,
                        state.wallFace
                                .getStepZ()
                );


        /*
         * Entity位置からCamera方向。
         */
        Vec3 toCamera =
                new Vec3(
                        camera.pos.x
                                - state.x,
                        camera.pos.y
                                - state.y,
                        camera.pos.z
                                - state.z
                );


        /*
         * Normalと反対側にCameraがいれば
         * 裏面。
         */
        boolean rearView =
                toCamera.dot(
                        frontNormal
                ) < 0.0;


        /*
         * outward側にどちらの面を表示するかを反映する。
         *
         * backSideOutward=false:
         *   外側 = front
         *   裏側 = back
         *
         * backSideOutward=true:
         *   外側 = back
         *   裏側 = front
         */
        boolean useBackTexture =
                rearView
                        ^ state.backSideOutward;


        Identifier texture =
                useBackTexture
                        ? state.backTexture
                        : state.frontTexture;


        /*
         * frontから見た右方向。
         *
         * Y-up × frontNormal
         */
        Vec3 frontRight =
                new Vec3(
                        0.0,
                        1.0,
                        0.0
                )
                        .cross(
                                frontNormal
                        )
                        .normalize();


        /*
         * PNGのAspect Ratioを維持する。
         */
        float width =
                DEFAULT_WIDTH
                        * state.displayScale;


        float height =
                DEFAULT_WIDTH
                        * state.displayScale
                        * (
                        (float) state.textureHeight
                                / state.textureWidth
                );


        float halfWidth =
                width
                        * 0.5F;


        float halfHeight =
                height
                        * 0.5F;

        /*
         * Entityのpositionは当たり判定の底面中央。
         *
         * Quadは中心基準で作っているため、
         * 半分の高さだけ上へ移動して
         * Entityの当たり判定中央へ合わせる。
         */
        poseStack.pushPose();


        poseStack.translate(
                0.0F,
                halfHeight,
                0.0F
        );


        /*
         * クリック位置を画像中央として描画する。
         *
         * 現在EntityのHitboxはY方向に
         * 0～1.5なのでVisualとは少しずれる。
         *
         * Hitboxは編集UI実装時に
         * Visualへ合わせる。
         */
        submitNodeCollector
                .submitCustomGeometry(
                        poseStack,
                        RenderTypes.entityTranslucent(
                                texture
                        ),
                        (pose, consumer) ->
                                renderQuad(
                                        consumer,
                                        pose,
                                        frontRight,
                                        frontNormal,
                                        halfWidth,
                                        halfHeight,
                                        rearView,
                                        state.displayAngle,
                                        state.lightCoords
                                )
                );


        poseStack.popPose();
    }


    /*
     * Cameraから見えている側だけ
     * 4頂点のQuadとして描画する。
     */
    private static void renderQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 frontRight,
            Vec3 frontNormal,
            float halfWidth,
            float halfHeight,
            boolean rearView,
            int displayAngle,
            int lightCoords
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
         * 表裏とも同じUV。
         *
         * 裏側へ回り込んだ際の左右反転は
         * Quadを反対側から見ることで
         * 自然に発生する。
         */
        addVertex(
                consumer,
                pose,
                frontRight,
                -halfWidth,
                -halfHeight,
                0.0F,
                1.0F,
                normalX,
                normalY,
                normalZ,
                displayAngle,
                lightCoords
        );


        addVertex(
                consumer,
                pose,
                frontRight,
                halfWidth,
                -halfHeight,
                1.0F,
                1.0F,
                normalX,
                normalY,
                normalZ,
                displayAngle,
                lightCoords
        );


        addVertex(
                consumer,
                pose,
                frontRight,
                halfWidth,
                halfHeight,
                1.0F,
                0.0F,
                normalX,
                normalY,
                normalZ,
                displayAngle,
                lightCoords
        );


        addVertex(
                consumer,
                pose,
                frontRight,
                -halfWidth,
                halfHeight,
                0.0F,
                0.0F,
                normalX,
                normalY,
                normalZ,
                displayAngle,
                lightCoords
        );
    }


    private static void addVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 frontRight,
            float localX,
            float localY,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ,
            int displayAngle,
            int lightCoords
    ) {

        /*
         * Settings画面のAngleと
         * 見た目の回転方向を合わせる。
         *
         * GUIではY軸が下向きなので、
         * MinecraftのY-up座標では
         * angleの符号を反転して適用する。
         */
        double radians =
                Math.toRadians(
                        -displayAngle
                );


        double cos =
                Math.cos(
                        radians
                );

        double sin =
                Math.sin(
                        radians
                );


        float rotatedX =
                (float) (
                        localX
                                * cos
                                - localY
                                * sin
                );


        float rotatedY =
                (float) (
                        localX
                                * sin
                                + localY
                                * cos
                );

        /*
         * Quadのlocal Xを
         * 壁面方向へ変換する。
         */
        float x =
                (float) (
                        frontRight.x
                                * rotatedX
                );

        float z =
                (float) (
                        frontRight.z
                                * rotatedX
                );


        consumer
                .addVertex(
                        pose,
                        x,
                        rotatedY,
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
                        lightCoords
                )
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }
}