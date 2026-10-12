package dev.blocklens.gametest.mixin;
import dev.blocklens.gametest.PaletteSignBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Negative framebuffer control only; excluded from the distribution. */
@Mixin({SignEditScreen.class,HangingSignEditScreen.class})
abstract class PaletteSignBackgroundMixin {
    @Inject(method="extractSignBackground",at=@At("HEAD"),cancellable=true)
    private void blocklens$absentBoard(GuiGraphicsExtractor graphics,CallbackInfo info) {
        if(PaletteSignBackground.hidden()) info.cancel();
    }
}
