package com.nyankoro.origamimod.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.registration.HandlerThread;


/*
 * Origami Modのネットワーク登録。
 */
public final class OrigamiNetwork {

    private OrigamiNetwork() {
    }


    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {

        /*
         * パケット仕様を変更した場合は
         * このバージョンを上げる。
         */
        PayloadRegistrar registrar =
                event.registrar(
                        "6"
                );


        /*
         * Client -> Server
         */
        registrar.playToServer(
                CreateOrigamiItemPayload.TYPE,
                CreateOrigamiItemPayload.STREAM_CODEC,
                CreateOrigamiItemPayload::handle
        );
        /*
         * Server -> Client
         *
         * handler自体はClient専用イベント側で登録する。
         */
        registrar.playToClient(
                OrigamiVisualAssetDownloadChunkPayload.TYPE,
                OrigamiVisualAssetDownloadChunkPayload.STREAM_CODEC
        );
        /*
         * PNGチャンクの再構築・decode・hash計算は
         * main server tickを止めないよう
         * network threadで処理する。
         */
        PayloadRegistrar uploadRegistrar =
                registrar.executesOn(
                        HandlerThread.NETWORK
                );


        uploadRegistrar.playToServer(
                UploadOrigamiVisualAssetChunkPayload.TYPE,
                UploadOrigamiVisualAssetChunkPayload.STREAM_CODEC,
                UploadOrigamiVisualAssetChunkPayload::handle
        );

        /*
         * Client -> Server
         *
         * 保存済み画像を要求する。
         * Disk I/OがあるためNETWORK thread。
         */
        uploadRegistrar.playToServer(
                RequestOrigamiVisualAssetPayload.TYPE,
                RequestOrigamiVisualAssetPayload.STREAM_CODEC,
                RequestOrigamiVisualAssetPayload::handle
        );
    }
}