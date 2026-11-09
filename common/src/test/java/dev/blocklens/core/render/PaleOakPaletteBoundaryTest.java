package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * CTFL 4.2.2 three-value BVA of the three comparisons in {@link PaleOakPalette#neutralWood} and a
 * 4.2.3 collapsed decision table for {@link PaleOakPalette#applies}; see
 * {@code knowledge/current/test-design-ctfl.md} §6 (TD-P*). The existing palette test checks the warm
 * chroma boundary 48/49 only; the red-vs-green and green-vs-blue equalities were untested, and PIT
 * reported their boundary mutants as survivors.
 */
final class PaleOakPaletteBoundaryTest {
    /** The pixel stays as it is when red < green, green < blue or red - blue > 48; else it turns gray. */
    @ParameterizedTest(name = "TD-P1 {0} -> {1}")
    @CsvSource({
            // red - blue: 47, 48 (neutralized), 49 (kept)
            "FF806F51, FF717171", "FF806F50, FF717171", "FF806F4F, FF806F4F",
            // red vs green: red = green - 1 (kept), red = green (neutralized), red = green + 1
            "FF808170, FF808170", "FF808070, FF7E7E7E", "FF807F70, FF7E7E7E",
            // green vs blue: green = blue - 1 (kept), green = blue, green = blue + 1
            "FF907071, FF907071", "FF907070, FF7A7A7A", "FF90706F, FF797979",
            // alpha 0 is untouched whatever the color; alpha 1 is processed
            "00806F50, 00806F50", "01806F50, 01717171",
    })
    void neutralWoodBoundaries(String argb, String expected) {
        assertEquals(expected, String.format("%08X", PaleOakPalette.neutralWood((int) Long.parseLong(argb, 16))));
    }

    /**
     * Decision table: C1 enabled, C2 namespace minecraft, C3 owner vanilla, C4 path in the set,
     * C5 width in 1..256, C6 height in 1..256. All true -> applies; one false -> does not, whatever
     * the others, so the table collapses to one rule per condition plus the all-true rule. The size
     * conditions carry their own two boundaries.
     */
    @ParameterizedTest(name = "TD-P2 enabled={0} ns={1} owner={2} path={3} {4}x{5} -> {6}")
    @CsvSource(delimiter = '|', value = {
            "true  | minecraft | vanilla | textures/block/pale_oak_planks.png | 16  | 16  | true",
            "false | minecraft | vanilla | textures/block/pale_oak_planks.png | 16  | 16  | false",
            "true  | blocklens | vanilla | textures/block/pale_oak_planks.png | 16  | 16  | false",
            "true  | minecraft | fabric  | textures/block/pale_oak_planks.png | 16  | 16  | false",
            "true  | minecraft | vanilla | textures/block/oak_planks.png      | 16  | 16  | false",
            "true  | minecraft | vanilla | textures/block/pale_oak_planks.png | 0   | 16  | false",
            "true  | minecraft | vanilla | textures/block/pale_oak_planks.png | 1   | 1   | true",
            "true  | minecraft | vanilla | textures/block/pale_oak_planks.png | 256 | 256 | true",
            "true  | minecraft | vanilla | textures/block/pale_oak_planks.png | 257 | 16  | false",
            "true  | minecraft | vanilla | textures/block/pale_oak_planks.png | 16  | 0   | false",
            "true  | minecraft | vanilla | textures/block/pale_oak_planks.png | 16  | 257 | false",
    })
    void appliesDecisionTable(boolean enabled, String namespace, String owner, String path, int width, int height,
            boolean applies) {
        assertEquals(applies, PaleOakPalette.applies(enabled, namespace, path, owner, width, height));
    }
}
