package com.nyankoro.origamimod.origami;

import com.nyankoro.origamimod.OrigamiMod;

import java.io.InputStream;
import java.util.Objects;

public final class OrieditaSmokeTest {

    private OrieditaSmokeTest() {
    }

    public static void run() {
        try (InputStream input = Objects.requireNonNull(
                OrieditaSmokeTest.class.getResourceAsStream(
                        "/assets/origamimod/origami/birdbase.cp"
                ),
                "birdbase.cp was not found"
        )) {

            OrigamiFoldResult result =
                    OrieditaAdapter.fold(input);

            OrigamiMod.LOGGER.info(
                    "===== Oriedita Minecraft fold test ====="
            );

            OrigamiMod.LOGGER.info(
                    "Face count = {}",
                    result.faces().size()
            );

            for (OrigamiFace face : result.faces()) {
                OrigamiMod.LOGGER.info(
                        "Order={}, Face={}, Side={}, Vertices={}",
                        face.renderOrder(),
                        face.faceId(),
                        face.frontSideUp() ? "FRONT" : "BACK",
                        face.vertices().size()
                );

                for (int i = 0;
                     i < face.vertices().size();
                     i++) {

                    OrigamiVertex vertex =
                            face.vertices().get(i);

                    OrigamiMod.LOGGER.info(
                            "  Vertex {} = ({}, {})",
                            i + 1,
                            vertex.x(),
                            vertex.y()
                    );
                }
            }

            OrigamiMod.LOGGER.info(
                    "===== Fold test finished ====="
            );

        } catch (Exception e) {
            OrigamiMod.LOGGER.error(
                    "Oriedita fold test failed",
                    e
            );
        }
    }
}