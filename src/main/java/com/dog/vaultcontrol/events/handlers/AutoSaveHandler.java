package com.dog.vaultcontrol.events.handlers;

import com.dog.vaultcontrol.ServerConfig;
import com.dog.vaultcontrol.events.CustomAutoSaveEvent;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoSaveHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean autosaveEnabled = true;
    private static boolean autosaveCrash = false;
    private static long lastAutosaveTime = System.nanoTime();

    private final ScheduledExecutorService autosaveExecutor = Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService monitorExecutor = Executors.newScheduledThreadPool(1);

    public static boolean isAutosaveEnabled() {
        return autosaveEnabled;
    }

    public static void setAutosaveEnabled(boolean enabled) {
        autosaveEnabled = enabled;
        LOGGER.info("Autosave has been " + (enabled ? "enabled" : "disabled") + ".");
    }

    private void logInGame(String message) {
        if (ServerConfig.CONFIG_VALUES.AutoSaveLogs.get()) {
            com.dog.vaultcontrol.VaultControl.sendMessageToOppedPlayers(message);
        }
    }

    @SubscribeEvent
    public void onCustomAutoSave(CustomAutoSaveEvent event) {
        if (!autosaveEnabled) {
            LOGGER.info("Autosave is disabled. Skipping autosave process.");
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        LOGGER.info("Autosave started...");
        logInGame("Starting autosave...");
        long startTime = System.nanoTime();

        server.getAllLevels().forEach(this::saveLevel);

        savePlayerData(server);
        long totalDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        LOGGER.info("Autosave completed in {} ms.", totalDuration);
        logInGame("Autosave completed in " + totalDuration + " ms.");

        lastAutosaveTime = System.nanoTime();
    }

    private void saveLevel(ServerLevel level) {
        if (isMainDimension(level)) {
            LOGGER.info("Saving chunks for dimension: {}", level.dimension().location());
            try {
                long levelStartTime = System.nanoTime();
                level.save(null, false, false);
                LOGGER.info("Completed saving dimension in {} ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - levelStartTime));
            } catch (Exception e) {
                LOGGER.error("Error while saving dimension:", e);
                logInGame("An error occurred during autosave.");
                autosaveCrash = true;
            }
        } else {
            LOGGER.info("Skipping dimension: {}", level.dimension().location());
        }
    }

    private boolean isMainDimension(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER || level.dimension() == Level.END;
    }

    private void savePlayerData(MinecraftServer server) {
        long startTime = System.nanoTime();
        server.getPlayerList().saveAll();
        LOGGER.info("Player data saved in {} ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
    }

    public void startAutoSaveTask() {
        autosaveExecutor.scheduleAtFixedRate(() -> MinecraftForge.EVENT_BUS.post(new CustomAutoSaveEvent()), 120, 300, TimeUnit.SECONDS);
        monitorExecutor.scheduleAtFixedRate(this::checkLastAutoSave, 180, 300, TimeUnit.SECONDS);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getCommands().performCommand(server.createCommandSourceStack(), "save-off");
        }
    }

    private void checkLastAutoSave() {
        if (!autosaveEnabled || !needsRestart()) return;

        LOGGER.info("Autosave task stopped. Restarting scheduler...");
        logInGame("Restarting autosave task.");

        shutdownAutosaveExecutor();
        reinitializeAutosaveExecutor();
    }

    private boolean needsRestart() {
        long timeSinceLastSave = TimeUnit.NANOSECONDS.toMinutes(System.nanoTime() - lastAutosaveTime);
        return timeSinceLastSave >= 7 || autosaveCrash;
    }

    private void shutdownAutosaveExecutor() {
        LOGGER.info("Shutting down autosave executor.");
        autosaveExecutor.shutdownNow();
        try {
            if (!autosaveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Autosave executor did not shut down gracefully.");
                logInGame("Error while restarting the autosave process.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while shutting down the executor.", e);
            logInGame("Error: interrupted while restarting. Please report.");
        }
    }

    private void reinitializeAutosaveExecutor() {
        LOGGER.info("Re-initializing and scheduling autosave task.");
        autosaveExecutor.scheduleAtFixedRate(() -> MinecraftForge.EVENT_BUS.post(new CustomAutoSaveEvent()), 0, 300, TimeUnit.SECONDS);
        autosaveCrash = false;
        logInGame("Autosave scheduler restarted.");
    }
}