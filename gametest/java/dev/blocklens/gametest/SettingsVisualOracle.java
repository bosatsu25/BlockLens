package dev.blocklens.gametest;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.BlockLensConfigScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/** Real framebuffer evidence for the settings screen; contains no third-party assets. */
final class SettingsVisualOracle {
    private SettingsVisualOracle() {
    }

    static void verify(ClientGameTestContext context) {
        Screen previous = context.computeOnClient(BlockLensConfigScreen::current);
        String originalLanguage = context.computeOnClient(client -> client.getLanguageManager().getSelected());
        String originalOption = context.computeOnClient(client -> client.options.languageCode);
        try {
            for (String locale : new String[] {"en_us", "ja_jp"}) {
                context.runOnClient(client -> {
                    client.getLanguageManager().setSelected(locale);
                    client.options.languageCode = locale;
                });
                reload(context);
                context.runOnClient(client -> BlockLensConfigScreen.show(client,
                        new BlockLensConfigScreen(previous)));
                context.waitTicks(5);
                for (CapabilityId.Category category : CapabilityId.Category.values()) {
                    context.runOnClient(client -> ResponsiveSettingsScreenOracle.select(
                            (BlockLensConfigScreen) BlockLensConfigScreen.current(client), category));
                    context.waitTicks(2);
                    capture(context, "settings-" + locale + "-" + category.name().toLowerCase(java.util.Locale.ROOT),
                            854, 480);
                }
                capture(context, "settings-" + locale + "-compact", 320, 240);
            }
        } finally {
            context.runOnClient(client -> {
                client.getLanguageManager().setSelected(originalLanguage);
                client.options.languageCode = originalOption;
                BlockLensConfigScreen.show(client, previous);
            });
            reload(context);
        }
        System.out.println("BLOCKLENS_SETTINGS_VISUAL locales=en_us,ja_jp categories=4 compact=true");
    }

    private static void reload(ClientGameTestContext context) {
        CompletableFuture<Void> reloaded = context.computeOnClient(client -> client.reloadResourcePacks());
        context.waitFor(client -> reloaded.isDone(), 1200);
        reloaded.join();
        context.waitTicks(20);
    }

    private static void capture(ClientGameTestContext context, String name, int width, int height) {
        Path destination = FabricLoader.getInstance().getGameDir().resolve("m3-visual");
        try {
            Files.createDirectories(destination);
            Path image = context.takeScreenshot(TestScreenshotOptions.of(name)
                    .withDestinationDir(destination).withSize(width, height).disableCounterPrefix());
            if (!Files.isRegularFile(image) || Files.size(image) < 1000) {
                throw new AssertionError("Settings framebuffer capture is missing or empty");
            }
        } catch (IOException exception) {
            throw new AssertionError("Settings framebuffer capture failed");
        }
    }
}
