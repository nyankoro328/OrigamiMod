package com.nyankoro.origamimod.server;

import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.stream.Stream;


/*
 * 検証済みの折り紙画像を
 * ワールドフォルダへ永続保存する。
 *
 * 保存形式:
 *
 * <world>/
 *   origamimod/
 *     visual_assets/
 *       <visualAssetId>/
 *         front.png
 *         back.png
 *
 * visualAssetIdはSHA-256形式なので、
 * ファイル名として安全な値のみ受け付ける。
 */
public final class OrigamiVisualAssetStore {

    private static final String FRONT_FILE =
            "front.png";

    private static final String BACK_FILE =
            "back.png";


    /*
     * サーバーのディスクを
     * 無制限に使用しないための仮上限。
     *
     * 後でconfig化できる。
     */
    private static final int MAX_STORED_ASSETS =
            4096;

    private static final long MAX_TOTAL_BYTES =
            256L
                    * 1024L
                    * 1024L;

    private static final int MAX_SINGLE_IMAGE_BYTES =
            1024 * 1024;


    /*
     * 複数network threadから
     * 同時保存されても壊れないようにする。
     */
    private static final Object WRITE_LOCK =
            new Object();


    private OrigamiVisualAssetStore() {
    }


    public record SaveResult(
            boolean created,
            Path directory
    ) {
    }


    private record StorageUsage(
            int assetCount,
            long totalBytes
    ) {
    }


    /*
     * 指定assetがディスク上に
     * 完全な状態で存在するか確認する。
     */
    public static boolean exists(
            MinecraftServer server,
            String visualAssetId
    ) {

        validateAssetId(
                visualAssetId
        );


        Path directory =
                assetDirectory(
                        server,
                        visualAssetId
                );


        return Files.isRegularFile(
                directory.resolve(
                        FRONT_FILE
                )
        )
                && Files.isRegularFile(
                directory.resolve(
                        BACK_FILE
                )
        );
    }

    /*
     * 保存済みの表裏PNGを読み込む。
     *
     * assetが存在しない場合はnull。
     */
    public static AssetData read(
            MinecraftServer server,
            String visualAssetId
    ) throws IOException {

        validateAssetId(
                visualAssetId
        );


        Path directory =
                assetDirectory(
                        server,
                        visualAssetId
                );


        Path frontPath =
                directory.resolve(
                        FRONT_FILE
                );

        Path backPath =
                directory.resolve(
                        BACK_FILE
                );


        if (!Files.isRegularFile(
                frontPath
        )
                || !Files.isRegularFile(
                backPath
        )) {

            return null;
        }


        long frontSize =
                Files.size(
                        frontPath
                );

        long backSize =
                Files.size(
                        backPath
                );


        if (frontSize <= 0
                || frontSize > MAX_SINGLE_IMAGE_BYTES
                || backSize <= 0
                || backSize > MAX_SINGLE_IMAGE_BYTES) {

            throw new IOException(
                    "Stored origami visual asset has invalid size"
            );
        }


        return new AssetData(
                Files.readAllBytes(
                        frontPath
                ),
                Files.readAllBytes(
                        backPath
                )
        );
    }


    /*
     * 検証済みPNGを永続保存する。
     *
     * まず一時ディレクトリへ2枚を書き、
     * 最後にディレクトリ単位で移動する。
     *
     * これにより、
     *
     * front.pngだけ存在する
     *
     * といった中途半端な状態を
     * できるだけ避ける。
     */
    public static SaveResult save(
            MinecraftServer server,
            String visualAssetId,
            byte[] frontPng,
            byte[] backPng
    ) throws IOException {

        validateAssetId(
                visualAssetId
        );


        if (frontPng == null
                || frontPng.length == 0
                || backPng == null
                || backPng.length == 0) {

            throw new IllegalArgumentException(
                    "PNG data must not be empty"
            );
        }


        synchronized (WRITE_LOCK) {

            Path assetDirectory =
                    assetDirectory(
                            server,
                            visualAssetId
                    );


            /*
             * 同じ画像が既に存在するなら
             * 新しく保存しない。
             */
            if (exists(
                    server,
                    visualAssetId
            )) {

                return new SaveResult(
                        false,
                        assetDirectory
                );
            }


            Path root =
                    assetRoot(
                            server
                    );


            Files.createDirectories(
                    root
            );


            /*
             * 永続保存前に
             * サーバー全体の画像容量を確認する。
             */
            StorageUsage usage =
                    calculateUsage(
                            root
                    );


            long newBytes =
                    (long) frontPng.length
                            + backPng.length;


            if (usage.assetCount()
                    >= MAX_STORED_ASSETS) {

                throw new IOException(
                        "Origami visual asset count limit reached"
                );
            }


            if (usage.totalBytes()
                    + newBytes
                    > MAX_TOTAL_BYTES) {

                throw new IOException(
                        "Origami visual asset storage quota exceeded"
                );
            }


            Path temporaryDirectory =
                    Files.createTempDirectory(
                            root,
                            ".upload-"
                    );


            try {

                Files.write(
                        temporaryDirectory.resolve(
                                FRONT_FILE
                        ),
                        frontPng
                );


                Files.write(
                        temporaryDirectory.resolve(
                                BACK_FILE
                        ),
                        backPng
                );


                /*
                 * 同一ファイルシステムなら
                 * atomic moveを優先する。
                 */
                try {

                    Files.move(
                            temporaryDirectory,
                            assetDirectory,
                            StandardCopyOption.ATOMIC_MOVE
                    );

                } catch (
                        AtomicMoveNotSupportedException e
                ) {

                    Files.move(
                            temporaryDirectory,
                            assetDirectory
                    );
                }


                return new SaveResult(
                        true,
                        assetDirectory
                );


            } catch (FileAlreadyExistsException e) {

                /*
                 * 別threadが同じassetを
                 * 先に保存した場合。
                 */
                return new SaveResult(
                        false,
                        assetDirectory
                );


            } finally {

                cleanupTemporaryDirectory(
                        temporaryDirectory
                );
            }
        }
    }

    public record AssetData(
            byte[] frontPng,
            byte[] backPng
    ) {

        public AssetData {

            frontPng =
                    frontPng.clone();

            backPng =
                    backPng.clone();
        }


        @Override
        public byte[] frontPng() {

            return frontPng.clone();
        }


        @Override
        public byte[] backPng() {

            return backPng.clone();
        }
    }


    /*
     * Origami Modの画像保存ルート。
     */
    public static Path assetRoot(
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
                        "visual_assets"
                );
    }


    public static Path assetDirectory(
            MinecraftServer server,
            String visualAssetId
    ) {

        validateAssetId(
                visualAssetId
        );


        return assetRoot(
                server
        )
                .resolve(
                        visualAssetId
                );
    }


    /*
     * 現在の保存容量を数える。
     *
     * .upload-xxxx の一時ディレクトリは
     * assetId形式ではないため無視される。
     */
    private static StorageUsage calculateUsage(
            Path root
    ) throws IOException {

        if (!Files.isDirectory(
                root
        )) {

            return new StorageUsage(
                    0,
                    0L
            );
        }


        int assetCount =
                0;

        long totalBytes =
                0L;


        try (
                Stream<Path> stream =
                        Files.list(
                                root
                        )
        ) {

            for (Path directory :
                    stream.toList()) {

                String name =
                        directory
                                .getFileName()
                                .toString();


                if (!OrigamiVisualAssetId
                        .isValidFormat(
                                name
                        )) {

                    continue;
                }


                Path front =
                        directory.resolve(
                                FRONT_FILE
                        );

                Path back =
                        directory.resolve(
                                BACK_FILE
                        );


                if (!Files.isRegularFile(
                        front
                )
                        || !Files.isRegularFile(
                        back
                )) {

                    continue;
                }


                assetCount++;


                long frontBytes =
                        Files.size(
                                front
                        );

                long backBytes =
                        Files.size(
                                back
                        );


                totalBytes =
                        Math.addExact(
                                totalBytes,
                                Math.addExact(
                                        frontBytes,
                                        backBytes
                                )
                        );
            }
        }


        return new StorageUsage(
                assetCount,
                totalBytes
        );
    }


    private static void validateAssetId(
            String visualAssetId
    ) {

        if (!OrigamiVisualAssetId.isValidFormat(
                visualAssetId
        )) {

            throw new IllegalArgumentException(
                    "Invalid visualAssetId"
            );
        }
    }


    private static void cleanupTemporaryDirectory(
            Path directory
    ) {

        try {

            Files.deleteIfExists(
                    directory.resolve(
                            FRONT_FILE
                    )
            );

            Files.deleteIfExists(
                    directory.resolve(
                            BACK_FILE
                    )
            );

            Files.deleteIfExists(
                    directory
            );

        } catch (IOException ignored) {

            /*
             * 一時ファイル掃除の失敗だけで
             * 保存済みassetを無効にはしない。
             */
        }
    }
}