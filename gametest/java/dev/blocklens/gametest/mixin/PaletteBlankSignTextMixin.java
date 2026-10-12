package dev.blocklens.gametest.mixin;
import dev.blocklens.gametest.PaletteSignBackground;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Empty synthetic text only: retain actual native sign/background/buttons, omit wall-clock caret. */
@Mixin(AbstractSignEditScreen.class)
abstract class PaletteBlankSignTextMixin {
    @Inject(method="extractSignText",at=@At("HEAD"),cancellable=true)
    private void blocklens$blankFixtureText(CallbackInfo info) {
        if(PaletteSignBackground.testing())info.cancel();
    }
}
