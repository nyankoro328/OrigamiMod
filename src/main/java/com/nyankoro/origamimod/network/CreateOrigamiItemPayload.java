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
 * Client -> Server
 *
 * 折り紙アイテムの作成要求。
 *
 * CP本体はこのPayloadでは送信しない。
 *
 * 大容量データは今後、
 * チャンク分割方式で別途送信する。
 */
public record CreateOrigamiItemPayload(
        String cpFileName,
        OrigamiUseType useType,
        int frontColor,
        int backColor,
        int edgeColor,
        int angle
) implements CustomPacketPayload {

    /*
     * クライアント側で読み込むCPの
     * 異常サイズ防止用。
     *
     * CP自体をこのPayloadで
     * 送るための上限ではない。
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


    private static void encode(
            RegistryFriendlyByteBuf buffer,
            CreateOrigamiItemPayload payload
    ) {

        buffer.writeUtf(
                payload.cpFileName()
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


    private static CreateOrigamiItemPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {

        String cpFileName =
                buffer.readUtf(
                        MAX_FILE_NAME_LENGTH
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
         * 現段階では引き続き
         * 汎用OrigamiItemを1個生成するだけ。
         *
         * visualAssetIdなどをItemStackへ
         * 保存する処理はまだ追加しない。
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
                        + "useType={}, "
                        + "angle={}",
                player.getName()
                        .getString(),
                payload.cpFileName(),
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


        if (payload.useType() == null) {

            return false;
        }


        return payload.angle() >= 0
                && payload.angle() < 360;
    }
}