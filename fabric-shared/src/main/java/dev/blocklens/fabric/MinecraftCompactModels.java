package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.CompactModelTransforms;
import dev.blocklens.core.render.CompactModelTransforms.Context;
import java.util.Map;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Vector3f;

/** Select after ordinary decode; geometry, texture slots and Minecraft parent stay untouched. */
public final class MinecraftCompactModels {
    private MinecraftCompactModels() { }
    public static CuboidModel apply(CuboidModel model, Identifier id, Resource source) {
        var config = BlockLensRuntime.config();
        boolean blocks = config.isEnabled(CapabilityId.COMPACT_BLOCKS);
        boolean items = config.isEnabled(CapabilityId.COMPACT_ITEMS);
        boolean tools = config.isEnabled(CapabilityId.COMPACT_TOOLS);
        if (!(blocks || items || tools) || !id.getNamespace().equals("minecraft")
                || !source.sourcePackId().equals("vanilla")) return model;
        var selected = CompactModelTransforms.find(id.getNamespace(), id.getPath(), source.sourcePackId(),
                blocks, items, tools);
        if (selected == null) return model;
        var old = model.transforms() == null ? ItemTransforms.NO_TRANSFORMS : model.transforms();
        var changed = new ItemTransforms(
                transform(selected, Context.THIRD_LEFT, old.thirdPersonLeftHand()),
                transform(selected, Context.THIRD_RIGHT, old.thirdPersonRightHand()),
                transform(selected, Context.FIRST_LEFT, old.firstPersonLeftHand()),
                transform(selected, Context.FIRST_RIGHT, old.firstPersonRightHand()),
                transform(selected, Context.HEAD, old.head()), transform(selected, Context.GUI, old.gui()),
                transform(selected, Context.GROUND, old.ground()), transform(selected, Context.FIXED, old.fixed()),
                transform(selected, Context.SHELF, old.fixedFromBottom()));
        return new CuboidModel(model.geometry(), model.guiLight(), model.ambientOcclusion(), changed,
                model.textureSlots(), model.parent());
    }
    private static ItemTransform transform(Map<Context, CompactModelTransforms.Transform> profile,
            Context context, ItemTransform original) {
        var value = profile.get(context);
        if (value == null) return original;
        return new ItemTransform(new Vector3f(value.rx(), value.ry(), value.rz()),
                new Vector3f(value.tx(), value.ty(), value.tz()), new Vector3f(value.sx(), value.sy(), value.sz()));
    }
}
