package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;

/**
 * Keeps the pack-vs-mod comparison measuring the pinned source pack's structure against exactly
 * the pack-equivalent capabilities, on every supported line, as a required CI job.
 */
final class PackCostComparisonContractTest {
    private static final String PIN = "070bc338936f89320408e7b78bb6f74856917f5944deecc1a1b52390f5363616";

    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(root().resolve(path));
    }

    @Test
    void twinProfileOraclePinAndPrivateReferencePackAreTheSameArchive() throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                Files.newInputStream(root().resolve("scripts/source_pack_structure.jsonl.gz"))), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            assertTrue(header.contains("\"format\":\"blocklens-pack-structure/1\""));
            assertTrue(header.contains("\"sha256\":\"" + PIN + "\""), "profile must come from the pinned archive");
        }
        assertTrue(read("gametest/java/dev/blocklens/gametest/PackCostComparisonOracle.java")
                .contains("SOURCE_SHA256 = \"" + PIN + "\""));
        assertTrue(read("scripts/external_compatibility_dependencies.json").contains("\"sha256\": \"" + PIN + "\""),
                "the private source run uses the same reference pack as the external pack matrix");
    }

    @Test
    void modConditionEnablesExactlyThePackEquivalentCapabilities() throws IOException {
        List<String> sourceKeys = Files.readAllLines(root().resolve("knowledge/current/capability-contract.tsv")).stream()
                .skip(1).filter(line -> !line.isBlank()).map(line -> line.split("\t")[0]).sorted().toList();
        List<String> first = Arrays.stream(CapabilityId.values()).limit(37).map(CapabilityId::sourceKey).sorted().toList();
        assertEquals(sourceKeys, first, "the first 37 capabilities are the source pack's options");
        String oracle = read("gametest/java/dev/blocklens/gametest/PackCostComparisonOracle.java");
        assertTrue(oracle.contains("PACK_EQUIVALENT_CAPABILITIES = 37;"));
        assertTrue(oracle.contains("capability.ordinal() < PACK_EQUIVALENT_CAPABILITIES"));
        assertTrue(oracle.contains("BlockLensConfig[] configs = {off, off, mod};"));
    }

    @Test
    void comparisonIsARequiredJobOnEverySupportedLine() throws IOException {
        String workflow = read(".github/workflows/pack-cost.yml");
        assertTrue(workflow.contains("workflow_call:"));
        assertFalse(workflow.contains("continue-on-error:"));
        for (String module : SupportedVersionFixtures.MODULES) {
            assertTrue(workflow.contains("module: " + module), module);
        }
        assertTrue(workflow.contains("scripts/pack_structure_twin.py generate"));
        assertTrue(workflow.contains("scripts/pack_structure_twin.py compare"), "twin structure must match the source");
        assertTrue(workflow.contains("-PverifyPackagedRuntime=true -PclientTestFocus=pack-cost"));
        assertTrue(workflow.contains("scripts/summarize_pack_cost.py"));
        assertTrue(workflow.contains("--jfr \"$JAVA_HOME/bin/jfr\""), "the compile profile must be summarized");
        String ci = read(".github/workflows/ci.yml");
        assertTrue(ci.contains("uses: ./.github/workflows/pack-cost.yml"));

        String gradle = read("gradle/version-module.gradle");
        assertTrue(gradle.contains("'settings', 'pack-cost'])"));
        assertTrue(gradle.contains("systemProperty 'blocklens.test.packCost.pack'"));
        String smoke = read("gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java");
        assertTrue(smoke.contains("PackCostComparisonOracle.verify(context, world);"));
        assertTrue(read("gametest/resources/blocklens-gametest.mixins.json").contains("\"SectionCompileTimingProbeMixin\""));
    }
}
