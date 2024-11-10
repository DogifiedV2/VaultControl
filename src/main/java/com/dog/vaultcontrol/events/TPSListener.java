package com.dog.vaultcontrol.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mod.EventBusSubscriber
public class TPSListener {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static boolean isServerLagging = false;
    private static int lagCount = 0;

    @SubscribeEvent
    public void onTPSMonitor(TPSMonitor event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        if (isLagging(server.getAverageTickTime())) {
            handleLag(server);
        } else {
            resetLagState();
        }
    }

    private boolean isLagging(float averageTickTime) {
        return averageTickTime > 75;
    }

    private void handleLag(MinecraftServer server) {
        if (isServerLagging) {
            if (++lagCount > 5) {
                executeServerCommand(server);
                resetLagState();
            }
        } else {
            isServerLagging = true;
        }
    }

    private void resetLagState() {
        isServerLagging = false;
        lagCount = 0;
    }

    private void executeServerCommand(MinecraftServer server) {
        server.execute(() -> server.getCommands().performCommand(server.createCommandSourceStack(), "chunkymcchunkface disable_all minecraft:overworld"));
    }

    public static void despawnVanillaItems(ServerLevel world) {
        List<Entity> entities = StreamSupport.stream(world.getEntities().getAll().spliterator(), false)
                .collect(Collectors.toList());

        entities.stream()
                .filter(entity -> entity instanceof ItemEntity)
                .map(entity -> (ItemEntity) entity)
                .forEach(TPSListener::discardIfVanillaOrSoulDust);
    }

    private static void discardIfVanillaOrSoulDust(ItemEntity itemEntity) {
        ResourceLocation itemId = itemEntity.getItem().getItem().getRegistryName();

        if (itemId == null) return;

        if (isVaultSoulDust(itemEntity, itemId) || "minecraft".equals(itemId.getNamespace())) {
            itemEntity.discard();
        }
    }

    private static boolean isVaultSoulDust(ItemEntity itemEntity, ResourceLocation itemId) {
        return "the_vault".equals(itemId.getNamespace()) &&
                itemEntity.getItem().getDisplayName().getString().contains("Soul Dust");
    }

    public void startTPSMonitor() {
        executor.scheduleAtFixedRate(() -> MinecraftForge.EVENT_BUS.post(new TPSMonitor()), 120, 5, TimeUnit.SECONDS);
    }
}
