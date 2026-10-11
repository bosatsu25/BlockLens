package dev.blocklens.core.scene;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * JSTQB FL Decision Table test suite for SceneFilterPolicy.
 * Completely covers all combinations of (enabled, localPlayer, mode, inBlacklist, inWhitelist).
 */
final class SceneFilterPolicyTest {

    @ParameterizedTest(name = "Rule: enabled={0}, mode={1}, black={2}, white={3}, local={4} -> hidden={5}")
    @CsvSource({
            // Decision Table: Disabled state (enabled=false) -> Always NOT hidden
            "false, NONE,      false, false, false, false",
            "false, NONE,      true,  true,  true,  false",
            "false, BLACKLIST, true,  false, false, false",
            "false, BLACKLIST, true,  false, true,  false",
            "false, WHITELIST, false, false, false, false",
            "false, WHITELIST, false, true,  true,  false",

            // Decision Table: Local player exception (enabled=true, localPlayer=true) -> Always NOT hidden
            "true,  NONE,      false, false, true,  false",
            "true,  NONE,      true,  true,  true,  false",
            "true,  BLACKLIST, true,  false, true,  false",
            "true,  BLACKLIST, false, true,  true,  false",
            "true,  WHITELIST, false, false, true,  false",
            "true,  WHITELIST, true,  true,  true,  false",

            // Decision Table: Enabled, Remote entity, Mode = NONE -> Always NOT hidden
            "true,  NONE,      false, false, false, false",
            "true,  NONE,      true,  false, false, false",
            "true,  NONE,      false, true,  false, false",
            "true,  NONE,      true,  true,  false, false",

            // Decision Table: Enabled, Remote entity, Mode = BLACKLIST -> Hidden iff inBlacklist
            "true,  BLACKLIST, false, false, false, false",
            "true,  BLACKLIST, false, true,  false, false",
            "true,  BLACKLIST, true,  false, false, true",
            "true,  BLACKLIST, true,  true,  false, true",

            // Decision Table: Enabled, Remote entity, Mode = WHITELIST -> Hidden iff NOT inWhitelist
            "true,  WHITELIST, false, false, false, true",
            "true,  WHITELIST, true,  false, false, true",
            "true,  WHITELIST, false, true,  false, false",
            "true,  WHITELIST, true,  true,  false, false"
    })
    @DisplayName("JSTQB Decision Table verification for SceneFilterPolicy.hidden")
    void verifyDecisionTable(boolean enabled, SceneFilterOptions.Mode mode,
                             boolean inBlacklist, boolean inWhitelist,
                             boolean localPlayer, boolean expectedHidden) {
        boolean actual = SceneFilterPolicy.hidden(enabled, mode, inBlacklist, inWhitelist, localPlayer);
        assertEquals(expectedHidden, actual,
                () -> String.format("Failed for: enabled=%b, mode=%s, inBlack=%b, inWhite=%b, localPlayer=%b",
                        enabled, mode, inBlacklist, inWhitelist, localPlayer));
    }

    @Test
    void nullModeThrowsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> SceneFilterPolicy.hidden(true, null, false, false, false));
    }
}
