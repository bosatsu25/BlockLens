package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.blocklens.core.render.NetherTweaksTargetCatalog;
import dev.blocklens.core.render.ResourceTargetCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class M5M6CapabilityContractTest {
    private record Row(
            String targets,
            String stateProperties,
            int variantCount,
            int directFileCount,
            String directPathHash) {
    }

    @Test
    void M5ResourceCatalogMatchesAllEighteenFrozenM0Rows() throws IOException {
        Map<String, Row> rows = loadRows();
        Map<CapabilityId, String> hashes = new EnumMap<>(CapabilityId.class);
        hashes.put(CapabilityId.ANCIENT_DEBRIS, "744a1e0e907d46eca89d30a4b10111fabad5ed2e390ace98ba6481f84724507b");
        hashes.put(CapabilityId.COAL_ORE, "a61570c0a9401ba93e1f1e2884b6d200ad81fa7a95bb53136c98a017d96e0b09");
        hashes.put(CapabilityId.COPPER_ORE, "32bd21f1f1ed7002bcfb4ed035ffb0e29d4da1b33534cb198e586a06d34d79aa");
        hashes.put(CapabilityId.DEEPSLATE_COAL_ORE, "ec73667f055dcfd107207ec5952b799d68e0e0fe3d41b90cf808c7ce895ff8df");
        hashes.put(CapabilityId.DEEPSLATE_COPPER_ORE, "a7048b41f0117ff813af83565a72d20fe1d4e06a90b913fcc746ff090eed1ae9");
        hashes.put(CapabilityId.DEEPSLATE_DIAMOND_ORE, "de4517e82d649c77ca1a3bce4a0a344c3f51a14d69d5190b30291b65a4796f97");
        hashes.put(CapabilityId.DEEPSLATE_EMERALD_ORE, "6d4973de277c29524c0191c154b81af939518a5cb5277c3cd48c21d60aa8d723");
        hashes.put(CapabilityId.DEEPSLATE_GOLD_ORE, "7e095c4a085c571aab153c55c6f23734eeed4042318d0fe1fac9c5f534e80aa0");
        hashes.put(CapabilityId.DEEPSLATE_IRON_ORE, "2522a68501834fdc27924bbf0f57320b5a485339b607d24b529b50bd54e3163e");
        hashes.put(CapabilityId.DEEPSLATE_LAPIS_ORE, "8b8205cdf17d148f9a0ccbb1d99c5ce5aaa51b6ddcc3592e78457667a34e34d0");
        hashes.put(CapabilityId.DEEPSLATE_REDSTONE_ORE, "4300aeed263d1386d14fc6846a3cbc24ead8664007ca35b96f834852ef2bf933");
        hashes.put(CapabilityId.DIAMOND_ORE, "53b544581aed98d12a66b60b61852f965caf87a9da0f1c2c53400a42763b0494");
        hashes.put(CapabilityId.EMERALD_ORE, "b428a0d752ee016fb4ae28109b8767d4d67d94c239caf295d6e940597d109190");
        hashes.put(CapabilityId.GOLD_ORE, "eeec07eda7d406f3abb90bb977a2c9cd6d2b87d15d0c07c6f5382f60dcaabf1f");
        hashes.put(CapabilityId.IRON_ORE, "a8c47a1d3fe565f5f644e94abc6a44f328bebc1bf4094da0051291a2d0bc0766");
        hashes.put(CapabilityId.LAPIS_ORE, "bea3f523a3cb465968f061273ec823e5c1636b1ee2a2881c4a6d9e651002b25e");
        hashes.put(CapabilityId.OBSIDIAN, "ca0efbbc1255dd487d22967eeb6c03cb68aefbcbf8ec83dc9af7b73cd8dbd393");
        hashes.put(CapabilityId.REDSTONE_ORE, "85880309d416ae679d3d905850cc0902dd7f9f4b49c0c557ae13e6428ae258f8");

        assertEquals(18, hashes.size());
        for (Map.Entry<CapabilityId, String> entry : hashes.entrySet()) {
            CapabilityId capability = entry.getKey();
            Row row = rows.get(capability.sourceKey());
            assertNotNull(row, capability.sourceKey());
            assertEquals(String.join("|", ResourceTargetCatalog.targets(capability)), row.targets());
            assertEquals("", row.stateProperties());
            assertEquals(1, row.variantCount());
            assertEquals(1, row.directFileCount());
            assertEquals(entry.getValue(), row.directPathHash());
        }
        assertEquals(18, ResourceTargetCatalog.totalBindingCount());
    }

    @Test
    void M6NetherTweaksTargetsAndHashMatchFrozenM0RowWithoutInventingState() throws IOException {
        Row row = loadRows().get(CapabilityId.NETHER_TWEAKS.sourceKey());
        assertNotNull(row);
        assertEquals(String.join("|", NetherTweaksTargetCatalog.targets(CapabilityId.NETHER_TWEAKS)), row.targets());
        assertEquals("", row.stateProperties());
        assertEquals(0, row.variantCount());
        assertEquals(34, row.directFileCount());
        assertEquals(NetherTweaksTargetCatalog.DIRECT_PATH_SET_SHA256, row.directPathHash());
        assertEquals(27, NetherTweaksTargetCatalog.totalBindingCount());
    }

    private static Map<String, Row> loadRows() throws IOException {
        Path root = Path.of(System.getProperty("blocklens.repoRoot"));
        List<String> lines = Files.readAllLines(root.resolve("knowledge/current/capability-contract.tsv"));
        Map<String, Row> rows = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split("\\t", -1);
            rows.put(fields[0], new Row(
                    fields[2],
                    fields[3],
                    Integer.parseInt(fields[4]),
                    Integer.parseInt(fields[5]),
                    fields[7]));
        }
        return rows;
    }
}
