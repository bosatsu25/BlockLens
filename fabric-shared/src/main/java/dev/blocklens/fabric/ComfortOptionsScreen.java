package dev.blocklens.fabric;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ComfortOptions;
import dev.blocklens.core.ui.SettingsCatalog;
import dev.blocklens.core.ui.SettingsDraft;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Numeric edits share the main screen draft; returning never writes configuration. */
public final class ComfortOptionsScreen extends Screen {
    private final Screen parent;
    private final SettingsDraft draft;
    private final CapabilityId capability;
    private final Consumer<Screen> show;

    ComfortOptionsScreen(Screen parent, SettingsDraft draft, CapabilityId capability, Consumer<Screen> show) {
        super(Component.translatable(SettingsCatalog.nameKey(capability)));
        if (capability != CapabilityId.LOW_FIRE && capability != CapabilityId.HANDHELD_SIZE) {
            throw new IllegalArgumentException("capability has no comfort options");
        }
        this.parent = Objects.requireNonNull(parent, "parent");
        this.draft = Objects.requireNonNull(draft, "draft");
        this.capability = capability;
        this.show = Objects.requireNonNull(show, "show");
    }
    @Override protected void init() {
        int contentWidth = Math.min(420, width - 24);
        int left = (width - contentWidth) / 2;
        ComfortOptions options = draft.config().comfortOptions();
        if (capability == CapabilityId.LOW_FIRE) {
            for (int preset = 0; preset < 3; preset++) {
                int selected = preset;
                Button button = addRenderableWidget(Button.builder(
                        Component.translatable("blocklens.comfort.fire." + preset), ignored -> {
                            var old = draft.config().comfortOptions();
                            draft.setComfortOptions(new ComfortOptions(selected, old.blocksPercent(),
                                    old.itemsPercent(), old.toolsPercent()));
                            rebuildWidgets();
                        }).bounds(left, 48 + preset * 30, contentWidth, 20).build());
                button.active = options.fireSize() != preset;
            }
        } else {
            for (int category = 0; category < 3; category++) {
                int selected = category;
                addRenderableWidget(Button.builder(Component.literal("−"), ignored -> adjust(selected, -5))
                        .createNarration(ignored -> label(selected).append(" ")
                                .append(Component.translatable("blocklens.comfort.decrease")))
                        .bounds(left + contentWidth - 60, 48 + category * 30, 26, 20).build());
                addRenderableWidget(Button.builder(Component.literal("+"), ignored -> adjust(selected, 5))
                        .createNarration(ignored -> label(selected).append(" ")
                                .append(Component.translatable("blocklens.comfort.increase")))
                        .bounds(left + contentWidth - 28, 48 + category * 30, 26, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("blocklens.settings.back"), ignored -> onClose())
                .bounds(left, height - 28, contentWidth, 20).build());
    }
    @Override protected void setInitialFocus() {
        for (var child : children()) {
            if (child instanceof Button button && button.active && button.visible) {
                setInitialFocus(button);
                return;
            }
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (getFocused() != null && !children().contains(getFocused())) {
            clearFocus();
            setInitialFocus();
        }
        return handled;
    }
    private MutableComponent label(int category) {
        return Component.translatable("blocklens.comfort.held." + category);
    }
    private int percent(int category) {
        var options = draft.config().comfortOptions();
        return switch (category) {
            case 0 -> options.blocksPercent();
            case 1 -> options.itemsPercent();
            case 2 -> options.toolsPercent();
            default -> throw new IllegalArgumentException("invalid held category");
        };
    }
    private void adjust(int category, int step) {
        var old = draft.config().comfortOptions();
        int value = Math.clamp(percent(category) + step, 40, 100);
        draft.setComfortOptions(new ComfortOptions(old.fireSize(),
                category == 0 ? value : old.blocksPercent(), category == 1 ? value : old.itemsPercent(),
                category == 2 ? value : old.toolsPercent()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
        int contentWidth = Math.min(420, width - 24);
        int left = (width - contentWidth) / 2;
        if (capability == CapabilityId.HANDHELD_SIZE) {
            for (int category = 0; category < 3; category++) {
                Component text = label(category).append(": " + percent(category) + "%");
                extractor.text(font, font.plainSubstrByWidth(text.getString(), contentWidth - 72), left + 4,
                        54 + category * 30, 0xFFFFFFFF);
            }
        }
        int y = 146;
        for (var line : font.split(Component.translatable("blocklens.comfort.draft_hint"), contentWidth - 8)) {
            if (y + font.lineHeight > height - 34) break;
            extractor.text(font, line, left + 4, y, 0xFFAAAAAA);
            y += font.lineHeight;
        }
    }
    @Override public void onClose() {
        show.accept(parent);
    }
}
