package com.nyankoro.origamimod.origami;

import origami.crease_pattern.FoldingException;
import origami.crease_pattern.LineSegmentSet;
import origami.crease_pattern.PointSet;
import origami.folding.FoldedFigure;
import origami.folding.element.SubFace;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

public final class OrieditaSmokeTest {

    private OrieditaSmokeTest() {
    }

    public static void main(String[] args)
            throws Exception {

        System.out.println(
                "===== Standalone Oriedita core test ====="
        );

        printClassLocation(
                "FoldedFigure",
                FoldedFigure.class
        );

        printClassLocation(
                "LineSegmentSet",
                LineSegmentSet.class
        );

        printClassLocation(
                "PointSet",
                PointSet.class
        );

        printJarHash();

        Path debugDirectory =
                Path.of(
                        "standalone-origami-debug"
                );

        Files.createDirectories(
                debugDirectory
        );

        try (InputStream input =
                     Objects.requireNonNull(
                             OrieditaSmokeTest.class
                                     .getResourceAsStream(
                                             "/assets/origamimod/origami/birdbase.cp"
                                     ),
                             "birdbase.cp not found"
                     )) {

            /*
             * Minecraft側と全く同じCpLoaderを使用する。
             */
            CpLoader.LoadResult loaded =
                    CpLoader.loadForFolding(
                            input
                    );

            LineSegmentSet foldingInput =
                    loaded.foldingLineSegmentSet();

            System.out.println(
                    "rawCpLines = "
                            + loaded.rawLineCount()
            );

            System.out.println(
                    "foldingInputLines = "
                            + loaded.foldingLineCount()
            );

            /*
             * 入力を書き出す。
             */
            OrigamiDebugCpExporter.export(
                    foldingInput,
                    debugDirectory.resolve(
                            "01-folding-input.cp"
                    )
            );

            OrigamiDebugSvgExporter
                    .exportLineSegmentSet(
                            foldingInput,
                            debugDirectory.resolve(
                                    "01-folding-input.svg"
                            )
                    );

            /*
             * Minecraftを一切介さず、
             * Oriedita coreを直接実行する。
             */
            FoldedFigure foldedFigure =
                    new FoldedFigure(
                            new NoOpBulletinBoard()
                    );

            foldedFigure.estimationOrder =
                    FoldedFigure
                            .EstimationOrder
                            .ORDER_5;

            foldedFigure.folding_estimated(
                    foldingInput,
                    -1
            );

            PointSet foldedWire =
                    foldedFigure
                            .wireFrameWorker_foldedNotSubdivided
                            .get();

            PointSet subdivided =
                    foldedFigure
                            .wireFrameWorker_foldedSubdivided
                            .get();

            OrigamiDebugSvgExporter
                    .exportPointSetWireframe(
                            foldedWire,
                            debugDirectory.resolve(
                                    "02-folded-wire.svg"
                            )
                    );

            OrigamiDebugSvgExporter
                    .exportPointSetWireframe(
                            subdivided,
                            debugDirectory.resolve(
                                    "03-subdivided-wire.svg"
                            )
                    );

            /*
             * 上下関係も計算する。
             */
            SubFace[] subFaces =
                    foldedFigure
                            .foldedFigure_worker
                            .s0;

            int front = 0;
            int back = 0;

            for (int regionId = 1;
                 regionId <= subdivided.getNumFaces();
                 regionId++) {

                if (regionId >= subFaces.length) {
                    break;
                }

                SubFace subFace =
                        subFaces[regionId];

                if (subFace == null
                        || subFace.getFaceIdCount() == 0) {
                    continue;
                }

                subFace
                        .set_FaceId2fromTop_counted_position(
                                foldedFigure
                                        .foldedFigure_worker
                                        .hierarchyList
                        );

                int topFace =
                        subFace
                                .fromTop_count_FaceId(
                                        1
                                );

                int position =
                        foldedFigure
                                .wireFrameWorker_flatCp
                                .getIFacePosition(
                                        topFace
                                );

                if ((position % 2) == 1) {
                    front++;
                } else {
                    back++;
                }
            }

            System.out.println();
            System.out.println(
                    "actualStartingFaceId = "
                            + foldedFigure.ip3
            );

            System.out.println(
                    "estimationStep = "
                            + foldedFigure.estimationStep
            );

            System.out.println(
                    "discoveredFoldCases = "
                            + foldedFigure
                            .discovered_fold_cases
            );

            System.out.println();

            System.out.println(
                    "foldedWire:"
            );

            System.out.println(
                    "  points = "
                            + foldedWire
                            .getNumPoints()
            );

            System.out.println(
                    "  lines  = "
                            + foldedWire
                            .getNumLines()
            );

            System.out.println(
                    "  faces  = "
                            + foldedWire
                            .getNumFaces()
            );

            System.out.println();

            System.out.println(
                    "subdivided:"
            );

            System.out.println(
                    "  points = "
                            + subdivided
                            .getNumPoints()
            );

            System.out.println(
                    "  lines  = "
                            + subdivided
                            .getNumLines()
            );

            System.out.println(
                    "  faces  = "
                            + subdivided
                            .getNumFaces()
            );

            System.out.println();

            System.out.println(
                    "visible FRONT = "
                            + front
            );

            System.out.println(
                    "visible BACK  = "
                            + back
            );

            System.out.println();

            System.out.println(
                    "Debug output = "
                            + debugDirectory
                            .toAbsolutePath()
            );
        }
    }

    private static void printClassLocation(
            String name,
            Class<?> clazz
    ) {

        try {

            System.out.println(
                    name
                            + " = "
                            + clazz
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
            );

        } catch (Exception e) {

            System.out.println(
                    name
                            + " = unknown"
            );
        }
    }

    /**
     * 実際に使われているorigami JARのSHA-256を表示。
     */
    private static void printJarHash() {

        try {

            Path jarPath =
                    Path.of(
                            FoldedFigure.class
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .toURI()
                    );

            if (!Files.isRegularFile(
                    jarPath
            )) {

                System.out.println(
                        "origami SHA-256 = not a JAR"
                );

                return;
            }

            byte[] bytes =
                    Files.readAllBytes(
                            jarPath
                    );

            MessageDigest digest =
                    MessageDigest
                            .getInstance(
                                    "SHA-256"
                            );

            String sha256 =
                    HexFormat
                            .of()
                            .formatHex(
                                    digest.digest(
                                            bytes
                                    )
                            );

            System.out.println(
                    "origami SHA-256 = "
                            + sha256
            );

        } catch (Exception e) {

            System.out.println(
                    "origami SHA-256 = ERROR: "
                            + e.getMessage()
            );
        }
    }
}