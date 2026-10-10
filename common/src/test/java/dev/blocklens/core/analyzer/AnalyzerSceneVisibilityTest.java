package dev.blocklens.core.analyzer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

final class AnalyzerSceneVisibilityTest {
    private static final int HIDDEN=256;
    private static final AnalyzerEngine.Point EYE=new AnalyzerEngine.Point(.5,1.5,.5);
    @Test void hiddenSourcesDoNotBecomeLavaBoundaryNeighbors() {
        assertFalse(AnalyzerEngine.lavaBoundary(new int[]{2,2|HIDDEN,2,2,2,-1}));
        assertTrue(AnalyzerEngine.lavaBoundary(new int[]{2,2|HIDDEN,0|HIDDEN,2,2,-1}));
    }
    @Test void hiddenBeaconBasesStillContributePhysicalRange() {
        var engine=new AnalyzerEngine();
        var identity=new Object();
        var view=new AnalyzerEngineTest.FakeWorld() {
            @Override public int cell(int x,int y,int z) {
                reads++;
                if(x==0 && y==0 && z==0) return AnalyzerEngine.BEACON;
                return y==-1 && Math.abs(x)<=1 && Math.abs(z)<=1?AnalyzerEngine.BASE|HIDDEN:AnalyzerEngine.OTHER;
            }
        };
        for(int i=0;i<80;i++) {
            engine.tick(identity,view,EYE,4,AnalyzerOptions.defaults());
            assertTrue(engine.work().cells()<=512);
        }
        assertTrue(engine.markers().stream().anyMatch(marker->marker.radius()==20));
    }
    @Test void hiddenJobEndpointsAreNotPublishedOrRetained() {
        var engine=new AnalyzerEngine();
        var identity=new Object();
        var site=new AnalyzerEngine.Point(.5,.5,.5);
        var view=new AnalyzerEngineTest.FakeWorld() {
            boolean hidden;
            @Override public int cell(int x,int y,int z) { reads++;return hidden?AnalyzerEngine.OTHER|HIDDEN:AnalyzerEngine.OTHER; }
            @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye,int h,int v,int sections,int entities,int candidates) {
                return new AnalyzerEngine.VillageSample(List.of(new AnalyzerEngine.Villager(EYE,site)),1,1);
            }
        };
        engine.tick(identity,view,EYE,16,AnalyzerOptions.defaults());
        assertEquals(1,engine.markers().size());
        view.hidden=true;
        engine.tick(identity,view,EYE,16,AnalyzerOptions.defaults());
        assertTrue(engine.markers().isEmpty(),"hidden retained endpoint");
        for(int i=0;i<40;i++) engine.tick(identity,view,EYE,16,AnalyzerOptions.defaults());
        assertTrue(engine.markers().isEmpty(),"hidden newly sampled endpoint");
        assertTrue(engine.work().cells()<=512);
    }
}
