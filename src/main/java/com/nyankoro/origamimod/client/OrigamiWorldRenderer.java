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

import java.io.InputStream;
import java.util.Objects;

@EventBusSubscriber(
        value = Dist.CLIENT,
        modid = OrigamiMod.MODID
)
public final class OrigamiWorldRenderer {

    // Oriedita座標 → Minecraftブロック単位
    private static final double SCALE = 0.006;

    private static OrigamiFoldResult foldResult;

    // 最初に決めた表示位置
    private static Vec3 anchor;

    private static boolean loadAttempted = false;

    private OrigamiWorldRenderer() {
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(
            SubmitCustomGeometryEvent event
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null ||
                minecraft.level == null) {
            return;
        }

        loadFoldResult();

        if (foldResult == null) {
            return;
        }

        // 最初の1回だけ、プレイヤーの約4ブロック前を
        // 折り紙の表示位置にする
        if (anchor == null) {

            Vec3 look = minecraft.player.getLookAngle();

            Vec3 horizontalLook =
                    new Vec3(look.x, 0.0, look.z);

            if (horizontalLook.lengthSqr() < 0.000001) {
                horizontalLook =
                        new Vec3(0.0, 0.0, 1.0);
            }

            horizontalLook =
                    horizontalLook.normalize();

            Vec3 forwardPosition =
                    minecraft.player.position()
                            .add(horizontalLook.scale(4.0));

            anchor = new Vec3(
                    forwardPosition.x,
                    minecraft.player.getY() + 0.03,
                    forwardPosition.z
            );

            OrigamiMod.LOGGER.info(
                    "Origami render anchor = {}",
                    anchor
            );
        }

        Vec3 camera =
                event.getLevelRenderState()
                        .cameraRenderState
                        .pos;

        PoseStack poseStack =
                event.getPoseStack();

        poseStack.pushPose();

        // ワールド座標 → カメラ相対座標
        poseStack.translate(
                anchor.x - camera.x,
                anchor.y - camera.y,
                anchor.z - camera.z
        );

        for (OrigamiFace face : foldResult.faces()) {

            poseStack.pushPose();

            // 確認しやすいように表裏で色を変更
            int color = face.frontSideUp()
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

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void addVertex(
            com.mojang.blaze3d.vertex.VertexConsumer consumer,
            com.mojang.blaze3d.vertex.PoseStack.Pose pose,
            OrigamiVertex vertex,
            int color
    ) {
        consumer.addVertex(
                        pose,
                        (float) (vertex.x() * SCALE),
                        0.0F,
                        (float) (-vertex.y() * SCALE)
                )
                .setColor(color);
    }

    private static void loadFoldResult() {

        if (loadAttempted) {
            return;
        }

        loadAttempted = true;

        try (InputStream input =
                     Objects.requireNonNull(
                             OrigamiWorldRenderer.class
                                     .getResourceAsStream(
                                             "/assets/origamimod/origami/birdbase.cp"
                                     ),
                             "birdbase.cp was not found"
                     )) {

            foldResult =
                    OrieditaAdapter.fold(input);
            try {
                var svgPath =
                        com.nyankoro.origamimod.origami
                                .OrigamiDebugSvgExporter.export(
                                        foldResult,
                                        java.nio.file.Path.of(
                                                "origami-debug.svg"
                                        )
                                );

                OrigamiMod.LOGGER.info(
                        "Origami debug SVG written to {}",
                        svgPath
                );

            } catch (Exception e) {
                OrigamiMod.LOGGER.error(
                        "Failed to export origami debug SVG",
                        e
                );
            }

            OrigamiMod.LOGGER.info(
                    "Origami render data loaded. Faces={}",
                    foldResult.faces().size()
            );

        } catch (Exception e) {

            OrigamiMod.LOGGER.error(
                    "Failed to prepare origami render data",
                    e
            );
        }
    }
}