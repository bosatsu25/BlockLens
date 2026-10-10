package dev.blocklens.gametest;

import dev.blocklens.api.OreHighlightExtensions;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Test-only custom content; this mod and its models never enter the distributed BlockLens JAR. */
public final class OreExtensionFixture implements ModInitializer {
    static final String EXPLICIT = "blocklens-gametest:explicit_ore";
    static final String TAGGED = "blocklens-gametest:tagged_ore";
    static final String UNSUPPORTED = "blocklens-gametest:unsupported_ore";
    static final String CONFLICTED = "blocklens-gametest:conflicted_ore";
    static final String MANY_STATES = "blocklens-gametest:many_states_ore";
    static final int OVERFLOW_TARGETS = 257;

    @Override
    public void onInitialize() {
        for (String raw : new String[]{EXPLICIT, TAGGED, UNSUPPORTED, CONFLICTED}) register(raw, false);
        register(MANY_STATES, true);
        for (int i = 0; i < OVERFLOW_TARGETS; i++) register(overflowId(i), false);
        require(OreHighlightExtensions.register(1, EXPLICIT, "gaming.diamond_ore"), "explicit API rejected fixture");
        require(OreHighlightExtensions.register(1, EXPLICIT, "gaming.diamond_ore"), "explicit duplicate rejected");
        require(!OreHighlightExtensions.register(2, UNSUPPORTED, "gaming.diamond_ore"), "unsupported API accepted");
        require(OreHighlightExtensions.register(1, CONFLICTED, "gaming.diamond_ore"), "conflict setup rejected");
        require(!OreHighlightExtensions.register(1, CONFLICTED, "gaming.gold_ore"), "conflict was accepted");
        require(OreHighlightExtensions.register(1, MANY_STATES, "gaming.diamond_ore"), "state-limit setup rejected");
        require(OreHighlightExtensions.register(1, "missing-test-mod:ore", "gaming.diamond_ore"), "absent-mod setup rejected");
        require(!OreHighlightExtensions.register(1, "minecraft:stone", "gaming.diamond_ore"), "vanilla override accepted");
    }

    static String overflowId(int index) { return "blocklens-gametest:overflow_ore_" + index; }

    private static void register(String raw, boolean manyStates) {
        Identifier id = Identifier.tryParse(raw);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id)).noLootTable();
        Registry.register(BuiltInRegistries.BLOCK, id, manyStates ? new ManyStates(properties) : new Block(properties));
    }

    private static final class ManyStates extends Block {
        private static final IntegerProperty VALUE = IntegerProperty.create("value", 0, 64);
        private ManyStates(BlockBehaviour.Properties properties) { super(properties); }
        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(VALUE);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
