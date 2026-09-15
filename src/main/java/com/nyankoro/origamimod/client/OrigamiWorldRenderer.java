package com.nyankoro.origamimod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.origami.OrigamiFace;
import com.nyankoro.origamimod.origami.OrigamiFoldResult;
import com.nyankoro.origamimod.origami.OrigamiVertex;
import com.nyankoro.origamimod.origami.OrieditaAdapter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import com.nyankoro.origamimod.origami.OrigamiEdge;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

@EventBusSubscriber(
        value = Dist.CLIENT,
        modid = OrigamiMod.MODID
)
public final class OrigamiWorldRenderer {

    /*
     * Oriedita座標 → Minecraftブロック単位
     *
     * 現在はデバッグ用の固定値。
     * 将来的にはアイテムごとのscaleとして持たせる予定。
     */
    private static final double SCALE = 0.006;

    private static final double DEBUG_HEIGHT_OFFSET = 2.0;

    private static final double EDGE_WIDTH = 0.010;
    private static final float EDGE_OFFSET = 0.002F;
    private static final int EDGE_COLOR = 0xFF202020;

    /*
     * Orieditaによる折り畳み結果。
     */
    private static OrigamiFoldResult frontFoldResult;
    private static OrigamiFoldResult backFoldResult;

    /*
     * Minecraftワールド内での表示基準位置。
     *
     * 現在はゲーム開始後、
     * 最初にプレイヤーの約4ブロック前へ配置する。
     */
    private static Vec3 anchor;

    /*
     * 毎フレーム.cpを読み込まないようにするためのフラグ。
     */
    private static boolean loadAttempted = false;

    private OrigamiWorldRenderer() {
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(
            SubmitCustomGeometryEvent event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {
            return;
        }

        /*
         * 初回のみOrieditaで折り畳み計算を行う。
         */
        loadFoldResult();

        if (frontFoldResult == null
                || backFoldResult == null) {
            return;
        }

        /*
         * 初回のみMinecraft内の表示位置を決める。
         */
        if (anchor == null) {

            Vec3 look =
                    minecraft.player.getLookAngle();

            /*
             * 上下方向を無視して、
             * 水平方向だけを使用する。
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
             * プレイヤーから約4ブロック前。
             */
            Vec3 forwardPosition =
                    minecraft.player
                            .position()
                            .add(
                                    horizontalLook
                                            .scale(4.0)
                            );

            /*
             * 現在は地面に水平に表示。
             */
            anchor =
                    new Vec3(
                            forwardPosition.x,
                            minecraft.player.getY()
                                    + DEBUG_HEIGHT_OFFSET,
                            forwardPosition.z
                    );

            OrigamiMod.LOGGER.info(
                    "Origami render anchor = {}",
                    anchor
            );
        }

        /*
         * Minecraftのカメラ位置。
         */
        Vec3 camera =
                event
                        .getLevelRenderState()
                        .cameraRenderState
                        .pos;

        PoseStack poseStack =
                event.getPoseStack();

        poseStack.pushPose();

        /*
         * ワールド座標から
         * カメラ相対座標へ変換する。
         */
        poseStack.translate(
                anchor.x - camera.x,
                anchor.y - camera.y,
                anchor.z - camera.z
        );

        /*
         * Orieditaから取得した可視面を
         * 1面ずつMinecraftへ描画する。
         */

        /*
         * カメラが紙の上側にいる場合は正面、
         * 下側にいる場合は裏面を描画する。
         */
        boolean rearView =
                camera.y < anchor.y;

        OrigamiFoldResult visibleResult =
                rearView
                        ? backFoldResult
                        : frontFoldResult;

        /*
         * 境界線は、現在カメラがいる側へ
         * わずかに浮かせてZ-fightingを防ぐ。
         */
        float edgeOffset =
                rearView
                        ? -EDGE_OFFSET
                        : EDGE_OFFSET;

        renderFoldResult(
                event,
                poseStack,
                visibleResult,
                edgeOffset
        );

        poseStack.popPose();
    }

    /**
     * Oriedita座標の頂点を
     * Minecraft座標としてVertexConsumerへ渡す。
     */
    private static void addVertex(
            com.mojang.blaze3d.vertex.VertexConsumer consumer,
            com.mojang.blaze3d.vertex.PoseStack.Pose pose,
            OrigamiVertex vertex,
            int color
    ) {

        consumer
                .addVertex(
                        pose,

                        /*
                         * Oriedita X
                         *      ↓
                         * Minecraft X
                         */
                        (float) (
                                vertex.x()
                                        * SCALE
                        ),

                        /*
                         * 現在は完全な2D折り上がり図なので
                         * 高さ方向は0。
                         */
                        0.0F,

                        /*
                         * Oriedita Y
                         *      ↓
                         * Minecraft Z
                         *
                         * Minecraft座標との向きを合わせるため
                         * 符号を反転。
                         */
                        (float) (
                                -vertex.y()
                                        * SCALE
                        )
                )
                .setColor(color);
    }

    private static void renderVisibleEdges(
            SubmitCustomGeometryEvent event,
            PoseStack poseStack,
            OrigamiFoldResult result,
            float edgeOffset
    ) {

        for (OrigamiEdge edge :
                result.edges()) {

            renderEdge(
                    event,
                    poseStack,
                    edge.a(),
                    edge.b(),
                    edgeOffset
            );
        }
    }


    /**
     * .cpファイルを読み、
     * Orieditaで折り畳み計算を行う。
     *
     * 同時に段階別デバッグSVGを生成する。
     */
    private static void loadFoldResult() {

        /*
         * 一度ロードを試したら
         * 毎フレーム再実行しない。
         */
        if (loadAttempted) {
            return;
        }

        loadAttempted = true;

        /*
         * 同じCPを正面用・裏面用に2回読み込む。
         *
         * InputStreamは一度読み込むと再利用できないため、
         * 2つ別々に取得する。
         */
        try (
                InputStream frontInput =
                        Objects.requireNonNull(
                                OrigamiWorldRenderer.class
                                        .getResourceAsStream(
                                                "/assets/origamimod/origami/birdbase.cp"
                                        ),
                                "birdbase.cp was not found for front"
                        );

                InputStream backInput =
                        Objects.requireNonNull(
                                OrigamiWorldRenderer.class
                                        .getResourceAsStream(
                                                "/assets/origamimod/origami/birdbase.cp"
                                        ),
                                "birdbase.cp was not found for back"
                        )
        ) {

            /*
             * FRONT / BACKでデバッグファイルを
             * 上書きしないように保存先を分ける。
             */
            Path debugRoot =
                    Path.of(
                            "origami-debug"
                    );

            Path frontDebugDirectory =
                    debugRoot.resolve(
                            "front"
                    );

            Path backDebugDirectory =
                    debugRoot.resolve(
                            "back"
                    );

            /*
             * 正面から見た折り紙。
             */
            frontFoldResult =
                    OrieditaAdapter.fold(
                            frontInput,
                            frontDebugDirectory
                    );

            /*
             * 裏面から見た折り紙。
             */
            backFoldResult =
                    OrieditaAdapter.foldBack(
                            backInput,
                            backDebugDirectory
                    );

            OrigamiMod.LOGGER.info(
                    "Origami FRONT debug files written to {}",
                    frontDebugDirectory
                            .toAbsolutePath()
            );

            OrigamiMod.LOGGER.info(
                    "Origami BACK debug files written to {}",
                    backDebugDirectory
                            .toAbsolutePath()
            );

            OrigamiMod.LOGGER.info(
                    "Origami render data loaded. FRONT faces={}, BACK faces={}",
                    frontFoldResult
                            .faces()
                            .size(),
                    backFoldResult
                            .faces()
                            .size()
            );

        } catch (Exception e) {

            OrigamiMod.LOGGER.error(
                    "Failed to prepare origami render data",
                    e
            );
        }
    }

    private static void renderFoldResult(
            SubmitCustomGeometryEvent event,
            PoseStack poseStack,
            OrigamiFoldResult result,
            float edgeOffset
    ) {

        poseStack.pushPose();

        for (OrigamiFace face : result.faces()) {

            int color =
                    face.frontSideUp()
                            ? 0xFFFFC857
                            : 0xFF4EA5D9;

            try {

                for (PolygonTriangulator.Triangle triangle
                        : PolygonTriangulator.triangulate(
                        face.vertices()
                )) {

                    event.getSubmitNodeCollector()
                            .submitCustomGeometry(
                                    poseStack,
                                    RenderTypes.debugTriangleFan(),
                                    (pose, consumer) -> {

                                        addVertex(
                                                consumer,
                                                pose,
                                                triangle.a(),
                                                color
                                        );

                                        addVertex(
                                                consumer,
                                                pose,
                                                triangle.b(),
                                                color
                                        );

                                        addVertex(
                                                consumer,
                                                pose,
                                                triangle.c(),
                                                color
                                        );
                                    }
                            );
                }

            } catch (IllegalArgumentException e) {

                OrigamiMod.LOGGER.warn(
                        "Could not triangulate origami face {}",
                        face.faceId()
                );
            }

            /*
             * 三角形分割の辺ではなく、
             * SubFace本来の外周だけを描画する。
             */
        }

        renderVisibleEdges(
                event,
                poseStack,
                result,
                edgeOffset
        );

        poseStack.popPose();
    }

    private static void renderEdge(
            SubmitCustomGeometryEvent event,
            PoseStack poseStack,
            OrigamiVertex a,
            OrigamiVertex b,
            float yOffset
    ) {

        double x1 =
                a.x() * SCALE;

        double z1 =
                -a.y() * SCALE;

        double x2 =
                b.x() * SCALE;

        double z2 =
                -b.y() * SCALE;

        double dx =
                x2 - x1;

        double dz =
                z2 - z1;

        double length =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );

        if (length < 0.000001) {
            return;
        }

        double halfWidth =
                EDGE_WIDTH / 2.0;

        double offsetX =
                -dz / length
                        * halfWidth;

        double offsetZ =
                dx / length
                        * halfWidth;

        float p1x =
                (float) (x1 + offsetX);

        float p1z =
                (float) (z1 + offsetZ);

        float p2x =
                (float) (x1 - offsetX);

        float p2z =
                (float) (z1 - offsetZ);

        float p3x =
                (float) (x2 - offsetX);

        float p3z =
                (float) (z2 - offsetZ);

        float p4x =
                (float) (x2 + offsetX);

        float p4z =
                (float) (z2 + offsetZ);

        submitEdgeTriangle(
                event,
                poseStack,
                p1x, yOffset, p1z,
                p2x, yOffset, p2z,
                p3x, yOffset, p3z
        );

        submitEdgeTriangle(
                event,
                poseStack,
                p1x, yOffset, p1z,
                p3x, yOffset, p3z,
                p4x, yOffset, p4z
        );
    }

    private static void submitEdgeTriangle(
            SubmitCustomGeometryEvent event,
            PoseStack poseStack,

            float x1,
            float y1,
            float z1,

            float x2,
            float y2,
            float z2,

            float x3,
            float y3,
            float z3
    ) {

        event.getSubmitNodeCollector()
                .submitCustomGeometry(
                        poseStack,
                        RenderTypes.debugTriangleFan(),
                        (pose, consumer) -> {

                            consumer
                                    .addVertex(
                                            pose,
                                            x1,
                                            y1,
                                            z1
                                    )
                                    .setColor(
                                            EDGE_COLOR
                                    );

                            consumer
                                    .addVertex(
                                            pose,
                                            x2,
                                            y2,
                                            z2
                                    )
                                    .setColor(
                                            EDGE_COLOR
                                    );

                            consumer
                                    .addVertex(
                                            pose,
                                            x3,
                                            y3,
                                            z3
                                    )
                                    .setColor(
                                            EDGE_COLOR
                                    );
                        }
                );
    }
}