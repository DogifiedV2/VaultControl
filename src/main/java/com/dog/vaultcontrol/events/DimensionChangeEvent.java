package com.dog.vaultcontrol.events;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class DimensionChangeEvent {

    @SubscribeEvent
    public static void onDimChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        String previousDimensionNamespace = event.getFrom().location().getNamespace();

        if ("the_vault".equals(previousDimensionNamespace) && event.getTo().equals(Level.OVERWORLD)) {
            event.getPlayer().removeEffect(MobEffects.BAD_OMEN);
            event.getPlayer().removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
        }
    }

}
