package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class VisibilityVisualCueInstructionTest {
    @Test
    void everyM4CueOwnsOneEmissiveTexturePreservingInstruction() {
        for (VisibilityVisualCue cue : VisibilityVisualCue.values()) {
            DecorationQuadInstruction instruction = cue.instruction();
            assertSame(instruction, cue.instruction());
            assertEquals(cue.accentArgb(), instruction.multiplyArgb());
            assertTrue(instruction.changesColor());
            assertTrue(instruction.emissive());
            assertFalse(instruction.forceSolid());
        }
    }
}
