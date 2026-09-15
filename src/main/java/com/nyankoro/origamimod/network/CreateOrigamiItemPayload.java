package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.origami.OrigamiUseType;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Locale;


/*
 * 折り紙設定画面からサーバーへ送る
 * 「折り紙をアイテム化してほしい」という要求。
 *
 * 最終的には、このデータを
 * サーバー側の折り紙データ保存処理へ渡す。
 */
public record CreateOrigamiItemPayload(
        String cpFileName,
        byte[] cpData,
        OrigamiUseType useType,
        int frontColor,
        int backColor,
        int edgeColor,
        int angle
) implements CustomPacketPayload {

    /*
     * 不正または異常に大きいCPを
     * 無制限に受け取らないための上限。
     *
     * 現段階では1MiB。
     */
    public static final int MAX_CP_BYTES =
            1024 * 1024;

    public static final int MAX_FILE_NAME_LENGTH =
            128;


    public static final
    CustomPacketPayload.Type<CreateOrigamiItemPayload>
            TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(
                            OrigamiMod.MODID,
                            "create_origami_item"
                    )
            );


    public static final
    StreamCodec<
            RegistryFriendlyByteBuf,
            CreateOrigamiItemPayload
            >
            STREAM_CODEC =
            StreamCodec.of(
                    CreateOrigamiItemPayload::encode,
                    CreateOrigamiItemPayload::decode
            );


    /*
     * Client -> Network
     */
    private static void encode(
            RegistryFriendlyByteBuf buffer,
            CreateOrigamiItemPayload payload
    ) {

        buffer.writeUtf(
                payload.cpFileName()
        );

        /*
         * CP本体。
         *
         * サイズを明示的に書いてから
         * 生バイト列を書き込む。
         */
        buffer.writeVarInt(
                payload.cpData().length
        );

        buffer.writeBytes(
                payload.cpData()
        );


        buffer.writeEnum(
                payload.useType()
        );

        buffer.writeInt(
                payload.frontColor()
        );

        buffer.writeInt(
                payload.backColor()
        );

        buffer.writeInt(
                payload.edgeColor()
        );

        buffer.writeInt(
                payload.angle()
        );
    }


    /*
     * Network -> Server
     */
    private static CreateOrigamiItemPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {

        String cpFileName =
                buffer.readUtf(
                        MAX_FILE_NAME_LENGTH
                );


        int cpLength =
                buffer.readVarInt();


        if (cpLength <= 0
                || cpLength > MAX_CP_BYTES) {

            throw new IllegalArgumentException(
                    "Invalid CP data size: "
                            + cpLength
            );
        }


        byte[] cpData =
                new byte[
                        cpLength
                        ];

        buffer.readBytes(
                cpData
        );


        OrigamiUseType useType =
                buffer.readEnum(
                        OrigamiUseType.class
                );


        int frontColor =
                buffer.readInt();

        int backColor =
                buffer.readInt();

        int edgeColor =
                buffer.readInt();

        int angle =
                buffer.readInt();


        return new CreateOrigamiItemPayload(
                cpFileName,
                cpData,
                useType,
                frontColor,
                backColor,
                edgeColor,
                angle
        );
    }


    @Override
    public CustomPacketPayload.Type<
            ? extends CustomPacketPayload
            > type() {

        return TYPE;
    }


    /*
     * サーバー側処理。
     */
    public static void handle(
            CreateOrigamiItemPayload payload,
            IPayloadContext context
    ) {

        if (!(context.player()
                instanceof ServerPlayer player)) {

            return;
        }


        if (!isValid(
                payload
        )) {

            OrigamiMod.LOGGER.warn(
                    "Rejected invalid origami create request from {}",
                    player.getName()
                            .getString()
            );

            return;
        }


        /*
         * 現段階ではまず
         * サーバーがItemStackを生成できることを確認する。
         *
         * 次の段階で、
         * payloadの設定値をData Componentとして
         * ItemStackへ保存する。
         */
        ItemStack stack =
                new ItemStack(
                        OrigamiMod.ORIGAMI_ITEM.get()
                );


        boolean inserted =
                player.getInventory()
                        .add(
                                stack
                        );


        /*
         * インベントリ満杯なら
         * プレイヤーの足元へ落とす。
         */
        if (!inserted) {

            player.drop(
                    stack,
                    false
            );
        }


        OrigamiMod.LOGGER.info(
                "Received origami create request: "
                        + "player={}, "
                        + "file={}, "
                        + "bytes={}, "
                        + "useType={}, "
                        + "angle={}",
                player.getName()
                        .getString(),
                payload.cpFileName(),
                payload.cpData().length,
                payload.useType(),
                payload.angle()
        );


        player.sendSystemMessage(
                Component.literal(
                        "折り紙をアイテム化しました: "
                                + payload.cpFileName()
                )
        );
    }


    private static boolean isValid(
            CreateOrigamiItemPayload payload
    ) {

        if (payload.cpData() == null
                || payload.cpData().length == 0
                || payload.cpData().length
                > MAX_CP_BYTES) {

            return false;
        }


        if (payload.cpFileName() == null
                || payload.cpFileName()
                .isBlank()
                || payload.cpFileName()
                .length()
                > MAX_FILE_NAME_LENGTH) {

            return false;
        }


        if (!payload.cpFileName()
                .toLowerCase(
                        Locale.ROOT
                )
                .endsWith(
                        ".cp"
                )) {

            return false;
        }


        return payload.angle() >= 0
                && payload.angle() < 360;
    }
}