package com.nyankoro.origamimod.origami;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

/*
 * 1個の折り紙ItemStackが持つ基本情報。
 *
 * 今の段階では、
 *
 * ・どの折り紙か
 * ・用途
 * ・表色
 * ・裏色
 * ・輪郭色
 * ・完成品の2D角度
 *
 * を保持する。
 *
 * CP本体や巨大な画像データは
 * ItemStackには直接保存しない。
 */
public record OrigamiItemData(
        String origamiId,
        String useType,
        int frontColor,
        int backColor,
        int edgeColor,
        int angle
) {

    /*
     * /give などで普通に生成したときの初期値。
     */
    public static final OrigamiItemData DEFAULT =
            new OrigamiItemData(
                    "unassigned",
                    "WALL",
                    0xFFFFC857,
                    0xFF4EA5D9,
                    0xFF202020,
                    0
            );


    /*
     * ワールド保存用。
     */
    public static final Codec<OrigamiItemData> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.STRING
                                                    .fieldOf(
                                                            "origami_id"
                                                    )
                                                    .forGetter(
                                                            OrigamiItemData::origamiId
                                                    ),

                                            Codec.STRING
                                                    .fieldOf(
                                                            "use_type"
                                                    )
                                                    .forGetter(
                                                            OrigamiItemData::useType
                                                    ),

                                            Codec.INT
                                                    .fieldOf(
                                                            "front_color"
                                                    )
                                                    .forGetter(
                                                            OrigamiItemData::frontColor
                                                    ),

                                            Codec.INT
                                                    .fieldOf(
                                                            "back_color"
                                                    )
                                                    .forGetter(
                                                            OrigamiItemData::backColor
                                                    ),

                                            Codec.INT
                                                    .fieldOf(
                                                            "edge_color"
                                                    )
                                                    .forGetter(
                                                            OrigamiItemData::edgeColor
                                                    ),

                                            Codec.INT
                                                    .fieldOf(
                                                            "angle"
                                                    )
                                                    .forGetter(
                                                            OrigamiItemData::angle
                                                    )
                                    )
                                    .apply(
                                            instance,
                                            OrigamiItemData::new
                                    )
            );


    /*
     * サーバー ↔ クライアント同期用。
     */
    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OrigamiItemData
            > STREAM_CODEC =
            StreamCodec.of(
                    (buffer, data) -> {

                        buffer.writeUtf(
                                data.origamiId()
                        );

                        buffer.writeUtf(
                                data.useType()
                        );

                        buffer.writeInt(
                                data.frontColor()
                        );

                        buffer.writeInt(
                                data.backColor()
                        );

                        buffer.writeInt(
                                data.edgeColor()
                        );

                        buffer.writeInt(
                                data.angle()
                        );
                    },

                    buffer ->
                            new OrigamiItemData(
                                    buffer.readUtf(),
                                    buffer.readUtf(),
                                    buffer.readInt(),
                                    buffer.readInt(),
                                    buffer.readInt(),
                                    buffer.readInt()
                            )
            );


    /*
     * 不正値を最低限補正する。
     */
    public OrigamiItemData {

        Objects.requireNonNull(
                origamiId
        );

        Objects.requireNonNull(
                useType
        );

        if (origamiId.isBlank()) {
            origamiId =
                    "unassigned";
        }

        if (useType.isBlank()) {
            useType =
                    "WALL";
        }

        angle =
                normalizeAngle(
                        angle
                );
    }


    private static int normalizeAngle(
            int angle
    ) {

        int result =
                angle % 360;

        if (result < 0) {
            result +=
                    360;
        }

        return result;
    }
}