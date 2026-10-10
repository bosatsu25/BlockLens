package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RepositoryContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void everyCapabilityHasEnglishAndJapaneseName() throws IOException {
        String english = Files.readString(root().resolve("common/src/main/resources/assets/blocklens/lang/en_us.json"));
        String japanese = Files.readString(root().resolve("common/src/main/resources/assets/blocklens/lang/ja_jp.json"));

        for (CapabilityId capability : CapabilityId.values()) {
            String id = capability.sourceKey().substring(capability.sourceKey().indexOf('.') + 1);
            String key = "\"blocklens.capability." + id + "\"";
            assertTrue(english.contains(key), () -> "Missing English key: " + key);
            assertTrue(japanese.contains(key), () -> "Missing Japanese key: " + key);
        }
    }

    @Test
    void allVersionModulesRemainThinExactClientOnlyAdapters() throws IOException {
        assertVersionModule("mc26_1_2", "rootProject.minecraft_26_1_2", "rootProject.fabric_api_26_1_2");
        assertVersionModule("mc26_2", "rootProject.minecraft_26_2", "rootProject.fabric_api_26_2");
        assertVersionModule("mc26_3", "rootProject.minecraft_26_3", "rootProject.fabric_api_26_3");
    }

    @Test
    void clientSmokeIsSharedAndExecutedForEveryVersion() throws IOException {
        Path gameTestSource = root().resolve("gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java");
        Path adapterOracleSource = root().resolve("gametest/java/dev/blocklens/gametest/MinecraftStateAdapterOracle.java");
        Path targetOracleSource = root().resolve("gametest/java/dev/blocklens/gametest/MinecraftDecorationTargetOracle.java");
        Path retiredPlainJUnitOracle = root().resolve(
                "version-tests/java/dev/blocklens/fabric/MinecraftStateAdapterTest.java");
        Path gameTestMetadata = root().resolve("gametest/resources/fabric.mod.json");
        String source = Files.readString(gameTestSource);
        String adapterOracle = Files.readString(adapterOracleSource);
        String targetOracle = Files.readString(targetOracleSource);
        String metadata = Files.readString(gameTestMetadata);
        String convention = Files.readString(root().resolve("gradle/version-module.gradle"));
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));

        assertTrue(source.contains("implements FabricClientGameTest"));
        assertTrue(source.contains("EXPECTED_CAPABILITY_COUNT = 53"));
        assertTrue(source.contains("context.worldBuilder().create()"));
        assertTrue(source.contains("persisted.asMap().equals(BlockLensRuntime.config().asMap())"));
        assertTrue(source.contains("MinecraftStateAdapterOracle.verify()"));
        assertTrue(source.contains("MinecraftDecorationTargetOracle.verify()"));

        assertTrue(adapterOracle.contains("MinecraftStateAdapter.interpret"));
        assertTrue(adapterOracle.contains("BuiltInRegistries.BLOCK.getValue"));
        assertTrue(adapterOracle.contains("white_glazed_terracotta"));
        assertTrue(adapterOracle.contains("white_stained_glass_pane"));
        assertTrue(targetOracle.contains("MinecraftDecorationTargetIndex.build()"));
        assertTrue(targetOracle.contains("bindingCount() == 366"));
        assertTrue(targetOracle.contains("mappedCount() == 328"));
        assertTrue(targetOracle.contains("BlockLensTargetCatalog.targets"));
        assertTrue(targetOracle.contains("CapabilityId.NETHER_TWEAKS"));
        assertTrue(targetOracle.contains("CapabilityId.CRYING_OBSIDIAN"));
        assertTrue(targetOracle.contains("CapabilityId.NETHER_GOLD_ORE"));
        assertTrue(targetOracle.contains("CapabilityId.NETHER_QUARTZ_ORE"));
        assertTrue(targetOracle.contains("tripwire_hook"));
        assertTrue(targetOracle.contains("polished_basalt"));
        assertTrue(targetOracle.contains("MinecraftDecorationModelPlugin.isModelPipelineReady()"));
        assertTrue(targetOracle.contains("MinecraftDecorationModelPlugin.wrappedModelCount() >= 328"));
        assertFalse(Files.exists(retiredPlainJUnitOracle),
                "mapped BlockState oracle must run after real client bootstrap, not in plain JUnit");

        assertTrue(metadata.contains("\"fabric-client-gametest\""));
        assertTrue(metadata.contains("dev.blocklens.gametest.BlockLensSmokeClientGameTest"));

        assertTrue(convention.contains("enableClientGameTests = true"));
        assertTrue(convention.contains("rootProject.file('gametest/java')"));
        assertTrue(convention.contains("rootProject.file('gametest/resources')"));
        assertTrue(ci.contains(":versions:${{ matrix.module }}:runClientGameTest"));

        // The loading-overlay API moved in 26.2; only this tiny test bridge may vary.
        assertSettingsOverlayBridge("mc26_1_2", "client.getOverlay()");
        assertSettingsOverlayBridge("mc26_2", "client.gui.overlay()");
        assertSettingsOverlayBridge("mc26_3", "client.gui.overlay()");
    }

    private void assertSettingsOverlayBridge(String module, String overlayApi) throws IOException {
        Path directory = SupportedVersionFixtures.testSourceRoot(root(), module);
        Path bridge = directory.resolve("java/dev/blocklens/gametest/SettingsClientAccess.java");
        var roots = new java.util.LinkedHashSet<Path>();
        roots.add(directory);
        Path moduleRoot = root().resolve("versions").resolve(module);
        roots.add(moduleRoot.resolve("src/gametest"));
        roots.add(moduleRoot.resolve("src/compat/gametest"));
        var allowed = java.util.Set.of("SettingsClientAccess.java", "FireTransformProbeMixin.java",
                "HeldTransformProbeMixin.java", "SceneRenderAccess.java");
        for (Path sourceRoot : roots) {
            if (!Files.isDirectory(sourceRoot)) continue;
            try (var paths = Files.walk(sourceRoot)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    assertTrue(allowed.contains(path.getFileName().toString()),
                            "Only narrow Minecraft UI/render API test bridges may vary");
                    assertTrue(Files.readString(path).lines().count() <= 35,
                            "Version-specific test bridges must not duplicate an oracle");
                }
            }
        }
        String source = Files.readString(bridge);
        assertTrue(source.contains("static boolean ready(Minecraft client)"));
        assertTrue(source.contains(overlayApi + " == null"));
        assertTrue(source.lines().count() <= 20, "The version bridge must not duplicate an oracle");
    }

    @Test
    void sharedRenderContractsAndExactTargetCatalogsStayPresent() throws IOException {
        Path decorationPolicy = root().resolve(
                "common/src/main/java/dev/blocklens/core/render/DecorationRenderPolicy.java");
        Path resourcePolicy = root().resolve(
                "common/src/main/java/dev/blocklens/core/render/ResourceHighlightPolicy.java");
        Path visibilityQuadPolicy = root().resolve(
                "common/src/main/java/dev/blocklens/core/render/VisibilityQuadCuePolicy.java");
        String descriptor = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/DecorationRenderDescriptor.java"));
        String cue = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/DecorationVisualCue.java"));
        String quadPolicy = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/DecorationQuadCuePolicy.java"));
        String catalog = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/DecorationTargetCatalog.java"));
        String unifiedCatalog = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/BlockLensTargetCatalog.java"));
        String resourceCatalog = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/ResourceTargetCatalog.java"));
        String visibilityCatalog = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/VisibilityTargetCatalog.java"));
        String netherCatalog = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/NetherTweaksTargetCatalog.java"));
        String config = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/BlockLensConfig.java"));
        String v2612 = Files.readString(root().resolve(
                "versions/mc26_1_2/src/main/java/dev/blocklens/fabric/MinecraftDecorationTargetIndex.java"));
        String v262 = Files.readString(root().resolve(
                "versions/mc26_2/src/main/java/dev/blocklens/fabric/MinecraftDecorationTargetIndex.java"));

        assertFalse(Files.exists(decorationPolicy),
                "test-only M3 policy wrapper must not return to the runtime JAR");
        assertFalse(Files.exists(resourcePolicy),
                "test-only M5 policy wrapper must not return to the runtime JAR");
        assertFalse(Files.exists(visibilityQuadPolicy),
                "M4 adapter must use the enum-owned immutable instruction directly");
        assertTrue(descriptor.contains("DecorationVisualCue.forCapability"));
        assertTrue(cue.contains("OPAQUE_STAINED_GLASS"));
        assertTrue(quadPolicy.contains("case OPAQUE_STAINED_GLASS -> DecorationQuadInstruction.OPAQUE"));
        assertTrue(quadPolicy.contains("case X ->"));
        assertTrue(quadPolicy.contains("case Y ->"));
        assertTrue(quadPolicy.contains("case Z ->"));
        assertTrue(catalog.contains("CapabilityId.STAIRS"));
        assertTrue(catalog.contains("CapabilityId.STAINED_GLASS"));
        assertTrue(catalog.contains("CapabilityId.TRAPDOOR"));
        assertTrue(resourceCatalog.contains("CapabilityId.ANCIENT_DEBRIS"));
        assertTrue(resourceCatalog.contains("CapabilityId.DEEPSLATE_DIAMOND_ORE"));
        assertTrue(resourceCatalog.contains("CapabilityId.CRYING_OBSIDIAN"));
        assertTrue(resourceCatalog.contains("CapabilityId.NETHER_GOLD_ORE"));
        assertTrue(resourceCatalog.contains("CapabilityId.NETHER_QUARTZ_ORE"));
        assertTrue(visibilityCatalog.contains("tripwire_hook"));
        assertTrue(netherCatalog.contains("CapabilityId.NETHER_TWEAKS"));
        assertTrue(netherCatalog.contains("polished_basalt"));
        assertTrue(unifiedCatalog.contains("DecorationTargetCatalog.targets(capability)"));
        assertTrue(unifiedCatalog.contains("VisibilityTargetCatalog.targets(capability)"));
        assertTrue(unifiedCatalog.contains("ResourceTargetCatalog.targets(capability)"));
        assertTrue(unifiedCatalog.contains("NetherTweaksTargetCatalog.targets(capability)"));
        assertTrue(config.contains("private final long enabledMask"));
        assertTrue(config.contains("public long enabledMask()"));
        assertTrue(v2612.contains("BlockLensTargetCatalog.targets(capability)"));
        assertTrue(v262.contains("BlockLensTargetCatalog.targets(capability)"));
        assertTrue(v2612.contains("TargetCapabilityIndex.builder(BuiltInRegistries.BLOCK.size())"));
        assertTrue(v262.contains("TargetCapabilityIndex.builder(BuiltInRegistries.BLOCK.size())"));
        assertFalse(catalog.contains("endsWith(\"_stairs\")"),
                "exact target scope must not silently broaden by suffix");
    }

    @Test
    void unifiedModelPipelineRemainsZeroScanAndHasPrimitiveOffFastPath() throws IOException {
        for (String module : SupportedVersionFixtures.MODULES) {
            Path sourceRoot = SupportedVersionFixtures.sourceRoot(root(), module);
            String client = Files.readString(sourceRoot.resolve("BlockLensClient.java"));
            String plugin = Files.readString(sourceRoot.resolve("MinecraftDecorationModelPlugin.java"));
            String model = Files.readString(sourceRoot.resolve("MinecraftDecorationModel.java"));

            assertTrue(client.contains("MinecraftDecorationModelPlugin.register()"));
            assertTrue(plugin.contains("ModelModifier.WRAP_PHASE"));
            assertTrue(plugin.contains("BuiltInRegistries.BLOCK.getId(state.getBlock())"));
            assertTrue(plugin.contains("targetIndex.mask(rawId)"));
            assertTrue(plugin.contains("MinecraftStateAdapter.interpret(capability, state)"));

            assertTrue(model.contains("BlockLensRuntime.config().enabledMask() & representedRenderableMask"));
            assertTrue(model.contains("if (enabledCapabilities == 0L)"));
            assertTrue(model.contains("super.emitQuads(emitter, level, pos, state, random, cullTest)"));
            assertTrue(model.contains("quad.chunkLayer(ChunkSectionLayer.SOLID)"));
            assertTrue(model.contains("quad.multiplyColor(instruction.multiplyArgb())"));
            assertTrue(model.contains("quad.emissive(true)"));
            assertTrue(model.contains("wrapped.createGeometryKey"));
            assertTrue(model.contains("instruction = visibility.cue().instruction()"));
            assertTrue(model.contains("ResourceHighlightCue.forCapability"));
            assertFalse(model.contains("VisibilityQuadCuePolicy"));

            assertFalse(plugin.contains("level.getBlockState("),
                    "model classification must not scan world state");
            assertFalse(model.contains("BuiltInRegistries"),
                    "render hot path must not perform registry lookup");
            assertFalse(model.contains("MinecraftStateAdapter.interpret"),
                    "render hot path must not reinterpret BlockState");
        }
    }

    @Test
    void runtimeArtifactAuditsStayEnabled() throws IOException {
        String convention = Files.readString(root().resolve("gradle/version-module.gradle"));
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));

        String audit = Files.readString(root().resolve("gradle/runtime-container-audit.gradle"));
        assertTrue(convention.contains("runtime-container-audit.gradle"));
        assertTrue(audit.contains("forbiddenPayloadMarkers"));
        assertTrue(audit.contains("respackopts.json5"));
        assertTrue(audit.contains("Runtime archive contains forbidden test or local residue"));
        assertTrue(convention.contains("MinecraftDecorationModelPlugin.class"));
        assertTrue(convention.contains("DecorationQuadCuePolicy.class"));
        assertTrue(convention.contains("runtimeBaselineBytes"));
        assertTrue(convention.contains("runtimeReleaseBudgetBytes"));
        assertTrue(convention.contains("writeRuntimeJarSizeReport"));
        assertTrue(ci.contains("Verify reproducible runtime JAR"));
        assertTrue(ci.contains("first_sha"));
        assertTrue(ci.contains("second_sha"));
    }

    @Test
    void engineeringGraphLoopBilingualReadmesAndMigrationDirectionRemainDocumented() throws IOException {
        String agents = Files.readString(root().resolve("AGENTS.md"));
        String index = Files.readString(root().resolve("knowledge/index.md"));
        String graphLoop = Files.readString(root().resolve("knowledge/current/engineering-loop.md"));
        String migration = Files.readString(root().resolve("knowledge/current/chisetweaks-migration.md"));
        String englishReadme = Files.readString(root().resolve("README.md"));
        String japaneseReadme = Files.readString(root().resolve("README_ja.md"));

        assertTrue(agents.contains("## Engineering Graph Loop"));
        assertTrue(agents.contains("DIAGNOSE -> FIX -> VERIFY"));
        assertTrue(agents.contains("DONE`, `BLOCKED`, or explicitly scoped `PARTIAL`"));

        assertTrue(index.contains("current/engineering-loop.md"));
        assertTrue(index.contains("current/m3-visual-semantics.md"));
        assertTrue(index.contains("current/m4-visual-semantics.md"));
        assertTrue(index.contains("current/chisetweaks-migration.md"));
        assertTrue(migration.contains("BlockLens is the destination product"));
        assertTrue(migration.contains("runtime capabilities: **40**"));
        assertTrue(migration.contains("**100 KiB release budget remains unchanged**"));
        assertTrue(graphLoop.contains("DISCOVER"));
        assertTrue(graphLoop.contains("IMPLEMENT"));
        assertTrue(graphLoop.contains("SELF REVIEW"));
        assertTrue(graphLoop.contains("ISSUE UPDATE"));
        assertTrue(graphLoop.contains("DIAGNOSE"));
        assertTrue(graphLoop.contains("PARTIAL"));
        assertTrue(graphLoop.contains("```mermaid"));

        assertTrue(englishReadme.contains("[日本語](README_ja.md)"));
        assertTrue(japaneseReadme.contains("[English](README.md)"));
        assertTrue(englishReadme.contains("## Engineering Graph Loop"));
        assertTrue(japaneseReadme.contains("## Engineering Graph Loop"));
        assertTrue(englishReadme.contains("```mermaid"));
        assertTrue(japaneseReadme.contains("```mermaid"));
    }

    @Test
    void startupSourceContainsNoNetworkTelemetryOrReflectionDiscovery() throws IOException {
        List<String> banned = List.of(
                "Class.forName(",
                "ServiceLoader.load(",
                "java.net.http",
                "java.net.URL",
                "HttpClient.newHttpClient",
                "Telemetry",
                "UpdateChecker");

        for (Path sourceRoot : List.of(
                root().resolve("common/src/main/java"),
                root().resolve("versions/mc26_1_2/src/main/java"),
                root().resolve("versions/mc26_2/src/main/java"),
                root().resolve("versions/mc26_2/src/compat/java"),
                root().resolve("versions/mc26_3/src/main/java"),
                root().resolve("fabric-shared/src/main/java"))) {
            try (var paths = Files.walk(sourceRoot)) {
                for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    for (String marker : banned) {
                        assertFalse(source.contains(marker), () -> "Forbidden startup/runtime marker '" + marker + "' in " + file);
                    }
                }
            }
        }
    }

    private static void assertVersionModule(String module, String minecraftProperty, String apiProperty) throws IOException {
        Path moduleRoot = root().resolve("versions").resolve(module);
        String build = Files.readString(moduleRoot.resolve("build.gradle"));
        String metadata = Files.readString(moduleRoot.resolve("src/main/resources/fabric.mod.json"));

        assertTrue(build.contains(minecraftProperty));
        assertTrue(build.contains(apiProperty));
        assertTrue(build.contains("gradle/version-module.gradle"));
        assertTrue(metadata.contains("\"id\": \"blocklens\""));
        assertTrue(metadata.contains("\"environment\": \"client\""));
        assertTrue(metadata.contains("\"minecraft\": \"=${minecraft_version}\""));
        assertTrue(metadata.contains("dev.blocklens.fabric.BlockLensClient"));
        assertFalse(metadata.contains("\"main\":"));
        assertFalse(metadata.contains("\"server\":"));
    }
}
