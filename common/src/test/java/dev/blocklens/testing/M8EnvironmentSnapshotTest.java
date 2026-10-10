package dev.blocklens.testing;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import org.junit.jupiter.api.Test;

final class M8EnvironmentSnapshotTest {
    @Test void osFamiliesNeverRetainArbitraryPlatformText() {
        assertEquals(M8EnvironmentSnapshot.OsFamily.WINDOWS, M8EnvironmentSnapshot.osFamily("Windows 11"));
        assertEquals(M8EnvironmentSnapshot.OsFamily.LINUX, M8EnvironmentSnapshot.osFamily("Linux"));
        assertEquals(M8EnvironmentSnapshot.OsFamily.MACOS, M8EnvironmentSnapshot.osFamily("Mac OS X"));
        assertEquals(M8EnvironmentSnapshot.OsFamily.MACOS, M8EnvironmentSnapshot.osFamily("Darwin"));
        assertEquals(M8EnvironmentSnapshot.OsFamily.OTHER, M8EnvironmentSnapshot.osFamily("private-custom-platform"));
        assertEquals(M8EnvironmentSnapshot.OsFamily.OTHER, M8EnvironmentSnapshot.osFamily(null));
    }

    @Test void captureReportsActualVmOptionAndNumericRuntimeVersion() {
        var snapshot = M8EnvironmentSnapshot.capture();
        var bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        assertEquals(Boolean.parseBoolean(bean.getVMOption("DoEscapeAnalysis").getValue()),
                snapshot.escapeAnalysisEnabled());
        assertEquals(Runtime.version().feature(), snapshot.javaFeature());
        assertEquals(Runtime.version().update(), snapshot.javaUpdate());
        assertEquals(Runtime.getRuntime().availableProcessors(), snapshot.availableProcessors());
        assertEquals(Boolean.parseBoolean(bean.getVMOption("TieredCompilation").getValue()),
                snapshot.compilation().tieredCompilation());
        assertEquals(Integer.parseInt(bean.getVMOption("TieredStopAtLevel").getValue()),
                snapshot.compilation().tieredStopAtLevel());
        assertEquals(Boolean.parseBoolean(bean.getVMOption("BackgroundCompilation").getValue()),
                snapshot.compilation().backgroundCompilation());
        assertEquals(Long.parseLong(bean.getVMOption("ReservedCodeCacheSize").getValue()),
                snapshot.compilation().reservedCodeCacheBytes());
    }

    @Test void manifestContainsOnlyFixedKeysAndBoundedValues() {
        var snapshot = new M8EnvironmentSnapshot(false, 25, 0, 4, 1,
                M8EnvironmentSnapshot.OsFamily.LINUX, 8, compilation());
        assertEquals("vmDoEscapeAnalysis=false\njavaFeature=25\njavaInterim=0\njavaUpdate=4\n"
                + "javaPatch=1\nosFamily=linux\navailableProcessors=8\n"
                + "vmTieredCompilation=true\nvmTieredStopAtLevel=4\nvmBackgroundCompilation=true\n"
                + "vmReservedCodeCacheBytes=251658240\n", snapshot.manifest());
    }

    @Test void invalidNumericOrMissingFamilyValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new M8EnvironmentSnapshot(true, 0, 0, 0, 0,
                M8EnvironmentSnapshot.OsFamily.OTHER, 1, compilation()));
        assertThrows(IllegalArgumentException.class, () -> new M8EnvironmentSnapshot(true, 25, -1, 0, 0,
                M8EnvironmentSnapshot.OsFamily.OTHER, 1, compilation()));
        assertThrows(IllegalArgumentException.class, () -> new M8EnvironmentSnapshot(true, 25, 0, -1, 0,
                M8EnvironmentSnapshot.OsFamily.OTHER, 1, compilation()));
        assertThrows(IllegalArgumentException.class, () -> new M8EnvironmentSnapshot(true, 25, 0, 0, -1,
                M8EnvironmentSnapshot.OsFamily.OTHER, 1, compilation()));
        assertThrows(IllegalArgumentException.class, () -> new M8EnvironmentSnapshot(true, 25, 0, 0, 0,
                M8EnvironmentSnapshot.OsFamily.OTHER, 0, compilation()));
        assertThrows(NullPointerException.class, () -> new M8EnvironmentSnapshot(true, 25, 0, 0, 0, null, 1, compilation()));
        assertThrows(NullPointerException.class, () -> new M8EnvironmentSnapshot(true, 25, 0, 0, 0,
                M8EnvironmentSnapshot.OsFamily.OTHER, 1, null));
    }

    @Test void compilationLevelsAcceptBothBoundariesAndRejectValuesOutsideZeroThroughFour() {
        assertEquals(0, new M8EnvironmentSnapshot.Compilation(true, 0, true, 1).tieredStopAtLevel());
        assertEquals(4, new M8EnvironmentSnapshot.Compilation(false, 4, false, 1).tieredStopAtLevel());
        assertThrows(IllegalArgumentException.class, () -> new M8EnvironmentSnapshot.Compilation(true, -1, true, 1));
        assertThrows(IllegalArgumentException.class, () -> new M8EnvironmentSnapshot.Compilation(true, 5, true, 1));
    }

    @Test void codeCacheCapacityMustBePositiveAndWithinJdk25Maximum() {
        assertEquals(2_147_483_648L,
                new M8EnvironmentSnapshot.Compilation(false, 4, false, 2_147_483_648L).reservedCodeCacheBytes());
        assertThrows(IllegalArgumentException.class, () -> new M8EnvironmentSnapshot.Compilation(true, 4, true, 0));
        assertThrows(IllegalArgumentException.class, () -> new M8EnvironmentSnapshot.Compilation(true, 4, true, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new M8EnvironmentSnapshot.Compilation(true, 4, true, 2_147_483_649L));
    }

    private static M8EnvironmentSnapshot.Compilation compilation() {
        return new M8EnvironmentSnapshot.Compilation(true, 4, true, 251_658_240L);
    }
}
