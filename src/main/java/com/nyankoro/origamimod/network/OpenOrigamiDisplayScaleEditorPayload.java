package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;


/*
 * Server -> Client
 *
 * 壁掛け折り紙の
 * 大きさ編集画面を開く。
 */
public record OpenOrigamiDisplayScaleEditorPayload(
        int entityId,
        float scale
) implements CustomPacketPayload {

    public static final Type<OpenOrigamiDisplayScaleEditorPayload>
            TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "open_origami_display_scale_editor"
                    )
            );


    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OpenOrigamiDisplayScaleEditorPayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    OpenOrigamiDisplayScaleEditorPayload::encode,
                    OpenOrigamiDisplayScaleEditorPayload::decode
            );


    private static void encode(
            RegistryFriendlyByteBuf buffer,
            OpenOrigamiDisplayScaleEditorPayload payload
    ) {

        buffer.writeVarInt(
                payload.entityId()
        );

        buffer.writeFloat(
                payload.scale()
        );
    }


    private static OpenOrigamiDisplayScaleEditorPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {

        return new OpenOrigamiDisplayScaleEditorPayload(
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
}