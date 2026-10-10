package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.blocklens.core.ui.SettingsCatalog;
import dev.blocklens.core.ui.SettingsDraft;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BuilderCapabilitiesTest {
    private static final List<String> KEYS = List.of(
            "builder.block_inspector", "builder.placement_preview",
            "builder.pattern_consistency", "builder.schematic_comparison");

    @Test
    void fourBuilderControlsAppendInOtherAndDefaultOff() {
        var defaults = BlockLensConfig.defaults();
        for (int index = 0; index < KEYS.size(); index++) {
            CapabilityId capability = CapabilityId.bySourceKey().get(KEYS.get(index));
            assertNotNull(capability, "Missing independently selectable builder tool: " + KEYS.get(index));
            assertEquals(53 + index, capability.ordinal());
            assertEquals(CapabilityId.Category.OTHER, capability.category());
            assertFalse(defaults.isEnabled(capability));
            assertTrue(SettingsCatalog.capabilities(CapabilityId.Category.OTHER).contains(capability));
        }
    }

    @Test
    void everyBuilderToolCanBeEnabledAndRoundTrippedIndependently() {
        var defaults = BlockLensConfig.defaults();
        for (String key : KEYS) {
            CapabilityId selected = CapabilityId.bySourceKey().get(key);
            assertNotNull(selected, "Missing builder config key: " + key);
            var restored = BlockLensConfigCodec.decode(
                    BlockLensConfigCodec.encode(defaults.withEnabled(selected, true)));
            for (CapabilityId capability : CapabilityId.values()) {
                assertEquals(capability == selected || defaults.isEnabled(capability),
                        restored.isEnabled(capability), capability.sourceKey());
            }
        }
    }

    @Test
    void legacyConfigKeepsExistingValuesAndLeavesBuilderToolsOff() {
        var restored = BlockLensConfigCodec.decode("deco.anvil=true\nscene.block_filter=true\n");
        assertTrue(restored.isEnabled(CapabilityId.ANVIL));
        assertTrue(restored.isEnabled(CapabilityId.BLOCK_FILTER));
        for (String key : KEYS) {
            CapabilityId capability = CapabilityId.bySourceKey().get(key);
            assertNotNull(capability, "Missing builder config key: " + key);
            assertFalse(restored.isEnabled(capability));
        }
    }

    @Test
    void builderDraftDoesNotInstallRuntimeSettingsAndDiscardRestoresIt() {
        var original = BlockLensRuntime.config();
        try {
            BlockLensRuntime.installConfig(BlockLensConfig.defaults());
            var saved = BlockLensRuntime.config();
            var draft = new SettingsDraft(saved);
            for (String key : KEYS) {
                CapabilityId capability = CapabilityId.bySourceKey().get(key);
                assertNotNull(capability, "Missing builder config key: " + key);
                draft.toggle(capability);
            }
            assertTrue(draft.changed());
            assertSame(saved, BlockLensRuntime.config());
            draft.discard();
            assertFalse(draft.changed());
            assertSame(saved, draft.config());
        } finally {
            BlockLensRuntime.installConfig(original);
        }
    }
}
