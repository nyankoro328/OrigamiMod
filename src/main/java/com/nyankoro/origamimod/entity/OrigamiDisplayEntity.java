package com.nyankoro.origamimod.entity;

import com.nyankoro.origamimod.origami.OrigamiItemData;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

import net.minecraft.core.Direction;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.damagesource.DamageSource;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.item.ItemStack;

import com.nyankoro.origamimod.OrigamiMod;

import net.minecraft.network.chat.Component;

import com.nyankoro.origamimod.network
        .OpenOrigamiDisplayScaleEditorPayload;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;

/*
 * 壁へ設置された折り紙1個を表すEntity。
 *
 * 画像本体は持たない。
 *
 * visualAssetIdによって、
 * Server / Client cache内の
 * front.png / back.pngを参照する。
 *
 * 将来的にはここへ、
 *
 * ・scale
 * ・rotation
 * ・pivot
 * ・壁面内offset
 *
 * などを追加する。
 */
public final class OrigamiDisplayEntity
        extends Entity {

    /*
     * Server -> Clientへ自動同期される
     * visualAssetId。
     */
    private static final EntityDataAccessor<String>
            DATA_VISUAL_ASSET_ID =
            SynchedEntityData.defineId(
                    OrigamiDisplayEntity.class,
                    EntityDataSerializers.STRING
            );


    /*
     * 取り付けた壁の面。
     *
     * Direction#get3DDataValue()を保存する。
     */
    private static final EntityDataAccessor<Integer>
            DATA_WALL_FACE =
            SynchedEntityData.defineId(
                    OrigamiDisplayEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Float>
            DATA_SCALE =
            SynchedEntityData.defineId(
                    OrigamiDisplayEntity.class,
                    EntityDataSerializers.FLOAT
            );


    private static final EntityDataAccessor<Float>
            DATA_HORIZONTAL_OFFSET =
            SynchedEntityData.defineId(
                    OrigamiDisplayEntity.class,
                    EntityDataSerializers.FLOAT
            );


    private static final EntityDataAccessor<Float>
            DATA_VERTICAL_OFFSET =
            SynchedEntityData.defineId(
                    OrigamiDisplayEntity.class,
                    EntityDataSerializers.FLOAT
            );


    private static final EntityDataAccessor<Float>
            DATA_DEPTH_OFFSET =
            SynchedEntityData.defineId(
                    OrigamiDisplayEntity.class,
                    EntityDataSerializers.FLOAT
            );

    private static final EntityDataAccessor<Boolean>
            DATA_BACK_SIDE_OUTWARD =
            SynchedEntityData.defineId(
                    OrigamiDisplayEntity.class,
                    EntityDataSerializers.BOOLEAN
            );


    private static final EntityDataAccessor<Integer>
            DATA_DISPLAY_ANGLE =
            SynchedEntityData.defineId(
                    OrigamiDisplayEntity.class,
                    EntityDataSerializers.INT
            );

    /*
     * このEntityを設置するときに使った
     * Origami Itemを1個分保持する。
     *
     * Client描画には使わず、
     * 破壊時のItem復元とワールド保存に使用する。
     */
    private ItemStack storedItem =
            ItemStack.EMPTY;
    /*
     * 壁掛け折り紙の初期Hitbox。
     *
     * 横幅・高さは現在の表示サイズ1.5 block。
     * 厚さは絵画風にかなり薄くする。
     */
    private static final double HITBOX_WIDTH =
            1.5;

    private static final double HITBOX_HEIGHT =
            1.5;

    private static final double HITBOX_THICKNESS =
            1.0 / 16.0;


    public OrigamiDisplayEntity(
            EntityType<? extends OrigamiDisplayEntity> type,
            Level level
    ) {

        super(
                type,
                level
        );


        /*
         * 壁掛けなので物理移動させない。
         */
        this.noPhysics =
                true;

        this.setNoGravity(
                true
        );
    }


    /*
     * Entity生成直後に呼ぶ。
     */
    public void initialize(
            String visualAssetId,
            Direction wallFace,
            boolean backSideOutward,
            int displayAngle,
            ItemStack sourceStack
    ) {

        if (!OrigamiVisualAssetId.isValidFormat(
                visualAssetId
        )) {

            throw new IllegalArgumentException(
                    "Invalid visualAssetId"
            );
        }


        if (!isHorizontalWallFace(
                wallFace
        )) {

            throw new IllegalArgumentException(
                    "Origami display must be attached "
                            + "to a horizontal wall face"
            );
        }


        setVisualAssetId(
                visualAssetId
        );

        setWallFace(
                wallFace
        );

        setBackSideOutward(
                backSideOutward
        );

        setDisplayAngle(
                displayAngle
        );

        if (sourceStack == null
                || sourceStack.isEmpty()) {

            throw new IllegalArgumentException(
                    "Source origami item is empty"
            );
        }


        /*
         * Stackが複数個あっても、
         * Entity1個につき保存するのは1個だけ。
         *
         * Data Componentもそのままコピーされる。
         */
        this.storedItem =
                sourceStack.copyWithCount(
                        1
                );
    }


    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder entityData
    ) {

        entityData.define(
                DATA_VISUAL_ASSET_ID,
                OrigamiItemData.UNASSIGNED
        );


        entityData.define(
                DATA_WALL_FACE,
                Direction.SOUTH
                        .get3DDataValue()
        );

        entityData.define(
                DATA_SCALE,
                1.0F
        );


        entityData.define(
                DATA_HORIZONTAL_OFFSET,
                0.0F
        );


        entityData.define(
                DATA_VERTICAL_OFFSET,
                0.0F
        );


        entityData.define(
                DATA_DEPTH_OFFSET,
                0.0F
        );

        entityData.define(
                DATA_BACK_SIDE_OUTWARD,
                false
        );


        entityData.define(
                DATA_DISPLAY_ANGLE,
                0
        );
    }


    public String getVisualAssetId() {

        return this.entityData.get(
                DATA_VISUAL_ASSET_ID
        );
    }


    public boolean hasVisualAsset() {

        return OrigamiVisualAssetId
                .isValidFormat(
                        getVisualAssetId()
                );
    }


    public Direction getWallFace() {

        Direction direction =
                Direction.from3DDataValue(
                        this.entityData.get(
                                DATA_WALL_FACE
                        )
                );


        if (!isHorizontalWallFace(
                direction
        )) {

            return Direction.SOUTH;
        }


        return direction;
    }


    private void setVisualAssetId(
            String visualAssetId
    ) {

        this.entityData.set(
                DATA_VISUAL_ASSET_ID,
                visualAssetId
        );
    }


    public void setWallFace(
            Direction wallFace
    ) {

        if (!isHorizontalWallFace(
                wallFace
        )) {

            throw new IllegalArgumentException(
                    "Invalid wall face"
            );
        }


        this.entityData.set(
                DATA_WALL_FACE,
                wallFace.get3DDataValue()
        );


        /*
         * SOUTH =   0
         * WEST  =  90
         * NORTH = 180
         * EAST  = 270
         *
         * Direction#toYRot()が
         * この対応になっている。
         */
        this.setYRot(
                wallFace.toYRot()
        );

        this.setXRot(
                0.0F
        );

        /*
         * 壁方向が変わったため、
         * NORTH/SOUTH用とEAST/WEST用の
         * Hitboxを作り直す。
         */
        this.setBoundingBox(
                makeBoundingBox(
                        this.position()
                )
        );
    }

    public float getDisplayScale() {

        return this.entityData.get(
                DATA_SCALE
        );
    }


    public float getHorizontalOffset() {

        return this.entityData.get(
                DATA_HORIZONTAL_OFFSET
        );
    }


    public float getVerticalOffset() {

        return this.entityData.get(
                DATA_VERTICAL_OFFSET
        );
    }


    public float getDepthOffset() {

        return this.entityData.get(
                DATA_DEPTH_OFFSET
        );
    }


    public void setDisplayScale(
            float scale
    ) {

        this.entityData.set(
                DATA_SCALE,
                clamp(
                        scale,
                        0.1F,
                        10.0F
                )
        );
    }


    public void setHorizontalOffset(
            float offset
    ) {

        this.entityData.set(
                DATA_HORIZONTAL_OFFSET,
                clamp(
                        offset,
                        -16.0F,
                        16.0F
                )
        );
    }


    public void setVerticalOffset(
            float offset
    ) {

        this.entityData.set(
                DATA_VERTICAL_OFFSET,
                clamp(
                        offset,
                        -16.0F,
                        16.0F
                )
        );
    }


    public void setDepthOffset(
            float offset
    ) {

        this.entityData.set(
                DATA_DEPTH_OFFSET,
                clamp(
                        offset,
                        -2.0F,
                        2.0F
                )
        );
    }


    private static float clamp(
            float value,
            float min,
            float max
    ) {

        if (!Float.isFinite(
                value
        )) {

            return min;
        }


        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    public boolean isBackSideOutward() {

        return this.entityData.get(
                DATA_BACK_SIDE_OUTWARD
        );
    }


    public void setBackSideOutward(
            boolean backSideOutward
    ) {

        this.entityData.set(
                DATA_BACK_SIDE_OUTWARD,
                backSideOutward
        );
    }


    public int getDisplayAngle() {

        return this.entityData.get(
                DATA_DISPLAY_ANGLE
        );
    }


    public void setDisplayAngle(
            int angle
    ) {

        this.entityData.set(
                DATA_DISPLAY_ANGLE,
                normalizeAngle(
                        angle
                )
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


    /*
     * visualAssetIdとwallFaceを
     * ワールドデータへ保存する。
     *
     * position / yawなどのEntity標準情報は
     * Entity側が保存してくれる。
     */
    @Override
    protected void addAdditionalSaveData(
            ValueOutput output
    ) {

        output.putString(
                "VisualAssetId",
                getVisualAssetId()
        );


        output.putInt(
                "WallFace",
                getWallFace()
                        .get3DDataValue()
        );


        /*
         * 編集用Transformを
         * ワールドデータへ保存する。
         */
        output.putFloat(
                "DisplayScale",
                getDisplayScale()
        );


        output.putFloat(
                "HorizontalOffset",
                getHorizontalOffset()
        );


        output.putFloat(
                "VerticalOffset",
                getVerticalOffset()
        );


        output.putFloat(
                "DepthOffset",
                getDepthOffset()
        );

        output.putBoolean(
                "BackSideOutward",
                isBackSideOutward()
        );


        output.putInt(
                "DisplayAngle",
                getDisplayAngle()
        );

        /*
         * 設置元のOrigami Itemを丸ごと保存する。
         */
        if (!this.storedItem.isEmpty()) {

            output.store(
                    "StoredItem",
                    ItemStack.CODEC,
                    this.storedItem
            );
        }
    }


    /*
     * ワールド再読込時。
     */
    @Override
    protected void readAdditionalSaveData(
            ValueInput input
    ) {

        String visualAssetId =
                input.getStringOr(
                        "VisualAssetId",
                        OrigamiItemData.UNASSIGNED
                );


        if (OrigamiVisualAssetId.isValidFormat(
                visualAssetId
        )) {

            setVisualAssetId(
                    visualAssetId
            );

        } else {

            setVisualAssetId(
                    OrigamiItemData.UNASSIGNED
            );
        }


        Direction wallFace =
                Direction.from3DDataValue(
                        input.getIntOr(
                                "WallFace",
                                Direction.SOUTH
                                        .get3DDataValue()
                        )
                );


        if (!isHorizontalWallFace(
                wallFace
        )) {

            wallFace =
                    Direction.SOUTH;
        }


        setWallFace(
                wallFace
        );


        /*
         * 編集用Transform。
         *
         * 古いEntityには値が保存されていないため、
         * デフォルト値を使用する。
         */
        setDisplayScale(
                input.getFloatOr(
                        "DisplayScale",
                        1.0F
                )
        );


        setHorizontalOffset(
                input.getFloatOr(
                        "HorizontalOffset",
                        0.0F
                )
        );


        setVerticalOffset(
                input.getFloatOr(
                        "VerticalOffset",
                        0.0F
                )
        );


        setDepthOffset(
                input.getFloatOr(
                        "DepthOffset",
                        0.0F
                )
        );

        setBackSideOutward(
                input.getBooleanOr(
                        "BackSideOutward",
                        false
                )
        );


        setDisplayAngle(
                input.getIntOr(
                        "DisplayAngle",
                        0
                )
        );


        /*
         * Load後も壁掛けEntityとして固定。
         */
        this.noPhysics =
                true;

        this.setNoGravity(
                true
        );

        this.storedItem =
                input.read(
                                "StoredItem",
                                ItemStack.CODEC
                        )
                        .orElse(
                                ItemStack.EMPTY
                        );
    }

    /*
     * 折り紙画像に合わせた薄いHitbox。
     *
     * Entity positionを画像中心として扱う。
     *
     * NORTH / SOUTH:
     *   X方向に広く、Z方向に薄い。
     *
     * EAST / WEST:
     *   Z方向に広く、X方向に薄い。
     */
    @Override
    protected AABB makeBoundingBox(
            Vec3 position
    ) {

        double halfWidth =
                HITBOX_WIDTH
                        * 0.5;

        double halfHeight =
                HITBOX_HEIGHT
                        * 0.5;

        double halfThickness =
                HITBOX_THICKNESS
                        * 0.5;


        /*
         * SOUTH / NORTHではyawが
         * 0 / 180付近になる。
         *
         * EAST / WESTでは
         * 90 / 270付近になる。
         *
         * Entity生成途中でも安全に使えるよう、
         * SynchedEntityDataではなくyawから判断する。
         */
        double absoluteCos =
                Math.abs(
                        Math.cos(
                                Math.toRadians(
                                        this.getYRot()
                                )
                        )
                );


        /*
         * NORTH / SOUTH
         *
         * 壁面:
         *   X-Y平面
         *
         * 厚さ:
         *   Z方向
         */
        if (absoluteCos >= 0.5) {

            return new AABB(
                    position.x
                            - halfWidth,

                    position.y
                            - halfHeight,

                    position.z
                            - halfThickness,

                    position.x
                            + halfWidth,

                    position.y
                            + halfHeight,

                    position.z
                            + halfThickness
            );
        }


        /*
         * EAST / WEST
         *
         * 壁面:
         *   Z-Y平面
         *
         * 厚さ:
         *   X方向
         */
        return new AABB(
                position.x
                        - halfThickness,

                position.y
                        - halfHeight,

                position.z
                        - halfWidth,

                position.x
                        + halfThickness,

                position.y
                        + halfHeight,

                position.z
                        + halfWidth
        );
    }

    /*
     * 壁掛け折り紙を右クリックすると、
     * 大きさ編集画面を開く。
     */
    @Override
    public InteractionResult interact(
            Player player,
            InteractionHand hand,
            Vec3 location
    ) {

        OrigamiMod.LOGGER.info(
                "Origami display interacted: entityId={}, player={}",
                this.getId(),
                player.getName().getString()
        );

        /*
         * Client側では画面を直接開かない。
         *
         * ServerからPayloadを送ることで、
         * 対象Entity IDと現在のscaleを
         * 正式な値としてClientへ渡す。
         */
        if (player instanceof ServerPlayer serverPlayer) {

            /*
             * 念のため8 block以内だけ許可。
             */
            if (serverPlayer.distanceToSqr(
                    this
            ) > 64.0) {

                return InteractionResult.FAIL;
            }


            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new OpenOrigamiDisplayScaleEditorPayload(
                            this.getId(),
                            this.getDisplayScale()
                    )
            );
        }


        return InteractionResult.SUCCESS;
    }

    /*
     * 将来、右クリックして
     * 編集UIを開けるようにするため、
     * raycast対象にはする。
     */
    @Override
    public boolean isPickable() {

        return true;
    }


    /*
     * プレイヤーや他Entityから
     * 押されない。
     */
    @Override
    public boolean isPushable() {

        return false;
    }


    /*
     * 現段階では攻撃で破壊しない。
     *
     * 後で、
     * 所有者・権限・アイテム回収処理を
     * 実装してから変更する。
     */
    @Override
    public boolean hurtServer(
            ServerLevel level,
            DamageSource source,
            float damage
    ) {

        /*
         * プレイヤーによる破壊だけを受け付ける。
         *
         * 爆発・炎・Mobなどでは
         * 現段階では壊さない。
         */
        if (!(source.getEntity()
                instanceof ServerPlayer player)) {

            return false;
        }


        /*
         * Creativeの攻撃なら、
         *
         * ・Entityは消す
         * ・Itemはドロップしない
         *
         * VanillaのCreativeらしい挙動。
         */
        if (source.isCreativePlayer()) {

            this.discard();

            return true;
        }


        /*
         * 古い開発用Entityなど、
         * StoredItem追加前に作られたEntityは
         * Itemを復元できない。
         *
         * 誤って作品を消さないよう、
         * Survivalでは破壊しない。
         *
         * Creativeなら上の処理で削除可能。
         */
        if (this.storedItem.isEmpty()) {

            OrigamiMod.LOGGER.warn(
                    "Cannot recover legacy OrigamiDisplayEntity "
                            + "without StoredItem: entityId={}",
                    this.getId()
            );

            player.sendSystemMessage(
                    Component.literal(
                            "この古い折り紙Entityには"
                                    + "回収用Item情報がありません"
                    )
            );

            return false;
        }


        /*
         * 元のItemを1個そのままドロップする。
         */
        this.spawnAtLocation(
                level,
                this.storedItem.copy(),
                0.0F
        );


        /*
         * 壁掛けEntityを削除。
         */
        this.discard();


        return true;
    }


    private static boolean isHorizontalWallFace(
            Direction direction
    ) {

        return direction == Direction.NORTH
                || direction == Direction.SOUTH
                || direction == Direction.EAST
                || direction == Direction.WEST;
    }
}