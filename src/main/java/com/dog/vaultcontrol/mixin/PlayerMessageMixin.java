package com.dog.vaultcontrol.mixin;

import com.dog.vaultcontrol.saving.VaultTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Objects;

@Mixin(ServerPlayer.class)
public abstract class PlayerMessageMixin {
    @Shadow @Nullable public abstract Component getTabListDisplayName();

    @Inject(method = "displayClientMessage", at = @At("HEAD"))
    public void onDisplayMessage(Component p_36216_, boolean p_36217_, CallbackInfo ci) {
        System.out.println(p_36216_.getString());
        if (p_36216_.getString().contains("Teleporting back in 10")) {
            String string = Objects.requireNonNull(this.getTabListDisplayName()).getString();
            VaultTracker.addCompletedVault(string);
        }
    }
}