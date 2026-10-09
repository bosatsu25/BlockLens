package dev.blocklens.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;

/** Registers the deterministic M5 compatibility fixture as a real active resource pack. */
public final class M5ActiveResourcePackFixture implements ClientModInitializer {
    static final Identifier PACK_ID = Identifier.fromNamespaceAndPath("blocklens-gametest", "m5-active-pack");

    @Override
    public void onInitializeClient() {
        ModContainer container = FabricLoader.getInstance()
                .getModContainer("blocklens-gametest")
                .orElseThrow(() -> new IllegalStateException("blocklens-gametest container is unavailable"));
        if (!ResourceLoader.registerBuiltinPack(PACK_ID, container, PackActivationType.ALWAYS_ENABLED)) {
            throw new IllegalStateException("failed to register M5 active resource-pack fixture: " + PACK_ID);
        }
        SettingsStateReloadOracle.register(container);
        System.out.println("BLOCKLENS_M5_ACTIVE_PACK_FIXTURE registered=true id=" + PACK_ID);
    }
}
