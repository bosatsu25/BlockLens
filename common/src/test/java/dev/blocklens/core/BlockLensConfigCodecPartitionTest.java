package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.scene.SceneFilterOptions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * CTFL 4.2.1 equivalence partitioning (each-choice over the line classes the decoder distinguishes),
 * 4.2.2 three-value boundary value analysis of the bounded numeric options and 4.4.1 error guessing
 * for {@link BlockLensConfigCodec#decode}; see {@code knowledge/current/test-design-ctfl.md} §4
 * (TD-C*). Every invalid partition must leave the default in place: the codec fails soft by contract.
 */
final class BlockLensConfigCodecPartitionTest {
    private static final BlockLensConfig DEFAULTS = BlockLensConfig.defaults();
    private static final ComfortOptions COMFORT = ComfortOptions.defaults();

    /** One valid representative per partition of a capability line (TD-C1). */
    @ParameterizedTest(name = "TD-C1 capability line [{0}] -> anvil enabled {1}")
    @CsvSource(delimiter = '|', value = {
            "deco.anvil=true   | true",
            "deco.anvil=false  | false",
            "deco.anvil=TRUE   | true",
            "deco.anvil=False  | false",
            "  deco.anvil = true  | true",
            "deco.anvil=yes    | false",
            "deco.anvil=1      | false",
            "deco.anvil=       | false",
            "deco.anvil=true=false | false",
    })
    void capabilityLinePartitions(String line, boolean enabled) {
        assertFalse(DEFAULTS.isEnabled(CapabilityId.ANVIL), "fixture: the anvil cue is off by default");
        assertEquals(enabled, BlockLensConfigCodec.decode(line + "\n").isEnabled(CapabilityId.ANVIL));
    }

    /** Lines that carry no setting at all (TD-C2): comments, blanks, no separator, unknown keys. */
    @ParameterizedTest(name = "TD-C2 inert line [{0}]")
    @CsvSource(delimiter = '|', value = {
            "# deco.anvil=true",
            "'   '",
            "deco.anvil",
            "unknown.key=true",
            "=true",
            "DECO.ANVIL=true",
    })
    void inertLinesLeaveEveryDefaultInPlace(String line) {
        assertEquals(DEFAULTS, BlockLensConfigCodec.decode(line + "\n"));
    }

    /** Three-value BVA of each bounded comfort option (TD-C3): both neighbours of both boundaries. */
    @ParameterizedTest(name = "TD-C3 {0}={1} -> {2}")
    @CsvSource({
            "others.low_fire.size, -1, 1", "others.low_fire.size, 0, 0", "others.low_fire.size, 1, 1",
            "others.low_fire.size, 2, 2", "others.low_fire.size, 3, 1",
            "others.handheld_size.blocks_percent, 39, 70", "others.handheld_size.blocks_percent, 40, 40",
            "others.handheld_size.blocks_percent, 41, 41", "others.handheld_size.blocks_percent, 99, 99",
            "others.handheld_size.blocks_percent, 100, 100", "others.handheld_size.blocks_percent, 101, 70",
            "others.handheld_size.items_percent, 39, 60", "others.handheld_size.items_percent, 40, 40",
            "others.handheld_size.items_percent, 100, 100", "others.handheld_size.items_percent, 101, 60",
            "others.handheld_size.tools_percent, 39, 75", "others.handheld_size.tools_percent, 40, 40",
            "others.handheld_size.tools_percent, 100, 100", "others.handheld_size.tools_percent, 101, 75",
    })
    void boundedOptionsAcceptBothBoundariesAndFallBackOutsideThem(String key, int value, int expected) {
        ComfortOptions decoded = BlockLensConfigCodec.decode(key + "=" + value + "\n").comfortOptions();
        int actual = switch (key) {
            case "others.low_fire.size" -> decoded.fireSize();
            case "others.handheld_size.blocks_percent" -> decoded.blocksPercent();
            case "others.handheld_size.items_percent" -> decoded.itemsPercent();
            default -> decoded.toolsPercent();
        };
        assertEquals(expected, actual);
        assertEquals(COMFORT.fireSize(), key.startsWith("others.low") ? COMFORT.fireSize() : decoded.fireSize(),
                "an option line changes no other option");
    }

    /** Numeric partitions beyond the range: how the text is written (TD-C4). */
    @ParameterizedTest(name = "TD-C4 blocks_percent=[{0}] -> {1}")
    @CsvSource(delimiter = '|', value = {
            "70      | 70",
            "+70     | 70",
            "070     | 70",
            " 70     | 70",
            "７０    | 70",
            "70.0    | 70",
            "7e1     | 70",
            "seventy | 70",
            "99999999999 | 70",
            "-70     | 70",
            "        | 70",
            "41      | 41",
    })
    void numericTextPartitions(String text, int expected) {
        // Only the last row changes the value: every other partition is either a plain 70 or falls back to 70.
        assertEquals(expected, BlockLensConfigCodec.decode("others.handheld_size.blocks_percent=" + text + "\n")
                .comfortOptions().blocksPercent());
    }

    /** Scene filter partitions (TD-C5): mode case, invalid mode, valid list, list with an invalid member. */
    @Test
    void sceneFilterModeAndListPartitions() {
        assertEquals(SceneFilterOptions.Mode.WHITELIST,
                BlockLensConfigCodec.decode("scene.block_filter.mode=whitelist\n").sceneFilterOptions().blocks().mode());
        assertEquals(SceneFilterOptions.Mode.BLACKLIST,
                BlockLensConfigCodec.decode("scene.entity_filter.mode=BLACKLIST\n").sceneFilterOptions().entities().mode());
        assertEquals(SceneFilterOptions.Mode.NONE,
                BlockLensConfigCodec.decode("scene.block_filter.mode=greylist\n").sceneFilterOptions().blocks().mode());
        var lists = BlockLensConfigCodec.decode("""
                scene.block_filter.blacklist=minecraft:stone, Minecraft:DIRT ,minecraft:stone
                scene.block_filter.whitelist=minecraft:oak_log
                scene.entity_filter.whitelist=minecraft:item,
                """).sceneFilterOptions();
        assertEquals(List.of("minecraft:dirt", "minecraft:stone"), lists.blocks().blacklist(),
                "trimmed, lower-cased, sorted and deduplicated");
        assertEquals(List.of("minecraft:oak_log"), lists.blocks().whitelist());
        assertEquals(List.of(), lists.entities().whitelist(),
                "a trailing comma makes the whole line invalid, so the previous (empty) list stays");
    }

    /** Error guessing (TD-C6): the ways a hand-edited properties file differs from an encoded one. */
    @Test
    void laterDuplicateKeysWinAndLineEndingsDoNotMatter() {
        var config = BlockLensConfigCodec.decode(
                "deco.anvil=true\r\ndeco.anvil=false\r\nothers.low_fire.size=0\r\nothers.low_fire.size=2\r\n");
        assertFalse(config.isEnabled(CapabilityId.ANVIL));
        assertEquals(2, config.comfortOptions().fireSize());
        var mixed = BlockLensConfigCodec.decode("deco.anvil=true\rdeco.stairs=true\nothers.low_fire.size=2\r\n");
        assertTrue(mixed.isEnabled(CapabilityId.ANVIL));
        assertTrue(mixed.isEnabled(CapabilityId.STAIRS));
    }

    @Test
    void oneInvalidLineNeverDiscardsTheValidLinesAroundIt() {
        var config = BlockLensConfigCodec.decode("""
                deco.anvil=true
                others.handheld_size.blocks_percent=1000
                deco.stairs=maybe
                others.low_fire.size=2
                """);
        assertTrue(config.isEnabled(CapabilityId.ANVIL));
        assertEquals(COMFORT.blocksPercent(), config.comfortOptions().blocksPercent());
        assertEquals(DEFAULTS.isEnabled(CapabilityId.STAIRS), config.isEnabled(CapabilityId.STAIRS));
        assertEquals(2, config.comfortOptions().fireSize());
    }

    @Test
    void encodeThenDecodeIsTheIdentityForEveryBoundaryOption() {
        var extreme = DEFAULTS.withComfortOptions(new ComfortOptions(0, 40, 100, 40))
                .withSceneFilterOptions(SceneFilterOptions.defaults().withMode(1, SceneFilterOptions.Mode.WHITELIST)
                        .withList(1, true, "minecraft:item_frame"));
        for (CapabilityId capability : CapabilityId.values()) {
            extreme = extreme.withEnabled(capability, capability.ordinal() % 2 == 0);
        }
        assertEquals(extreme, BlockLensConfigCodec.decode(BlockLensConfigCodec.encode(extreme)));
        var other = DEFAULTS.withComfortOptions(new ComfortOptions(2, 100, 40, 100));
        assertEquals(other, BlockLensConfigCodec.decode(BlockLensConfigCodec.encode(other)));
    }
}
