package com.lx.bettertellraw.mixins;

import com.lx.bettertellraw.Cmds.btellraw;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {
    @Shadow public abstract CommandDispatcher<ServerCommandSource> getDispatcher();

    @Inject(at = @At("RETURN"), method = "<init>")
    private void registerCommand(CommandManager.RegistrationEnvironment environment, CallbackInfo ci) {
        btellraw.register(getDispatcher());
    }
}
