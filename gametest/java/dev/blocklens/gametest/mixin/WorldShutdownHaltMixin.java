package dev.blocklens.gametest.mixin;

import dev.blocklens.gametest.TestWorldShutdown;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServer.class)
public abstract class WorldShutdownHaltMixin {
    @Inject(method = "halt", at = @At("RETURN"))
    private void blocklens$recordClientHalt(boolean wait, CallbackInfo info) {
        TestWorldShutdown.afterHalt((IntegratedServer) (Object) this, Minecraft.getInstance().isSameThread());
    }
}
