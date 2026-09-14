package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.render.VisibilityTargetCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class M4CapabilityContractTest {
    private record Row(
            String sourceKey,
            String targets,
            String stateProperties,
            int variantCount,
            int directFileCount,
            String directPathHash) {
    }

    @Test
    void commonM4CatalogPreservesFrozenM0RowsAndAddsP0HookAtRuntime() throws IOException {
        Map<String, Row> rows = loadRows();
        assertRow(rows, CapabilityId.BLUE_ICE, "blue_ice", "", 0, 1,
                "b8777ecdb04cad9948e9f83751d0a7044e36d5ea467d1caad20a28fb6cfe27f5");
        assertRow(rows, CapabilityId.DEAD_CORAL,
                String.join("|", VisibilityTargetCatalog.targets(CapabilityId.DEAD_CORAL)), "", 0, 15,
                "c74346508f1282885ee20f9543671cccf9b835acad71e8126ecb856fbea7934f");
        assertRow(rows, CapabilityId.POWDER_SNOW, "powder_snow", "", 0, 1,
                "4291b1974c6845d3f006ecd413c7972f0c204b114ed8edb1fde94e52e478352f");
        assertRow(rows, CapabilityId.SCULK_CATALYST, "sculk_catalyst", "bloom=false,true", 2, 1,
                "25cc5a49483f4046254193499a5b7f28c20df1f41a01b6dbd22a9fa33c651081");
        assertRow(rows, CapabilityId.STRING_TWEAKS,
                "tripwire",
                "attached=false,true;east=false,true;north=false,true;powered=false,true;south=false,true;west=false,true",
                64,
                1,
                "0ede1e28d89226df14b50201f5264d4b6cd93f428fec4165a22cd32a066703a4");

        assertEquals(25, VisibilityTargetCatalog.totalBindingCount());
        assertEquals(List.of("tripwire", "tripwire_hook"),
                VisibilityTargetCatalog.targets(CapabilityId.STRING_TWEAKS));
        assertTrue(VisibilityTargetCatalog.contains(CapabilityId.STRING_TWEAKS, "tripwire_hook"));
    }

    private static void assertRow(
            Map<String, Row> rows,
            CapabilityId capability,
            String expectedTargets,
            String expectedStateProperties,
            int expectedVariants,
            int expectedDirectFiles,
            String expectedPathHash) {
        Row row = rows.get(capability.sourceKey());
        assertNotNull(row, capability.sourceKey());
        assertEquals(expectedTargets, row.targets());
        assertEquals(expectedStateProperties, row.stateProperties());
        assertEquals(expectedVariants, row.variantCount());
        assertEquals(expectedDirectFiles, row.directFileCount());
        assertEquals(expectedPathHash, row.directPathHash());
    }

    private static Map<String, Row> loadRows() throws IOException {
        Path root = Path.of(System.getProperty("blocklens.repoRoot"));
        List<String> lines = Files.readAllLines(root.resolve("knowledge/current/capability-contract.tsv"));
        Map<String, Row> rows = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split("\\t", -1);
            rows.put(fields[0], new Row(
                    fields[0],
                    fields[2],
                    fields[3],
                    Integer.parseInt(fields[4]),
                    Integer.parseInt(fields[5]),
                    fields[7]));
        }
        return rows;
    }
}
