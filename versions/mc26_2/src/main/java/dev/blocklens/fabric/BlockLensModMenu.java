package dev.blocklens.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional Mod Menu bridge; not loaded when Mod Menu is absent. */
public final class BlockLensModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BlockLensConfigScreen::new;
    }
}
