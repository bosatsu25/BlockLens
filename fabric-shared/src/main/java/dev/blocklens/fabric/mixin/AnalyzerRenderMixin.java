package dev.blocklens.fabric.mixin;

import dev.blocklens.fabric.AnalyzerClient;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class AnalyzerRenderMixin {
    @Inject(method="finalizeGizmoCollection",at=@At("HEAD"))
    private void blocklens$drawAnalyzers(CallbackInfo info) {
        AnalyzerClient.draw((LevelRenderer)(Object)this);
    }
}
