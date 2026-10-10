package dev.blocklens.fabric.mixin;

import dev.blocklens.fabric.AnalyzerClient;
import dev.blocklens.fabric.BuilderAssistClient;
import dev.blocklens.fabric.SceneFilterClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class AnalyzerLifecycleMixin {
    @Inject(method={
            "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;)V",
            "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V"},
            at=@At("HEAD"),require=2)
    private void blocklens$clearAnalyzers(CallbackInfo info) {
        AnalyzerClient.clear();
        SceneFilterClient.clear();
        BuilderAssistClient.clear();
    }
}
