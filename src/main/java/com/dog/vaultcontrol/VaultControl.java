package com.dog.vaultcontrol;

import com.dog.vaultcontrol.events.DimensionChangeEvent;
import com.dog.vaultcontrol.events.TPSListener;
import com.dog.vaultcontrol.saving.AutoSaveHandler;
import com.dog.vaultcontrol.mobai.AIControl;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Mod("vaultcontrol")
public class VaultControl {

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final AutoSaveHandler autoSaveHandler = new AutoSaveHandler();
    private final TPSListener tpsListener = new TPSListener();
    public static final Logger LOGGER = LogUtils.getLogger();


    public VaultControl() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStop);

        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG);
    }

    private void onServerStop(ServerStoppingEvent event) {
        executor.shutdownNow();
    }

    private void onServerStart(ServerStartingEvent event) {
        if (ServerConfig.CONFIG_VALUES.AutoSaves.get()) {
            MinecraftForge.EVENT_BUS.register(new AutoSaveHandler());
            autoSaveHandler.startAutoSaveTask();
        }

        if (ServerConfig.CONFIG_VALUES.TPSListener.get()) {
            MinecraftForge.EVENT_BUS.register(new TPSListener());
            tpsListener.startTPSMonitor();
        }

        if (ServerConfig.CONFIG_VALUES.MobAIControl.get()) {
            MinecraftForge.EVENT_BUS.register(AIControl.class);
        }

        if (ServerConfig.CONFIG_VALUES.VaultRaidEffect.get()) {
            MinecraftForge.EVENT_BUS.register(DimensionChangeEvent.class);
        }
    }

    public static void sendMessageToOppedPlayers(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.sendMessage(new TextComponent(message), player.getUUID());
            }
        }
    }
}