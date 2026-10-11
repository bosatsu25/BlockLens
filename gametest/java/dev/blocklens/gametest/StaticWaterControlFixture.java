package dev.blocklens.gametest;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;

/** Hold the fluid-texture background constant in both positive and negative kelp motion samples. */
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
