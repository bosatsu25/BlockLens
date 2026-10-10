package dev.blocklens.core.analyzer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Tick-thread bounded inspection; immutable snapshots are the only render-thread input. */
public final class AnalyzerEngine {
    public static final int UNKNOWN = -1, OTHER = 0, DEBRIS = 1, LAVA = 2, BEACON = 3, ROD = 4, BASE = 5;
    public static final int CELL_LIMIT = 512, SECTION_LIMIT = 64, STEP_LIMIT = 4096;
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
        int cell(int x, int y, int z);
        boolean debrisSection(int sectionX, int sectionY, int sectionZ);
        VillageSample villagers(Point eye, int horizontal, int vertical, int sectionLimit, int entityLimit, int candidates);
    }
    private final Scan[] scans = new Scan[4];
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
            for(int kind=0;kind<4;kind++) if ((mask & (1<<kind))!=0) scans[kind]=new Scan(kind,x,y,z,settings);
        }
        tick++;
        ArrayList<Marker> retained = new ArrayList<>();
        for (Marker marker : published.markers) {
            if ((mask & (1<<marker.kind)) == 0 || !inside(marker.kind, marker.to, eye)) continue;
            int value=read(view, marker.to);
            if (value != UNKNOWN && (marker.kind==4 || value==target(marker.kind))) retained.add(marker);
        }
        if ((mask & 16)!=0 && tick>=nextVillage && CELL_LIMIT-cells>=settings.value(4,2)) {
            VillageSample sample=view.villagers(eye,settings.value(4,0),settings.value(4,1),125,64,settings.value(4,2));
            if(sample.sections<0 || sample.sections>125 || sample.entities<0 || sample.entities>64
                    || sample.villagers.size()>settings.value(4,2)) throw new IllegalStateException("unbounded village adapter");
            entitySections=sample.sections; entities=sample.entities;
            retained.removeIf(marker -> marker.kind==4);
            for(Villager villager:sample.villagers) {
                if(villager.site!=null && inside(4,villager.site,eye) && read(view,villager.site)!=UNKNOWN) {
                    retained.add(new Marker(4,villager.from,villager.site,0));
                }
            }
            nextVillage=tick+settings.value(4,3);
        }
        int stalled=0;
        while(steps<STEP_LIMIT && stalled<4) {
            Scan scan=scans[turn];
            turn=(turn+1)%4;
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
        if(read(view,x,y,z)!=target(kind)) return null;
        int radius=0;
        if(kind==1) {
            int[] neighbors={read(view,x+1,y,z),read(view,x-1,y,z),read(view,x,y+1,z),
                    read(view,x,y-1,z),read(view,x,y,z+1),read(view,x,y,z-1)};
            if(!lavaBoundary(neighbors)) return null;
        } else if(kind==2) {
            int level=0;
            layers: for(int layer=1;layer<=4;layer++) {
                for(int dx=-layer;dx<=layer;dx++) for(int dz=-layer;dz<=layer;dz++) {
                    if(read(view,x+dx,y-layer,z+dz)!=BASE) break layers;
                }
                level=layer;
            }
            if(level==0) return null;
            radius=10+level*10;
        } else if(kind==3) radius=128;
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
        return kind==0 ? point.distanceSquared(eye)<=h*(double)h
                : Math.abs(Math.floor(point.x)-anchorX)<=h && Math.abs(Math.floor(point.y)-anchorY)<=v
                && Math.abs(Math.floor(point.z)-anchorZ)<=h;
    }
    private static int target(int kind) {
        return switch(kind) { case 0 -> DEBRIS; case 1 -> LAVA; case 2 -> BEACON; case 3 -> ROD;
            default -> throw new IllegalArgumentException("not a block analyzer"); };
    }
    public static boolean lavaBoundary(int[] neighbors) {
        for(int neighbor:neighbors) if(neighbor!=UNKNOWN && neighbor!=LAVA) return true;
        return false;
    }
    private final class Scan {
        final int kind,x0,y0,z0,h,v,nx,ny,nz,minX,minY,minZ;
        final ArrayList<Marker> found=new ArrayList<>();
        long index;
        int local=-1,ready,publishIndex;
        boolean completed,publicationStarted;
        Point referenceEye;
        Scan(int kind,int x,int y,int z,AnalyzerOptions settings) {
            this.kind=kind; x0=x;y0=y;z0=z;h=settings.value(kind,0);v=settings.value(kind,1);
            minX=kind==0?Math.floorDiv(x-h,16):x-h;
            minY=kind==0?Math.floorDiv(y-v,16):y-v;
            minZ=kind==0?Math.floorDiv(z-h,16):z-h;
            nx=kind==0?Math.floorDiv(x+h,16)-minX+1:2*h+1;
            ny=kind==0?Math.floorDiv(y+v,16)-minY+1:2*v+1;
            nz=kind==0?Math.floorDiv(z+h,16)-minZ+1:2*h+1;
        }
        void restart(int next) { index=0;local=-1;ready=next;completed=false;referenceEye=null;publishIndex=0;publicationStarted=false; }
        boolean step(WorldView view,Point eye) {
            int reserve=reservation();
            if(completed) return CELL_LIMIT-cells>=reserve;
            if(tick<ready || CELL_LIMIT-cells<reserve) return false;
            if(referenceEye==null) referenceEye=eye;
            int x=minX+(int)(index%nx),y=minY+(int)((index/nx)%ny),z=minZ+(int)(index/(nx*(long)ny));
            if(kind==0 && local<0) {
                if(sections>=SECTION_LIMIT) return false;
                sections++;steps++;
                if(!view.debrisSection(x,y,z)) { advance(); return true; }
                local=0;
            }
            if(steps>=STEP_LIMIT) return false;
            steps++;
            if(kind==0) {
                x=x*16+(local&15); y=y*16+((local>>8)&15); z=z*16+((local>>4)&15);
                local++; if(local==4096) { local=-1;advance(); }
            } else advance();
            Point position=new Point(x+.5,y+.5,z+.5);
            if(!inside(kind,position,referenceEye)) return true;
            Marker candidate=describe(view,kind,position);
            if(candidate==null) return true;
            found.add(candidate);
            found.sort(Comparator.comparingDouble(marker -> marker.from.distanceSquared(referenceEye)));
            if(found.size()>options.value(kind,2)) found.removeLast();
            return true;
        }
        int reservation() { return kind==2?165:kind==1?7:1; }
        void advance() { if(++index>=nx*(long)ny*nz) completed=true; }
    }
}
