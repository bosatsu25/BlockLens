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
}
