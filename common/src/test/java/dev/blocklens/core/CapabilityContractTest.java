package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class CapabilityContractTest {
    @Test
    void compiledCatalogMatchesPinnedM0Contract() throws IOException {
        Path root = Path.of(System.getProperty("blocklens.repoRoot"));
        List<String> lines = Files.readAllLines(root.resolve("knowledge/current/capability-contract.tsv"));
        assertFalse(lines.isEmpty());

        Map<String, Boolean> baseline = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] columns = line.split("\\t", -1);
            assertTrue(columns.length >= 9, () -> "Invalid M0 row: " + line);
            baseline.put(columns[0], Boolean.parseBoolean(columns[1]));
        }

        assertEquals(37, baseline.size());
        assertEquals(37, CapabilityId.values().length);
        assertEquals(baseline.keySet(), CapabilityId.bySourceKey().keySet());
        for (CapabilityId capability : CapabilityId.values()) {
            assertEquals(baseline.get(capability.sourceKey()), capability.defaultEnabled(), capability.sourceKey());
        }
    }

    @Test
    void categoryAndPresetCountsStayFrozen() {
        EnumMap<CapabilityId.Category, Integer> counts = new EnumMap<>(CapabilityId.Category.class);
        Set<String> enabled = new HashSet<>();
        for (CapabilityId capability : CapabilityId.values()) {
            counts.merge(capability.category(), 1, Integer::sum);
            if (capability.defaultEnabled()) {
                enabled.add(capability.sourceKey());
            }
        }

        assertEquals(13, counts.get(CapabilityId.Category.DECORATION));
        assertEquals(18, counts.get(CapabilityId.Category.RESOURCE));
        assertEquals(4, counts.get(CapabilityId.Category.OUTLINE));
        assertEquals(2, counts.get(CapabilityId.Category.OTHER));
        assertEquals(Set.of(
                "outline.blueice",
                "outline.deadcoral",
                "outline.powdersnow",
                "outline.sculk_catalyst",
                "others.stringtweaks"), enabled);
    }

    @Test
    void sourceKeyIndexIsStableAndReadOnly() {
        Map<String, CapabilityId> index = CapabilityId.bySourceKey();
        assertEquals(CapabilityId.ANVIL, index.get("deco.anvil"));
        assertEquals(CapabilityId.STRING_TWEAKS, index.get("others.stringtweaks"));
        assertEquals(37, index.size());
        try {
            index.clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Capability source-key index must be unmodifiable");
    }
}
