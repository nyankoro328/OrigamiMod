package com.nyankoro.origamimod.server;

import com.nyankoro.origamimod.origami.CreasePatternId;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;


/*
 * ユーザーが登録したCPを
 * ワールドフォルダへ保存する。
 *
 * <world>/
 *   origamimod/
 *     crease_patterns/
 *       <creasePatternId>/
 *         source.cp
 *         name.txt
 */
public final class CreasePatternStore {

    private static final String SOURCE_FILE =
            "source.cp";

    private static final String NAME_FILE =
            "name.txt";


    public static final int MAX_CP_BYTES =
            1024 * 1024;


    private static final Object WRITE_LOCK =
            new Object();


    private CreasePatternStore() {
    }


    public record SaveResult(
            boolean created,
            Path directory
    ) {
    }

    public record LibraryEntry(
            String creasePatternId,
            String fileName
    ) {
    }

    public static boolean exists(
            MinecraftServer server,
            String creasePatternId
    ) {

        validateId(
                creasePatternId
        );


        Path directory =
                patternDirectory(
                        server,
                        creasePatternId
                );


        return Files.isRegularFile(
                directory.resolve(
                        SOURCE_FILE
                )
        );
    }

    /*
     * 保存済みCPの一覧を取得する。
     *
     * CP本体は読み込まず、
     * IDと表示名だけ返す。
     */
    public static List<LibraryEntry> list(
            MinecraftServer server
    ) throws IOException {

        Path root =
                patternRoot(
                        server
                );


        if (!Files.isDirectory(
                root
        )) {

            return List.of();
        }


        List<LibraryEntry> entries =
                new ArrayList<>();


        try (
                Stream<Path> stream =
                        Files.list(
                                root
                        )
        ) {

            for (Path directory :
                    stream.toList()) {

                if (!Files.isDirectory(
                        directory
                )) {

                    continue;
                }


                String creasePatternId =
                        directory
                                .getFileName()
                                .toString();


                if (!CreasePatternId.isValidFormat(
                        creasePatternId
                )) {

                    continue;
                }


                Path sourcePath =
                        directory.resolve(
                                SOURCE_FILE
                        );

                Path namePath =
                        directory.resolve(
                                NAME_FILE
                        );


                if (!Files.isRegularFile(
                        sourcePath
                )
                        || !Files.isRegularFile(
                        namePath
                )) {

                    continue;
                }


                try {

                    long cpSize =
                            Files.size(
                                    sourcePath
                            );


                    if (cpSize <= 0
                            || cpSize > MAX_CP_BYTES) {

                        continue;
                    }


                    String fileName =
                            Files.readString(
                                            namePath,
                                            StandardCharsets.UTF_8
                                    )
                                    .strip();


                    if (fileName.isBlank()
                            || fileName.length() > 128
                            || !fileName
                            .toLowerCase(
                                    Locale.ROOT
                            )
                            .endsWith(
                                    ".cp"
                            )) {

                        continue;
                    }


                    entries.add(
                            new LibraryEntry(
                                    creasePatternId,
                                    fileName
                            )
                    );


                } catch (IOException ignored) {

                    /*
                     * 1件壊れていても、
                     * 他の正常なCPは一覧表示する。
                     */
                }
            }
        }


        entries.sort(
                Comparator
                        .comparing(
                                LibraryEntry::fileName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                        .thenComparing(
                                LibraryEntry::creasePatternId
                        )
        );


        return List.copyOf(
                entries
        );
    }

    public static SaveResult save(
            MinecraftServer server,
            String creasePatternId,
            String fileName,
            byte[] cpData
    ) throws IOException {

        validateId(
                creasePatternId
        );


        if (fileName == null
                || fileName.isBlank()) {

            throw new IllegalArgumentException(
                    "CP file name is empty"
            );
        }


        if (cpData == null
                || cpData.length == 0
                || cpData.length > MAX_CP_BYTES) {

            throw new IllegalArgumentException(
                    "Invalid CP data size"
            );
        }


        /*
         * Server側でもIDを再計算する。
         */
        String calculatedId =
                CreasePatternId.calculate(
                        cpData
                );


        if (!calculatedId.equals(
                creasePatternId
        )) {

            throw new IllegalArgumentException(
                    "creasePatternId mismatch"
            );
        }


        synchronized (WRITE_LOCK) {

            Path finalDirectory =
                    patternDirectory(
                            server,
                            creasePatternId
                    );


            /*
             * 同じCPが既に保存されていれば
             * 重複保存しない。
             */
            if (exists(
                    server,
                    creasePatternId
            )) {

                return new SaveResult(
                        false,
                        finalDirectory
                );
            }


            Path root =
                    patternRoot(
                            server
                    );


            Files.createDirectories(
                    root
            );


            Path temporaryDirectory =
                    Files.createTempDirectory(
                            root,
                            ".upload-"
                    );


            try {

                Files.write(
                        temporaryDirectory.resolve(
                                SOURCE_FILE
                        ),
                        cpData
                );


                Files.writeString(
                        temporaryDirectory.resolve(
                                NAME_FILE
                        ),
                        fileName,
                        StandardCharsets.UTF_8
                );


                try {

                    Files.move(
                            temporaryDirectory,
                            finalDirectory,
                            StandardCopyOption.ATOMIC_MOVE
                    );

                } catch (
                        AtomicMoveNotSupportedException e
                ) {

                    Files.move(
                            temporaryDirectory,
                            finalDirectory
                    );
                }


                return new SaveResult(
                        true,
                        finalDirectory
                );


            } catch (FileAlreadyExistsException e) {

                return new SaveResult(
                        false,
                        finalDirectory
                );


            } finally {

                cleanupTemporaryDirectory(
                        temporaryDirectory
                );
            }
        }
    }


    public static Path patternRoot(
            MinecraftServer server
    ) {

        return server
                .getWorldPath(
                        LevelResource.ROOT
                )
                .resolve(
                        "origamimod"
                )
                .resolve(
                        "crease_patterns"
                );
    }


    public static Path patternDirectory(
            MinecraftServer server,
            String creasePatternId
    ) {

        validateId(
                creasePatternId
        );


        return patternRoot(
                server
        )
                .resolve(
                        creasePatternId
                );
    }


    private static void validateId(
            String creasePatternId
    ) {

        if (!CreasePatternId.isValidFormat(
                creasePatternId
        )) {

            throw new IllegalArgumentException(
                    "Invalid creasePatternId"
            );
        }
    }


    private static void cleanupTemporaryDirectory(
            Path directory
    ) {

        try {

            Files.deleteIfExists(
                    directory.resolve(
                            SOURCE_FILE
                    )
            );

            Files.deleteIfExists(
                    directory.resolve(
                            NAME_FILE
                    )
            );

            Files.deleteIfExists(
                    directory
            );

        } catch (IOException ignored) {
        }
    }
}