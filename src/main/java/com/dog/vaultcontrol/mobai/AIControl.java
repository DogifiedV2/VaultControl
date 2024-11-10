package com.dog.vaultcontrol.mobai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class AIControl {

    private static final int CHECK_INTERVAL = 100; // 5 seconds
    private static final double ACTIVATION_RADIUS = 48.0; // Horizontal radius
    private static final double VERTICAL_RADIUS = 10.0; // Vertical radius

    @SubscribeEvent
    public static void onMobSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        if (event.getSpawnReason() == MobSpawnType.NATURAL) {
            mob.getPersistentData().putString("CustomSpawnReason", event.getSpawnReason().name());
            mob.setCanPickUpLoot(false);
        }

        if (event.getSpawnReason() == MobSpawnType.SPAWNER || !isPlayerNearby(mob)) {
            mob.setNoAi(true);
        }
    }

    @SubscribeEvent
    public static void onMobUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.getLevel().dimension() != Level.OVERWORLD) return;

        if (mob.tickCount % CHECK_INTERVAL == 0 && mob.getPersistentData().contains("CustomSpawnReason")) {
            mob.setNoAi(!isPlayerNearby(mob));
        }
    }

    @SubscribeEvent
    public static void onMobItemCheck(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.getLevel().dimension() != Level.OVERWORLD) return;

        if (!mob.getMainHandItem().isEmpty() || !mob.getOffhandItem().isEmpty()) {
            if (mob.getPersistentData().contains("CustomSpawnReason")) {
                mob.discard();
            }
        }
    }

    private static boolean isPlayerNearby(Mob mob) {
        List<? extends Player> players = mob.getLevel().players();
        return players.stream().anyMatch(player -> {
            double dx = player.getX() - mob.getX();
            double dz = player.getZ() - mob.getZ();
            double dy = Math.abs(player.getY() - mob.getY());
            return dx * dx + dz * dz <= ACTIVATION_RADIUS * ACTIVATION_RADIUS && dy <= VERTICAL_RADIUS;
        });
    }
}
