package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ui.SettingsCatalog;
import dev.blocklens.fabric.AbstractBlockLensConfigScreen;
import dev.blocklens.fabric.BlockLensConfigScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Real saved settings, keyboard reachability, discarded edits and failed-save draft retention. */
final class ConnectedOreSettingsOracle {
    private ConnectedOreSettingsOracle() { }
    static void verify(ClientGameTestContext context,Path output,BlockLensConfig baseline) throws IOException {
        Path directory=FabricLoader.getInstance().getConfigDir();
        byte[] original=Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME));
        Path fixture=null,occupied=null;
        try {
            BlockLensConfigFiles.save(directory,baseline);
            byte[] unchanged=Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME));
            context.runOnClient(c->{BlockLensRuntime.installConfig(baseline);var screen=new BlockLensConfigScreen(null);
                BlockLensConfigScreen.show(c,screen);toggle(screen);});
            require(Arrays.equals(unchanged,Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME)))
                    && !BlockLensRuntime.config().isEnabled(CapabilityId.CONNECTED_ORE_EDGES),"Unsaved connection draft applied");
            capture(context,output,"ui-connection-draft");
            context.runOnClient(c->BlockLensConfigScreen.current(c).onClose());
            require(Arrays.equals(unchanged,Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME)))
                    && !BlockLensRuntime.config().isEnabled(CapabilityId.CONNECTED_ORE_EDGES),"Discard saved connection");
            var priorModels=context.computeOnClient(c->c.getModelManager().getBlockStateModelSet());
            context.runOnClient(c->{var screen=new BlockLensConfigScreen(null);BlockLensConfigScreen.show(c,screen);toggle(screen);press(screen,"blocklens.settings.apply");});
            context.waitFor(c->SettingsClientAccess.ready(c)&&c.getModelManager().getBlockStateModelSet()!=priorModels,1200);context.waitTicks(20);
            var saved=BlockLensConfigFiles.loadOrCreate(directory);
            require(saved.isEnabled(CapabilityId.CONNECTED_ORE_EDGES)&&saved.isEnabled(CapabilityId.DIAMOND_ORE)
                    && BlockLensRuntime.config().equals(saved),"UI did not persist independent connection control");
            capture(context,output,"ui-connection-applied");
            BlockLensConfigFiles.save(directory,baseline);
            fixture=Files.createTempDirectory(directory,"connected-failure-");occupied=fixture.resolve("occupied");Files.writeString(occupied,"keep");
            Path blocked=occupied;
            context.runOnClient(c->{BlockLensRuntime.installConfig(baseline);var screen=new FailingScreen(blocked);
                BlockLensConfigScreen.show(c,screen);toggle(screen);press(screen,"blocklens.settings.apply");
                require(BlockLensConfigScreen.current(c)==screen && !BlockLensRuntime.config().isEnabled(CapabilityId.CONNECTED_ORE_EDGES),"Failed connection save lost draft/applied state");
                int index=SettingsCatalog.capabilities(CapabilityId.Category.RESOURCE).indexOf(CapabilityId.CONNECTED_ORE_EDGES);
                require(ResponsiveSettingsScreenOracle.buttons(screen).get(4+index).getMessage().getString()
                        .equals(Component.translatable("blocklens.state.enabled").getString()),"Failed save cleared enabled draft");});
            capture(context,output,"ui-connection-save-failed");require(Files.readString(occupied).equals("keep"),"Failed save overwrote a file");
        } finally {
            context.runOnClient(c->BlockLensConfigScreen.show(c,null));
            Files.write(directory.resolve(BlockLensConfigFiles.FILE_NAME),original);
            if(occupied!=null)Files.deleteIfExists(occupied);if(fixture!=null)Files.deleteIfExists(fixture);
        }
    }
    private static void toggle(Screen screen){
        press(screen,"blocklens.category.resource");
        int index=SettingsCatalog.capabilities(CapabilityId.Category.RESOURCE).indexOf(CapabilityId.CONNECTED_ORE_EDGES);
        var button=ResponsiveSettingsScreenOracle.buttons(screen).get(4+index);int remaining=ResponsiveSettingsScreenOracle.buttons(screen).size()*2;
        while(screen.getFocused()!=button && remaining-->0)screen.keyPressed(new KeyEvent(InputConstants.KEY_TAB,0,0));
        require(screen.getFocused()==button&&button.visible,"Connection row not keyboard reachable");screen.keyPressed(new KeyEvent(InputConstants.KEY_RETURN,0,0));
    }
    private static void press(Screen screen,String key){
        String text=Component.translatable(key).getString();ResponsiveSettingsScreenOracle.buttons(screen).stream()
                .filter(button->button.getMessage().getString().equals(text)).findFirst().orElseThrow().onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
    }
    private static void capture(ClientGameTestContext context,Path output,String name) throws IOException {
        ConnectedOreOracle.capture(context,output,name);
    }
    private static final class FailingScreen extends AbstractBlockLensConfigScreen {
        FailingScreen(Path directory) { super(null,directory); }
        @Override protected void showParent(Screen parent) { BlockLensConfigScreen.show(minecraft,parent); }
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}