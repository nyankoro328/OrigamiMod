package com.nyankoro.origamimod.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


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
                        "2"
                );


        /*
         * Client -> Server
         */
        registrar.playToServer(
                CreateOrigamiItemPayload.TYPE,
                CreateOrigamiItemPayload.STREAM_CODEC,
                CreateOrigamiItemPayload::handle
        );
    }
}