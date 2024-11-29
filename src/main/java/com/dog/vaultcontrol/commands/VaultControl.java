package com.dog.vaultcontrol.commands;

import com.dog.vaultcontrol.saving.AutoSaveHandler;
import com.dog.vaultcontrol.events.handlers.LockdownHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber
public class VaultControl {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("vaultcontrol")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("lockdown").executes(VaultControl::toggleLockdown))
                .then(Commands.literal("manualsave").executes(VaultControl::executeManualSave))
                .then(Commands.literal("toggleautosave").executes(VaultControl::toggleAutoSave))
        );
    }

    private static int toggleAutoSave(CommandContext<CommandSourceStack> context) {
        boolean newState = !AutoSaveHandler.isAutosaveEnabled();
        AutoSaveHandler.setAutosaveEnabled(newState);
        String statusMessage = "Autosave has been " + (newState ? "enabled" : "disabled") + ".";
        String noteMessage = "Note: this change is temporary until server restart. Use configuration for permanent change.";

        sendMessageToPlayer(context, statusMessage);
        sendMessageToPlayer(context, noteMessage);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeManualSave(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        com.dog.vaultcontrol.VaultControl.sendMessageToOppedPlayers("Manual Save starting...");

        server.getAllLevels().forEach(VaultControl::saveDimension);

        server.getPlayerList().saveAll();
        com.dog.vaultcontrol.VaultControl.sendMessageToOppedPlayers("Manual Save completed.");
        return Command.SINGLE_SUCCESS;
    }

    private static void saveDimension(ServerLevel level) {
        if (isMainDimension(level)) {
            String dimensionName = level.dimension().location().toString();
            LOGGER.info("Saving chunks for dimension: {}", dimensionName);

            try {
                long startTime = System.nanoTime();
                level.save(null, false, false);
                LOGGER.info("Completed saving dimension: {} in {} ms", dimensionName,
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            } catch (Exception e) {
                LOGGER.error("Error while saving dimension: ", e);
                com.dog.vaultcontrol.VaultControl.sendMessageToOppedPlayers("An error occurred during manual save.");
            }
        } else {
            LOGGER.info("Skipping dimension: {}", level.dimension().location());
        }
    }

    private static boolean isMainDimension(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER || level.dimension() == Level.END;
    }

    private static int toggleLockdown(CommandContext<CommandSourceStack> context) {
        boolean newState = !LockdownHandler.isLockdownEnabled();
        LockdownHandler.setLockdownEnabled(newState);
        String message = "Lockdown has been " + (newState ? "enabled" : "disabled") + ".";
        context.getSource().sendSuccess(new TextComponent(message), true);
        return Command.SINGLE_SUCCESS;
    }

    private static void sendMessageToPlayer(CommandContext<CommandSourceStack> context, String message) {
        if (context.getSource().getEntity() instanceof Player player) {
            player.sendMessage(new TextComponent(message), player.getUUID());
        }
    }
}
