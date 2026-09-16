package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.OrigamiMod;

import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event
        .EntityRenderersEvent;


/*
 * Client専用Entity Renderer登録。
 */
@EventBusSubscriber(
        value = Dist.CLIENT,
        modid = OrigamiMod.MODID
)
public final class OrigamiClientEntityRenderers {

    private OrigamiClientEntityRenderers() {
    }


    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {

        event.registerEntityRenderer(
                OrigamiMod
                        .ORIGAMI_DISPLAY_ENTITY
                        .get(),
                OrigamiDisplayEntityRenderer::new
        );
    }
}