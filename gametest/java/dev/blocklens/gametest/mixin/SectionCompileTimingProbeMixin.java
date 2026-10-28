package dev.blocklens.gametest.mixin;

import com.mojang.blaze3d.vertex.VertexSorting;
import dev.blocklens.gametest.SectionCompileTimingProbe;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Times the same verified compile descriptor as the allocation probe on all three targets. */
@Mixin(SectionCompiler.class)
abstract class SectionCompileTimingProbeMixin {
    @Inject(method="compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
            at=@At("HEAD"),require=1)
    private void blocklens$timeStart(SectionPos section, RenderSectionRegion region, VertexSorting sorting,
            SectionBufferBuilderPack buffers, CallbackInfoReturnable<SectionCompiler.Results> callback) {
        SectionCompileTimingProbe.head(section.x(), section.y(), section.z());
    }

    @Inject(method="compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
            at=@At("RETURN"),require=1)
    private void blocklens$timeEnd(SectionPos section, RenderSectionRegion region, VertexSorting sorting,
            SectionBufferBuilderPack buffers, CallbackInfoReturnable<SectionCompiler.Results> callback) {
        SectionCompileTimingProbe.returned();
    }
}
