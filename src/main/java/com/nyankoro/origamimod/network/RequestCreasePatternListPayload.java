package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.server.CreasePatternStore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;
import java.util.List;


/*
 * Client -> Server
 *
 * 保存済みCPの一覧を要求する。
 */
public record RequestCreasePatternListPayload()
        implements CustomPacketPayload {

    public static final RequestCreasePatternListPayload
            INSTANCE =
            new RequestCreasePatternListPayload();


    public static final Type<RequestCreasePatternListPayload>
            TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "request_crease_pattern_list"
                    )
            );


    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            RequestCreasePatternListPayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                    },
                    buffer -> INSTANCE
            );


    @Override
    public Type<
            ? extends CustomPacketPayload
            > type() {

        return TYPE;
    }


    public static void handle(
            RequestCreasePatternListPayload payload,
            IPayloadContext context
    ) {

        if (!(context.player()
                instanceof ServerPlayer player)) {

            return;
        }


        try {

            List<CreasePatternStore.LibraryEntry>
                    storedEntries =
                    CreasePatternStore.list(
                            player.level()
                                    .getServer()
                    );


            List<CreasePatternListPayload.Entry>
                    responseEntries =
                    storedEntries
                            .stream()
                            .limit(
                                    CreasePatternListPayload
                                            .MAX_ENTRIES
                            )
                            .map(
                                    entry ->
                                            new CreasePatternListPayload.Entry(
                                                    entry.creasePatternId(),
                                                    entry.fileName()
                                            )
                            )
                            .toList();


            PacketDistributor.sendToPlayer(
                    player,
                    new CreasePatternListPayload(
                            true,
                            "",
                            responseEntries
                    )
            );


            OrigamiMod.LOGGER.info(
                    "Sent crease pattern list: "
                            + "player={}, count={}",
                    player.getUUID(),
                    responseEntries.size()
            );


        } catch (IOException e) {

            OrigamiMod.LOGGER.error(
                    "Failed to list saved crease patterns",
                    e
            );


            PacketDistributor.sendToPlayer(
                    player,
                    new CreasePatternListPayload(
                            false,
                            "保存済み展開図の取得に失敗しました",
                            List.of()
                    )
            );
        }
    }
}