package dev.blocklens.gametest;

import dev.blocklens.core.render.CompactModelTransforms;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.resources.Identifier;

/** Test-only observations after the real model parser and production transform hook. */
public final class CompactModelProbe {
    public static volatile boolean active;
    public record Sample(String owner, ItemTransforms transforms) { }
    private static final Map<String, Sample> MODELS = new ConcurrentHashMap<>();
    private CompactModelProbe() { }
    public static void reset() { MODELS.clear(); active = true; }
    public static void stop() { active = false; MODELS.clear(); }
    public static Map<String, Sample> snapshot() { return Map.copyOf(MODELS); }
    public static void record(Identifier id, String owner, CuboidModel model) {
        if (!active || CompactModelTransforms.find(id.getNamespace(), id.getPath(), "vanilla", true, true, true) == null) return;
        MODELS.put(id.getPath(), new Sample(owner, model.transforms() == null ? ItemTransforms.NO_TRANSFORMS : model.transforms()));
    }
}
