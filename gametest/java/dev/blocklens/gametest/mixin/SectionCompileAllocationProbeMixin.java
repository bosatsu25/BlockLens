package dev.blocklens.gametest.mixin;

import dev.blocklens.gametest.SectionCompileAllocationProbe;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shares the verified compile descriptor on all three supported targets. */
@Mixin(SectionCompiler.class)
abstract class SectionCompileAllocationProbeMixin {
    @Inject(method="compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
            at=@At("HEAD"),require=1)
    private void blocklens$compileStart(CallbackInfoReturnable<SectionCompiler.Results> callback) {
        SectionCompileAllocationProbe.head();
    }
    @Inject(method="compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
            at=@At("RETURN"),require=1)
    private void blocklens$compileEnd(CallbackInfoReturnable<SectionCompiler.Results> callback) {
        SectionCompileAllocationProbe.returned();
    }
}
