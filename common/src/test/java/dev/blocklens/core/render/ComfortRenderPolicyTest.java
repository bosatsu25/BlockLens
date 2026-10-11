package dev.blocklens.core.render;

import dev.blocklens.core.ComfortOptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComfortRenderPolicyTest {
    @Test void firePresetsAreBoundedAndOrdered() {
        assertEquals(.78f, ComfortRenderPolicy.fireScaleY(0));
        assertEquals(.56f, ComfortRenderPolicy.fireScaleY(1));
        assertEquals(.34f, ComfortRenderPolicy.fireScaleY(2));
        assertEquals(-.14f, ComfortRenderPolicy.fireOffsetY(0));
        assertEquals(-.24f, ComfortRenderPolicy.fireOffsetY(1));
        assertEquals(-.34f, ComfortRenderPolicy.fireOffsetY(2));
        assertThrows(IllegalArgumentException.class, () -> ComfortRenderPolicy.fireScaleY(-1));
        assertThrows(IllegalArgumentException.class, () -> ComfortRenderPolicy.fireOffsetY(3));
    }
    @Test void heldCategoriesRemainIndependentWithShieldOverride() {
        var options = new ComfortOptions(1, 40, 60, 100);
        assertEquals(.4f, ComfortRenderPolicy.heldScale(options, true, false, false));
        assertEquals(.6f, ComfortRenderPolicy.heldScale(options, false, false, false));
        assertEquals(1f, ComfortRenderPolicy.heldScale(options, false, true, false));
        assertEquals(.95f, ComfortRenderPolicy.heldScale(options, true, true, true));
        assertEquals(.4f, ComfortRenderPolicy.heldScale(options, true, true, false));
        assertThrows(NullPointerException.class, () -> ComfortRenderPolicy.heldScale(null, false, false, false));
    }

    @Test
    void verifyHeldScaleDecisionTable() {
        // Comfort options: blocks=40%, items=60%, tools=75%
        var options = new ComfortOptions(1, 40, 60, 75);

        // Rule 1: shield=T, block=T, tool=T -> 0.95f (shield has absolute priority)
        assertEquals(.95f, ComfortRenderPolicy.heldScale(options, true, true, true));
        // Rule 2: shield=T, block=T, tool=F -> 0.95f
        assertEquals(.95f, ComfortRenderPolicy.heldScale(options, true, false, true));
        // Rule 3: shield=T, block=F, tool=T -> 0.95f
        assertEquals(.95f, ComfortRenderPolicy.heldScale(options, false, true, true));
        // Rule 4: shield=T, block=F, tool=F -> 0.95f
        assertEquals(.95f, ComfortRenderPolicy.heldScale(options, false, false, true));

        // Rule 5: shield=F, block=T, tool=T -> 0.40f (block has priority over tool)
        assertEquals(.40f, ComfortRenderPolicy.heldScale(options, true, true, false));
        // Rule 6: shield=F, block=T, tool=F -> 0.40f
        assertEquals(.40f, ComfortRenderPolicy.heldScale(options, true, false, false));
        // Rule 7: shield=F, block=F, tool=T -> 0.75f (tool scale)
        assertEquals(.75f, ComfortRenderPolicy.heldScale(options, false, true, false));
        // Rule 8: shield=F, block=F, tool=F -> 0.60f (item fallback scale)
        assertEquals(.60f, ComfortRenderPolicy.heldScale(options, false, false, false));
    }
}
