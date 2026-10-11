package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class VisualGeometryKeyTest {
    @Test void aVisualGenerationCannotReuseAnotherEnabledMaskOrBase() {
        var base=new Object();var a=new VisualGeometryKey(base,1L<<61);
        assertEquals(a,new VisualGeometryKey(base,1L<<61));
        assertEquals(a.hashCode(),new VisualGeometryKey(base,1L<<61).hashCode());
        assertNotEquals(a,new VisualGeometryKey(base,1L<<18));
        assertNotEquals(a,new VisualGeometryKey(new Object(),1L<<61));
        assertEquals(base,a.base());assertEquals(1L<<61,a.enabledMask());
        assertThrows(NullPointerException.class,()->new VisualGeometryKey(null,0));
    }
}