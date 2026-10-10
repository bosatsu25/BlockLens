package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.AnalyzerOptionsScreen;
import dev.blocklens.fabric.BlockLensConfigScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Real numeric-only edits for all five analyzers; the parent remains the sole persistence action. */
final class AnalyzerSettingsOracle {
    private AnalyzerSettingsOracle() { }
    static void verify(ClientGameTestContext context) {
        var original=BlockLensRuntime.config();
        var previous=context.computeOnClient(BlockLensConfigScreen::current);
        var directory=FabricLoader.getInstance().getConfigDir();
        var file=directory.resolve(BlockLensConfigFiles.FILE_NAME);
        String language=context.computeOnClient(c->c.getLanguageManager().getSelected());
        String optionLanguage=context.computeOnClient(c->c.options.languageCode);
        int originalScale=context.computeOnClient(c->c.options.guiScale().get());
        int originalWidth=context.computeOnClient(c->c.getWindow().getWidth());
        int originalHeight=context.computeOnClient(c->c.getWindow().getHeight());
        try {
            context.runOnClient(c->{c.options.guiScale().set(1);c.getWindow().setWindowed(320,240);});
            context.waitFor(c->c.getWindow().getWidth()==320 && c.getWindow().getHeight()==240);
            context.runOnClient(c->c.resizeGui());
            context.runOnClient(client->{
                try {
                    byte[] baseline=Files.readAllBytes(file);
                    for(boolean save:new boolean[]{false,true}) {
                        var parent=new BlockLensConfigScreen(previous);
                        BlockLensConfigScreen.show(client,parent);
                        parent.resize(320,240);
                        ResponsiveSettingsScreenOracle.select(parent,CapabilityId.Category.OTHER);
                        var expected=original.analyzerOptions();
                        for(int kind=0;kind<5;kind++) {
                            details(parent,kind);
                            Screen child=BlockLensConfigScreen.current(client);
                            require(child instanceof AnalyzerOptionsScreen,"analyzer detail screen missing");
                            child.resize(320,240);
                            var buttons=ResponsiveSettingsScreenOracle.buttons(child);
                            require(buttons.size()==(kind==0?7:9),"unexpected numeric controls");
                            for(var button:buttons) require(button.getX()>=0 && button.getRight()<=320
                                    && button.getY()>=0 && button.getBottom()<=240,"numeric control outside compact bounds");
                            int row=0;
                            for(int field=0;field<4;field++) {
                                if(kind==0 && field==1) continue;
                                buttons.get(row*2).onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
                                buttons.get(row*2+1).onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
                                buttons.get(row*2+1).onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
                                expected=expected.with(kind,field,expected.value(kind,field)+1);
                                row++;
                            }
                            child.keyPressed(new KeyEvent(InputConstants.KEY_TAB,0,0));
                            require(child.getFocused()==null || child.children().contains(child.getFocused()),"numeric focus is stale");
                            child.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE,0,0));
                            require(BlockLensConfigScreen.current(client)==parent,"numeric Esc did not return");

                        }
                        require(BlockLensRuntime.config().analyzerOptions().equals(original.analyzerOptions()),"draft applied early");
                        require(Arrays.equals(baseline,Files.readAllBytes(file)),"draft saved early");
                        if(save) {
                            press(parent,"blocklens.settings.apply");
                            require(BlockLensRuntime.config().analyzerOptions().equals(expected),"numeric save lost edits");
                            require(BlockLensConfigFiles.loadOrCreate(directory).analyzerOptions().equals(expected),"numeric disk state differs");
                            require(BlockLensRuntime.config().enabledMask()==original.enabledMask(),"numeric save changed capability bits");
                        } else {
                            parent.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE,0,0));
                            require(Arrays.equals(baseline,Files.readAllBytes(file)),"discard wrote numeric edits");
                        }
                    }
                } catch(IOException e) { throw new AssertionError("analyzer draft storage failed",e); }
            });
            var destination=FabricLoader.getInstance().getGameDir().resolve("analyzer-ui");
            Files.createDirectories(destination);
            for(String locale:new String[]{"en_us","ja_jp"}) {
                context.runOnClient(c->{c.getLanguageManager().setSelected(locale);c.options.languageCode=locale;});
                reload(context);
                for(int kind=0;kind<5;kind++) {
                    int index=kind;
                    context.runOnClient(c->{
                        var parent=new BlockLensConfigScreen(previous);
                        BlockLensConfigScreen.show(c,parent);
                        ResponsiveSettingsScreenOracle.select(parent,CapabilityId.Category.OTHER);
                        details(parent,index);
                    });
                    context.runOnClient(c->{
                        c.resizeGui();
                        var child=BlockLensConfigScreen.current(c);
                        require(child instanceof AnalyzerOptionsScreen && child.width==320 && child.height==240
                                && SettingsClientAccess.ready(c),"numeric UI requires the complete requested viewport");
                        for(var button:ResponsiveSettingsScreenOracle.buttons(child)) require(button.getX()>=0
                                && button.getRight()<=320 && button.getY()>=0 && button.getBottom()<=240,
                                "numeric capture has an out-of-bounds control");
                    });
                    context.waitTicks(2);
                    var image=context.takeScreenshot(TestScreenshotOptions.of("analyzer-"+locale+"-"+kind)
                            .withDestinationDir(destination).disableCounterPrefix());
                    require(Files.isRegularFile(image) && Files.size(image)>1000,"numeric UI framebuffer missing");
                    var pixels=javax.imageio.ImageIO.read(image.toFile());
                    require(pixels!=null && pixels.getWidth()==320 && pixels.getHeight()==240,"numeric viewport dimensions differ");
                    int backdrop=pixels.getRGB(160,200);
                    require((backdrop>>16 & 255)<=64 && (backdrop>>8 & 255)<=64 && (backdrop & 255)<=64,
                            "numeric text backdrop lacks contrast");
                }
            }
        } catch(IOException e) { throw new AssertionError("analyzer UI evidence failed",e); }
        finally {
            try { BlockLensConfigFiles.save(directory,original); }
            catch(IOException e) { throw new AssertionError("analyzer settings restore failed",e); }
            finally {
                BlockLensRuntime.installConfig(original);
                context.runOnClient(c->{
                    c.getLanguageManager().setSelected(language);c.options.languageCode=optionLanguage;
                    c.options.guiScale().set(originalScale);c.getWindow().setWindowed(originalWidth,originalHeight);
                    c.resizeGui();
                    BlockLensConfigScreen.show(c,previous);
                });
                context.waitFor(c->c.getWindow().getWidth()==originalWidth && c.getWindow().getHeight()==originalHeight);
                context.runOnClient(c->c.resizeGui());
                reload(context);
            }
        }
        System.out.println("BLOCKLENS_ANALYZER_SETTINGS analyzers=5 numericDraft=true escape=true discard=true save=true locales=en_us,ja_jp");
    }
    private static void reload(ClientGameTestContext context) {
        var future=context.computeOnClient(c->c.reloadResourcePacks());
        context.waitFor(c->future.isDone(),1200);future.join();context.waitFor(SettingsClientAccess::ready,1200);
    }
    private static void details(Screen parent,int kind) {
        String label=Component.translatable("blocklens.settings.details").getString();
        ResponsiveSettingsScreenOracle.buttons(parent).stream().filter(b->b.getMessage().getString().equals(label))
                .toList().get(kind+2).onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
    }
    private static void press(Screen screen,String key) {
        String label=Component.translatable(key).getString();
        ResponsiveSettingsScreenOracle.buttons(screen).stream().filter(b->b.getMessage().getString().equals(label))
                .findFirst().orElseThrow().onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}