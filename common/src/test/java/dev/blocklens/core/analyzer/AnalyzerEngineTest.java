package dev.blocklens.core.analyzer;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

final class AnalyzerEngineTest {
    private final Object world = new Object();
    private final AnalyzerEngine engine = new AnalyzerEngine();
    private static final AnalyzerEngine.Point EYE = new AnalyzerEngine.Point(0.5, 1.5, 0.5);

    static class FakeWorld implements AnalyzerEngine.WorldView {
        int reads;
        int probes;
        int visits;
        int value = AnalyzerEngine.OTHER;
        @Override public int cell(int x, int y, int z) { reads++; return value; }
        @Override public boolean debrisSection(int x, int y, int z) { probes++; return false; }
        @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye, int h, int v, int sections, int entities, int candidates) {
            visits++; return new AnalyzerEngine.VillageSample(List.of(),0,0);
        }
    }

    @Test void offAndMissingContextNeverQueryWorld() {
        var view = new FakeWorld();
        engine.tick(world, view, EYE, 0, AnalyzerOptions.defaults());
        engine.tick(null, view, EYE, 31, AnalyzerOptions.defaults());
        assertEquals(0, view.reads + view.probes + view.visits);
        assertTrue(engine.markers().isEmpty());
    }

    @Test void everyTickHasIndependentHardBudgetsEvenWhenAllEnabled() {
        var view = new FakeWorld();
        for (int tick = 0; tick < 80; tick++) {
            engine.tick(world, view, EYE, 31, AnalyzerOptions.defaults());
            var work = engine.work();
            assertTrue(work.cells() <= 512);
            assertTrue(work.sections() <= 64);
            assertTrue(work.cursorSteps() <= 4096);
            assertTrue(work.entitySections() <= 125);
            assertTrue(work.entities() <= 64);
        }
        assertTrue(view.probes > 0);
        assertTrue(view.reads > 0);
    }

    @Test void paletteNegativeDebrisUsesNoCellReadsAndReachesEnd() {
        var view = new FakeWorld();
        for (int tick = 0; tick < 30; tick++) engine.tick(world, view, EYE, 1, AnalyzerOptions.defaults());
        assertEquals(0, view.reads);
        assertTrue(view.probes > 0);
        assertTrue(engine.markers().isEmpty());
    }

    @Test void sourceLavaNeedsOneKnownNonSourceNeighbor() {
        assertFalse(AnalyzerEngine.lavaBoundary(new int[]{2, 2, -1, 2, 2, 2}));
        assertTrue(AnalyzerEngine.lavaBoundary(new int[]{2, 2, -1, 0, 2, 2}));
        assertFalse(AnalyzerEngine.lavaBoundary(new int[]{-1,-1,-1,-1,-1,-1}));
    }

    @Test void worldChangeAndOffClearRetainedMarkersAndCursors() {
        var view = new FakeWorld();
        view.value = AnalyzerEngine.ROD;
        for(int tick=0; tick<300; tick++) engine.tick(world, view, EYE, 8, AnalyzerOptions.defaults());
        assertFalse(engine.markers().isEmpty());
        engine.tick(new Object(), view, EYE, 0, AnalyzerOptions.defaults());
        assertTrue(engine.markers().isEmpty());
        assertEquals(0, engine.work().cells());
        engine.clear();
        assertTrue(engine.markers().isEmpty());
    }

    @Test void numericOptionsAreImmutableAndRejectOutOfBounds() {
        var original = AnalyzerOptions.defaults();
        var changed = original.with(0, 0, 16);
        assertEquals(64, original.value(0,0));
        assertEquals(16, changed.value(0,0));
        assertEquals(16, changed.value(0,1));
        assertThrows(IllegalArgumentException.class, () -> original.with(1,0,9));
        assertThrows(IllegalArgumentException.class, () -> original.with(4,2,25));
        assertEquals(original, AnalyzerOptions.defaults());
        assertEquals(original.hashCode(), AnalyzerOptions.defaults().hashCode());
    }

    @Test void disappearingEarlyCandidateIsNotRepublishedAtPassCompletion() {
        for (int disappeared : new int[]{AnalyzerEngine.OTHER,AnalyzerEngine.UNKNOWN}) {
            var view = new FakeWorld() {
                boolean exists=true;
                @Override public int cell(int x,int y,int z) {
                    reads++;
                    return x==-16 && y==-7 && z==-16 ? (exists?AnalyzerEngine.ROD:disappeared) : AnalyzerEngine.OTHER;
                }
            };
            engine.clear();
            engine.tick(world,view,EYE,8,AnalyzerOptions.defaults());
            view.exists=false;
            for(int tick=0;tick<400;tick++) {
                engine.tick(world,view,EYE,8,AnalyzerOptions.defaults());
                assertTrue(engine.markers().isEmpty(),"stale completed-pass candidate");
                assertTrue(engine.work().cells()<=512);
            }
        }
    }

    @Test void denseLavaAndFullBeaconBasesRespectReservations() {
        var lava=new FakeWorld() {
            @Override public int cell(int x,int y,int z) { reads++;return AnalyzerEngine.LAVA; }
        };
        engine.tick(world,lava,EYE,2,AnalyzerOptions.defaults());
        assertEquals(511,engine.work().cells());
        assertTrue(engine.markers().isEmpty());
        engine.clear();
        var beacon=new FakeWorld() {
            @Override public int cell(int x,int y,int z) { reads++;return y<0?AnalyzerEngine.BASE:AnalyzerEngine.BEACON; }
        };
        var settings=AnalyzerOptions.defaults().with(2,0,1).with(2,1,1);
        engine.tick(world,beacon,EYE,4,settings);
        assertTrue(engine.work().cells()>=495,"four-layer candidates were not exercised");
        assertTrue(engine.work().cells()<=512);
        for(int tick=0;tick<30;tick++) engine.tick(world,beacon,EYE,4,settings);
        assertTrue(engine.markers().stream().anyMatch(marker->marker.radius()==50));
    }

    @Test void unknownHigherBeaconLayerKeepsCompletedLowerLayer() {
        var view=new FakeWorld() {
            @Override public int cell(int x,int y,int z) {
                reads++;
                if(x==0 && y==0 && z==0) return AnalyzerEngine.BEACON;
                return y==-1 && Math.abs(x)<=1 && Math.abs(z)<=1 ? AnalyzerEngine.BASE : AnalyzerEngine.UNKNOWN;
            }
        };
        var settings=AnalyzerOptions.defaults().with(2,0,1).with(2,1,1);
        for(int tick=0;tick<20;tick++) engine.tick(world,view,EYE,4,settings);
        assertEquals(1,engine.markers().size());
        assertEquals(20,engine.markers().getFirst().radius());
    }

    @Test void unchangedEnabledMaskStillClearsOnWorldAndAnchorChanges() {
        var view=new FakeWorld();view.value=AnalyzerEngine.ROD;
        for(int tick=0;tick<80;tick++) engine.tick(world,view,EYE,8,AnalyzerOptions.defaults());
        assertFalse(engine.markers().isEmpty());
        var nextWorld=new Object();
        engine.tick(nextWorld,view,EYE,8,AnalyzerOptions.defaults());
        assertTrue(engine.markers().isEmpty(),"same-mask world retained an old completed pass");
        for(int tick=0;tick<80;tick++) engine.tick(nextWorld,view,EYE,8,AnalyzerOptions.defaults());
        engine.tick(nextWorld,view,new AnalyzerEngine.Point(1.5,1.5,.5),8,AnalyzerOptions.defaults());
        assertTrue(engine.markers().isEmpty(),"moved anchor retained an old pass");
    }

    @Test void changedLimitsInvalidateBothRenderSnapshotAndScanCursor() {
        var view=new FakeWorld();view.value=AnalyzerEngine.ROD;
        for(int tick=0;tick<80;tick++) engine.tick(world,view,EYE,8,AnalyzerOptions.defaults());
        var changed=AnalyzerOptions.defaults().with(3,0,1).with(3,1,1).with(3,2,1);
        assertTrue(engine.markers(8,changed).isEmpty(),"old radius visible before next tick");
        engine.tick(world,view,EYE,8,changed);
        assertEquals(1,engine.markers().size());
        assertEquals(EYE,engine.markers().getFirst().from());
    }

    @Test void boundedVillageSamplesUseOnlyKnownEndpointsAndRejectAdapterOverrun() {
        var site=new AnalyzerEngine.Point(.5,1.5,-.5);
        var view=new FakeWorld() {
            @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye,int h,int v,int s,int e,int c) {
                return new AnalyzerEngine.VillageSample(List.of(new AnalyzerEngine.Villager(EYE,site)),125,64);
            }
        };
        view.value=AnalyzerEngine.OTHER;
        engine.tick(world,view,EYE,16,AnalyzerOptions.defaults());
        assertEquals(1,engine.markers().size());
        assertEquals(1,engine.work().cells());
        assertEquals(125,engine.work().entitySections());
        assertEquals(64,engine.work().entities());
        engine.clear();view.value=AnalyzerEngine.UNKNOWN;
        engine.tick(world,view,EYE,16,AnalyzerOptions.defaults());
        assertTrue(engine.markers().isEmpty());
        var unbounded=new FakeWorld() {
            @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye,int h,int v,int s,int e,int c) {
                return new AnalyzerEngine.VillageSample(List.of(),126,65);
            }
        };
        engine.clear();
        assertThrows(IllegalStateException.class,()->engine.tick(world,unbounded,EYE,16,AnalyzerOptions.defaults()));
    }

    @Test void sameAnchorMovementCannotPublishDebrisOutsideCurrentSphere() {
        var view=new FakeWorld() {
            @Override public boolean debrisSection(int x,int y,int z) { probes++;return x==1 && y==0 && z==0; }
            @Override public int cell(int x,int y,int z) {
                reads++; return x==16 && y==1 && z==0 ? AnalyzerEngine.DEBRIS : AnalyzerEngine.OTHER;
            }
        };
        var options=AnalyzerOptions.defaults().with(0,0,16);
        engine.tick(world,view,EYE,1,options);
        var moved=new AnalyzerEngine.Point(.1,1.5,.5);
        for(int tick=0;tick<50;tick++) {
            engine.tick(world,view,moved,1,options);
            assertTrue(engine.markers().isEmpty(),"completed candidate outside current sphere");
        }
        engine.clear();
        for(int tick=0;tick<20;tick++) engine.tick(world,view,EYE,1,options);
        assertEquals(1,engine.markers().size(),"exact sphere boundary should be inclusive");
    }

    @Test void nearestOrderingUsesCapturedPassReferenceWithinSameAnchor() {
        var view=new FakeWorld() {
            @Override public int cell(int x,int y,int z) {
                reads++;return (x==-1 || x==1) && y==-7 && z==-16?AnalyzerEngine.ROD:AnalyzerEngine.OTHER;
            }
        };
        var options=AnalyzerOptions.defaults().with(3,2,1).with(3,3,100);
        engine.tick(world,view,new AnalyzerEngine.Point(.1,1.5,.5),8,options);
        for(int tick=0;tick<45;tick++) engine.tick(world,view,new AnalyzerEngine.Point(.9,1.5,.5),8,options);
        assertEquals(1,engine.markers().size());
        assertEquals(-.5,engine.markers().getFirst().from().x());
    }
    @Test void allFiveAnalyzersMakeProgressWithinTheSharedBudget() {
        var view=new FakeWorld() {
            @Override public boolean debrisSection(int x,int y,int z) { probes++;return x==0 && y==0 && z==0; }
            @Override public int cell(int x,int y,int z) {
                reads++;
                if(y==1 && z==0 && x==0) return AnalyzerEngine.DEBRIS;
                if(y==1 && z==0 && x==1) return AnalyzerEngine.LAVA;
                if(y==1 && z==0 && x==-1) return AnalyzerEngine.BEACON;
                if(y==1 && z==-1 && x==0) return AnalyzerEngine.ROD;
                if(y==0 && x>=-2 && x<=0 && z>=-1 && z<=1) return AnalyzerEngine.BASE;
                return AnalyzerEngine.OTHER;
            }
            @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye,int h,int v,int s,int e,int c) {
                return new AnalyzerEngine.VillageSample(List.of(new AnalyzerEngine.Villager(EYE,
                        new AnalyzerEngine.Point(.5,1.5,1.5))),1,1);
            }
        };
        var options=AnalyzerOptions.defaults().with(0,0,16).with(0,3,100);
        for(int kind=1;kind<5;kind++) options=options.with(kind,0,1).with(kind,1,1).with(kind,3,100);
        int observed=0;
        for(int tick=0;tick<200;tick++) {
            engine.tick(world,view,EYE,31,options);
            assertTrue(engine.work().cells()<=512);
            assertTrue(engine.work().sections()<=64);
            assertTrue(engine.work().cursorSteps()<=4096);
            for(var marker:engine.markers()) observed|=1<<marker.kind();
        }
        assertEquals(31,observed,"an enabled analyzer was starved");
        assertEquals(5,engine.markers().stream().map(AnalyzerEngine.Marker::kind).distinct().count());
        assertThrows(UnsupportedOperationException.class,()->engine.markers().clear());
    }
    @Test void explicitLifecycleClearResetsReportedWorkWithoutFurtherWorldQueries() {
        var view=new FakeWorld();
        engine.tick(world,view,EYE,8,AnalyzerOptions.defaults());
        assertTrue(engine.work().cells()>0);
        int reads=view.reads;
        engine.clear();
        assertEquals(new AnalyzerEngine.Work(0,0,0,0,0),engine.work());
        assertEquals(reads,view.reads);
        assertTrue(engine.markers().isEmpty());
    }
    @Test void aNewPassCapturesItsReferenceAfterTheMinimumInterval() {
        var view=new FakeWorld() {
            @Override public int cell(int x,int y,int z) {
                reads++;return (x==-1 || x==1) && y==-7 && z==-16?AnalyzerEngine.ROD:AnalyzerEngine.OTHER;
            }
        };
        var options=AnalyzerOptions.defaults().with(3,2,1).with(3,3,100);
        for(int i=0;i<50;i++) engine.tick(world,view,new AnalyzerEngine.Point(.1,1.5,.5),8,options);
        assertEquals(-.5,engine.markers().getFirst().from().x());
        for(int i=0;i<150;i++) engine.tick(world,view,new AnalyzerEngine.Point(.9,1.5,.5),8,options);
        assertEquals(1.5,engine.markers().getFirst().from().x());
    }

    @Test void completedBeaconCandidatesMustStillHaveAValidBaseLayer() {
        var view=new FakeWorld() {
            boolean base=true;
            @Override public int cell(int x,int y,int z) {
                reads++;
                if(x==-16 && y==-7 && z==-16) return AnalyzerEngine.BEACON;
                return base && y==-8 && x>=-17 && x<=-15 && z>=-17 && z<=-15?AnalyzerEngine.BASE:AnalyzerEngine.OTHER;
            }
        };
        engine.tick(world,view,EYE,4,AnalyzerOptions.defaults());
        view.base=false;
        for(int i=0;i<80;i++) {
            engine.tick(world,view,EYE,4,AnalyzerOptions.defaults());
            assertTrue(engine.markers().isEmpty(),"removed base produced a newly published beacon range");
            assertTrue(engine.work().cells()<=512);
        }
    }

    @Test void completedLavaCandidatesMustStillHaveAKnownNonSourceNeighbor() {
        var view=new FakeWorld() {
            boolean enclosed;
            @Override public int cell(int x,int y,int z) {
                reads++;
                if(x==-8 && y==-4 && z==-8) return AnalyzerEngine.LAVA;
                return enclosed && Math.abs(x+8)+Math.abs(y+4)+Math.abs(z+8)==1?AnalyzerEngine.LAVA:AnalyzerEngine.OTHER;
            }
        };
        var options=AnalyzerOptions.defaults().with(1,0,8).with(1,1,5);
        engine.tick(world,view,EYE,2,options);
        view.enclosed=true;
        for(int i=0;i<30;i++) {
            engine.tick(world,view,EYE,2,options);
            assertTrue(engine.markers().stream().noneMatch(m->m.to().equals(new AnalyzerEngine.Point(-7.5,-3.5,-7.5))),
                    "enclosed source produced a newly published boundary marker");
            assertTrue(engine.work().cells()<=512);
        }
    }
}
