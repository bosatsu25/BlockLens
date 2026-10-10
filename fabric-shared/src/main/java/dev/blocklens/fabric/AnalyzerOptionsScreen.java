package dev.blocklens.fabric;

import dev.blocklens.core.analyzer.AnalyzerCatalog;
import dev.blocklens.core.analyzer.AnalyzerOptions;
import dev.blocklens.core.ui.SettingsCatalog;
import dev.blocklens.core.ui.SettingsDraft;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Bounded numeric edits remain in the parent draft until explicit Save and Apply. */
public final class AnalyzerOptionsScreen extends Screen {
    private final Screen parent;
    private final SettingsDraft draft;
    private final int kind;
    private final Consumer<Screen> show;
    AnalyzerOptionsScreen(Screen parent,SettingsDraft draft,int kind,Consumer<Screen> show) {
        super(Component.translatable(SettingsCatalog.nameKey(AnalyzerCatalog.CAPABILITIES.get(kind))));
        this.parent=parent;this.draft=draft;this.kind=kind;this.show=show;
    }
    @Override protected void init() {
        int size=Math.min(420,width-24),left=(width-size)/2,row=0;
        for(int field=0;field<4;field++) {
            if(kind==0 && field==1) continue;
            int selected=field,y=48+row++*30;
            for(int direction:new int[]{-1,1}) {
                int step=direction;
                addRenderableWidget(Button.builder(Component.literal(direction<0?"−":"+"),ignored->adjust(selected,step))
                        .createNarration(ignored->Component.translatable("blocklens.analyzer."+AnalyzerCatalog.FIELDS.get(selected))
                                .append(" ").append(Component.translatable(step<0?"blocklens.comfort.decrease":"blocklens.comfort.increase")))
                        .bounds(left+size-(direction<0?60:28),y,26,20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("blocklens.settings.back"),ignored->onClose())
                .bounds(left,height-28,size,20).build());
    }
    private void adjust(int field,int delta) {
        var options=draft.config().analyzerOptions();
        int value=Math.clamp(options.value(kind,field)+delta,AnalyzerOptions.minimum(kind,field),AnalyzerOptions.maximum(kind,field));
        draft.setAnalyzerOptions(options.with(kind,field,value));
    }
    @Override protected void setInitialFocus() {
        for(var child:children()) if(child instanceof Button button && button.active && button.visible) {
            setInitialFocus(button);return;
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        boolean handled=super.mouseClicked(event,doubleClick);
        if(getFocused()!=null && !children().contains(getFocused())) { clearFocus();setInitialFocus(); }
        return handled;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor extractor,int mouseX,int mouseY,float delta) {
        int size=Math.min(420,width-24),left=(width-size)/2,row=0;
        extractor.fill(left,8,left+size,height-32,0xEE000000);
        super.extractRenderState(extractor,mouseX,mouseY,delta);
        extractor.centeredText(font,font.plainSubstrByWidth(title.getString(),size),width/2,14,0xFFFFFFFF);
        for(int field=0;field<4;field++) {
            if(kind==0 && field==1) continue;
            var label=Component.translatable("blocklens.analyzer."+AnalyzerCatalog.FIELDS.get(field))
                    .append(": "+draft.config().analyzerOptions().value(kind,field));
            extractor.text(font,font.plainSubstrByWidth(label.getString(),size-72),left+4,54+row++*30,0xFFFFFFFF);
        }
        int y=48+row*30+8;
        for(var line:font.split(Component.translatable("blocklens.comfort.draft_hint"),size-8)) {
            if(y+font.lineHeight>height-34) break;
            extractor.text(font,line,left+4,y,0xFFAAAAAA);y+=font.lineHeight;
        }
    }
    @Override public void onClose() { show.accept(parent); }
}
