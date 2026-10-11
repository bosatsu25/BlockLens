package dev.blocklens.core.analyzer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Tick-thread bounded inspection; immutable snapshots are the only render-thread input. */
public final class AnalyzerEngine {
    public static final int UNKNOWN = -1, OTHER = 0, LAVA = 2, BEACON = 3, ROD = 4, BASE = 5;
    public static final int HIDDEN = 256;
    public static final int CELL_LIMIT = 512, STEP_LIMIT = 4096;
    public record Point(double x, double y, double z) {
        public Point {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("non-finite analyzer position");
            }
        }
        public double distanceSquared(Point other) {
            double dx=x-other.x, dy=y-other.y, dz=z-other.z;
            return dx*dx+dy*dy+dz*dz;
        }
    }
    public record Marker(int kind, Point from, Point to, int radius) { }
    public record Villager(Point from, Point site) { }
    public record VillageSample(List<Villager> villagers, int sections, int entities) {
        public VillageSample { villagers = List.copyOf(villagers); }
    }
    public record Work(int cells, int sections, int cursorSteps, int entitySections, int entities) { }
    public interface WorldView {
        /** Physical kind, optionally HIDDEN for presentation; UNKNOWN remains unmodified. */
        int cell(int x, int y, int z);
        VillageSample villagers(Point eye, int horizontal, int vertical, int sectionLimit, int entityLimit, int candidates);
    }
    private final Scan[] scans = new Scan[3];
    private record Snapshot(List<Marker> markers,int mask,AnalyzerOptions options) { }
    private static final Snapshot EMPTY_SNAPSHOT = new Snapshot(List.of(),0,null);
    private volatile Snapshot published = EMPTY_SNAPSHOT;
    private Object identity;
    private AnalyzerOptions options;
    private int enabled, tick, turn, cells, sections, steps, entitySections, entities, nextVillage;
    private int anchorX, anchorY, anchorZ;

    public List<Marker> markers() { return published.markers; }
    public List<Marker> markers(int mask,AnalyzerOptions settings) {
        Snapshot current=published;
        return current.mask==mask && settings.equals(current.options)?current.markers:List.of();
    }
    public Work work() { return new Work(cells, sections, steps, entitySections, entities); }
    public void clear() {
        identity = null; options = null; enabled = 0; published = EMPTY_SNAPSHOT;
        cells=sections=steps=entitySections=entities=0;
        java.util.Arrays.fill(scans, null); nextVillage=0; turn=0; tick=0;
    }
    public void tick(Object worldIdentity, WorldView view, Point eye, int mask, AnalyzerOptions settings) {
        cells=sections=steps=entitySections=entities=0;
        if (worldIdentity == null || view == null || eye == null || mask == 0) { clear(); return; }
        Objects.requireNonNull(settings, "settings");
        int x=(int)Math.floor(eye.x), y=(int)Math.floor(eye.y), z=(int)Math.floor(eye.z);
        if (identity != worldIdentity || enabled != mask || !settings.equals(options)
                || anchorX != x || anchorY != y || anchorZ != z) {
            clear(); identity=worldIdentity; enabled=mask; options=settings;
            anchorX=x; anchorY=y; anchorZ=z;
            for(int kind=0;kind<3;kind++) if ((mask & (1<<kind))!=0) scans[kind]=new Scan(kind,x,y,z,settings);
        }
        tick++;
        ArrayList<Marker> retained = new ArrayList<>();
        for (Marker marker : published.markers) {
            if ((mask & (1<<marker.kind)) == 0 || !inside(marker.kind, marker.to, eye)) continue;
            int value=read(view, marker.to);
            if (visible(value) && (marker.kind==3 || physical(value)==target(marker.kind))) retained.add(marker);
        }
        if ((mask & 8)!=0 && tick>=nextVillage && CELL_LIMIT-cells>=settings.value(3,2)) {
            VillageSample sample=view.villagers(eye,settings.value(3,0),settings.value(3,1),125,64,settings.value(3,2));
            if(sample.sections<0 || sample.sections>125 || sample.entities<0 || sample.entities>64
                    || sample.villagers.size()>settings.value(3,2)) throw new IllegalStateException("unbounded village adapter");
            entitySections=sample.sections; entities=sample.entities;
            retained.removeIf(marker -> marker.kind==3);
            for(Villager villager:sample.villagers) {
                if(villager.site!=null && inside(3,villager.site,eye) && visible(read(view,villager.site))) {
                    retained.add(new Marker(3,villager.from,villager.site,0));
                }
            }
            nextVillage=tick+settings.value(3,3);
        }
        int stalled=0;
        while(steps<STEP_LIMIT && stalled<3) {
            Scan scan=scans[turn];
            turn=(turn+1)%3;
            if(scan==null || !scan.step(view,eye)) { stalled++; continue; }
            stalled=0;
            if(scan.completed && CELL_LIMIT-cells>=scan.reservation()) {
                if(!scan.publicationStarted) {
                    retained.removeIf(marker -> marker.kind==scan.kind);
                    scan.publicationStarted=true;
                }
                if(scan.publishIndex<scan.found.size()) {
                    Point position=scan.found.get(scan.publishIndex++).to;
                    if(inside(scan.kind,position,eye)) {
                        Marker fresh=describe(view,scan.kind,position);
                        if(fresh!=null) retained.add(fresh);
                    }
                }
                if(scan.publishIndex==scan.found.size()) {
                    scan.found.clear();scan.restart(tick+settings.value(scan.kind,3));
                }
            }
        }
        published=new Snapshot(List.copyOf(retained),mask,settings);
    }
    private Marker describe(WorldView view,int kind,Point position) {
        int x=(int)Math.floor(position.x),y=(int)Math.floor(position.y),z=(int)Math.floor(position.z);
        int value=read(view,x,y,z);
        if(!visible(value) || physical(value)!=target(kind)) return null;
        int radius=0;
        if(kind==0) {
            int[] neighbors={read(view,x+1,y,z),read(view,x-1,y,z),read(view,x,y+1,z),
                    read(view,x,y-1,z),read(view,x,y,z+1),read(view,x,y,z-1)};
            if(!lavaBoundary(neighbors)) return null;
        } else if(kind==1) {
            int level=0;
            layers: for(int layer=1;layer<=4;layer++) {
                for(int dx=-layer;dx<=layer;dx++) for(int dz=-layer;dz<=layer;dz++) {
                    if(physical(read(view,x+dx,y-layer,z+dz))!=BASE) break layers;
                }
                level=layer;
            }
            if(level==0) return null;
            radius=10+level*10;
        } else if(kind==2) radius=128;
        return new Marker(kind,position,position,radius);
    }
    private int read(WorldView view, Point point) {
        if(cells>=CELL_LIMIT) throw new IllegalStateException("cell budget exceeded");
        cells++;
        return view.cell((int)Math.floor(point.x),(int)Math.floor(point.y),(int)Math.floor(point.z));
    }
    private int read(WorldView view, int x,int y,int z) { return read(view,new Point(x+.5,y+.5,z+.5)); }
    private boolean inside(int kind, Point point, Point eye) {
        int h=options.value(kind,0),v=options.value(kind,1);
        return Math.abs(Math.floor(point.x)-anchorX)<=h && Math.abs(Math.floor(point.y)-anchorY)<=v
                && Math.abs(Math.floor(point.z)-anchorZ)<=h;
    }
    private static int target(int kind) {
        return switch(kind) { case 0 -> LAVA; case 1 -> BEACON; case 2 -> ROD;
            default -> throw new IllegalArgumentException("not a block analyzer"); };
    }
    public static boolean lavaBoundary(int[] neighbors) {
        for(int neighbor:neighbors) if(physical(neighbor)!=UNKNOWN && physical(neighbor)!=LAVA) return true;
        return false;
    }
    private static int physical(int value) { return value==UNKNOWN?UNKNOWN:value & ~HIDDEN; }
    private static boolean visible(int value) { return value!=UNKNOWN && (value & HIDDEN)==0; }
    private final class Scan {
        final int kind,x0,y0,z0,h,v,nx,ny,nz,minX,minY,minZ;
        final ArrayList<Marker> found=new ArrayList<>();
        long index;
        int ready,publishIndex;
        boolean completed,publicationStarted;
        Point referenceEye;
        Scan(int kind,int x,int y,int z,AnalyzerOptions settings) {
            this.kind=kind; x0=x;y0=y;z0=z;h=settings.value(kind,0);v=settings.value(kind,1);
            minX=x-h;minY=y-v;minZ=z-h;
            nx=2*h+1;ny=2*v+1;nz=2*h+1;
        }
        void restart(int next) { index=0;ready=next;completed=false;referenceEye=null;publishIndex=0;publicationStarted=false; }
        boolean step(WorldView view,Point eye) {
            int reserve=reservation();
            if(completed) return CELL_LIMIT-cells>=reserve;
            if(tick<ready || CELL_LIMIT-cells<reserve) return false;
            if(referenceEye==null) referenceEye=eye;
            int x=minX+(int)(index%nx),y=minY+(int)((index/nx)%ny),z=minZ+(int)(index/(nx*(long)ny));
            if(steps>=STEP_LIMIT) return false;
            steps++;
            advance();
            Point position=new Point(x+.5,y+.5,z+.5);
            if(!inside(kind,position,referenceEye)) return true;
            Marker candidate=describe(view,kind,position);
            if(candidate==null) return true;
            found.add(candidate);
            found.sort(Comparator.comparingDouble(marker -> marker.from.distanceSquared(referenceEye)));
            if(found.size()>options.value(kind,2)) found.removeLast();
            return true;
        }
        int reservation() { return kind==1?165:kind==0?7:1; }
        void advance() { if(++index>=nx*(long)ny*nz) completed=true; }
    }
}
