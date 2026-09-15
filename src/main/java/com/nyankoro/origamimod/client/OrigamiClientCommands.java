package com.nyankoro.origamimod.client;

import com.nyankoro.origamimod.OrigamiMod;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(
        modid = OrigamiMod.MODID,
        value = Dist.CLIENT
)
public final class OrigamiClientCommands {

    private OrigamiClientCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(
            RegisterClientCommandsEvent event
    ) {

        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "origami_editor"
                                )
                                .executes(
                                        context -> {

                                            Minecraft minecraft =
                                                    Minecraft.getInstance();

                                            minecraft.execute(
                                                    () ->
                                                            minecraft.gui
                                                                    .setScreen(
                                                                            new OrigamiEditorScreen()
                                                                    )
                                            );

                                            return 1;
                                        }
                                )
                );
    }
}