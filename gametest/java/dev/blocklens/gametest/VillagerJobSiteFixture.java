package dev.blocklens.gametest;

import dev.blocklens.testing.IdentityBoundFixture;
import dev.blocklens.fabric.AnalyzerClient;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;

/** A scoped, test-only JOB_SITE input; normal clients never synthesize unavailable memories. */
final class VillagerJobSiteFixture implements AutoCloseable {
    private static final BlockPos SITE = new BlockPos(2, -59, 6);
    private static VillagerJobSiteFixture active;

    static {
        // Register once. The listener retains no world/entity after the scoped fixture closes.
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            var fixture = active;
            if (fixture != null) fixture.refresh(level, entity);
        });
    }

    private final ClientGameTestContext context;
    private final AnalyzerReturnProbe.Seed seed;
    private final int entityId;
    private final UUID entityUuid;
    private final IdentityBoundFixture<Villager> binding;

    private VillagerJobSiteFixture(ClientGameTestContext context, ClientLevel level, Villager villager) {
        this.context = context;
        seed = new AnalyzerReturnProbe.Seed(level, villager);
        entityId = villager.getId();
        entityUuid = villager.getUUID();
        var site = GlobalPos.of(level.dimension(), SITE);
        binding = new IdentityBoundFixture<>(level,
                entity -> entity.getBrain().setMemory(MemoryModuleType.JOB_SITE, site));
    }

    static VillagerJobSiteFixture open(ClientGameTestContext context, int timeoutTicks) {
        context.waitFor(client -> find(client.level) != null, timeoutTicks);
        return context.computeOnClient(client -> {
            require(active == null, "nested villager JOB_SITE fixture");
            var villager = find(client.level);
            require(villager != null, "controlled villager disappeared before fixture binding");
            var fixture = new VillagerJobSiteFixture(context, client.level, villager);
            fixture.refresh(client.level, villager);
            active = fixture;
            return fixture;
        });
    }

    AnalyzerReturnProbe.Seed seed() { return seed; }

    private void refresh(ClientLevel level, Entity entity) {
        if (level != seed.level() || !(entity instanceof Villager villager)
                || entity.getId() != entityId || !entityUuid.equals(entity.getUUID()) || !entity.isAlive()) return;
        // Repeated notifications for the same instance must not repair an erased memory.
        binding.refresh(level, villager);
    }

    @Override public void close() {
        context.runOnClient(client -> {
            if (active == this) active = null;
        });
    }

    /** Reproduces the lost-input boundary with actual client entity storage, then checks teardown. */
    static void verifyReplacement(ClientGameTestContext context) {
        try (var fixture = open(context, 1200)) {
            context.runOnClient(client -> {
                var replacement = replace(client.level);
                require(hasSite(replacement, client.level), "replacement villager lost controlled JOB_SITE input");
                replacement.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
                fixture.refresh(client.level, replacement);
                require(replacement.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty(),
                        "same-instance fixture refresh concealed erased JOB_SITE memory");
                replacement = replace(client.level);
                require(hasSite(replacement, client.level), "second replacement did not receive controlled JOB_SITE input");
            });
            context.waitFor(client -> AnalyzerClient.engine().markers().stream().anyMatch(marker -> marker.kind() == 4), 600);
        }
        context.runOnClient(client -> {
            var replacement = replace(client.level);
            require(replacement.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty(),
                    "closed fixture retained its entity-load input hook");
        });
        context.waitFor(client -> AnalyzerClient.engine().markers().stream().noneMatch(marker -> marker.kind() == 4), 600);
    }

    private static Villager replace(ClientLevel level) {
        var previous = find(level);
        require(previous != null, "controlled replacement villager missing");
        // Preserve the controlled Villager's type without a version-specific registry constant.
        @SuppressWarnings("unchecked")
        var type = (EntityType<? extends Villager>) previous.getType();
        var replacement = new Villager(type, level);
        replacement.setId(previous.getId());
        replacement.setUUID(previous.getUUID());
        replacement.snapTo(previous.getX(), previous.getY(), previous.getZ(), previous.getYRot(), previous.getXRot());
        replacement.setVillagerData(previous.getVillagerData());
        replacement.setNoAi(true);
        require(replacement.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty(),
                "replacement control unexpectedly contains JOB_SITE memory");
        level.removeEntity(previous.getId(), Entity.RemovalReason.DISCARDED);
        level.addEntity(replacement);
        require(level.getEntity(replacement.getId()) == replacement && replacement != previous,
                "client entity storage did not replace the controlled instance");
        return replacement;
    }

    private static boolean hasSite(Villager villager, ClientLevel level) {
        return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                .filter(site -> site.dimension().equals(level.dimension()) && site.pos().equals(SITE)).isPresent();
    }

    private static Villager find(ClientLevel level) {
        if (level != null) {
            for (var entity : level.entitiesForRendering()) {
                if (entity instanceof Villager villager && villager.isAlive()) return villager;
            }
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
