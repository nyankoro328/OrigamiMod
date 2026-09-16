package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.network.RequestOrigamiVisualAssetPayload;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

import net.minecraft.client.Minecraft;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.io.IOException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/*
 * ワールド内のOrigamiDisplayEntityから
 * 必要になったvisual assetを自動準備する。
 *
 * GPUロード済み
 *   -> 何もしない
 *
 * Client disk cacheあり
 *   -> GPUへロード
 *
 * Client cacheなし
 *   -> Serverへ1回だけ要求
 */
public final class OrigamiVisualAssetAutoLoader {

    /*
     * 同じAssetを複数Entityが同時に要求しても、
     * Serverへ何度もRequestを送らない。
     */
    private static final Set<String>
            SERVER_REQUESTED =
            ConcurrentHashMap.newKeySet();


    /*
     * Disk cacheからGPUへロードする処理も
     * 同時に複数回予約しない。
     */
    private static final Set<String>
            GPU_LOAD_QUEUED =
            ConcurrentHashMap.newKeySet();


    private OrigamiVisualAssetAutoLoader() {
    }


    public static void ensureAvailable(
            String visualAssetId
    ) {

        if (!OrigamiVisualAssetId.isValidFormat(
                visualAssetId
        )) {

            return;
        }


        /*
         * すでにGPUへロード済み。
         */
        if (OrigamiTextureCache.isLoaded(
                visualAssetId
        )) {

            SERVER_REQUESTED.remove(
                    visualAssetId
            );

            return;
        }


        /*
         * Client PCにPNGがある場合。
         */
        if (OrigamiVisualAssetClientCache.isCached(
                visualAssetId
        )) {

            queueGpuLoad(
                    visualAssetId
            );

            return;
        }


        /*
         * Client PCにも無い場合は
         * Serverへ要求する。
         *
         * Set#addがtrueの最初の1回だけ送る。
         */
        if (SERVER_REQUESTED.add(
                visualAssetId
        )) {

            ClientPacketDistributor.sendToServer(
                    new RequestOrigamiVisualAssetPayload(
                            visualAssetId
                    )
            );


            OrigamiMod.LOGGER.info(
                    "Automatically requested origami visual asset: {}",
                    visualAssetId
            );
        }
    }


    /*
     * ServerからAsset受信が完了したときに
     * Request中フラグを解除する。
     */
    public static void markDownloadComplete(
            String visualAssetId
    ) {

        SERVER_REQUESTED.remove(
                visualAssetId
        );
    }


    private static void queueGpuLoad(
            String visualAssetId
    ) {

        if (!GPU_LOAD_QUEUED.add(
                visualAssetId
        )) {

            return;
        }


        Minecraft minecraft =
                Minecraft.getInstance();


        /*
         * TextureManager操作を
         * Client thread上で実行する。
         */
        minecraft.execute(
                () -> {

                    try {

                        OrigamiTextureCache.TexturePair textures =
                                OrigamiTextureCache.getOrLoad(
                                        visualAssetId
                                );


                        if (textures == null) {

                            OrigamiMod.LOGGER.warn(
                                    "Could not load cached origami "
                                            + "visual asset: {}",
                                    visualAssetId
                            );
                        }


                    } catch (
                            IOException
                            | RuntimeException e
                    ) {

                        OrigamiMod.LOGGER.error(
                                "Failed to automatically load "
                                        + "origami GPU texture: {}",
                                visualAssetId,
                                e
                        );


                    } finally {

                        GPU_LOAD_QUEUED.remove(
                                visualAssetId
                        );
                    }
                }
        );
    }
}