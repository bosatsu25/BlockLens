package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.DecorationStateKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class DecorationTargetCatalogTest {
    @Test
    void targetCountsMatchFrozenM0Baseline() {
        Map<CapabilityId, Integer> expected = new EnumMap<>(CapabilityId.class);
        expected.put(CapabilityId.ANVIL, 1);
        expected.put(CapabilityId.BEEHIVE, 1);
        expected.put(CapabilityId.CAMPFIRE, 2);
        expected.put(CapabilityId.FENCE_GATE, 12);
        expected.put(CapabilityId.FROGLIGHT, 3);
        expected.put(CapabilityId.GLAZED_TERRACOTTA, 16);
        expected.put(CapabilityId.GRINDSTONE, 1);
        expected.put(CapabilityId.LOG, 24);
        expected.put(CapabilityId.SLABS, 61);
        expected.put(CapabilityId.STAINED_GLASS, 32);
        expected.put(CapabilityId.STAIRS, 58);
        expected.put(CapabilityId.TRAPDOOR, 21);
        expected.put(CapabilityId.WOOD, 22);

        int total = 0;
        for (Map.Entry<CapabilityId, Integer> entry : expected.entrySet()) {
            List<String> targets = DecorationTargetCatalog.targets(entry.getKey());
            assertEquals(entry.getValue().intValue(), targets.size(), entry.getKey()::sourceKey);
            assertEquals(targets.size(), new HashSet<>(targets).size(),
                    () -> "Duplicate target in " + entry.getKey().sourceKey());
            total += targets.size();
        }
        assertEquals(254, total);
        assertEquals(254, DecorationTargetCatalog.totalBindingCount());
    }

    @Test
    void runtimeCatalogExactlyMatchesMachineReadableM0Contract() throws IOException {
        Path contract = Path.of(System.getProperty("blocklens.repoRoot"))
                .resolve("knowledge/current/capability-contract.tsv");
        List<String> lines = Files.readAllLines(contract);
        int checked = 0;

        for (String line : lines.subList(1, lines.size())) {
            String[] columns = line.split("\\t", -1);
            CapabilityId capability = CapabilityId.bySourceKey().get(columns[0]);
            if (capability == null || !DecorationStateKind.isDecorationCapability(capability)) {
                continue;
            }
            Set<String> expectedTargets = columns[2].isEmpty()
                    ? Set.of()
                    : new HashSet<>(Arrays.asList(columns[2].split("\\|")));
            assertEquals(expectedTargets, new HashSet<>(DecorationTargetCatalog.targets(capability)),
                    capability::sourceKey);
            checked++;
        }
        assertEquals(13, checked);
    }

    @Test
    void catalogIsExactImmutableAndDoesNotBroadenBySuffix() {
        assertTrue(DecorationTargetCatalog.contains(CapabilityId.STAIRS, "oak_stairs"));
        assertTrue(DecorationTargetCatalog.contains(CapabilityId.LOG, "crimson_stem"));
        assertFalse(DecorationTargetCatalog.contains(CapabilityId.STAIRS, "future_example_stairs"));
        assertFalse(DecorationTargetCatalog.contains(CapabilityId.ANVIL, "chipped_anvil"));
        assertTrue(DecorationTargetCatalog.targets(CapabilityId.OBSIDIAN).isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> DecorationTargetCatalog.targets(CapabilityId.ANVIL).add("other"));
        assertThrows(NullPointerException.class, () -> DecorationTargetCatalog.targets(null));
        assertThrows(NullPointerException.class,
                () -> DecorationTargetCatalog.contains(CapabilityId.ANVIL, null));
    }
}
