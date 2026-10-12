package dev.blocklens.gametest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;

/** Hold water and vanilla base animation constant in both positive and negative cue-motion samples. */
final class StaticWaterControlFixture implements AutoCloseable {
    private static final String PACK = "blocklens-static-water-control";
    private final ClientGameTestContext context;
    private final List<String> original;
    private final String selected;

    private StaticWaterControlFixture(ClientGameTestContext context, List<String> original, String selected) {
        this.context = context;
        this.original = original;
        this.selected = selected;
    }

    static StaticWaterControlFixture open(ClientGameTestContext context) {
        var original = context.computeOnClient(c -> List.copyOf(c.getResourcePackRepository().getSelectedIds()));
        String selected = context.computeOnClient(c -> {
            var repository = c.getResourcePackRepository();
            repository.reload();
            var matches = repository.getAvailableIds().stream().filter(id -> id.endsWith(PACK)).toList();
            if (matches.size() != 1) throw new AssertionError("expected one generated static water control pack");
            return matches.getFirst();
        });
        var fixture = new StaticWaterControlFixture(context, original, selected);
        try {
            context.runOnClient(c -> {
                var packs = new ArrayList<>(original);
                packs.add(selected);
                c.getResourcePackRepository().setSelected(packs);
            });
            reload(context);
            fixture.verify();
            return fixture;
        } catch (RuntimeException | Error failure) {
            try { fixture.close(); }
            catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    void verify() {
        context.runOnClient(c -> {
            for (String name : new String[] {"water_still", "water_flow"}) {
                var resource = c.getResourceManager().getResource(Identifier.fromNamespaceAndPath(
                        "minecraft", "textures/block/" + name + ".png"));
                if (resource.isEmpty() || !resource.get().sourcePackId().equals(selected)) {
                    throw new AssertionError("static water control did not win resource priority: " + name);
                }
            }
            for (String name : new String[] {"kelp", "kelp_plant"}) {
                var id = Identifier.fromNamespaceAndPath("minecraft", "textures/block/" + name + ".png");
                var resource = c.getResourceManager().getResource(id).orElseThrow();
                var metadata = c.getResourceManager().getResource(id.withPath(id.getPath() + ".mcmeta"));
                if (!resource.sourcePackId().equals("vanilla") || metadata.isEmpty()
                        || !metadata.get().sourcePackId().equals(selected)) {
                    throw new AssertionError("base kelp control must preserve vanilla PNG provenance");
                }
                try {
                    var animation = resource.metadata().getSection(AnimationMetadataSection.TYPE).orElseThrow();
                    var frames = animation.frames().orElseThrow();
                    var texture = resource.metadata().getSection(TextureMetadataSection.TYPE).orElseThrow();
                    if (frames.size() != 2 || frames.stream().anyMatch(frame -> frame.index() != 0)
                            || animation.defaultFrameTime() != 2 || animation.interpolatedFrames()
                            || texture.alphaCutoffBias() != 0.1f) {
                        throw new AssertionError("base kelp metadata did not resolve the static/alpha contract");
                    }
                } catch (IOException failure) {
                    throw new AssertionError("base kelp metadata could not be verified", failure);
                }
            }
        });
    }

    @Override public void close() {
        context.runOnClient(c -> c.getResourcePackRepository().setSelected(original));
        reload(context);
    }

    private static void reload(ClientGameTestContext context) {
        var future = context.computeOnClient(c -> c.reloadResourcePacks());
        context.waitFor(c -> future.isDone(), 1200);
        future.join();
        context.waitFor(SettingsClientAccess::ready, 1200);
        context.waitTicks(2);
    }
}
