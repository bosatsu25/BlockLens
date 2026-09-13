package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M5ActiveResourcePackEvidenceContractTest {
    private static final String PACK_ROOT = "gametest/resources/resourcepacks/m5-active-pack";

    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void representativePackFixturesAndOracleRemainRealClientBacked() throws IOException {
        String smoke = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java"));
        String fixture = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/M5ActiveResourcePackFixture.java"));
        String oracle = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/M5ActiveResourcePackOracle.java"));
        String diamond = Files.readString(root().resolve(
                PACK_ROOT + "/assets/minecraft/models/block/diamond_ore.json"));
        String deepslateRedstone = Files.readString(root().resolve(
                PACK_ROOT + "/assets/minecraft/models/block/deepslate_redstone_ore.json"));
        String obsidian = Files.readString(root().resolve(
                PACK_ROOT + "/assets/minecraft/models/block/obsidian.json"));
        String packMeta = Files.readString(root().resolve(PACK_ROOT + "/pack.mcmeta"));

        assertTrue(smoke.contains("M5ActiveResourcePackOracle.verify(context, singleplayer)"));
        assertTrue(fixture.contains("ResourceLoader.registerBuiltinPack"));
        assertTrue(fixture.contains("PackActivationType.ALWAYS_ENABLED"));
        assertTrue(fixture.contains("m5-active-pack"));
        assertTrue(packMeta.contains("\"min_format\": 71"));
        assertTrue(packMeta.contains("\"max_format\": 2048"));

        assertTrue(oracle.contains("EXPECTED_ENABLED_CAPABILITIES = 3"));
        assertTrue(oracle.contains("CapabilityId.DIAMOND_ORE"));
        assertTrue(oracle.contains("CapabilityId.DEEPSLATE_REDSTONE_ORE"));
        assertTrue(oracle.contains("CapabilityId.OBSIDIAN"));
        assertTrue(oracle.contains("m5-pack-on"));
        assertTrue(oracle.contains("m5-pack-off"));
        assertTrue(oracle.contains("m5-pack-air-control"));
        assertTrue(oracle.contains("baseRetentionPermille"));
        assertTrue(oracle.contains("MIN_BASE_RETENTION_PERMILLE"));
        assertTrue(oracle.contains("geometryIouPermille"));
        assertTrue(oracle.contains("reloadResources(context)"));
        assertTrue(oracle.contains("MinecraftTerrainInvalidator::invalidateAll"));
        assertTrue(oracle.contains("install(original)"));

        assertTrue(diamond.contains("minecraft:block/magenta_concrete"));
        assertTrue(diamond.contains("\"to\": [16, 8, 16]"));
        assertTrue(deepslateRedstone.contains("minecraft:block/lime_concrete"));
        assertTrue(deepslateRedstone.contains("\"from\": [3, 0, 3]"));
        assertTrue(obsidian.contains("minecraft:block/yellow_concrete"));
        assertTrue(obsidian.contains("\"from\": [2, 2, 2]"));
        assertTrue(obsidian.contains("\"to\": [14, 14, 14]"));
    }

    @Test
    void representativePackFixturesRemainGameTestOnlyAndCiArchivesEvidence() throws IOException {
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));
        String fabricMod = Files.readString(root().resolve("gametest/resources/fabric.mod.json"));

        assertTrue(Files.isRegularFile(root().resolve(
                PACK_ROOT + "/assets/minecraft/models/block/diamond_ore.json")));
        assertTrue(Files.notExists(root().resolve(
                "gametest/resources/assets/minecraft/models/block/diamond_ore.json")));
        assertTrue(Files.notExists(root().resolve(
                "common/src/main/resources/assets/minecraft/models/block/diamond_ore.json")));
        assertTrue(Files.notExists(root().resolve(
                "common/src/main/resources/assets/minecraft/models/block/deepslate_redstone_ore.json")));
        assertTrue(Files.notExists(root().resolve(
                "common/src/main/resources/assets/minecraft/models/block/obsidian.json")));
        assertTrue(fabricMod.contains("M5ActiveResourcePackFixture"));
        assertTrue(fabricMod.contains("fabric-resource-loader-v1"));

        assertTrue(ci.contains("Collect M5 active resource-pack evidence"));
        assertTrue(ci.contains("Expected exactly three M5 active-pack screenshots"));
        assertTrue(ci.contains("m5-pack-visual-manifest.txt"));
        assertTrue(ci.contains("blocklens-m5-pack-visual-${{ matrix.minecraft }}"));
    }
}
