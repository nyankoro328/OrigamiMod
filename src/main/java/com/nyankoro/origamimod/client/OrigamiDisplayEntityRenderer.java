package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.entity.OrigamiDisplayEntity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.minecraft.client.renderer.entity.state.EntityRenderState;


/*
 * OrigamiDisplayEntityの
 * 仮Renderer。
 *
 * 今回はEntity同期テストだけを行うため、
 * 何も描画しない。
 *
 * 次の段階で、
 * OrigamiImageWorldRendererの
 * 1Quad処理をここへ移す。
 */
public final class OrigamiDisplayEntityRenderer
        extends EntityRenderer<
        OrigamiDisplayEntity,
        EntityRenderState
        > {

    public OrigamiDisplayEntityRenderer(
            EntityRendererProvider.Context context
    ) {

        super(
                context
        );


        /*
         * 何も描画しないので
         * shadowも不要。
         */
        this.shadowRadius =
                0.0F;
    }


    @Override
    public EntityRenderState createRenderState() {

        return new EntityRenderState();
    }
}