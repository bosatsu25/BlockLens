package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import java.io.IOException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Bounded settings screen that reflows and scrolls with the current GUI size. */
public final class BlockLensConfigScreen extends Screen {
    private static final CapabilityId[] CAPABILITIES = CapabilityId.values();
    private static final int TOP = 34;
    private static final int ROW = 24;
    private final Screen parent;
    private final Button[] toggles = new Button[CAPABILITIES.length];
    private BlockLensConfig draft = BlockLensRuntime.config();
    private long appliedMask = draft.enabledMask();
    private int left;
    private int contentWidth;
    private int bottom;
    private int columns;
    private int scroll;
    private int maxScroll;
    private boolean saveFailed;

    public BlockLensConfigScreen(Screen parent) {
        super(Component.translatable("blocklens.title"));
        this.parent = parent;
    }

    public static Screen current(Minecraft client) {
        return client.gui.screen();
    }

    public static void show(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }

    @Override
    protected void init() {
        contentWidth = Math.min(620, Math.max(80, width - 24));
        left = (width - contentWidth) / 2;
        bottom = Math.max(TOP, height - 38);
        columns = width >= 520 ? 2 : 1;
        int buttonWidth = Math.max(36, (contentWidth - (columns - 1) * 8) / columns);
        maxScroll = Math.max(0,
                ((CAPABILITIES.length + columns - 1) / columns) * ROW - (bottom - TOP));
        scroll = Math.min(scroll, maxScroll);
        for (int index = 0; index < CAPABILITIES.length; index++) {
            CapabilityId capability = CAPABILITIES[index];
            toggles[index] = addRenderableWidget(Button.builder(
                    label(capability),
                    ignored -> toggle(capability))
                    .bounds(0, 0, buttonWidth, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                ignored -> onClose())
                .bounds(left, Math.max(4, height - 28), contentWidth, 20)
                .build());
        position(buttonWidth);
    }

    private void toggle(CapabilityId capability) {
        draft = draft.withEnabled(capability, !draft.isEnabled(capability));
        saveFailed = false;
        for (int index = 0; index < CAPABILITIES.length; index++) {
            toggles[index].setMessage(label(CAPABILITIES[index]));
        }
    }

    private Component label(CapabilityId capability) {
        String key = capability.sourceKey();
        return Component.translatable(
                "blocklens.capability." + key.substring(key.indexOf('.') + 1))
                .append(draft.isEnabled(capability) ? ": ON" : ": OFF");
    }

    private void position(int buttonWidth) {
        for (int index = 0; index < toggles.length; index++) {
            int x = left + (index % columns) * (buttonWidth + 8);
            int y = TOP + (index / columns) * ROW - scroll;
            toggles[index].setRectangle(buttonWidth, 20, x, y);
            toggles[index].visible = y >= TOP && y + 20 <= bottom;
        }
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount) {
        if (mouseX >= left && mouseX <= left + contentWidth
                && mouseY >= TOP && mouseY <= bottom && maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll + (int) (-verticalAmount * ROW * 2)));
            position(Math.max(36, (contentWidth - (columns - 1) * 8) / columns));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        if (saveFailed) {
            extractor.centeredText(
                    font,
                    Component.translatable("blocklens.save_failed"),
                    width / 2,
                    Math.max(24, height - 48),
                    0xFFFFD166);
        }
    }

    @Override
    public void onClose() {
        if (draft.enabledMask() != appliedMask) {
            try {
                BlockLensConfigFiles.save(FabricLoader.getInstance().getConfigDir(), draft);
                BlockLensRuntime.installConfig(draft);
                MinecraftTerrainInvalidator.invalidateAll(minecraft);
                appliedMask = draft.enabledMask();
            } catch (IOException | SecurityException exception) {
                saveFailed = true;
                return;
            }
        }
        show(minecraft, parent);
    }
}
