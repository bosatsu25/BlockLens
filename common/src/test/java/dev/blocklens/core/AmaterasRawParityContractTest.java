package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.render.BlockLensTargetCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Executable guard for the frozen AMATERAS raw-source contract.
 *
 * <p>The source resource pack is intentionally not redistributed byte-for-byte. This test instead
 * freezes the effective RPO-derived logical contract and proves that the current compiled catalog
 * never drops an original source target while allowing only reviewed additive P0 targets.</p>
 */
final class AmaterasRawParityContractTest {
    private static final int FROZEN_CAPABILITIES = 37;
    private static final int FROZEN_EFFECTIVE_GATED_ROOTS = 336;
    private static final int FROZEN_BINDINGS = 323;
    private static final int FROZEN_UNIQUE_TARGETS = 320;

    private static final Map<String, Set<String>> REVIEWED_P0_EXTRAS = Map.of(
            "others.stringtweaks", Set.of("tripwire_hook"),
            "others.nethertweaks", Set.of("polished_basalt"));

    @Test
    void frozenRawSourceContractIsCompleteAndInternallyConsistent() throws IOException {
        List<Row> rows = rows();
        assertEquals(FROZEN_CAPABILITIES, rows.size());

        int effectiveGatedRoots = rows.stream().mapToInt(Row::directFileCount).sum();
        int bindings = rows.stream().mapToInt(row -> row.targets().size()).sum();
        Set<String> uniqueTargets = new HashSet<>();
        rows.forEach(row -> uniqueTargets.addAll(row.targets()));

        assertEquals(FROZEN_EFFECTIVE_GATED_ROOTS, effectiveGatedRoots);
        assertEquals(FROZEN_BINDINGS, bindings);
        assertEquals(FROZEN_UNIQUE_TARGETS, uniqueTargets.size());
    }

    @Test
    void everyFrozenCapabilityAndTargetRemainsRepresentedByCurrentRuntime() throws IOException {
        Map<String, CapabilityId> bySourceKey = CapabilityId.bySourceKey();

        for (Row row : rows()) {
            CapabilityId capability = bySourceKey.get(row.sourceKey());
            assertNotNull(capability, () -> "Missing frozen AMATERAS capability: " + row.sourceKey());

            Set<String> currentTargets = Set.copyOf(BlockLensTargetCatalog.targets(capability));
            assertTrue(currentTargets.containsAll(row.targets()),
                    () -> "Current runtime dropped AMATERAS targets for " + row.sourceKey()
                            + "; frozen=" + row.targets() + " current=" + currentTargets);

            Set<String> extras = new HashSet<>(currentTargets);
            extras.removeAll(row.targets());
            assertEquals(REVIEWED_P0_EXTRAS.getOrDefault(row.sourceKey(), Set.of()), extras,
                    () -> "Unreviewed target drift for frozen capability " + row.sourceKey());
        }
    }

    @Test
    void rawParityAuditDocumentsPhysicalSourceVsSemanticRuntimeBoundary() throws IOException {
        Path root = Path.of(System.getProperty("blocklens.repoRoot"));
        String audit = Files.readString(root.resolve("knowledge/current/amateras-raw-parity-audit.md"));

        assertTrue(audit.contains("337"), "audit must retain observed raw RPO-sidecar count");
        assertTrue(audit.contains("336"), "audit must retain effective gated-root count");
        assertTrue(audit.contains("4,101"), "audit must retain reachable raw-asset evidence");
        assertTrue(audit.contains("95"), "audit must retain source-residue evidence");
        assertTrue(audit.contains("37"), "audit must retain frozen capability count");
        assertTrue(audit.contains("323"), "audit must retain frozen logical binding count");
        assertTrue(audit.contains("320"), "audit must retain frozen unique-target count");
    }

    private static List<Row> rows() throws IOException {
        Path root = Path.of(System.getProperty("blocklens.repoRoot"));
        List<String> lines = Files.readAllLines(root.resolve("knowledge/current/capability-contract.tsv"));
        assertTrue(!lines.isEmpty(), "frozen AMATERAS capability contract must exist");

        return lines.subList(1, lines.size()).stream()
                .filter(line -> !line.isBlank())
                .map(AmaterasRawParityContractTest::parse)
                .toList();
    }

    private static Row parse(String line) {
        String[] columns = line.split("\\t", -1);
        assertTrue(columns.length >= 9, () -> "Invalid AMATERAS contract row: " + line);

        Set<String> targets = columns[2].isBlank()
                ? Set.of()
                : Set.copyOf(Arrays.asList(columns[2].split("\\|")));
        return new Row(columns[0], targets, Integer.parseInt(columns[5]));
    }

    private record Row(String sourceKey, Set<String> targets, int directFileCount) {
    }
}
