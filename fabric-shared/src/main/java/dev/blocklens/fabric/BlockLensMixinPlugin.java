package dev.blocklens.fabric;

import dev.blocklens.core.render.BBEChestCompatibility;
import dev.blocklens.core.render.SodiumTerrainCompatibility;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Optional renderer boundaries are selected once from metadata before external classes link. */
public final class BlockLensMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("dev.blocklens.fabric.mixin.BBEBrightChestSpriteMixin")
                || mixinClassName.equals("dev.blocklens.fabric.mixin.BBEBrightChestEmitterMixin")) {
            return BBEChestCompatibility.supported(version("minecraft"), version("betterblockentities"), version("sodium"));
        }
        return !mixinClassName.equals("dev.blocklens.fabric.mixin.SodiumTerrainMixin")
                || SodiumTerrainCompatibility.supported(version("minecraft"), version("sodium"));
    }

    private static String version(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse(null);
    }

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
