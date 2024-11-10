package com.dog.vaultcontrol.events.handlers;

import com.dog.vaultcontrol.TimeFormatter;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.*;
import java.util.logging.ConsoleHandler;

@Mod.EventBusSubscriber
public class LockdownHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean isLockdownEnabled = false;

    private static final Set<String> disconnectedPlayers = new HashSet<>();

    public static boolean isLockdownEnabled() {
        return isLockdownEnabled;
    }

    public static void setLockdownEnabled(boolean enabled) {
        isLockdownEnabled = enabled;
        LOGGER.info("Lockdown has been " + (enabled ? "enabled" : "disabled") + ".");

        if (enabled) {
            kickNonExemptPlayers();

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            server.getCommands().performCommand(
                    server.createCommandSourceStack(),
                    "say Server now in lockdown until restart, saving world.."
            );

        } else {
            disconnectedPlayers.clear();
        }
    }

    // Method to kick all online players except those in the exempt list
    private static void kickNonExemptPlayers() {
        List<ServerPlayer> players = new ArrayList<>(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers());

        for (ServerPlayer player : players) {
            String playerName = player.getName().getString();

            // Check if the player is exempt or already disconnected
            if (!disconnectedPlayers.contains(playerName)) {
                if (player.connection != null && !player.hasDisconnected()) {
                    player.connection.disconnect(new net.minecraft.network.chat.TextComponent("The server is now in lockdown. You have been kicked."));
                    disconnectedPlayers.add(playerName);
                    LOGGER.info("Kicked player: " + playerName);
                }
            }
        }
    }




    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (isLockdownEnabled) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            String playerName = player.getName().getString();
                player.connection.disconnect(new net.minecraft.network.chat.TextComponent("The server is currently in lockdown. No players are allowed to join."));
                disconnectedPlayers.add(playerName);
                LOGGER.info("Blocked player login attempt: " + playerName);
        }
    }
}
