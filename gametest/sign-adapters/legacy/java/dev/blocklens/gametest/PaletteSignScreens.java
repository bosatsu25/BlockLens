package dev.blocklens.gametest;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;
/** Typed test adapter for the supported sign editor constructor. */
final class PaletteSignScreens {
    private PaletteSignScreens() { }
    static Screen create(SignBlockEntity sign,boolean hanging) {
        return hanging ? new HangingSignEditScreen(sign,true,true)
                : new SignEditScreen(sign,true,true);
    }
}
