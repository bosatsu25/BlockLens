package dev.blocklens.gametest.mixin;

import dev.blocklens.fabric.AnalyzerClient;
import dev.blocklens.gametest.AnalyzerTickProbe;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=AnalyzerClient.class,remap=false)
public abstract class AnalyzerTickProbeMixin {
    @Inject(method="tick",at=@At("HEAD"))
    private static void blocklens$measureStart(Minecraft client,CallbackInfo info) { AnalyzerTickProbe.startTick(); }
    @Inject(method="tick",at=@At("RETURN"))
    private static void blocklens$measureEnd(Minecraft client,CallbackInfo info) { AnalyzerTickProbe.endTick(); }
}