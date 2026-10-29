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
        int visits;
        int value = AnalyzerEngine.OTHER;
        @Override public int cell(int x, int y, int z) { reads++; return value; }
        @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye, int h, int v, int sections, int entities, int candidates) {
            visits++; return new AnalyzerEngine.VillageSample(List.of(),0,0);
        }
    }

    @Test void offAndMissingContextNeverQueryWorld() {
        var view = new FakeWorld();
        engine.tick(world, view, EYE, 0, AnalyzerOptions.defaults());
        engine.tick(null, view, EYE, 15, AnalyzerOptions.defaults());
        assertEquals(0, view.reads + view.visits);
        assertTrue(engine.markers().isEmpty());
    }

    @Test void everyTickHasIndependentHardBudgetsEvenWhenAllEnabled() {
        var view = new FakeWorld();
        for (int tick = 0; tick < 80; tick++) {
            engine.tick(world, view, EYE, 15, AnalyzerOptions.defaults());
            var work = engine.work();
            assertTrue(work.cells() <= 512);
            assertTrue(work.sections() <= 64);
            assertTrue(work.cursorSteps() <= 4096);
            assertTrue(work.entitySections() <= 125);
            assertTrue(work.entities() <= 64);
        }
        assertTrue(view.reads > 0);
    }

    @Test void sourceLavaNeedsOneKnownNonSourceNeighbor() {
        assertFalse(AnalyzerEngine.lavaBoundary(new int[]{2, 2, -1, 2, 2, 2}));
        assertTrue(AnalyzerEngine.lavaBoundary(new int[]{2, 2, -1, 0, 2, 2}));
        assertFalse(AnalyzerEngine.lavaBoundary(new int[]{-1,-1,-1,-1,-1,-1}));
    }

    @Test void worldChangeAndOffClearRetainedMarkersAndCursors() {
        var view = new FakeWorld();
        view.value = AnalyzerEngine.ROD;
        for(int tick=0; tick<300; tick++) engine.tick(world, view, EYE, 4, AnalyzerOptions.defaults());
        assertFalse(engine.markers().isEmpty());
        engine.tick(new Object(), view, EYE, 0, AnalyzerOptions.defaults());
        assertTrue(engine.markers().isEmpty());
        assertEquals(0, engine.work().cells());
        engine.clear();
        assertTrue(engine.markers().isEmpty());
    }

    @Test void numericOptionsAreImmutableAndRejectOutOfBounds() {
        var original = AnalyzerOptions.defaults();
        var changed = original.with(0, 0, 8);
        assertEquals(5, original.value(0,0));
        assertEquals(8, changed.value(0,0));
        assertEquals(3, changed.value(0,1));
        assertThrows(IllegalArgumentException.class, () -> original.with(0,0,9));
        assertThrows(IllegalArgumentException.class, () -> original.with(3,2,25));
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
            engine.tick(world,view,EYE,4,AnalyzerOptions.defaults());
            view.exists=false;
            for(int tick=0;tick<400;tick++) {
                engine.tick(world,view,EYE,4,AnalyzerOptions.defaults());
                assertTrue(engine.markers().isEmpty(),"stale completed-pass candidate");
                assertTrue(engine.work().cells()<=512);
            }
        }
    }

    @Test void denseLavaAndFullBeaconBasesRespectReservations() {
        var lava=new FakeWorld() {
            @Override public int cell(int x,int y,int z) { reads++;return AnalyzerEngine.LAVA; }
        };
        engine.tick(world,lava,EYE,1,AnalyzerOptions.defaults());
        assertEquals(511,engine.work().cells());
        assertTrue(engine.markers().isEmpty());
        engine.clear();
        var beacon=new FakeWorld() {
            @Override public int cell(int x,int y,int z) { reads++;return y<0?AnalyzerEngine.BASE:AnalyzerEngine.BEACON; }
        };
        var settings=AnalyzerOptions.defaults().with(1,0,1).with(1,1,1);
        engine.tick(world,beacon,EYE,2,settings);
        assertTrue(engine.work().cells()>=495,"four-layer candidates were not exercised");
        assertTrue(engine.work().cells()<=512);
        for(int tick=0;tick<30;tick++) engine.tick(world,beacon,EYE,2,settings);
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
        var settings=AnalyzerOptions.defaults().with(1,0,1).with(1,1,1);
        for(int tick=0;tick<20;tick++) engine.tick(world,view,EYE,2,settings);
        assertEquals(1,engine.markers().size());
        assertEquals(20,engine.markers().getFirst().radius());
    }

    @Test void unchangedEnabledMaskStillClearsOnWorldAndAnchorChanges() {
        var view=new FakeWorld();view.value=AnalyzerEngine.ROD;
        for(int tick=0;tick<80;tick++) engine.tick(world,view,EYE,4,AnalyzerOptions.defaults());
        assertFalse(engine.markers().isEmpty());
        var nextWorld=new Object();
        engine.tick(nextWorld,view,EYE,4,AnalyzerOptions.defaults());
        assertTrue(engine.markers().isEmpty(),"same-mask world retained an old completed pass");
        for(int tick=0;tick<80;tick++) engine.tick(nextWorld,view,EYE,4,AnalyzerOptions.defaults());
        engine.tick(nextWorld,view,new AnalyzerEngine.Point(1.5,1.5,.5),4,AnalyzerOptions.defaults());
        assertTrue(engine.markers().isEmpty(),"moved anchor retained an old pass");
    }

    @Test void changedLimitsInvalidateBothRenderSnapshotAndScanCursor() {
        var view=new FakeWorld();view.value=AnalyzerEngine.ROD;
        for(int tick=0;tick<80;tick++) engine.tick(world,view,EYE,4,AnalyzerOptions.defaults());
        var changed=AnalyzerOptions.defaults().with(2,0,1).with(2,1,1).with(2,2,1);
        assertTrue(engine.markers(4,changed).isEmpty(),"old radius visible before next tick");
        engine.tick(world,view,EYE,4,changed);
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
        engine.tick(world,view,EYE,8,AnalyzerOptions.defaults());
        assertEquals(1,engine.markers().size());
        assertEquals(1,engine.work().cells());
        assertEquals(125,engine.work().entitySections());
        assertEquals(64,engine.work().entities());
        engine.clear();view.value=AnalyzerEngine.UNKNOWN;
        engine.tick(world,view,EYE,8,AnalyzerOptions.defaults());
        assertTrue(engine.markers().isEmpty());
        var unbounded=new FakeWorld() {
            @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye,int h,int v,int s,int e,int c) {
                return new AnalyzerEngine.VillageSample(List.of(),126,65);
            }
        };
        engine.clear();
        assertThrows(IllegalStateException.class,()->engine.tick(world,unbounded,EYE,8,AnalyzerOptions.defaults()));
    }

    @Test void nearestOrderingUsesCapturedPassReferenceWithinSameAnchor() {
        var view=new FakeWorld() {
            @Override public int cell(int x,int y,int z) {
                reads++;return (x==-1 || x==1) && y==-7 && z==-16?AnalyzerEngine.ROD:AnalyzerEngine.OTHER;
            }
        };
        var options=AnalyzerOptions.defaults().with(2,2,1).with(2,3,100);
        engine.tick(world,view,new AnalyzerEngine.Point(.1,1.5,.5),4,options);
        for(int tick=0;tick<45;tick++) engine.tick(world,view,new AnalyzerEngine.Point(.9,1.5,.5),4,options);
        assertEquals(1,engine.markers().size());
        assertEquals(-.5,engine.markers().getFirst().from().x());
    }
    @Test void allFourAnalyzersMakeProgressWithinTheSharedBudget() {
        var view=new FakeWorld() {
            @Override public int cell(int x,int y,int z) {
                reads++;
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
        var options=AnalyzerOptions.defaults().with(0,0,1).with(0,1,1).with(0,3,100);
        for(int kind=1;kind<4;kind++) options=options.with(kind,0,1).with(kind,1,1).with(kind,3,100);
        int observed=0;
        for(int tick=0;tick<200;tick++) {
            engine.tick(world,view,EYE,15,options);
            assertTrue(engine.work().cells()<=512);
            assertTrue(engine.work().sections()<=64);
            assertTrue(engine.work().cursorSteps()<=4096);
            for(var marker:engine.markers()) observed|=1<<marker.kind();
        }
        assertEquals(15,observed,"an enabled analyzer was starved");
        assertEquals(4,engine.markers().stream().map(AnalyzerEngine.Marker::kind).distinct().count());
        assertThrows(UnsupportedOperationException.class,()->engine.markers().clear());
    }
    @Test void explicitLifecycleClearResetsReportedWorkWithoutFurtherWorldQueries() {
        var view=new FakeWorld();
        engine.tick(world,view,EYE,4,AnalyzerOptions.defaults());
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
        var options=AnalyzerOptions.defaults().with(2,2,1).with(2,3,100);
        for(int i=0;i<50;i++) engine.tick(world,view,new AnalyzerEngine.Point(.1,1.5,.5),4,options);
        assertEquals(-.5,engine.markers().getFirst().from().x());
        for(int i=0;i<150;i++) engine.tick(world,view,new AnalyzerEngine.Point(.9,1.5,.5),4,options);
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
        engine.tick(world,view,EYE,2,AnalyzerOptions.defaults());
        view.base=false;
        for(int i=0;i<80;i++) {
            engine.tick(world,view,EYE,2,AnalyzerOptions.defaults());
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
        var options=AnalyzerOptions.defaults().with(0,0,8).with(0,1,5);
        engine.tick(world,view,EYE,1,options);
        view.enclosed=true;
        for(int i=0;i<30;i++) {
            engine.tick(world,view,EYE,1,options);
            assertTrue(engine.markers().stream().noneMatch(m->m.to().equals(new AnalyzerEngine.Point(-7.5,-3.5,-7.5))),
                    "enclosed source produced a newly published boundary marker");
            assertTrue(engine.work().cells()<=512);
        }
    }

    private static AnalyzerOptions small(int kind) {
        return AnalyzerOptions.defaults().with(kind,0,1).with(kind,1,1).with(kind,3,5);
    }
    private static long count(List<AnalyzerEngine.Marker> markers,int kind) {
        return markers.stream().filter(marker->marker.kind()==kind).count();
    }

    @Test void renderSnapshotMatchesOnlyItsOwnMaskAndOptions() {
        var view=new FakeWorld();view.value=AnalyzerEngine.ROD;
        engine.tick(world,view,EYE,4,small(2));
        assertFalse(engine.markers().isEmpty());
        assertSame(engine.markers(),engine.markers(4,small(2)));
        assertTrue(engine.markers(5,small(2)).isEmpty());
        assertTrue(engine.markers(4,AnalyzerOptions.defaults()).isEmpty());
    }

    @Test void aRodScanSpendsExactlyTheCellBudgetAtOneCellPerStep() {
        var view=new FakeWorld();
        engine.tick(world,view,EYE,4,AnalyzerOptions.defaults());
        assertEquals(new AnalyzerEngine.Work(512,0,512,0,0),engine.work());
        assertEquals(512,view.reads);
    }

    @Test void switchingAnalyzersDropsTheOldScanAndSamplesVillagesOnlyWhenEnabled() {
        var view=new FakeWorld() {
            @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye,int h,int v,int s,int e,int c) {
                visits++;
                return new AnalyzerEngine.VillageSample(List.of(new AnalyzerEngine.Villager(EYE,EYE)),1,1);
            }
        };
        view.value=AnalyzerEngine.ROD;
        for(int tick=0;tick<5;tick++) engine.tick(world,view,EYE,4,AnalyzerOptions.defaults());
        assertEquals(0,view.visits,"village sampling ran while disabled");
        for(int tick=0;tick<60;tick++) {
            engine.tick(world,view,EYE,1,AnalyzerOptions.defaults());
            assertEquals(0,count(engine.markers(),2),"a disabled analyzer's scan kept publishing");
        }
    }

    @Test void villageSamplesFollowTheIntervalAndReplaceOnlyVillageMarkers() {
        var view=new FakeWorld() {
            @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye,int h,int v,int s,int e,int c) {
                visits++;
                var villagers=new java.util.ArrayList<AnalyzerEngine.Villager>();
                // Exactly as many candidates as requested is within the adapter bound.
                for(int i=0;i<c;i++) villagers.add(new AnalyzerEngine.Villager(EYE,new AnalyzerEngine.Point(.5,1.5,-.5)));
                return new AnalyzerEngine.VillageSample(villagers,1,c);
            }
        };
        view.value=AnalyzerEngine.ROD;
        // Rod passes repeat every 20 ticks, so a village sample cannot hide behind a rod republication.
        var options=small(2).with(2,3,20).with(3,3,5);
        for(int tick=1;tick<=11;tick++) {
            engine.tick(world,view,EYE,12,options);
            assertEquals(tick<6?1:tick<11?2:3,view.visits,"village samples at ticks 1, 6 and 11");
            assertEquals(8,count(engine.markers(),2),"a village sample dropped rod markers");
            assertEquals(12,count(engine.markers(),3),"village markers were not replaced");
        }
    }

    @Test void villageSitesOutsideTheAnchoredBoxAreDropped() {
        var eye=new AnalyzerEngine.Point(100.5,70.5,-50.5);
        var inside=new AnalyzerEngine.Point(102.5,71.5,-48.5);
        var view=new FakeWorld() {
            @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point at,int h,int v,int s,int e,int c) {
                return new AnalyzerEngine.VillageSample(List.of(
                        new AnalyzerEngine.Villager(at,inside),
                        new AnalyzerEngine.Villager(at,new AnalyzerEngine.Point(103.5,70.5,-50.5)),
                        new AnalyzerEngine.Villager(at,new AnalyzerEngine.Point(100.5,72.5,-50.5)),
                        new AnalyzerEngine.Villager(at,new AnalyzerEngine.Point(100.5,70.5,-53.5))),1,4);
            }
        };
        engine.tick(world,view,eye,8,AnalyzerOptions.defaults().with(3,0,2).with(3,1,1));
        assertEquals(List.of(new AnalyzerEngine.Marker(3,eye,inside,0)),engine.markers());
    }

    @Test void everyCandidateOfAPassIsPublishedOnceAndPassesRepeatAtTheInterval() {
        var view=new FakeWorld() {
            @Override public int cell(int x,int y,int z) {
                reads++;
                return y==1 && (x==1 && z==0 || x==-1 && z==0 || x==0 && z==1) ? AnalyzerEngine.ROD : AnalyzerEngine.OTHER;
            }
        };
        int[] reads={30,33,36,39,42,75};
        for(int tick=0;tick<6;tick++) {
            engine.tick(world,view,EYE,4,small(2));
            assertEquals(reads[tick],view.reads,"tick "+(tick+1));
            assertEquals(3,engine.markers().size());
            assertTrue(engine.markers().stream().allMatch(marker->marker.radius()==128));
        }
        assertEquals(java.util.Set.of(new AnalyzerEngine.Point(1.5,1.5,.5),new AnalyzerEngine.Point(-.5,1.5,.5),
                new AnalyzerEngine.Point(.5,1.5,1.5)),
                engine.markers().stream().map(AnalyzerEngine.Marker::to).collect(java.util.stream.Collectors.toSet()));
    }

    @Test void aScanCoversItsWholeBoxAndNothingBeyondIt() {
        var view=new FakeWorld() {
            @Override public int cell(int x,int y,int z) {
                reads++;
                // The far corner of the 3 x 3 x 3 box, and the first cell past its last row.
                return x==1 && y==2 && z==1 || x==-1 && y==0 && z==2 ? AnalyzerEngine.ROD : AnalyzerEngine.OTHER;
            }
        };
        engine.tick(world,view,EYE,4,small(2));
        assertEquals(List.of(new AnalyzerEngine.Point(1.5,2.5,1.5)),
                engine.markers().stream().map(AnalyzerEngine.Marker::to).toList());
        assertEquals(28,view.reads);
    }

    @Test void lavaBoundaryReadsEachOfTheSixNeighbors() {
        int[][] directions={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for(int[] d:directions) {
            var view=new FakeWorld() {
                @Override public int cell(int x,int y,int z) {
                    reads++;
                    if(x==0 && y==1 && z==0) return AnalyzerEngine.LAVA;
                    return x==d[0] && y==1+d[1] && z==d[2] ? AnalyzerEngine.OTHER : AnalyzerEngine.UNKNOWN;
                }
            };
            engine.clear();
            engine.tick(world,view,EYE,1,small(0));
            assertEquals(1,engine.markers().size(),java.util.Arrays.toString(d));
        }
    }

    @Test void publicationMayUseTheLastCellOfTheBudget() {
        // A 13 x 3 x 13 rod box is 507 reads; the village endpoints bring the scan's completion to
        // 512 - candidates, so the last candidate fits exactly into the budget.
        for(int[] scenario:new int[][]{{4,1},{3,2}}) {
            int candidates=scenario[0],rods=scenario[1];
            var view=new FakeWorld() {
                @Override public int cell(int x,int y,int z) {
                    reads++;
                    return y==1 && z==0 && (x==0 || rods==2 && x==1) ? AnalyzerEngine.ROD : AnalyzerEngine.OTHER;
                }
                @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye,int h,int v,int s,int e,int c) {
                    return new AnalyzerEngine.VillageSample(java.util.Collections.nCopies(c,
                            new AnalyzerEngine.Villager(EYE,new AnalyzerEngine.Point(.5,1.5,-.5))),1,c);
                }
            };
            var options=AnalyzerOptions.defaults().with(2,0,6).with(2,1,1).with(3,2,candidates);
            engine.clear();
            engine.tick(world,view,EYE,12,options);
            assertEquals(512,engine.work().cells(),"candidates "+candidates);
            assertEquals(rods,count(engine.markers(),2),"candidates "+candidates);
        }
    }
}
