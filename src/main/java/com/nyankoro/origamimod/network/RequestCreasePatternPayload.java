package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.origami.CreasePatternId;
import com.nyankoro.origamimod.server.CreasePatternStore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;
import java.util.Arrays;


/*
 * Client -> Server
 *
 * 保存済みCP本体を要求する。
 */
public record RequestCreasePatternPayload(
        String creasePatternId
) implements CustomPacketPayload {

    public static final Type<RequestCreasePatternPayload>
            TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "request_crease_pattern"
                    )
            );


    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            RequestCreasePatternPayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    RequestCreasePatternPayload::encode,
                    RequestCreasePatternPayload::decode
            );


    private static void encode(
            RegistryFriendlyByteBuf buffer,
            RequestCreasePatternPayload payload
    ) {

        buffer.writeUtf(
                payload.creasePatternId()
        );
    }


    private static RequestCreasePatternPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {

        return new RequestCreasePatternPayload(
                buffer.readUtf(
                        64
                )
        );
    }


    @Override
    public Type<
            ? extends CustomPacketPayload
            > type() {

        return TYPE;
    }


    public static void handle(
            RequestCreasePatternPayload payload,
            IPayloadContext context
    ) {

        if (!(context.player()
                instanceof ServerPlayer player)) {

            return;
        }


        if (!CreasePatternId.isValidFormat(
                payload.creasePatternId()
        )) {

            return;
        }


        try {

            CreasePatternStore.PatternData pattern =
                    CreasePatternStore.read(
                            player.level()
                                    .getServer(),
                            payload.creasePatternId()
                    );


            if (pattern == null) {

                OrigamiMod.LOGGER.warn(
                        "Requested crease pattern does not exist: {}",
                        payload.creasePatternId()
                );

                return;
            }


            byte[] cpData =
                    pattern.cpData();


            int chunkCount =
                    (
                            cpData.length
                                    + CreasePatternDownloadChunkPayload.CHUNK_SIZE
                                    - 1
                    )
                            / CreasePatternDownloadChunkPayload.CHUNK_SIZE;


            for (int chunkIndex = 0;
                 chunkIndex < chunkCount;
                 chunkIndex++) {

                int start =
                        chunkIndex
                                * CreasePatternDownloadChunkPayload.CHUNK_SIZE;


                int end =
                        Math.min(
                                start
                                        + CreasePatternDownloadChunkPayload.CHUNK_SIZE,
                                cpData.length
                        );


                byte[] chunk =
                        Arrays.copyOfRange(
                                cpData,
                                start,
                                end
                        );


                PacketDistributor.sendToPlayer(
                        player,
                        new CreasePatternDownloadChunkPayload(
                                pattern.creasePatternId(),
                                pattern.fileName(),
                                cpData.length,
                                chunkIndex,
                                chunkCount,
                                chunk
                        )
                );
            }


        } catch (IOException e) {

            OrigamiMod.LOGGER.error(
                    "Failed to read crease pattern: {}",
                    payload.creasePatternId(),
                    e
            );
        }
    }
}