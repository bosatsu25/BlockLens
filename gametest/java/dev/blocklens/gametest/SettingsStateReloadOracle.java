package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.BlockLensConfigScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Keeps an edited screen alive through actual reload/GUI-scale changes with long test-only text. */
final class SettingsStateReloadOracle {
    private static final Identifier PACK_ID =
            Identifier.fromNamespaceAndPath("blocklens-gametest", "settings-long-text");

    private SettingsStateReloadOracle() {
    }

    static void register(ModContainer container) {
        if (!ResourceLoader.registerBuiltinPack(PACK_ID, container, PackActivationType.NORMAL)) {
            throw new IllegalStateException("Settings state fixture registration failed");
        }
    }

    static void verify(ClientGameTestContext context) {
        Screen parent = context.computeOnClient(BlockLensConfigScreen::current);
        int originalScale = context.computeOnClient(client -> client.options.guiScale().get());
        String originalLanguage = context.computeOnClient(client -> client.getLanguageManager().getSelected());
        String originalOption = context.computeOnClient(client -> client.options.languageCode);
        List<String> originalPacks = context.computeOnClient(client ->
                List.copyOf(client.getResourcePackRepository().getSelectedIds()));
        long baseline = BlockLensRuntime.config().enabledMask();
        boolean expectedObsidian = !BlockLensRuntime.config().isEnabled(CapabilityId.OBSIDIAN);
        try {
            context.runOnClient(client -> {
                var repository = client.getResourcePackRepository();
                repository.reload();
                String id = repository.getAvailableIds().stream()
                        .filter(candidate -> candidate.contains("blocklens-gametest")
                                && candidate.endsWith("settings-long-text"))
                        .findFirst().orElseThrow(() -> new AssertionError("Optional settings fixture missing"));
                var selected = new ArrayList<>(originalPacks);
                selected.add(id);
                repository.setSelected(selected);
            });
            for (String locale : new String[] {"en_us", "ja_jp"}) {
                context.runOnClient(client -> {
                    client.getLanguageManager().setSelected(locale);
                    client.options.languageCode = locale;
                });
                reload(context);
                BlockLensConfigScreen screen = context.computeOnClient(client -> new BlockLensConfigScreen(parent));
                context.runOnClient(client -> {
                    BlockLensConfigScreen.show(client, screen);
                    String category = Component.translatable("blocklens.category.resource").getString();
                    ResponsiveSettingsScreenOracle.buttons(screen).stream()
                            .filter(button -> button.getMessage().getString().equals(category)).findFirst()
                            .orElseThrow(() -> new AssertionError("Resource category missing"))
                            .onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                    ResponsiveSettingsScreenOracle.buttons(screen).get(4).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                });
                for (int scale : new int[] {1, 2}) {
                    context.runOnClient(client -> {
                        client.options.guiScale().set(scale);
                        client.resizeGui();
                        verifyDraft(screen, expectedObsidian);
                        require(BlockLensConfigScreen.current(client) == screen,
                                "GUI scale replaced the edited screen");
                    });
                    context.waitTicks(2);
                }
                context.runOnClient(client -> {
                    screen.keyPressed(new KeyEvent(InputConstants.KEY_END, 0, 0));
                    require(lastToggle(screen).visible, "End did not reveal the final long-text row");
                    screen.resize(screen.width, screen.height);
                    require(lastToggle(screen).visible, "Rebuild lost the long-text category scroll");
                    verifyDraft(screen, expectedObsidian);
                });
                reload(context);
                context.runOnClient(client -> {
                    require(BlockLensConfigScreen.current(client) == screen,
                            "Reload replaced the edited screen");
                    screen.resize(screen.width, screen.height);
                    require(lastToggle(screen).visible, "Reload lost the category scroll");
                    verifyDraft(screen, expectedObsidian);
                    require(BlockLensRuntime.config().enabledMask() == baseline, "Reload applied draft edits");
                    screen.onClose();
                });
            }
        } finally {
            context.runOnClient(client -> {
                client.getResourcePackRepository().setSelected(originalPacks);
                client.getLanguageManager().setSelected(originalLanguage);
                client.options.languageCode = originalOption;
                client.options.guiScale().set(originalScale);
                client.resizeGui();
                BlockLensConfigScreen.show(client, parent);
            });
            reload(context);
        }
        require(BlockLensRuntime.config().enabledMask() == baseline, "State oracle changed runtime config");
        System.out.println("BLOCKLENS_SETTINGS_STATE locales=en_us,ja_jp reload=true scale=1,2 draft=true scroll=true");
    }

    private static void verifyDraft(BlockLensConfigScreen screen, boolean expected) {
        List<Button> controls = ResponsiveSettingsScreenOracle.buttons(screen);
        require(controls.size() == 27, "Category changed during rebuild");
        String state = Component.translatable(expected
                ? "blocklens.state.enabled" : "blocklens.state.disabled").getString();
        require(controls.get(4).getMessage().getString().equals(state), "Rebuild lost the draft");
        require(screen.getFocused() == null || controls.contains(screen.getFocused())
                && screen.getFocused() instanceof Button focused && focused.visible && focused.active,
                "Rebuild retained stale or hidden focus");
    }

    private static Button lastToggle(Screen screen) {
        return ResponsiveSettingsScreenOracle.buttons(screen).get(24);
    }

    private static void reload(ClientGameTestContext context) {
        CompletableFuture<Void> reloaded = context.computeOnClient(client -> client.reloadResourcePacks());
        context.waitFor(client -> reloaded.isDone(), 1200);
        reloaded.join();
        context.waitFor(SettingsClientAccess::ready, 1200);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
