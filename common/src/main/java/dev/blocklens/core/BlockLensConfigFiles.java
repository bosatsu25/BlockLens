package dev.blocklens.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Filesystem boundary for BlockLens native configuration. */
public final class BlockLensConfigFiles {
    public static final String FILE_NAME = "blocklens.properties";

    private BlockLensConfigFiles() {
    }

    public static BlockLensConfig loadOrCreate(Path configDirectory) throws IOException {
        Path directory = configDirectory.toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path configPath = directory.resolve(FILE_NAME);
        if (!Files.isRegularFile(configPath)) {
            BlockLensConfig defaults = BlockLensConfig.defaults();
            save(directory, defaults);
            return defaults;
        }
        return BlockLensConfigCodec.decode(Files.readString(configPath, StandardCharsets.UTF_8));
    }

    public static void save(Path configDirectory, BlockLensConfig config) throws IOException {
        Path directory = configDirectory.toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path configPath = directory.resolve(FILE_NAME);
        Path temporary = directory.resolve(FILE_NAME + ".tmp");
        Files.writeString(
                temporary,
                BlockLensConfigCodec.encode(config),
                StandardCharsets.UTF_8);
        Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
