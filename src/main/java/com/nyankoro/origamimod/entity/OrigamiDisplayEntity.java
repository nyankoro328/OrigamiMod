package com.nyankoro.origamimod.entity;

import com.nyankoro.origamimod.origami.OrigamiItemData;
import com.nyankoro.origamimod.origami.OrigamiVisualAssetId;

import net.minecraft.core.Direction;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.damagesource.DamageSource;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;


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
            Direction wallFace
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
         * 念のためLoad後にも固定化。
         */
        this.noPhysics =
                true;

        this.setNoGravity(
                true
        );
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

        return false;
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