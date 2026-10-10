package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.scene.SceneFilterOptions;
import dev.blocklens.fabric.BlockLensConfigScreen;
import dev.blocklens.fabric.SceneFilterOptionsScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/** Actual list editing, mouse-to-keyboard focus, pending resize, presets and parent-only saving. */
final class SceneFilterSettingsOracle {
    private SceneFilterSettingsOracle() { }
    static void verify(ClientGameTestContext context) {
        var original=BlockLensRuntime.config();
        var previous=context.computeOnClient(BlockLensConfigScreen::current);
        var directory=FabricLoader.getInstance().getConfigDir();
        var file=directory.resolve(BlockLensConfigFiles.FILE_NAME);
        String language=context.computeOnClient(c->c.getLanguageManager().getSelected());
        String optionLanguage=context.computeOnClient(c->c.options.languageCode);
        int scale=context.computeOnClient(c->c.options.guiScale().get());
        int width=context.computeOnClient(c->c.getWindow().getWidth());
        int height=context.computeOnClient(c->c.getWindow().getHeight());
        try {
            context.runOnClient(c->{c.options.guiScale().set(1);c.getWindow().setWindowed(320,240);});
            context.waitFor(c->c.getWindow().getWidth()==320 && c.getWindow().getHeight()==240);
            context.runOnClient(c->c.resizeGui());
            context.runOnClient(c->{
                try {
                    byte[] baseline=Files.readAllBytes(file);
                    for(boolean save:new boolean[]{false,true}) {
                        var parent=new BlockLensConfigScreen(previous);
                        BlockLensConfigScreen.show(c,parent);
                        ResponsiveSettingsScreenOracle.select(parent,CapabilityId.Category.OTHER);
                        var expected=original.sceneFilterOptions();
                        for(int kind=0;kind<2;kind++) {
                            details(parent,kind);
                            var child=BlockLensConfigScreen.current(c);
                            require(child instanceof SceneFilterOptionsScreen,"filter details missing");
                            require(child.width==320 && child.height==240,"filter details viewport differs");
                            require(ResponsiveSettingsScreenOracle.buttons(child).size()==(kind==0?5:9),"filter controls differ");
                            for(int i=0;i<3;i++) {
                                click(child,ResponsiveSettingsScreenOracle.buttons(child).getFirst());
                                tab(child);
                            }
                            require(BlockLensRuntime.config().sceneFilterOptions().equals(original.sceneFilterOptions()),"mode applied before save");
                            String black=kind==0?"minecraft:stone,minecraft:chest":"minecraft:item,minecraft:villager";
                            input(child).setValue(black);
                            child.resize(640,480);
                            require(input(child).getValue().equals(black),"pending list lost on resize");
                            child.resize(320,240);
                            require(input(child).getValue().equals(black),"pending list lost returning to compact size");
                            click(child,ResponsiveSettingsScreenOracle.buttons(child).getFirst());
                            require(input(child).getValue().equals(black),"mode rebuild lost pending list");
                            click(child,button(child,"blocklens.scene.use_list"));
                            expected=expected.withMode(kind,SceneFilterOptions.Mode.BLACKLIST).withList(kind,false,black);
                            input(child).setValue("minecraft:missing_blocklens_fixture");
                            click(child,button(child,"blocklens.scene.use_list"));
                            input(child).setValue("#minecraft:invalid");
                            click(child,button(child,"blocklens.scene.use_list"));
                            click(child,ResponsiveSettingsScreenOracle.buttons(child).get(1));
                            click(child,ResponsiveSettingsScreenOracle.buttons(child).get(1));
                            require(input(child).getValue().equals(String.join(",",SceneFilterOptions.parseList(black))),
                                    "invalid filter text changed the last valid draft list");
                            click(child,ResponsiveSettingsScreenOracle.buttons(child).get(1));
                            tab(child);
                            String white=kind==0?"minecraft:dirt":"minecraft:armor_stand";
                            input(child).setValue(white);
                            click(child,button(child,"blocklens.scene.use_list"));
                            expected=expected.withList(kind,true,white);
                            click(child,button(child,"blocklens.scene.clear_list"));
                            expected=expected.withList(kind,true,"");
                            require(input(child).getValue().isEmpty(),"clear retained selected input");
                            input(child).setValue(white);
                            click(child,button(child,"blocklens.scene.use_list"));
                            expected=expected.withList(kind,true,white);
                            if(kind==1) {
                                String[] presets={"build","trace","photo","reset"};
                                for(int p=0;p<4;p++) {
                                    click(child,button(child,"blocklens.scene.preset."+presets[p]));
                                    tab(child);
                                    expected=expected.withEntityPreset(p);
                                }
                                click(child,button(child,"blocklens.scene.preset.build"));
                                expected=expected.withEntityPreset(0);
                            }
                            child.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE,0,0));
                            require(BlockLensConfigScreen.current(c)==parent,"filter Esc did not return to parent");
                        }
                        require(Arrays.equals(baseline,Files.readAllBytes(file)),"filter draft wrote before save");
                        require(BlockLensRuntime.config().sceneFilterOptions().equals(original.sceneFilterOptions()),"filter draft applied early");
                        if(save) {
                            click(parent,button(parent,"blocklens.settings.apply"));
                            require(BlockLensRuntime.config().sceneFilterOptions().equals(expected),"filter save lost valid edits or accepted invalid list");
                            require(BlockLensConfigFiles.loadOrCreate(directory).sceneFilterOptions().equals(expected),"filter disk state differs");
                            require(BlockLensRuntime.config().enabledMask()==original.enabledMask(),"list save altered independent toggles");
                            require(BlockLensRuntime.config().analyzerOptions().equals(original.analyzerOptions()),"list save altered analyzers");
                        } else {
                            parent.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE,0,0));
                            require(Arrays.equals(baseline,Files.readAllBytes(file)),"filter discard wrote edits");
                        }
                    }
                    // A valid file can fill the aggregate budget with unknown IDs. Presets must fail safely.
                    var ids=new ArrayList<String>();
                    for(int i=0;i<191;i++) {
                        String prefix="fixture:"+i+"_";
                        ids.add(prefix+"a".repeat(256-prefix.length()));
                    }
                    ids.add("fixture:"+"z".repeat(57));
                    var full=SceneFilterOptions.defaults().withList(0,false,String.join(",",ids));
                    BlockLensRuntime.installConfig(original.withSceneFilterOptions(full));
                    var parent=new BlockLensConfigScreen(previous);
                    BlockLensConfigScreen.show(c,parent);
                    ResponsiveSettingsScreenOracle.select(parent,CapabilityId.Category.OTHER);
                    details(parent,1);
                    var child=BlockLensConfigScreen.current(c);
                    for(String preset:new String[]{"build","trace","photo"}) {
                        click(child,button(child,"blocklens.scene.preset."+preset));
                        require(input(child).getValue().isEmpty(),"oversized preset partially changed the draft");
                        tab(child);
                    }
                    child.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE,0,0));
                    click(parent,button(parent,"blocklens.settings.apply"));
                    require(BlockLensRuntime.config().sceneFilterOptions().equals(full),"failed oversized preset changed a rule");
                    BlockLensConfigFiles.save(directory,original);
                    BlockLensRuntime.installConfig(original);
                } catch(IOException e) { throw new AssertionError("filter draft storage failed",e); }
            });
            // A native window/GUI resize also preserves text which has not been copied into the draft.
            context.runOnClient(c->{
                var parent=new BlockLensConfigScreen(previous);
                BlockLensConfigScreen.show(c,parent);
                ResponsiveSettingsScreenOracle.select(parent,CapabilityId.Category.OTHER);
                details(parent,0);input(BlockLensConfigScreen.current(c)).setValue("minecraft:stone");
                c.getWindow().setWindowed(640,480);
            });
            context.waitFor(c->c.getWindow().getWidth()==640 && c.getWindow().getHeight()==480);
            context.runOnClient(c->{c.options.guiScale().set(2);c.resizeGui();
                require(input(BlockLensConfigScreen.current(c)).getValue().equals("minecraft:stone"),"native GUI-scale resize lost pending input");
                c.options.guiScale().set(1);c.getWindow().setWindowed(320,240);});
            context.waitFor(c->c.getWindow().getWidth()==320 && c.getWindow().getHeight()==240);
            context.runOnClient(c->c.resizeGui());
            var destination=FabricLoader.getInstance().getGameDir().resolve("scene-filter-ui");
            Files.createDirectories(destination);
            for(String locale:new String[]{"en_us","ja_jp"}) {
                context.runOnClient(c->{c.getLanguageManager().setSelected(locale);c.options.languageCode=locale;});
                reload(context);
                for(int kind=0;kind<2;kind++) {
                    int index=kind;
                    context.runOnClient(c->{
                        var parent=new BlockLensConfigScreen(previous);BlockLensConfigScreen.show(c,parent);
                        ResponsiveSettingsScreenOracle.select(parent,CapabilityId.Category.OTHER);details(parent,index);
                        c.resizeGui();var child=BlockLensConfigScreen.current(c);
                        require(child.width==320 && child.height==240 && SettingsClientAccess.ready(c),"filter capture viewport incomplete");
                        for(var b:ResponsiveSettingsScreenOracle.buttons(child)) require(b.getX()>=0 && b.getRight()<=320
                                && b.getY()>=0 && b.getBottom()<=240,"filter control outside compact bounds");
                    });
                    context.waitTicks(2);
                    var path=context.takeScreenshot(TestScreenshotOptions.of("scene-"+locale+"-"+kind)
                            .withDestinationDir(destination).disableCounterPrefix());
                    var image=javax.imageio.ImageIO.read(path.toFile());
                    require(image!=null && image.getWidth()==320 && image.getHeight()==240,"filter framebuffer viewport differs");
                    int backdrop=image.getRGB(160,200);
                    require((backdrop>>16 & 255)<=64 && (backdrop>>8 & 255)<=64 && (backdrop & 255)<=64,"filter backdrop lacks contrast");
                }
            }
        } catch(IOException e) { throw new AssertionError("filter UI evidence failed",e); }
        finally {
            try { BlockLensConfigFiles.save(directory,original); }
            catch(IOException e) { throw new AssertionError("filter settings restore failed",e); }
            finally {
                BlockLensRuntime.installConfig(original);
                context.runOnClient(c->{c.getLanguageManager().setSelected(language);c.options.languageCode=optionLanguage;
                    c.options.guiScale().set(scale);c.getWindow().setWindowed(width,height);c.resizeGui();BlockLensConfigScreen.show(c,previous);});
                context.waitFor(c->c.getWindow().getWidth()==width && c.getWindow().getHeight()==height);
                context.runOnClient(c->c.resizeGui());reload(context);
            }
        }
        System.out.println("BLOCKLENS_SCENE_SETTINGS listDraft=true invalidRejected=true modes=true presets=true presetOverflowSafe=true mouseTab=true pendingResize=true discard=true save=true locales=en_us,ja_jp");
    }
    private static void details(Screen parent,int kind) {
        String label=Component.translatable("blocklens.settings.details").getString();
        ResponsiveSettingsScreenOracle.buttons(parent).stream().filter(b->b.getMessage().getString().equals(label))
                .toList().get(kind+7).onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
    }
    private static EditBox input(Screen screen) {
        return screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
    }
    private static Button button(Screen screen,String key) {
        String label=Component.translatable(key).getString();
        return ResponsiveSettingsScreenOracle.buttons(screen).stream().filter(b->b.getMessage().getString().equals(label)).findFirst().orElseThrow();
    }
    private static void click(Screen screen,Button button) {
        require(screen.mouseClicked(new MouseButtonEvent(button.getX()+5.0,button.getY()+5.0,
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false),"filter mouse click not handled");
        screen.afterMouseAction();require(screen.getFocused()==null || screen.children().contains(screen.getFocused()),"filter click retained removed focus");
    }
    private static void tab(Screen screen) {
        screen.keyPressed(new KeyEvent(InputConstants.KEY_TAB,0,0));
        require(screen.getFocused()!=null && screen.children().contains(screen.getFocused()),"filter Tab focus is stale");
    }
    private static void reload(ClientGameTestContext context) {
        var future=context.computeOnClient(c->c.reloadResourcePacks());
        context.waitFor(c->future.isDone(),1200);future.join();context.waitFor(SettingsClientAccess::ready,1200);
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
