package dev.blocklens.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Filesystem boundary for BlockLens native configuration. */
public final class BlockLensConfigFiles {
    public static final String FILE_NAME = "blocklens.properties";
    public static final int MAX_CONFIG_BYTES = 64 * 1024;

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
        byte[] content;
        try (var input = Files.newInputStream(configPath)) {
            content = input.readNBytes(MAX_CONFIG_BYTES + 1);
        }
        if (content.length > MAX_CONFIG_BYTES) {
            throw new IOException("BlockLens config exceeds " + MAX_CONFIG_BYTES + " bytes");
        }
        return BlockLensConfigCodec.decode(new String(content, StandardCharsets.UTF_8));
    }

    public static void save(Path configDirectory, BlockLensConfig config) throws IOException {
        Path directory = configDirectory.toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path configPath = directory.resolve(FILE_NAME);
        String content = BlockLensConfigCodec.encode(config);

        Path temporary = Files.createTempFile(directory, FILE_NAME + ".", ".tmp");
        try {
            Files.writeString(
                    temporary,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.SYNC);
            try {
                Files.move(
                        temporary,
                        configPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
