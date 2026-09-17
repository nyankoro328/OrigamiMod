package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.origami.CreasePatternId;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;


/*
 * Server -> Client
 *
 * 保存済みCPの一覧。
 */
public record CreasePatternListPayload(
        boolean success,
        String message,
        List<Entry> entries
) implements CustomPacketPayload {

    public static final int MAX_ENTRIES =
            256;

    public static final int MAX_FILE_NAME_LENGTH =
            128;

    public static final int MAX_MESSAGE_LENGTH =
            160;


    public record Entry(
            String creasePatternId,
            String fileName
    ) {
    }


    public CreasePatternListPayload {

        entries =
                List.copyOf(
                        entries
                );
    }


    public static final Type<CreasePatternListPayload>
            TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "crease_pattern_list"
                    )
            );


    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            CreasePatternListPayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    CreasePatternListPayload::encode,
                    CreasePatternListPayload::decode
            );


    private static void encode(
            RegistryFriendlyByteBuf buffer,
            CreasePatternListPayload payload
    ) {

        buffer.writeBoolean(
                payload.success()
        );

        buffer.writeUtf(
                payload.message()
        );


        buffer.writeVarInt(
                payload.entries().size()
        );


        for (Entry entry :
                payload.entries()) {

            buffer.writeUtf(
                    entry.creasePatternId()
            );

            buffer.writeUtf(
                    entry.fileName()
            );
        }
    }


    private static CreasePatternListPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {

        boolean success =
                buffer.readBoolean();

        String message =
                buffer.readUtf(
                        MAX_MESSAGE_LENGTH
                );

        int count =
                buffer.readVarInt();


        if (count < 0
                || count > MAX_ENTRIES) {

            throw new IllegalArgumentException(
                    "Invalid crease pattern list size"
            );
        }


        List<Entry> entries =
                new ArrayList<>(
                        count
                );


        for (int i = 0;
             i < count;
             i++) {

            String creasePatternId =
                    buffer.readUtf(
                            64
                    );

            String fileName =
                    buffer.readUtf(
                            MAX_FILE_NAME_LENGTH
                    );


            if (!CreasePatternId.isValidFormat(
                    creasePatternId
            )) {

                throw new IllegalArgumentException(
                        "Invalid creasePatternId"
                );
            }


            entries.add(
                    new Entry(
                            creasePatternId,
                            fileName
                    )
            );
        }


        return new CreasePatternListPayload(
                success,
                message,
                entries
        );
    }


    @Override
    public Type<
            ? extends CustomPacketPayload
            > type() {

        return TYPE;
    }
}