package dev.blocklens.fabric;

import com.mojang.blaze3d.platform.InputConstants;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ui.SettingsCatalog;
import dev.blocklens.core.ui.SettingsDraft;
import dev.blocklens.core.analyzer.AnalyzerCatalog;
import dev.blocklens.core.scene.SceneFilterCatalog;
import dev.blocklens.core.ui.SettingsLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** One settings implementation compiled against both supported Minecraft APIs. */
public abstract class AbstractBlockLensConfigScreen extends Screen {
    private final Screen parent;
    private final Path configDirectory;
    private final SettingsDraft draft = new SettingsDraft(BlockLensRuntime.config());
    private final int[] categoryScroll = new int[CapabilityId.Category.values().length];
    private final List<Button> tabs = new ArrayList<>();
    private final List<Button> toggles = new ArrayList<>();
    private final List<Button> details = new ArrayList<>();
    private final List<Integer> detailRows = new ArrayList<>();
    private CapabilityId.Category category = CapabilityId.Category.DECORATION;
    private List<CapabilityId> capabilities = List.of();
    private SettingsLayout layout;
    private Button discardButton;
    private Button applyButton;
    private boolean saveFailed;

    protected AbstractBlockLensConfigScreen(Screen parent, Path configDirectory) {
        super(Component.translatable("blocklens.title"));
        this.parent = parent;
        this.configDirectory = Objects.requireNonNull(configDirectory, "configDirectory");
    }

    protected abstract void showParent(Screen parent);

    @Override
    protected final void init() {
        clearFocus();
        tabs.clear();
        toggles.clear();
        details.clear();
        detailRows.clear();
        capabilities = SettingsCatalog.capabilities(category);
        layout = SettingsLayout.create(width, height, capabilities.size(), categoryScroll[category.ordinal()]);
        int rowHeight = 48;
        for (CapabilityId capability : capabilities) {
            rowHeight = Math.max(rowHeight, 28 + font.wordWrapHeight(
                    Component.translatable(SettingsCatalog.descriptionKey(capability)), layout.contentWidth() - 16));
        }
        layout = SettingsLayout.create(width, height, capabilities.size(), categoryScroll[category.ordinal()], rowHeight);
        categoryScroll[category.ordinal()] = layout.scroll();
        int tabWidth = (layout.contentWidth() - 12) / 4;
        for (CapabilityId.Category candidate : CapabilityId.Category.values()) {
            Button tab = addRenderableWidget(Button.builder(
                    Component.translatable("blocklens.category." + candidate.name().toLowerCase(Locale.ROOT)),
                    ignored -> selectCategory(candidate))
                    .bounds(layout.left() + candidate.ordinal() * (tabWidth + 4), 34, tabWidth, 20).build());
            tab.active = candidate != category;
            tabs.add(tab);
        }
        for (CapabilityId capability : capabilities) {
            toggles.add(addRenderableWidget(Button.builder(state(capability), ignored -> {
                draft.toggle(capability);
                refreshStates();
            }).createNarration(ignored -> Component.translatable(SettingsCatalog.nameKey(capability))
                    .append(": ").append(state(capability)))
                    .tooltip(Tooltip.create(Component.translatable(SettingsCatalog.nameKey(capability))
                            .append("\n").append(Component.translatable(SettingsCatalog.descriptionKey(capability)))))
                    .bounds(0, 0, 72, 20).build()));
        }
        for (int index = 0; index < capabilities.size(); index++) {
            CapabilityId capability = capabilities.get(index);
            if (capability != CapabilityId.LOW_FIRE && capability != CapabilityId.HANDHELD_SIZE
                    && AnalyzerCatalog.index(capability)<0 && SceneFilterCatalog.index(capability)<0) continue;
            details.add(addRenderableWidget(Button.builder(Component.translatable("blocklens.settings.details"),
                    ignored -> showParent(SceneFilterCatalog.index(capability)>=0
                            ? new SceneFilterOptionsScreen(this,draft,SceneFilterCatalog.index(capability),this::showParent)
                            : AnalyzerCatalog.index(capability)>=0
                            ? new AnalyzerOptionsScreen(this,draft,AnalyzerCatalog.index(capability),this::showParent)
                            : new ComfortOptionsScreen(this, draft, capability, this::showParent)))
                    .createNarration(ignored -> Component.translatable(SettingsCatalog.nameKey(capability))
                            .append(": ").append(Component.translatable("blocklens.settings.details")))
                    .bounds(0, 0, 68, 20).build()));
            detailRows.add(index);
        }
        int footerWidth = (layout.contentWidth() - 8) / 2;
        discardButton = addRenderableWidget(Button.builder(Component.translatable("blocklens.settings.discard"),
                ignored -> onClose()).bounds(layout.left(), layout.footerY(), footerWidth, 20).build());
        applyButton = addRenderableWidget(Button.builder(Component.translatable("blocklens.settings.apply"),
                ignored -> saveAndClose()).bounds(layout.left() + footerWidth + 8, layout.footerY(),
                        footerWidth, 20).build());
        positionRows();
        setInitialFocus();
    }

    private Component state(CapabilityId capability) {
        return Component.translatable(draft.config().isEnabled(capability)
                ? "blocklens.state.enabled" : "blocklens.state.disabled");
    }

    private void refreshStates() {
        for (int index = 0; index < toggles.size(); index++) {
            toggles.get(index).setMessage(state(capabilities.get(index)));
        }
    }

    private void selectCategory(CapabilityId.Category selected) {
        category = Objects.requireNonNull(selected, "category");
        rebuildWidgets();
    }

    private void positionRows() {
        for (int index = 0; index < toggles.size(); index++) {
            Button toggle = toggles.get(index);
            toggle.setRectangle(72, 20, layout.left() + layout.contentWidth() - 80, layout.rowY(index) + 3);
            toggle.visible = layout.visible(index);
            if (!toggle.visible && getFocused() == toggle) clearFocus();
        }
        positionDetails();
    }

    private void positionDetails() {
        for (int index = 0; index < details.size(); index++) {
            int row = detailRows.get(index);
            Button detail = details.get(index);
            detail.setRectangle(68, 20, layout.left() + layout.contentWidth() - 152, layout.rowY(row) + 3);
            detail.visible = layout.visible(row);
            if (!detail.visible && getFocused() == detail) clearFocus();
        }
    }

    private void scrollTo(int scroll) {
        layout = SettingsLayout.create(width, height, capabilities.size(), scroll, layout.rowHeight());
        categoryScroll[category.ordinal()] = layout.scroll();
        positionRows();
    }

    @Override
    protected final void setInitialFocus() {
        for (Button toggle : toggles) {
            if (toggle.visible) {
                setInitialFocus(toggle);
                return;
            }
        }
        setInitialFocus(discardButton);
    }

    @Override
    public final boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_TAB) {
            List<Button> order = new ArrayList<>(tabs);
            for (int row = 0; row < toggles.size(); row++) {
                order.add(toggles.get(row));
                int detail = detailRows.indexOf(row);
                if (detail >= 0) order.add(details.get(detail));
            }
            order.add(discardButton);
            order.add(applyButton);
            int direction = event.hasShiftDown() ? -1 : 1;
            int cursor = order.indexOf(getFocused());
            if (cursor < 0) cursor = direction > 0 ? -1 : 0;
            for (int step = 0; step < order.size(); step++) {
                cursor = Math.floorMod(cursor + direction, order.size());
                Button target = order.get(cursor);
                if (!target.active) continue;
                int row = toggles.indexOf(target);
                if (row < 0 && details.contains(target)) row = detailRows.get(details.indexOf(target));
                if (row >= 0) scrollTo(layout.scrollToReveal(row));
                clearFocus();
                setFocused(target);
                target.setFocused(true);
                return true;
            }
            return false;
        }
        if (event.key() == InputConstants.KEY_PAGEUP || event.key() == InputConstants.KEY_PAGEDOWN
                || event.key() == InputConstants.KEY_HOME || event.key() == InputConstants.KEY_END) {
            int viewport = layout.listBottom() - layout.listTop();
            int scroll = switch (event.key()) {
                case InputConstants.KEY_PAGEUP -> layout.scroll() - viewport;
                case InputConstants.KEY_PAGEDOWN -> layout.scroll() + viewport;
                case InputConstants.KEY_HOME -> 0;
                default -> layout.maxScroll();
            };
            scrollTo(scroll);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (getFocused() != null && !children().contains(getFocused())) {
            clearFocus();
            setInitialFocus();
        }
        return handled;
    }

    @Override
    public final boolean mouseScrolled(
            double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= layout.left() && mouseX <= layout.left() + layout.contentWidth()
                && mouseY >= layout.listTop() && mouseY <= layout.listBottom() && layout.maxScroll() > 0) {
            scrollTo(layout.scroll() + (int) (-verticalAmount * layout.rowHeight()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        extractor.fill(layout.left(), layout.listTop(), layout.left() + layout.contentWidth(),
                layout.footerY() - 2, 0xF4000000);
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        extractor.horizontalLine(layout.left(), layout.left() + layout.contentWidth(), 60, 0x80FFFFFF);
        for (int index = 0; index < capabilities.size(); index++) {
            if (!layout.visible(index)) continue;
            int y = layout.rowY(index);
            String name = Component.translatable(SettingsCatalog.nameKey(capabilities.get(index))).getString();
            int nameWidth = layout.nameWidth() - (detailRows.contains(index) ? 72 : 0);
            String shown = font.width(name) <= nameWidth ? name
                    : font.plainSubstrByWidth(name, nameWidth - font.width("…")) + "…";
            extractor.text(font, shown, layout.left() + 4, y + 7, 0xFFFFFFFF);
            int textY = y + 25;
            for (var line : font.split(Component.translatable(
                    SettingsCatalog.descriptionKey(capabilities.get(index))), layout.contentWidth() - 16)) {
                if (textY + font.lineHeight > y + layout.rowHeight() - 3) break;
                extractor.text(font, line, layout.left() + 4, textY, 0xFFAAAAAA);
                textY += font.lineHeight;
            }
            extractor.horizontalLine(layout.left(), layout.left() + layout.contentWidth() - 8,
                    y + layout.rowHeight() - 1, 0x30FFFFFF);
        }
        if (layout.maxScroll() > 0) {
            int x = layout.left() + layout.contentWidth() - 5;
            int viewport = layout.listBottom() - layout.listTop();
            int thumb = Math.max(8, viewport * viewport / (capabilities.size() * layout.rowHeight()));
            int y = layout.listTop() + (viewport - thumb) * layout.scroll() / layout.maxScroll();
            extractor.fill(x, layout.listTop(), x + 3, layout.listBottom(), 0x55000000);
            extractor.fill(x, y, x + 3, y + thumb, 0xFFAAAAAA);
        }
        if (saveFailed || draft.changed()) {
            Component message = Component.translatable(saveFailed
                    ? "blocklens.settings.saved_error" : "blocklens.settings.unsaved");
            int y = layout.footerY() - 20;
            for (var line : font.split(message, layout.contentWidth() - 8)) {
                if (y + font.lineHeight > layout.footerY() - 1) break;
                extractor.centeredText(font, line, width / 2, y, saveFailed ? 0xFFFFD166 : 0xFFAAAAAA);
                y += font.lineHeight;
            }
        }
    }

    private void saveAndClose() {
        try {
            if (draft.save(configDirectory)) {
                BlockLensRuntime.installConfig(draft.config());
                MinecraftTerrainInvalidator.invalidateAll(minecraft);
            }
            showParent(parent);
        } catch (IOException | SecurityException exception) {
            saveFailed = true;
        }
    }

    @Override
    public final void onClose() {
        draft.discard();
        showParent(parent);
    }
}
