package dev.blocklens.testing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class IdentityBoundFixtureTest {
    private static final String JOB_SITE = "controlled-barrel";

    @Test
    void seedsReplacementEvenWhenItsLogicalIdIsUnchanged() {
        Object level = new Object();
        var fixture = new IdentityBoundFixture<VillagerState>(level, v -> v.jobSite = JOB_SITE);
        var original = new VillagerState(17);
        var replacement = new VillagerState(17);
        assertEquals(original, replacement);
        assertNotSame(original, replacement);

        assertTrue(fixture.refresh(level, original));
        assertEquals(JOB_SITE, original.jobSite);
        boolean initialized = fixture.refresh(level, replacement);

        assertEquals(JOB_SITE, replacement.jobSite,
                "a replacement client object needs its own controlled JOB_SITE memory");
        assertTrue(initialized);
    }

    @Test
    void leavesErasedMemoryAbsentOnTheSameInstance() {
        Object level = new Object();
        var fixture = new IdentityBoundFixture<VillagerState>(level, v -> v.jobSite = JOB_SITE);
        var villager = new VillagerState(17);
        assertTrue(fixture.refresh(level, villager));
        villager.jobSite = null;

        assertFalse(fixture.refresh(level, villager));

        assertNull(villager.jobSite, "refresh must not conceal a same-instance memory loss");
    }

    @Test
    void temporaryAbsenceDoesNotForgetTheBoundInstance() {
        Object level = new Object();
        var fixture = new IdentityBoundFixture<VillagerState>(level, v -> v.jobSite = JOB_SITE);
        var villager = new VillagerState(17);
        assertTrue(fixture.refresh(level, villager));
        villager.jobSite = null;

        assertFalse(fixture.refresh(level, null));
        assertFalse(fixture.refresh(level, villager));

        assertNull(villager.jobSite);
    }

    @Test
    void waitsForBothTheExpectedScopeAndAnEntity() {
        Object level = new Object();
        var fixture = new IdentityBoundFixture<VillagerState>(level, v -> v.jobSite = JOB_SITE);
        var villager = new VillagerState(17);

        assertFalse(fixture.refresh(level, null));
        assertFalse(fixture.refresh(null, villager));
        assertNull(villager.jobSite);
        assertTrue(fixture.refresh(level, villager));

        assertEquals(JOB_SITE, villager.jobSite);
    }

    @Test
    void ignoresAnotherScopeEvenWhenItsValueIsEqual() {
        var level = new Scope("overworld");
        var otherLevel = new Scope("overworld");
        var fixture = new IdentityBoundFixture<VillagerState>(level, v -> v.jobSite = JOB_SITE);
        var original = new VillagerState(17);
        var otherVillager = new VillagerState(17);
        assertTrue(fixture.refresh(level, original));
        original.jobSite = null;

        assertFalse(fixture.refresh(otherLevel, otherVillager));
        assertNull(otherVillager.jobSite);
        assertFalse(fixture.refresh(level, original));

        assertNull(original.jobSite, "a scope mismatch must not reset the original binding");
    }

    @Test
    void failedReplacementInitializationCanRetryWithoutForgettingTheOriginal() {
        Object level = new Object();
        var failure = new IllegalStateException("controlled initialization failure");
        var fixture = new IdentityBoundFixture<VillagerState>(level, v -> {
            if (v.rejectInitialization) throw failure;
            v.jobSite = JOB_SITE;
        });
        var original = new VillagerState(17);
        var replacement = new VillagerState(17);
        assertTrue(fixture.refresh(level, original));
        original.jobSite = null;
        replacement.rejectInitialization = true;

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> fixture.refresh(level, replacement)));
        assertNull(replacement.jobSite);
        assertFalse(fixture.refresh(level, original));
        assertNull(original.jobSite);
        replacement.rejectInitialization = false;
        assertTrue(fixture.refresh(level, replacement));

        assertEquals(JOB_SITE, replacement.jobSite);
    }

    private record Scope(String dimension) { }

    private static final class VillagerState {
        private final int id;
        private String jobSite;
        private boolean rejectInitialization;

        private VillagerState(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof VillagerState villager && id == villager.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }
}
