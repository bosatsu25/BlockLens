package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ResourceHighlightCueInstructionTest {
    @Test
    void eachCueReusesOneImmutableEmissiveInstruction() {
        for (ResourceHighlightCue cue : ResourceHighlightCue.values()) {
            DecorationQuadInstruction first = cue.instruction();
            DecorationQuadInstruction second = cue.instruction();

            assertSame(first, second, () -> cue + " should reuse one instruction instance");
            assertEquals(cue.accentArgb(), first.multiplyArgb());
            assertFalse(first.forceSolid());
            assertTrue(first.emissive());
            assertTrue(first.changesColor());
        }
    }
}
