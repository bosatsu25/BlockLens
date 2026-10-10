package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensRuntime;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Screenshot;

/** Uses completed native frames for the 26.1.2 Sodium screenshot lifecycle exception. */
final class ExternalFrameCapture {
    private ExternalFrameCapture() { }

    static boolean required() {
        return "26.1.2".equals(BlockLensRuntime.minecraftVersion())
                && "external".equals(System.getProperty("blocklens.test.focus"))
                && "shader-off".equals(System.getProperty("blocklens.test.external.profile"));
    }

    static Path capture(ClientGameTestContext context, Path directory, String name, int width, int height)
            throws IOException {
        Files.createDirectories(directory);
        Path output = directory.resolve(name + ".png");
        int[] previous = context.computeOnClient(client -> new int[] {
                client.getWindow().getWidth(), client.getWindow().getHeight()});
        try {
            context.runOnClient(client -> {
                client.getWindow().setWidth(width);
                client.getWindow().setHeight(height);
                ExternalCaptureTarget.read(client).resize(width, height);
            });
            // Each GameTest tick yields to the full Minecraft frame. Do not invoke the renderer
            // again outside that frame: Sodium 0.9.2 has already disposed its terrain uniforms.
            context.waitTicks(2);
            CompletableFuture<Path> captured = new CompletableFuture<>();
            context.runOnClient(client -> Screenshot.takeScreenshot(ExternalCaptureTarget.read(client), image -> {
                try (image) {
                    ExternalCompatibilityOracle.require(image.getWidth() == width && image.getHeight() == height,
                            "Completed-frame capture dimensions differ");
                    image.writeToFile(output);
                    captured.complete(output);
                } catch (IOException | RuntimeException | AssertionError failure) {
                    captured.completeExceptionally(failure);
                }
            }));
            context.waitFor(client -> captured.isDone(), 200);
            Path result = captured.join();
            System.out.println("BLOCKLENS_EXTERNAL_FRAME completedNativeFrames=2 width=" + width
                    + " height=" + height + " file=" + result.getFileName());
            return result;
        } finally {
            context.runOnClient(client -> {
                client.getWindow().setWidth(previous[0]);
                client.getWindow().setHeight(previous[1]);
                ExternalCaptureTarget.read(client).resize(previous[0], previous[1]);
            });
        }
    }
}
