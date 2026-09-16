package com.nyankoro.origamimod.origami;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;


/*
 * 1個のOrigami ItemStackが持つ情報。
 *
 * origamiId:
 *   将来の「作品そのもの」の識別ID。
 *   現段階では未使用。
 *
 * visualAssetId:
 *   front.png / back.png の描画Assetを
 *   識別するSHA-256 ID。
 *
 * 画像本体はItemStackには保存しない。
 */
public record OrigamiItemData(
        String origamiId,
        String visualAssetId,
        String useType,
        int frontColor,
        int backColor,
        int edgeColor,
        int angle
) {

    public static final String UNASSIGNED =
            "unassigned";


    /*
     * /giveなどで通常生成された場合。
     */
    public static final OrigamiItemData DEFAULT =
            new OrigamiItemData(
                    UNASSIGNED,
                    UNASSIGNED,
                    "WALL",
                    0xFFFFC857,
                    0xFF4EA5D9,
                    0xFF202020,
                    0
            );


    /*
     * ワールド保存用。
     *
     * visual_asset_idはoptionalにしておき、
     * このフィールド追加前に作られた
     * 開発用ItemStackも読み込めるようにする。
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
                                                    .optionalFieldOf(
                                                            "visual_asset_id",
                                                            UNASSIGNED
                                                    )
                                                    .forGetter(
                                                            OrigamiItemData::visualAssetId
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
     * Server <-> Client同期用。
     */
    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OrigamiItemData
            >
            STREAM_CODEC =
            StreamCodec.of(
                    (buffer, data) -> {

                        buffer.writeUtf(
                                data.origamiId()
                        );

                        buffer.writeUtf(
                                data.visualAssetId()
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
                                    buffer.readUtf(),
                                    buffer.readInt(),
                                    buffer.readInt(),
                                    buffer.readInt(),
                                    buffer.readInt()
                            )
            );


    public OrigamiItemData {

        Objects.requireNonNull(
                origamiId
        );

        Objects.requireNonNull(
                visualAssetId
        );

        Objects.requireNonNull(
                useType
        );


        if (origamiId.isBlank()) {

            origamiId =
                    UNASSIGNED;
        }


        if (visualAssetId.isBlank()) {

            visualAssetId =
                    UNASSIGNED;
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


    public boolean hasVisualAsset() {

        return OrigamiVisualAssetId
                .isValidFormat(
                        visualAssetId
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