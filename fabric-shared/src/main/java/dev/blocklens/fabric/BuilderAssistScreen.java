package dev.blocklens.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.builder.BuilderState;
import dev.blocklens.core.builder.PatternInspector;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Read-only session panel. Settings drafts and persistent writes remain with their owning screen. */
public final class BuilderAssistScreen extends Screen {
    private final Screen parent;
    private final Consumer<Screen> show;
    private List<FormattedCharSequence> lines = List.of();
    private BuilderAssistClient.Snapshot lastTarget;
    private PatternInspector.Snapshot lastPattern;
    private BlockLensConfig lastConfig;
    private Button select, clear;
    private int scroll;

    public BuilderAssistScreen(Screen parent, Consumer<Screen> show) {
        super(text("title"));
        this.parent = parent;
        this.show = show;
    }

    @Override protected void init() {
        int size = contentWidth(), left = (width - size) / 2, half = (size - 8) / 2;
        select = addRenderableWidget(Button.builder(text("select"), ignored -> BuilderAssistClient.selectReference(minecraft))
                .bounds(left, height - 54, half, 20).build());
        clear = addRenderableWidget(Button.builder(text("clear"), ignored -> BuilderAssistClient.clearReference())
                .bounds(left + half + 8, height - 54, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("blocklens.settings.back"), ignored -> onClose())
                .bounds(left, height - 28, size, 20).build());
        lastTarget = null;
        updateLines();
    }

    @Override public void tick() { updateLines(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { show.accept(parent); }

    private void updateLines() {
        var config = BlockLensRuntime.config();
        var target = BuilderAssistClient.snapshot();
        var pattern = BuilderAssistClient.pattern();
        select.active = config.isEnabled(CapabilityId.PATTERN_CONSISTENCY) && target.kind() == 1;
        clear.active = config.isEnabled(CapabilityId.PATTERN_CONSISTENCY) && pattern.reference() != null;
        if (lastTarget == target && lastPattern == pattern && lastConfig == config) return;
        lastTarget = target; lastPattern = pattern; lastConfig = config;
        var content = new ArrayList<Component>();
        content.add(text("hint").withStyle(ChatFormatting.GRAY));
        section(content, "block", CapabilityId.BLOCK_INSPECTOR);
        if (config.isEnabled(CapabilityId.BLOCK_INSPECTOR)) {
            if (target.target() == null) content.add(text("no_target"));
            else {
                if (target.kind() == 2) content.add(text("entity"));
                state(content, target.target());
            }
        }
        section(content, "preview", CapabilityId.PLACEMENT_PREVIEW);
        if (config.isEnabled(CapabilityId.PLACEMENT_PREVIEW)) {
            state(content, target.prediction());
            if (!target.face().isEmpty()) content.add(text("face", target.face(), text(target.upper() ? "upper" : "lower")));
        }
        section(content, "pattern", CapabilityId.PATTERN_CONSISTENCY);
        if (config.isEnabled(CapabilityId.PATTERN_CONSISTENCY)) {
            if (pattern.reference() == null) content.add(text("no_reference"));
            else {
                content.add(text("reference", pattern.reference().id()));
                content.add(text("progress", pattern.scanned(), PatternInspector.DOMAIN_SIZE,
                        text(pattern.complete() ? "complete" : "scanning")));
                content.add(text("counts", pattern.compared(), pattern.matches(), pattern.mismatches()));
                content.add(text("unknown", pattern.unknown()));
                for (var mismatch : pattern.propertyMismatches().entrySet()) if (mismatch.getValue() > 0)
                    content.add(text("mismatch", mismatch.getKey(), mismatch.getValue()));
            }
        }
        section(content, "schematic", CapabilityId.SCHEMATIC_COMPARISON);
        if (config.isEnabled(CapabilityId.SCHEMATIC_COMPARISON)) {
            if (target.expected() == null) content.add(text("schematic_unavailable"));
            else {
                content.add(text("expected")); state(content, target.expected());
                content.add(text("predicted")); state(content, target.prediction());
                content.add(text(switch (BuilderState.compare(target.expected(), target.prediction())) {
                    case BuilderState.MATCH -> "match";
                    case BuilderState.ADJUSTED -> "adjusted";
                    case BuilderState.DIFFERENT -> "different";
                    default -> "unavailable";
                }));
            }
        }
        var formatted = new ArrayList<FormattedCharSequence>();
        for (Component line : content) formatted.addAll(font.split(line, contentWidth() - 16));
        lines = List.copyOf(formatted);
        scroll = Math.min(scroll, maxScroll());
    }

    private static void state(List<Component> lines, BuilderState state) {
        if (state == null) { lines.add(text("unavailable")); return; }
        lines.add(Component.literal(state.id()));
        for (var property : state.properties().entrySet()) lines.add(Component.literal(property.getKey() + " = " + property.getValue()));
    }

    private static void section(List<Component> lines, String name, CapabilityId capability) {
        lines.add(text(name).withStyle(ChatFormatting.AQUA));
        if (!BlockLensRuntime.config().isEnabled(capability)) lines.add(text("disabled").withStyle(ChatFormatting.GRAY));
    }

    private static net.minecraft.network.chat.MutableComponent text(String key, Object... arguments) {
        return Component.translatable("blocklens.builder." + key, arguments);
    }

    private int contentWidth() { return Math.min(520, width - 24); }
    private int visibleLines() { return Math.max(1, (height - 100) / (font.lineHeight + 2)); }
    private int maxScroll() { return Math.max(0, lines.size() - visibleLines()); }

    @Override public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case InputConstants.KEY_HOME -> scroll = 0;
            case InputConstants.KEY_END -> scroll = maxScroll();
            case InputConstants.KEY_PAGEUP -> scroll = Math.max(0, scroll - visibleLines());
            case InputConstants.KEY_PAGEDOWN -> scroll = Math.min(maxScroll(), scroll + visibleLines());
            default -> { return super.keyPressed(event); }
        }
        return true;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseY >= 34 && mouseY < height - 62) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (vertical * 3)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        updateLines();
        int size = contentWidth(), left = (width - size) / 2;
        extractor.fill(left, 8, left + size, height - 60, 0xEE000000);
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(font, font.plainSubstrByWidth(title.getString(), size - 8), width / 2, 14, 0xFFFFFFFF);
        for (int row = 0; row < visibleLines() && row + scroll < lines.size(); row++)
            extractor.text(font, lines.get(row + scroll), left + 4, 36 + row * (font.lineHeight + 2), 0xFFFFFFFF);
        if (maxScroll() > 0) {
            int top = 36, range = height - 100;
            int thumb = Math.max(8, range * visibleLines() / lines.size());
            int y = top + (range - thumb) * scroll / maxScroll();
            extractor.fill(left + size - 5, top, left + size - 2, top + range, 0xFF333333);
            extractor.fill(left + size - 5, y, left + size - 2, y + thumb, 0xFFAAAAAA);
        }
    }
}
