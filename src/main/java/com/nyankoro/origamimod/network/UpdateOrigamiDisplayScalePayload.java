package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.entity.OrigamiDisplayEntity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.Entity;

import net.neoforged.neoforge.network.handling.IPayloadContext;


/*
 * Client -> Server
 *
 * 設置済み折り紙の大きさを変更する。
 */
public record UpdateOrigamiDisplayScalePayload(
        int entityId,
        float scale
) implements CustomPacketPayload {

    public static final Type<UpdateOrigamiDisplayScalePayload>
            TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "update_origami_display_scale"
                    )
            );


    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            UpdateOrigamiDisplayScalePayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    UpdateOrigamiDisplayScalePayload::encode,
                    UpdateOrigamiDisplayScalePayload::decode
            );


    private static void encode(
            RegistryFriendlyByteBuf buffer,
            UpdateOrigamiDisplayScalePayload payload
    ) {

        buffer.writeVarInt(
                payload.entityId()
        );

        buffer.writeFloat(
                payload.scale()
        );
    }


    private static UpdateOrigamiDisplayScalePayload decode(
            RegistryFriendlyByteBuf buffer
    ) {

        return new UpdateOrigamiDisplayScalePayload(
                buffer.readVarInt(),
                buffer.readFloat()
        );
    }


    @Override
    public Type<
            ? extends CustomPacketPayload
            > type() {

        return TYPE;
    }


    public static void handle(
            UpdateOrigamiDisplayScalePayload payload,
            IPayloadContext context
    ) {

        if (!(context.player()
                instanceof ServerPlayer player)) {

            return;
        }


        /*
         * NaNやInfinityを拒否する。
         */
        if (!Float.isFinite(
                payload.scale()
        )) {

            return;
        }


        /*
         * UI側と同じ範囲だけ許可する。
         */
        if (payload.scale() < 0.1F
                || payload.scale() > 10.0F) {

            return;
        }


        Entity entity =
                player.level()
                        .getEntity(
                                payload.entityId()
                        );


        if (!(entity
                instanceof OrigamiDisplayEntity displayEntity)) {

            return;
        }


        /*
         * 遠距離からEntity IDだけ指定して
         * 編集することを防ぐ。
         *
         * 8 block以内。
         */
        if (player.distanceToSqr(
                displayEntity
        ) > 64.0) {

            return;
        }


        displayEntity.setDisplayScale(
                payload.scale()
        );
    }
}