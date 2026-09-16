package com.nyankoro.origamimod.network;

import com.nyankoro.origamimod.OrigamiMod;
import com.nyankoro.origamimod.origami.OrigamiItemData;
import com.nyankoro.origamimod.origami.OrigamiUseType;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

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
 * PNGやCP本体は含めず、
 * visualAssetIdだけをItemStackへ保存する。
 */
public record CreateOrigamiItemPayload(
        String cpFileName,
        String visualAssetId,
        OrigamiUseType useType,
        int frontColor,
        int backColor,
        int edgeColor,
        int angle
) implements CustomPacketPayload {

    public static final int MAX_CP_BYTES =
            1024 * 1024;


    public static final int MAX_FILE_NAME_LENGTH =
            128;


    public static final int MAX_ASSET_ID_LENGTH =
            64;


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


        buffer.writeUtf(
                payload.visualAssetId()
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


        String visualAssetId =
                buffer.readUtf(
                        MAX_ASSET_ID_LENGTH
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
                visualAssetId,
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


        ItemStack stack =
                new ItemStack(
                        OrigamiMod.ORIGAMI_ITEM.get()
                );


        /*
         * ItemStackへ作品情報を保存。
         *
         * origamiIdはまだ未使用。
         *
         * visualAssetIdが
         * Server保存済みfront/back PNGへの参照になる。
         */
        stack.set(
                OrigamiMod.ORIGAMI_DATA.get(),
                new OrigamiItemData(
                        OrigamiItemData.UNASSIGNED,
                        payload.visualAssetId(),
                        payload.useType()
                                .name(),
                        payload.frontColor(),
                        payload.backColor(),
                        payload.edgeColor(),
                        payload.angle()
                )
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
                "Created origami item: "
                        + "player={}, "
                        + "file={}, "
                        + "visualAssetId={}, "
                        + "useType={}, "
                        + "angle={}",
                player.getName()
                        .getString(),
                payload.cpFileName(),
                payload.visualAssetId(),
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


        if (!OrigamiVisualAssetId
                .isValidFormat(
                        payload.visualAssetId()
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