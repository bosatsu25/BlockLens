package dev.blocklens.testing;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class ReferencePresetTest {
    @Test void historicalVisualFixtureKeepsFiveControlsIndependentOfFreshDefaults() {
        var fixture = ReferencePreset.historical();
        assertEquals(Set.of("outline.blueice", "outline.deadcoral", "outline.powdersnow",
                "outline.sculk_catalyst", "others.stringtweaks"), fixture.asMap().entrySet().stream()
                .filter(entry -> entry.getValue()).map(entry -> entry.getKey().sourceKey()).collect(Collectors.toSet()));
        assertFalse(BlockLensConfig.defaults().isEnabled(CapabilityId.BLUE_ICE));
        assertFalse(fixture.isEnabled(CapabilityId.BRIGHT_CONCRETE));
        assertFalse(fixture.isEnabled(CapabilityId.BRIGHT_CHEST));
    }
}
