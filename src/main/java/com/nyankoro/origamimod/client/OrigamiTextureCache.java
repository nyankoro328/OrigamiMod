package com.nyankoro.origamimod.client;

import com.mojang.blaze3d.platform.NativeImage;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


/*
 * Clientディスクキャッシュ上のPNGを
 * MinecraftのGPU Textureへ変換して保持する。
 *
 * 同じvisualAssetIdは
 * 1度だけGPUへロードする。
 *
 * access-order LinkedHashMapを使用し、
 * 最近使われていないTextureから
 * 自動的に解放する。
 */
public final class OrigamiTextureCache {

    /*
     * GPU Textureに使用する
     * 推定メモリ量の上限。
     *
     * 現段階では128 MiB。
     */
    private static final long MAX_GPU_BYTES =
            128L
                    * 1024L
                    * 1024L;


    /*
     * accessOrder = true
     *
     * get()されたassetが
     * Mapの末尾へ移動する。
     */
    private static final LinkedHashMap<
            String,
            LoadedEntry
            >
            LOADED =
            new LinkedHashMap<>(
                    16,
                    0.75F,
                    true
            );


    private static long loadedGpuBytes =
            0L;


    private OrigamiTextureCache() {
    }


    /*
     * Rendererから使用する情報。
     *
     * front/backには
     * Minecraftに登録済みの
     * Texture Identifierが入る。
     */
    public record TexturePair(
            Identifier front,
            Identifier back,
            int width,
            int height
    ) {
    }


    private record LoadedEntry(
            TexturePair textures,
            long estimatedGpuBytes
    ) {
    }


    /*
     * visualAssetIdに対応するTextureを取得する。
     *
     * 既にGPUへロード済み:
     *   同じTexturePairを返す。
     *
     * ディスクにはあるがGPU未ロード:
     *   NativeImage -> DynamicTextureへ変換する。
     *
     * ディスクキャッシュ自体がない:
     *   nullを返す。
     */
    public static synchronized TexturePair getOrLoad(
            String visualAssetId
    ) throws IOException {

        if (!OrigamiVisualAssetId.isValidFormat(
                visualAssetId
        )) {

            throw new IllegalArgumentException(
                    "Invalid visualAssetId"
            );
        }


        LoadedEntry existing =
                LOADED.get(
                        visualAssetId
                );


        if (existing != null) {

            return existing.textures();
        }


        if (!OrigamiVisualAssetClientCache
                .isCached(
                        visualAssetId
                )) {

            return null;
        }


        Path frontPath =
                OrigamiVisualAssetClientCache
                        .frontPath(
                                visualAssetId
                        );

        Path backPath =
                OrigamiVisualAssetClientCache
                        .backPath(
                                visualAssetId
                        );


        NativeImage frontImage =
                readImage(
                        frontPath
                );

        NativeImage backImage =
                null;


        try {

            backImage =
                    readImage(
                            backPath
                    );


            if (frontImage.getWidth()
                    != backImage.getWidth()
                    || frontImage.getHeight()
                    != backImage.getHeight()) {

                throw new IOException(
                        "Front/back texture dimensions differ"
                );
            }


            int width =
                    frontImage.getWidth();

            int height =
                    frontImage.getHeight();


            /*
             * RGBA = 4 byte / pixel。
             *
             * 表裏2枚分を概算する。
             */
            long estimatedBytes =
                    Math.multiplyExact(
                            Math.multiplyExact(
                                    (long) width,
                                    height
                            ),
                            8L
                    );


            Identifier frontIdentifier =
                    createTextureIdentifier(
                            visualAssetId,
                            "front"
                    );

            Identifier backIdentifier =
                    createTextureIdentifier(
                            visualAssetId,
                            "back"
                    );


            /*
             * DynamicTextureがNativeImageの
             * 所有権を引き継ぐ。
             */
            DynamicTexture frontTexture =
                    new DynamicTexture(
                            () ->
                                    "Origami front "
                                            + visualAssetId,
                            frontImage
                    );


            DynamicTexture backTexture =
                    new DynamicTexture(
                            () ->
                                    "Origami back "
                                            + visualAssetId,
                            backImage
                    );


            /*
             * ここから先はDynamicTexture側が
             * NativeImageを管理するため、
             * finallyでcloseしない。
             */
            frontImage =
                    null;

            backImage =
                    null;


            Minecraft minecraft =
                    Minecraft.getInstance();

            TextureManager textureManager =
                    minecraft.getTextureManager();


            boolean frontRegistered =
                    false;

            boolean backRegistered =
                    false;


            try {

                textureManager.register(
                        frontIdentifier,
                        frontTexture
                );

                frontRegistered =
                        true;


                textureManager.register(
                        backIdentifier,
                        backTexture
                );

                backRegistered =
                        true;


            } catch (RuntimeException e) {

                /*
                 * 途中まで登録できていた場合も
                 * GPU Resourceを残さない。
                 */
                if (frontRegistered) {

                    textureManager.release(
                            frontIdentifier
                    );

                } else {

                    frontTexture.close();
                }


                if (backRegistered) {

                    textureManager.release(
                            backIdentifier
                    );

                } else {

                    backTexture.close();
                }


                throw e;
            }


            TexturePair pair =
                    new TexturePair(
                            frontIdentifier,
                            backIdentifier,
                            width,
                            height
                    );


            LOADED.put(
                    visualAssetId,
                    new LoadedEntry(
                            pair,
                            estimatedBytes
                    )
            );


            loadedGpuBytes =
                    Math.addExact(
                            loadedGpuBytes,
                            estimatedBytes
                    );


            /*
             * 上限を超えた場合は
             * 古いTextureから解放する。
             */
            evictIfNeeded(
                    visualAssetId
            );


            OrigamiMod.LOGGER.info(
                    "Loaded origami GPU textures: "
                            + "assetId={}, "
                            + "size={}x{}, "
                            + "estimated={} bytes, "
                            + "loadedAssets={}, "
                            + "loadedGpuBytes={}",
                    visualAssetId,
                    width,
                    height,
                    estimatedBytes,
                    LOADED.size(),
                    loadedGpuBytes
            );


            return pair;


        } finally {

            /*
             * DynamicTexture生成前に
             * エラーになったNativeImageだけ
             * ここで解放する。
             */
            if (frontImage != null) {

                frontImage.close();
            }


            if (backImage != null) {

                backImage.close();
            }
        }
    }

    /*
     * GPUへロード済みの場合だけ返す。
     *
     * Disk I/Oや新規Texture生成は行わないため、
     * Rendererから毎フレーム呼び出せる。
     *
     * LinkedHashMapがaccess-orderなので、
     * 使用中のTextureはLRU上でも新しい扱いになる。
     */
    public static synchronized TexturePair getIfLoaded(
            String visualAssetId
    ) {

        LoadedEntry entry =
                LOADED.get(
                        visualAssetId
                );


        if (entry == null) {

            return null;
        }


        return entry.textures();
    }


    public static synchronized boolean isLoaded(
            String visualAssetId
    ) {

        return LOADED.containsKey(
                visualAssetId
        );
    }


    public static synchronized int loadedAssetCount() {

        return LOADED.size();
    }


    public static synchronized long estimatedGpuBytes() {

        return loadedGpuBytes;
    }


    /*
     * 指定TextureだけGPUから解放する。
     */
    public static synchronized void release(
            String visualAssetId
    ) {

        LoadedEntry entry =
                LOADED.remove(
                        visualAssetId
                );


        if (entry == null) {

            return;
        }


        releaseTextures(
                entry
        );


        loadedGpuBytes =
                Math.max(
                        0L,
                        loadedGpuBytes
                                - entry.estimatedGpuBytes()
                );
    }


    /*
     * 全折り紙Textureを解放する。
     *
     * 将来、ログアウトやResource Reloadなどで
     * 使用できる。
     */
    public static synchronized void clear() {

        for (LoadedEntry entry :
                LOADED.values()) {

            releaseTextures(
                    entry
            );
        }


        LOADED.clear();

        loadedGpuBytes =
                0L;
    }


    private static void evictIfNeeded(
            String protectedAssetId
    ) {

        /*
         * 新しくロードしたasset自身は
         * この処理では削除しない。
         */
        while (loadedGpuBytes
                > MAX_GPU_BYTES
                && LOADED.size() > 1) {

            Iterator<
                    Map.Entry<
                            String,
                            LoadedEntry
                            >
                    >
                    iterator =
                    LOADED.entrySet()
                            .iterator();


            if (!iterator.hasNext()) {

                break;
            }


            Map.Entry<
                    String,
                    LoadedEntry
                    >
                    oldest =
                    iterator.next();


            /*
             * accessOrderなので通常、
             * protectedAssetは末尾にある。
             *
             * 念のため保護する。
             */
            if (oldest.getKey()
                    .equals(
                            protectedAssetId
                    )) {

                break;
            }


            LoadedEntry entry =
                    oldest.getValue();


            iterator.remove();


            releaseTextures(
                    entry
            );


            loadedGpuBytes =
                    Math.max(
                            0L,
                            loadedGpuBytes
                                    - entry
                                    .estimatedGpuBytes()
                    );


            OrigamiMod.LOGGER.info(
                    "Evicted origami GPU texture: "
                            + "assetId={}",
                    oldest.getKey()
            );
        }
    }


    private static void releaseTextures(
            LoadedEntry entry
    ) {

        TextureManager textureManager =
                Minecraft.getInstance()
                        .getTextureManager();


        textureManager.release(
                entry.textures()
                        .front()
        );


        textureManager.release(
                entry.textures()
                        .back()
        );
    }


    private static NativeImage readImage(
            Path path
    ) throws IOException {

        try (
                InputStream input =
                        Files.newInputStream(
                                path
                        )
        ) {

            return NativeImage.read(
                    input
            );
        }
    }


    private static Identifier createTextureIdentifier(
            String visualAssetId,
            String side
    ) {

        return Identifier.fromNamespaceAndPath(
                OrigamiMod.MODID,
                "dynamic/origami/"
                        + visualAssetId
                        + "/"
                        + side
        );
    }
}