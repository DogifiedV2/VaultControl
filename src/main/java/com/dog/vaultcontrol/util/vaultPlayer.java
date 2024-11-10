package com.dog.vaultcontrol.util;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class vaultPlayer {

    public static ServerPlayer getPlayerByUUID(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server.getPlayerList().getPlayer(uuid);
    }

    public static String getPlayerDimension(ServerPlayer player) {
        return player.level.dimension().location().getPath();
    }

    public static List<UUID> getVaultMembers(ServerPlayer player) {
        List<UUID> members = new ArrayList<>();

        String dimensionName = getPlayerDimension(player);

        for (ServerPlayer onlinePlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (getPlayerDimension(onlinePlayer).equals(dimensionName)) {
                members.add(onlinePlayer.getUUID());
            }
        }

        return members;
    }

    public static List<UUID> getVaultMembers(String dimensionName) {
        List<UUID> members = new ArrayList<>();

        for (ServerPlayer onlinePlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (getPlayerDimension(onlinePlayer).equals(dimensionName)) {
                members.add(onlinePlayer.getUUID());
            }
        }

        return members;
    }

    public static boolean isPlayerInVault(ServerPlayer player) {
        return getPlayerDimension(player).contains("vault");
    }

    public static void sendMessageToVaultMembers(ServerPlayer player, String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        List<UUID> playersInVault = vaultPlayer.getVaultMembers(player);
        for (UUID uuid : playersInVault) {
            ServerPlayer serverPlayer = getPlayerByUUID(uuid);
            serverPlayer.sendMessage(new TextComponent(message), serverPlayer.getUUID());
        }
    }

    public static void sendMessageToAllPlayers(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendMessage(new TextComponent(message), player.getUUID());
        }
    }

}
