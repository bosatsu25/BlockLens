package dev.blocklens.fabric;

import dev.blocklens.core.scene.SceneFilterCatalog;
import dev.blocklens.core.scene.SceneFilterOptions;
import dev.blocklens.core.ui.SettingsCatalog;
import dev.blocklens.core.ui.SettingsDraft;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Explicit list-to-draft editing; persistence remains exclusively on the main settings screen. */
public final class SceneFilterOptionsScreen extends Screen {
    private final Screen parent;
    private final SettingsDraft draft;
    private final int kind;
    private final Consumer<Screen> show;
    private boolean white;
    private EditBox input;
    private boolean invalid;
    private boolean applied;

    SceneFilterOptionsScreen(Screen parent,SettingsDraft draft,int kind,Consumer<Screen> show) {
        super(Component.translatable(SettingsCatalog.nameKey(SceneFilterCatalog.CAPABILITIES.get(kind))));
        this.parent=parent;
        this.draft=draft;
        this.kind=kind;
        this.show=show;
        white=draft.config().sceneFilterOptions().rule(kind).mode()==SceneFilterOptions.Mode.WHITELIST;
    }
    @Override protected void init() {
        int size=Math.min(420,width-24),left=(width-size)/2;
        String pending=input==null?String.join(",",draft.config().sceneFilterOptions().rule(kind).list(white)):input.getValue();
        addRenderableWidget(Button.builder(Component.translatable("blocklens.scene.mode")
                .append(": ").append(modeName()),ignored->{
                    var options=draft.config().sceneFilterOptions();
                    var modes=SceneFilterOptions.Mode.values();
                    draft.setSceneFilterOptions(options.withMode(kind,modes[(options.rule(kind).mode().ordinal()+1)%modes.length]));
                    invalid=false;applied=false;rebuildWidgets();
                }).bounds(left,44,size,20).build());
        addRenderableWidget(Button.builder(Component.translatable(white?"blocklens.scene.whitelist":"blocklens.scene.blacklist"),
                ignored->{white=!white;input=null;invalid=false;applied=false;rebuildWidgets();}).bounds(left,70,size,20).build());
        input=new EditBox(font,left,96,size,20,Component.translatable("blocklens.scene.ids"));
        input.setMaxLength(SceneFilterOptions.MAX_TOTAL_CHARS);
        input.setValue(pending);
        input.setResponder(ignored->{invalid=false;applied=false;});
        addRenderableWidget(input);
        int half=(size-8)/2;
        addRenderableWidget(Button.builder(Component.translatable("blocklens.scene.use_list"),ignored->applyList())
                .bounds(left,122,half,20).build());
        addRenderableWidget(Button.builder(Component.translatable("blocklens.scene.clear_list"),ignored->{
            draft.setSceneFilterOptions(draft.config().sceneFilterOptions().withList(kind,white,""));
            input.setValue("");invalid=false;applied=true;
        }).bounds(left+half+8,122,half,20).build());
        if(kind==1) {
            String[] names={"build","trace","photo","reset"};
            int part=(size-12)/4;
            for(int i=0;i<4;i++) {
                int preset=i;
                addRenderableWidget(Button.builder(Component.translatable("blocklens.scene.preset."+names[i]),ignored->{
                    try {
                        draft.setSceneFilterOptions(draft.config().sceneFilterOptions().withEntityPreset(preset));
                        white=draft.config().sceneFilterOptions().rule(kind).mode()==SceneFilterOptions.Mode.WHITELIST;
                        input=null;invalid=false;applied=true;rebuildWidgets();
                    } catch(IllegalArgumentException oversized) {
                        invalid=true;applied=false;
                    }
                }).bounds(left+i*(part+4),148,part,20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("blocklens.settings.back"),ignored->onClose())
                .bounds(left,height-28,size,20).build());
    }
    private Component modeName() {
        return Component.translatable("blocklens.scene.mode."+draft.config().sceneFilterOptions().rule(kind).mode()
                .name().toLowerCase(Locale.ROOT));
    }
    private void applyList() {
        try {
            var updated=draft.config().sceneFilterOptions().withList(kind,white,input.getValue());
            for(String id:updated.rule(kind).list(white)) {
                if(!SceneFilterClient.registered(kind,id)) throw new IllegalArgumentException("unregistered filter target");
            }
            draft.setSceneFilterOptions(updated);
            input.setValue(String.join(",",updated.rule(kind).list(white)));
            invalid=false;applied=true;
        } catch(IllegalArgumentException invalidInput) {
            invalid=true;applied=false;
        }
    }
    @Override protected void setInitialFocus() { if(input!=null) setInitialFocus(input); }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        boolean handled=super.mouseClicked(event,doubleClick);
        if(getFocused()!=null && !children().contains(getFocused())) { clearFocus();setInitialFocus(); }
        return handled;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor extractor,int mouseX,int mouseY,float delta) {
        int size=Math.min(420,width-24),left=(width-size)/2;
        extractor.fill(left,8,left+size,height-32,0xEE000000);
        super.extractRenderState(extractor,mouseX,mouseY,delta);
        extractor.centeredText(font,font.plainSubstrByWidth(title.getString(),size),width/2,14,0xFFFFFFFF);
        int y=kind==1?174:150;
        String key=invalid?"blocklens.scene.invalid":applied?"blocklens.scene.applied":"blocklens.scene.hint";
        for(var line:font.split(Component.translatable(key),size-8)) {
            if(y+font.lineHeight>height-34) break;
            extractor.text(font,line,left+4,y,invalid?0xFFFF7777:0xFFAAAAAA);
            y+=font.lineHeight;
        }
    }
    @Override public void onClose() { show.accept(parent); }
}
