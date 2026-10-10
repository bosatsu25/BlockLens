package dev.blocklens.core.scene;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SceneFilterOptionsTest {
    @Test void modesAreIndependentAndSelfAlwaysRetained() {
        for (var mode : SceneFilterOptions.Mode.values()) {
            assertFalse(SceneFilterPolicy.hidden(false, mode, true, false, false));
            assertFalse(SceneFilterPolicy.hidden(true, mode, true, false, true));
        }
        assertFalse(SceneFilterPolicy.hidden(true, SceneFilterOptions.Mode.NONE, true, false, false));
        assertTrue(SceneFilterPolicy.hidden(true, SceneFilterOptions.Mode.BLACKLIST, true, false, false));
        assertFalse(SceneFilterPolicy.hidden(true, SceneFilterOptions.Mode.BLACKLIST, false, false, false));
        assertTrue(SceneFilterPolicy.hidden(true, SceneFilterOptions.Mode.WHITELIST, true, false, false));
        assertFalse(SceneFilterPolicy.hidden(true, SceneFilterOptions.Mode.WHITELIST, false, true, false));
    }
    @Test void idsAreCanonicalImmutableAndDeterministic() {
        var input=new ArrayList<>(List.of(" Minecraft:Stone ","minecraft:dirt","MINECRAFT:STONE"));
        var parsed=SceneFilterOptions.normalize(input);
        input.clear();
        assertEquals(List.of("minecraft:dirt","minecraft:stone"),parsed);
        assertThrows(UnsupportedOperationException.class,()->parsed.add("minecraft:air"));
        assertEquals(parsed,SceneFilterOptions.parseList("minecraft:stone, minecraft:dirt, minecraft:stone"));
        assertEquals(List.of(),SceneFilterOptions.parseList(""));
    }
    @Test void unsupportedPredicatesAndLineInjectionAreRejected() {
        for(String id:List.of("stone","#c:ores","minecraft:stone[axis=x]","minecraft:stone\nscene.block_filter=true",
                "minecraft:stone/../dirt","minecraft:stone*","minecraft:STONE\u0000","minecraft:")) {
            assertThrows(IllegalArgumentException.class,()->SceneFilterOptions.parseList(id));
        }
    }
    @Test void listCountLengthAndAggregateAreBounded() {
        var ids=new ArrayList<String>();
        for(int i=0;i<512;i++) ids.add("test:block_"+i);
        assertEquals(512,SceneFilterOptions.normalize(ids).size());
        ids.add("test:extra");
        assertThrows(IllegalArgumentException.class,()->SceneFilterOptions.normalize(ids));
        assertThrows(IllegalArgumentException.class,()->SceneFilterOptions.normalize(List.of("test:"+"a".repeat(252))));
        var longIds=new ArrayList<String>();
        for(int i=0;i<512;i++) longIds.add("test:"+"a".repeat(120)+i);
        var rule=new SceneFilterOptions.Rule(SceneFilterOptions.Mode.NONE,longIds,List.of());
        assertThrows(IllegalArgumentException.class,()->new SceneFilterOptions(rule,SceneFilterOptions.Rule.empty()));
    }
    @Test void changingOneRulePreservesEveryOtherDimension() {
        var defaults=SceneFilterOptions.defaults();
        assertEquals(SceneFilterOptions.Mode.NONE,defaults.blocks().mode());
        var black=defaults.withList(0,false,"minecraft:stone");
        var white=black.withList(0,true,"minecraft:dirt");
        var entity=white.withList(1,false,"minecraft:item").withMode(1,SceneFilterOptions.Mode.BLACKLIST);
        assertEquals(List.of("minecraft:stone"),entity.blocks().blacklist());
        assertEquals(List.of("minecraft:dirt"),entity.blocks().whitelist());
        assertEquals(SceneFilterOptions.Mode.NONE,entity.blocks().mode());
        assertEquals(List.of(),defaults.blocks().blacklist());
        assertEquals(entity,white.withList(1,false,"minecraft:item").withMode(1,SceneFilterOptions.Mode.BLACKLIST));
        assertEquals(entity.hashCode(),white.withList(1,false,"minecraft:item").withMode(1,SceneFilterOptions.Mode.BLACKLIST).hashCode());
        assertThrows(IllegalArgumentException.class,()->defaults.rule(2));
    }
    @Test void presetsOnlyReplaceEntityRule() {
        var baseline=SceneFilterOptions.defaults().withList(0,false,"minecraft:stone");
        var build=baseline.withEntityPreset(0);
        assertEquals(baseline.blocks(),build.blocks());
        assertEquals(SceneFilterOptions.Mode.BLACKLIST,build.entities().mode());
        assertEquals(List.of("minecraft:area_effect_cloud","minecraft:experience_orb","minecraft:item"),build.entities().blacklist());
        var trace=baseline.withEntityPreset(1);
        assertEquals(SceneFilterOptions.Mode.WHITELIST,trace.entities().mode());
        assertEquals(6,trace.entities().whitelist().size());
        assertTrue(baseline.withEntityPreset(2).entities().blacklist().contains("minecraft:marker"));
        assertEquals(SceneFilterOptions.Rule.empty(),trace.withEntityPreset(3).entities());
        assertThrows(IllegalArgumentException.class,()->baseline.withEntityPreset(4));
    }
    @Test void aggregateBoundaryIsAcceptedAndOversizedPresetRetainsItsImmutableSource() {
        var ids=new ArrayList<String>();
        for(int i=0;i<191;i++) {
            String prefix="test:block_"+i+"_";
            ids.add(prefix+"a".repeat(256-prefix.length()));
        }
        ids.add("test:"+"b".repeat(60));
        String text=String.join(",",ids);
        assertEquals(SceneFilterOptions.MAX_TOTAL_CHARS,text.length());
        var boundary=SceneFilterOptions.defaults().withList(0,false,text);
        assertEquals(ids.size(),boundary.blocks().blacklist().size());
        assertThrows(IllegalArgumentException.class,()->boundary.withEntityPreset(0));
        assertThrows(IllegalArgumentException.class,()->boundary.withEntityPreset(1));
        assertThrows(IllegalArgumentException.class,()->boundary.withEntityPreset(2));
        assertEquals(SceneFilterOptions.Rule.empty(),boundary.entities());
        assertEquals(boundary,boundary.withEntityPreset(3));
        assertThrows(IllegalArgumentException.class,()->SceneFilterOptions.parseList(text+"a"));
        assertThrows(IllegalArgumentException.class,()->SceneFilterOptions.normalize(List.of(" ".repeat(257)+"test:a")));
    }
}
