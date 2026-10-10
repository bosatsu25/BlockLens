package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.fabric.BlockLensClient;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;

/** Proves packaged contents are used without logging local paths or relying on development fallback. */
final class PackagedRuntimeOracle {
    private PackagedRuntimeOracle() { }

    static void verify() {
        if (!Boolean.getBoolean("blocklens.test.packagedRuntime")) return;
        var loader = FabricLoader.getInstance();
        var outer = loader.getModContainer("blocklens").orElseThrow();
        var inner = loader.getModContainer("blocklens-runtime").orElseThrow();
        try {
            Path expected = Path.of(System.getProperty("blocklens.test.expectedArtifact"));
            require(outer.getOrigin().getKind() == ModOrigin.Kind.PATH, "outer artifact origin is not PATH");
            var paths = outer.getOrigin().getPaths();
            require(paths.size() == 1 && Files.isSameFile(paths.getFirst(), expected), "outer artifact differs from the requested JAR");
            var origin = inner.getOrigin();
            require(origin.getKind() == ModOrigin.Kind.NESTED, "runtime library origin is not NESTED");
            require(origin.getParentModId().equals("blocklens")
                    && origin.getParentSubLocation().equals("META-INF/jars/blocklens-runtime.jar"), "runtime library parent differs");
            require(inner.getContainingMod().orElseThrow().getMetadata().getId().equals("blocklens"), "runtime library is not contained in BlockLens");
            verifyLoadedClass(inner, BlockLensClient.class);
            verifyLoadedClass(inner, BlockLensRuntime.class);
        } catch (IOException | URISyntaxException exception) {
            throw new AssertionError("packaged runtime origin verification failed", exception);
        }
        System.out.println("BLOCKLENS_PACKAGED_RUNTIME outerMatch=true nestedOrigin=true clientFromNested=true commonFromNested=true");
    }

    private static void verifyLoadedClass(ModContainer inner, Class<?> type) throws IOException, URISyntaxException {
        var entry = inner.findPath(type.getName().replace('.', '/') + ".class").orElseThrow();
        var uri = entry.toUri();
        require(uri.getScheme().equals("jar"), "owned bytecode is not in a JAR filesystem");
        var connection = uri.toURL().openConnection();
        require(connection instanceof JarURLConnection, "owned bytecode URL is not a JAR");
        var jarUri = ((JarURLConnection) connection).getJarFileURL().toURI();
        require(jarUri.getScheme().equals("file"), "owned bytecode is not a local artifact");
        var source = type.getProtectionDomain().getCodeSource();
        require(source != null && source.getLocation().getProtocol().equals("file"), "loaded class origin is unavailable");
        require(Files.isSameFile(Path.of(source.getLocation().toURI()), Path.of(jarUri)), "loaded class came from development output");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
