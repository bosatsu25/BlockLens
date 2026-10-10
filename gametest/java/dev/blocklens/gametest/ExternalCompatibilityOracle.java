package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.testing.ExternalLifecycleDiagnostics;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;

/** Opt-in external rendering evidence; does not replace the full default or M8 graph. */
final class ExternalCompatibilityOracle {
    private ExternalCompatibilityOracle() { }

    static void verify(ClientGameTestContext context) {
        require(Boolean.getBoolean("blocklens.test.packagedRuntime"), "External tests require packaged runtime");
        Path directory = Path.of(System.getProperty("blocklens.test.external.directory"));
        String profile = System.getProperty("blocklens.test.external.profile");
        require(List.of("shader-off", "shader-on", "vulkan", "packs", "masa").contains(profile),
                "Unexpected external rendering profile");
        Path output = FabricLoader.getInstance().getGameDir().resolve("external-compatibility");
        Properties evidence = new Properties();
        evidence.setProperty("schema", "1");
        evidence.setProperty("profile", profile);
        evidence.setProperty("minecraft", BlockLensRuntime.minecraftVersion());
        evidence.setProperty("status", "started");
        evidence.setProperty("fullGraph", "false");
        evidence.setProperty("captureMode", ExternalFrameCapture.required()
                ? "completed-native-frame" : "fabric-test-capture");
        try (var lifecycle = ExternalLifecycleDiagnostics.open(output, profile.equals("masa"))) {
            Properties manifest = loadManifest(directory);
            require(profile.equals(manifest.getProperty("profile")), "Prepared profile differs");
            require(BlockLensRuntime.minecraftVersion().equals(manifest.getProperty("minecraft")),
                    "Prepared Minecraft version differs");
            Files.createDirectories(output);
            writeEvidence(output, evidence);
            lifecycle.phase("runtime_verify");
            context.runOnClient(client -> {
                PackagedRuntimeOracle.verify();
                Backend backend = ExternalBackendProbe.read();
                String actual = backend.name().toLowerCase(Locale.ROOT);
                String expected = manifest.getProperty("expectedBackend");
                require(List.of("opengl", "vulkan").contains(actual), "Unrecognized active graphics backend");
                require(actual.equals(expected), "Active backend differs; requested backend may have fallen back");
                evidence.setProperty("backend", actual);
                evidence.setProperty("backendDevice", boundedNativeValue(backend.device()));
                evidence.setProperty("backendDriver", boundedNativeValue(backend.driver()));
                evidence.setProperty("backendVerified", "true");
                evidence.setProperty("packagedRuntime", "true");
            });
            if (profile.startsWith("shader-")) {
                verifyMod("iris", manifest, evidence);
                verifyMod("sodium", manifest, evidence);
                String shaderName = manifest.getProperty("shaderPack");
                require("ComplementaryReimagined_r5.9.3.zip".equals(shaderName), "Unrecorded shader pack name");
                Path shaderFile = FabricLoader.getInstance().getGameDir().resolve("shaderpacks").resolve(shaderName);
                require(Files.isRegularFile(shaderFile) && Files.size(shaderFile) <= 8 * 1024 * 1024,
                        "Shader pack input missing or oversized");
                require(sha256(Files.readAllBytes(shaderFile)).equals(manifest.getProperty("shaderSha256")),
                        "Installed shader pack bytes differ");
                evidence.setProperty("shaderPackSha256", manifest.getProperty("shaderSha256"));
            }
            if (profile.equals("masa")) {
                verifyMod("litematica", manifest, evidence);
                verifyMod("malilib", manifest, evidence);
            }
            lifecycle.phase("world_create");
            try (var world = context.worldBuilder().create()) {
                lifecycle.phase("world_ready");
                context.waitTicks(30);
                verifyShader(context, manifest, evidence);
                if (profile.equals("masa")) {
                    lifecycle.phase("schematic_verify");
                    BuilderAssistOracle.verifyLitematica(context, world, lifecycle);
                    evidence.setProperty("schematicRead", "true");
                    lifecycle.phase("schematic_manifest_copy");
                    Path source = FabricLoader.getInstance().getGameDir().resolve("builder-assist")
                            .resolve("builder-schematic-manifest.txt");
                    require(Files.isRegularFile(source) && Files.size(source) <= 16384,
                            "Schematic native evidence missing or oversized");
                    Files.copy(source, output.resolve(source.getFileName()),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    lifecycle.phase("schematic_manifest_copied");
                } else if (profile.equals("packs")) {
                    ExternalResourcePackOracle.verify(context, world, directory, manifest, output);
                    evidence.setProperty("packOrderForward", "true");
                    evidence.setProperty("packOrderReverse", "true");
                    for (String pack : manifest.getProperty("packs").split(",")) {
                        evidence.setProperty("pack." + pack + ".sha256", manifest.getProperty("pack." + pack + ".sha256"));
                        evidence.setProperty("pack." + pack + ".formatDeclared",
                                manifest.getProperty("pack." + pack + ".formatDeclared"));
                    }
                } else {
                    M3VisualParityOracle.verify(context, world);
                    evidence.setProperty("m3Visual", "true");
                    verifyShader(context, manifest, evidence);
                    M5ActiveResourcePackOracle.verify(context, world);
                    evidence.setProperty("m5ActivePackVisual", "true");
                    verifyShader(context, manifest, evidence);
                    copyEvidence("m3-visual", output);
                    copyEvidence("m5-pack-visual", output);
                }
                lifecycle.phase("world_close");
            }
            lifecycle.phase("world_closed");
            evidence.setProperty("status", "passed");
            writeEvidence(output, evidence);
            System.out.println("BLOCKLENS_EXTERNAL_COMPATIBILITY minecraft=" + BlockLensRuntime.minecraftVersion()
                    + " profile=" + profile + " backend=" + evidence.getProperty("backend")
                    + " shaderInUse=" + evidence.getProperty("shaderInUse") + " fullGraph=false passed=true");
        } catch (IOException exception) {
            throw new AssertionError("External compatibility evidence failed", exception);
        } finally {
            try {
                if (Files.isDirectory(output)) writeEvidence(output, evidence);
            } catch (IOException exception) {
                throw new AssertionError("External compatibility manifest could not be saved", exception);
            }
        }
    }

    static Properties loadManifest(Path directory) throws IOException {
        Path path = directory.resolve("manifest.properties");
        require(Files.isRegularFile(path) && Files.size(path) <= 16384, "Prepared manifest missing or too large");
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        require("1".equals(properties.getProperty("schema")), "Prepared manifest schema differs");
        return properties;
    }

    private static void verifyShader(ClientGameTestContext context, Properties manifest, Properties evidence) {
        boolean expected = "true".equals(manifest.getProperty("shaderExpected"));
        context.runOnClient(client -> {
            boolean active = ExternalShaderProbe.verify(manifest);
            require(active == expected, "Actual shader activation differs");
            evidence.setProperty("shaderInUse", Boolean.toString(active));
            evidence.setProperty("shaderVerified", "true");
            if (active) {
                evidence.setProperty("shaderPack", manifest.getProperty("shaderPack"));
                evidence.setProperty("shaderProfile", manifest.getProperty("shaderProfile"));
                evidence.setProperty("shaderOptionsVerified", "true");
            }
        });
    }

    private static void verifyMod(String id, Properties manifest, Properties evidence) throws IOException {
        var mod = FabricLoader.getInstance().getModContainer(id)
                .orElseThrow(() -> new AssertionError("Expected external mod is missing"));
        require(mod.getOrigin().getKind() == ModOrigin.Kind.PATH && mod.getOrigin().getPaths().size() == 1,
                "External mod is not one recorded artifact");
        Path actual = mod.getOrigin().getPaths().getFirst();
        require(Files.size(actual) <= 8 * 1024 * 1024, "External mod exceeds the bounded recorded size");
        String digest = sha256(Files.readAllBytes(actual));
        require(digest.equals(manifest.getProperty("mod." + id + ".sha256")), "Loaded external mod bytes differ");
        evidence.setProperty("mod." + id + ".sha256", digest);
        evidence.setProperty("mod." + id + ".version", mod.getMetadata().getVersion().getFriendlyString());
    }

    private static String boundedNativeValue(String value) {
        if (value == null) return "unknown";
        String normalized = value.replaceAll("[^A-Za-z0-9 ._()+,:/=-]", "?");
        return normalized.substring(0, Math.min(normalized.length(), 160));
    }

    private static void copyEvidence(String sourceName, Path output) throws IOException {
        Path source = FabricLoader.getInstance().getGameDir().resolve(sourceName);
        try (var files = Files.list(source)) {
            for (Path path : files.toList()) {
                String name = path.getFileName().toString();
                if (name.endsWith(".png") || name.endsWith("-manifest.txt")) {
                    require(Files.size(path) <= 4 * 1024 * 1024, "Visual evidence exceeds the bounded size");
                    Files.copy(path, output.resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 unavailable", exception);
        }
    }

    static void writeEvidence(Path output, Properties evidence) throws IOException {
        try (var writer = Files.newBufferedWriter(output.resolve("external-compatibility-manifest.properties"),
                StandardCharsets.UTF_8)) {
            evidence.store(writer, "BlockLens focused external compatibility evidence");
        }
    }

    static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    record Backend(String name, String device, String driver) { }
}
