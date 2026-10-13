package dev.blocklens.gametest.mixin;

import dev.blocklens.gametest.TestWorldShutdown;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class WorldShutdownPumpMixin {
    @Inject(method = "runServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;processPacketsAndTick(Z)V", shift = At.Shift.AFTER))
    private void blocklens$pumpClosingWorld(CallbackInfo info) {
        TestWorldShutdown.beforeServerWait((MinecraftServer) (Object) this);
    }
}
