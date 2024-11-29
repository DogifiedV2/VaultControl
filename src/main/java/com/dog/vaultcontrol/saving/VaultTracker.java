package com.dog.vaultcontrol.saving;

import com.mojang.logging.LogUtils;
import iskallia.vault.init.ModBlocks;
import iskallia.vault.init.ModItems;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber
public class VaultTracker {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ChunkPos, Long> openedVaultChunks = new HashMap<>();
    private static final Map<String, Long> completedVaults = new HashMap<>();
    private static int tickCounter = 0;

    public static void addChunk(ChunkPos chunkPos) {
        openedVaultChunks.put(chunkPos, getCurrentTimestamp());
    }

    public static void removeChunk(ChunkPos chunkPos) {
        openedVaultChunks.remove(chunkPos);
    }

    public static void addCompletedVault(String playerName) {
        completedVaults.put(playerName, getCurrentTimestamp());
    }

    public static void removeCompletedVault(String playerName) {
        completedVaults.remove(playerName);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter >= 1200) {
                cleanupOldVaults();
                tickCounter = 0;
            }
        }
    }

    public static boolean hasCompletedVaults() {
        long currentTime = getCurrentTimestamp();
        completedVaults.entrySet().removeIf(entry -> currentTime - entry.getValue() > 30);
        return !completedVaults.isEmpty();
    }

    public static void cleanCompletedVaults() {
        completedVaults.clear();
    }

    private static void cleanupOldVaults() {
        long currentTime = getCurrentTimestamp();
        synchronized (openedVaultChunks) {
            openedVaultChunks.entrySet().removeIf(entry -> currentTime - entry.getValue() > 59);
        }
    }

    public static boolean shouldExcludeChunk(ChunkPos chunkPos) {
        return openedVaultChunks.containsKey(chunkPos);
    }

    private static long getCurrentTimestamp() {
        return System.currentTimeMillis() / 1000;
    }
}
