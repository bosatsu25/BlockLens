package dev.blocklens.core.render;

import java.util.Map;
import static dev.blocklens.core.render.CompactModelTransforms.Context.*;

/** Static functional display profiles; no source model geometry or runtime resource parsing. */
public final class CompactModelTransforms {
    public enum Context { THIRD_LEFT, THIRD_RIGHT, FIRST_LEFT, FIRST_RIGHT, HEAD, GUI, GROUND, FIXED, SHELF }
    private enum Group { BLOCKS, ITEMS, TOOLS }
    public record Transform(float rx, float ry, float rz, float tx, float ty, float tz,
                            float sx, float sy, float sz) { }
    private record Profile(Group group, Map<Context, Transform> transforms) { }

    private static final Transform P0 = new Transform(30.0f, 225.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.625f, 0.625f, 0.625f);
    private static final Transform P1 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.1875f, 0.0f, 0.25f, 0.25f, 0.25f);
    private static final Transform P2 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P3 = new Transform(0.0f, 180.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
    private static final Transform P4 = new Transform(75.0f, 45.0f, 0.0f, 0.0f, 0.15625f, 0.0f, 0.375f, 0.375f, 0.375f);
    private static final Transform P5 = new Transform(0.0f, 45.0f, 0.0f, 0.0f, 0.01875f, 0.0f, 0.28f, 0.28f, 0.28f);
    private static final Transform P6 = new Transform(0.0f, 225.0f, 0.0f, 0.0f, 0.01875f, 0.0f, 0.28f, 0.28f, 0.28f);
    private static final Transform P7 = new Transform(30.0f, 225.0f, 0.0f, 0.0f, 0.1875f, 0.0f, 1.0f, 1.0f, 1.0f);
    private static final Transform P8 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.1875f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P9 = new Transform(0.0f, 180.0f, 0.0f, 0.0f, 0.25f, 0.0f, 1.0f, 1.0f, 1.0f);
    private static final Transform P10 = new Transform(45.0f, 45.0f, 0.0f, 0.0f, 0.1875f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P11 = new Transform(0.0f, 45.0f, 0.0f, 0.0f, 0.15625f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P12 = new Transform(0.0f, 225.0f, 0.0f, 0.0f, 0.15625f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P13 = new Transform(0.0f, 135.0f, 0.0f, 0.0f, 0.0625f, 0.0f, 0.28f, 0.28f, 0.28f);
    private static final Transform P14 = new Transform(0.0f, 135.0f, 0.0f, 0.0f, 0.05625f, 0.0f, 0.29f, 0.29f, 0.29f);
    private static final Transform P15 = new Transform(0.0f, 135.0f, 0.0f, 0.0f, 0.01875f, 0.0f, 0.28f, 0.28f, 0.28f);
    private static final Transform P16 = new Transform(30.0f, 225.0f, 0.0f, 0.15625f, -0.09375f, 0.0f, 0.625f, 0.625f, 0.625f);
    private static final Transform P17 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.25f, 0.5f, 0.5f, 0.5f);
    private static final Transform P18 = new Transform(0.0f, 180.0f, 0.0f, 0.0f, 0.0f, 0.25f, 1.0f, 1.0f, 1.0f);
    private static final Transform P19 = new Transform(0.0f, 135.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.3f, 0.3f, 0.3f);
    private static final Transform P20 = new Transform(75.0f, 135.0f, 0.0f, -0.1875f, 0.0625f, 0.0f, 0.4f, 0.4f, 0.4f);
    private static final Transform P21 = new Transform(75.0f, 45.0f, 0.0f, 0.0f, 0.15625f, 0.125f, 0.375f, 0.375f, 0.375f);
    private static final Transform P22 = new Transform(0.0f, 45.0f, 0.0f, -0.0625f, 0.2625f, 0.0625f, 0.28f, 0.28f, 0.28f);
    private static final Transform P23 = new Transform(0.0f, 225.0f, 0.0f, -0.0625f, 0.2625f, 0.0625f, 0.28f, 0.28f, 0.28f);
    private static final Transform P24 = new Transform(0.0f, -90.0f, 25.0f, 0.070625f, 0.225f, 0.0f, 0.4f, 0.4f, 0.4f);
    private static final Transform P25 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.25f, 0.0625f, 0.55f, 0.55f, 0.55f);
    private static final Transform P26 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.875f, -0.3125f, 1.0f, 1.0f, 1.0f);
    private static final Transform P27 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.125f, 0.0f, 1.0f, 1.0f, 1.0f);
    private static final Transform P28 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.125f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P29 = new Transform(0.0f, 180.0f, 0.0f, 0.0f, 0.8125f, 0.4375f, 1.0f, 1.0f, 1.0f);
    private static final Transform P30 = new Transform(0.0f, -90.0f, 55.0f, 0.0f, 0.25f, 0.03125f, 0.85f, 0.85f, 0.85f);
    private static final Transform P31 = new Transform(0.0f, -90.0f, 25.0f, 0.070625f, 0.175f, 0.070625f, 0.4f, 0.4f, 0.4f);
    private static final Transform P32 = new Transform(0.0f, 90.0f, -25.0f, 0.070625f, 0.175f, 0.070625f, 0.4f, 0.4f, 0.4f);
    private static final Transform P33 = new Transform(0.0f, 90.0f, -55.0f, 0.0f, 0.25f, 0.03125f, 0.85f, 0.85f, 0.85f);
    private static final Transform P34 = new Transform(30.0f, 225.0f, 0.0f, 0.0f, -0.125f, 0.0f, 0.625f, 0.625f, 0.625f);
    private static final Transform P35 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.0625f, 0.5f, 0.5f, 0.5f);
    private static final Transform P36 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0625f, 0.0f, 0.55f, 0.55f, 0.55f);
    private static final Transform P37 = new Transform(0.0f, 0.0f, 0.0f, 0.070625f, 0.0f, 0.070625f, 0.4f, 0.4f, 0.4f);
    private static final Transform P38 = new Transform(0.0f, 0.0f, -45.0f, 0.0f, -0.28125f, -0.40625f, 1.0f, 1.0f, 1.0f);
    private static final Transform P39 = new Transform(-80.0f, 260.0f, -40.0f, -0.0625f, -0.125f, 0.15625f, 0.9f, 0.9f, 0.9f);
    private static final Transform P40 = new Transform(-80.0f, -280.0f, 40.0f, -0.0625f, -0.125f, 0.15625f, 0.9f, 0.9f, 0.9f);
    private static final Transform P41 = new Transform(0.0f, -90.0f, 25.0f, 0.125f, 0.2f, 0.09375f, 0.4f, 0.4f, 0.4f);
    private static final Transform P42 = new Transform(0.0f, 90.0f, -25.0f, 0.125f, 0.2f, 0.09375f, 0.4f, 0.4f, 0.4f);
    private static final Transform P43 = new Transform(30.0f, 45.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
    private static final Transform P44 = new Transform(75.0f, 315.0f, 0.0f, 0.0f, 0.15625f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P45 = new Transform(0.0f, 315.0f, 0.0f, 0.0f, 0.0625f, 0.0f, 0.52f, 0.52f, 0.52f);
    private static final Transform P46 = new Transform(-90.0f, 0.0f, -60.0f, 0.125f, 0.00625f, -0.1875f, 0.9f, 0.9f, 0.9f);
    private static final Transform P47 = new Transform(-90.0f, 0.0f, 30.0f, 0.125f, 0.00625f, -0.1875f, 0.9f, 0.9f, 0.9f);
    private static final Transform P48 = new Transform(-90.0f, 0.0f, -55.0f, 0.070625f, 0.2f, 0.070625f, 0.4f, 0.4f, 0.4f);
    private static final Transform P49 = new Transform(-90.0f, 0.0f, 35.0f, 0.070625f, 0.2f, 0.070625f, 0.4f, 0.4f, 0.4f);
    private static final Transform P50 = new Transform(0.0f, 90.0f, 0.0f, 0.0f, 0.125f, 0.03125f, 0.375f, 0.375f, 0.375f);
    private static final Transform P51 = new Transform(0.0f, 90.0f, 0.0f, 0.0f, 0.03125f, 0.0f, 0.27f, 0.27f, 0.27f);
    private static final Transform P52 = new Transform(30.0f, 45.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.6f, 0.6f, 0.6f);
    private static final Transform P53 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0625f, 0.0f, 0.25f, 0.25f, 0.25f);
    private static final Transform P54 = new Transform(0.0f, 180.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.5f, 1.5f, 1.5f);
    private static final Transform P55 = new Transform(0.0f, 180.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P56 = new Transform(30.0f, 45.0f, 0.0f, -0.125f, 0.125f, 0.0f, 0.6f, 0.6f, 0.6f);
    private static final Transform P57 = new Transform(0.0f, 0.0f, 0.0f, 0.125f, 0.1875f, -0.375f, 0.35f, 0.35f, 0.35f);
    private static final Transform P58 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.1875f, 0.0625f, 0.55f, 0.55f, 0.55f);
    private static final Transform P59 = new Transform(0.0f, -90.0f, 25.0f, 0.070625f, 0.15625f, 0.070625f, 0.4f, 0.4f, 0.4f);
    private static final Transform P60 = new Transform(0.0f, 180.0f, 0.0f, 0.0f, 0.1875f, 0.0625f, 0.55f, 0.55f, 0.55f);
    private static final Transform P61 = new Transform(0.0f, -90.0f, 25.0f, 0.070625f, 0.2f, 0.11875f, 0.4f, 0.4f, 0.4f);
    private static final Transform P62 = new Transform(0.0f, 90.0f, -25.0f, 0.070625f, 0.2f, 0.11875f, 0.4f, 0.4f, 0.4f);
    private static final Transform P63 = new Transform(0.0f, -90.0f, 25.0f, 0.070625f, 0.175f, 0.070625f, 0.5f, 0.5f, 0.5f);
    private static final Transform P64 = new Transform(0.0f, 90.0f, -25.0f, 0.070625f, 0.175f, 0.070625f, 0.5f, 0.5f, 0.5f);
    private static final Transform P65 = new Transform(0.0f, -90.0f, 55.0f, 0.0f, 0.25f, 0.0625f, 1.0f, 1.0f, 1.0f);
    private static final Transform P66 = new Transform(0.0f, 90.0f, -55.0f, 0.0f, 0.25f, 0.0625f, 1.0f, 1.0f, 1.0f);
    private static final Transform P67 = new Transform(0.0f, -90.0f, 25.0f, 0.0f, 0.1875f, 0.05f, 0.6f, 0.6f, 0.6f);
    private static final Transform P68 = new Transform(0.0f, 90.0f, -25.0f, 0.0f, 0.1875f, 0.05f, 0.6f, 0.6f, 0.6f);
    private static final Transform P69 = new Transform(0.0f, 90.0f, 55.0f, 0.0f, 0.25f, 0.15625f, 0.85f, 0.85f, 0.85f);
    private static final Transform P70 = new Transform(0.0f, -90.0f, -55.0f, 0.0f, 0.25f, 0.15625f, 0.85f, 0.85f, 0.85f);
    private static final Transform P71 = new Transform(0.0f, 90.0f, 25.0f, 0.0f, 0.1f, 0.05f, 0.4f, 0.4f, 0.4f);
    private static final Transform P72 = new Transform(0.0f, -90.0f, -25.0f, 0.0f, 0.1f, 0.05f, 0.4f, 0.4f, 0.4f);
    private static final Transform P73 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f, 0.55f, 0.55f, 0.55f);
    private static final Transform P74 = new Transform(0.0f, -90.0f, 25.0f, 0.070625f, 0.0625f, 0.070625f, 0.4f, 0.4f, 0.4f);
    private static final Transform P75 = new Transform(0.0f, 100.0f, 0.0f, -0.0625f, -0.0625f, 0.0f, 0.9f, 0.9f, 0.9f);
    private static final Transform P76 = new Transform(0.0f, -90.0f, 25.0f, 0.070625f, 0.125f, 0.070625f, 0.4f, 0.4f, 0.4f);
    private static final Transform P77 = new Transform(0.0f, 90.0f, 0.0f, 0.625f, 0.375f, -0.25f, 1.0f, 1.0f, 1.0f);
    private static final Transform P78 = new Transform(0.0f, 90.0f, 0.0f, 0.625f, 0.375f, 0.75f, 1.0f, 1.0f, 1.0f);
    private static final Transform P79 = new Transform(0.0f, 180.0f, 5.0f, -0.3125f, -0.103125f, -0.625f, 0.95f, 0.95f, 0.95f);
    private static final Transform P80 = new Transform(0.0f, 180.0f, 5.0f, 0.625f, -0.1875f, -0.625f, 0.95f, 0.95f, 0.95f);
    private static final Transform P81 = new Transform(15.0f, -25.0f, -5.0f, 0.125f, 0.1875f, 0.0f, 0.65f, 0.65f, 0.65f);
    private static final Transform P82 = new Transform(0.0f, 180.0f, 0.0f, -0.125f, 0.25f, -0.3125f, 0.5f, 0.5f, 0.5f);
    private static final Transform P83 = new Transform(0.0f, 0.0f, 0.0f, 0.25f, 0.25f, 0.125f, 0.25f, 0.25f, 0.25f);
    private static final Transform P84 = new Transform(45.0f, 135.0f, 0.0f, 0.219375f, 0.6875f, -0.125f, 1.0f, 1.0f, 1.0f);
    private static final Transform P85 = new Transform(45.0f, 135.0f, 0.0f, 0.844375f, 0.1875f, 0.3125f, 1.0f, 1.0f, 1.0f);
    private static final Transform P86 = new Transform(0.0f, 180.0f, -5.0f, -0.625f, -0.1875f, -0.6875f, 0.95f, 0.95f, 0.95f);
    private static final Transform P87 = new Transform(0.0f, 180.0f, -5.0f, 0.3125f, -0.103125f, -0.6875f, 0.95f, 0.95f, 0.95f);
    private static final Transform P88 = new Transform(0.0f, 45.0f, 0.0f, 0.0f, 0.2f, 0.0f, 0.28f, 0.28f, 0.28f);
    private static final Transform P89 = new Transform(-20.0f, 90.0f, -35.0f, 0.195625f, 0.125f, 0.008125f, 1.0f, 1.0f, 0.68f);
    private static final Transform P90 = new Transform(-20.0f, -90.0f, 35.0f, 0.195625f, 0.125f, 0.008125f, 1.0f, 1.0f, 0.68f);
    private static final Transform P91 = new Transform(5.0f, 270.0f, -40.0f, 0.0f, 0.125f, 0.125f, 1.7f, 1.7f, 0.85f);
    private static final Transform P92 = new Transform(5.0f, -270.0f, 40.0f, 0.0f, 0.125f, 0.125f, 1.7f, 1.7f, 0.85f);
    private static final Transform P93 = new Transform(0.0f, 90.0f, 0.0f, 0.0f, 0.125f, 0.03125f, 0.3f, 0.3f, 0.3f);
    private static final Transform P94 = new Transform(0.0f, 90.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.3f, 0.3f, 0.3f);
    private static final Transform P95 = new Transform(30.0f, 20.0f, 0.0f, 0.0f, -0.203125f, 0.0f, 0.5325f, 0.5325f, 0.5325f);
    private static final Transform P96 = new Transform(0.0f, 180.0f, 0.0f, 0.0f, 1.0f, 0.4375f, 1.5f, 1.5f, 1.5f);
    private static final Transform P97 = new Transform(30.0f, 160.0f, 0.0f, 0.0f, 0.1875f, -0.125f, 0.23f, 0.23f, 0.23f);
    private static final Transform P98 = new Transform(30.0f, 160.0f, 0.0f, 0.0f, 0.1875f, 0.0f, 0.27f, 0.27f, 0.27f);
    private static final Transform P99 = new Transform(30.0f, 160.0f, 0.0f, 0.125f, 0.1875f, 0.0f, 0.5325f, 0.5325f, 0.5325f);
    private static final Transform P100 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0625f, 0.125f, 0.25f, 0.25f, 0.25f);
    private static final Transform P101 = new Transform(0.0f, 180.0f, 0.0f, 0.0f, 0.625f, -0.5f, 1.0f, 1.0f, 1.0f);
    private static final Transform P102 = new Transform(270.0f, 0.0f, 0.0f, 0.0f, 0.25f, -0.125f, 0.5f, 0.5f, 0.5f);
    private static final Transform P103 = new Transform(30.0f, 45.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.625f, 0.625f, 0.625f);
    private static final Transform P104 = new Transform(75.0f, 315.0f, 0.0f, 0.0f, 0.15625f, 0.0f, 0.375f, 0.375f, 0.375f);
    private static final Transform P105 = new Transform(0.0f, 315.0f, 0.0f, 0.0f, 0.04375f, 0.0f, 0.28f, 0.28f, 0.28f);
    private static final Transform P106 = new Transform(30.0f, 45.0f, 180.0f, 0.0f, 0.125f, 0.0f, 0.55f, 0.55f, 0.55f);
    private static final Transform P107 = new Transform(0.0f, 0.0f, 180.0f, 0.0f, 0.0f, 0.0f, 0.35f, 0.35f, 0.35f);
    private static final Transform P108 = new Transform(0.0f, -45.0f, 180.0f, 0.0f, 0.3125f, 0.0f, 0.3f, 0.3f, 0.3f);
    private static final Transform P109 = new Transform(0.0f, -45.0f, 180.0f, 0.0f, 0.3125f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P110 = new Transform(0.0f, 180.0f, 180.0f, 0.0f, 0.1875f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P111 = new Transform(0.0f, 0.0f, 180.0f, 0.0f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f);
    private static final Transform P112 = new Transform(0.0f, 180.0f, 180.0f, 0.0f, 1.40625f, 0.0f, 1.0f, 1.0f, 1.0f);
    private static final Transform P113 = new Transform(0.0f, 315.0f, 0.0f, 0.0f, 0.01875f, 0.0f, 0.28f, 0.28f, 0.28f);
    private static final Transform P114 = new Transform(30.0f, 45.0f, 0.0f, 0.0f, 0.1875f, 0.0f, 1.0f, 1.0f, 1.0f);
    private static final Transform P115 = new Transform(0.0f, 0.0f, 0.0f, 0.0f, 0.1875f, 0.0f, 0.22f, 0.22f, 0.22f);
    private static final Transform P116 = new Transform(45.0f, 225.0f, 0.0f, 0.0f, 0.1875f, 0.0f, 0.5f, 0.5f, 0.5f);
    private static final Transform P117 = new Transform(0.0f, 0.0f, 0.0f, 0.0625f, 0.1875f, -0.0625f, 0.35f, 0.35f, 0.35f);
    private static final Transform P118 = new Transform(0.0f, -125.0f, 0.0f, -0.0625f, 0.125f, 0.125f, 0.5f, 0.5f, 0.5f);
    private static final Transform P119 = new Transform(0.0f, 55.0f, 0.0f, -0.0625f, 0.125f, 0.125f, 0.5f, 0.5f, 0.5f);
    private static final Transform P120 = new Transform(0.0f, -65.0f, -5.0f, -0.0625f, -0.15625f, -0.375f, 0.5f, 0.5f, 0.5f);
    private static final Transform P121 = new Transform(0.0f, 115.0f, 5.0f, 0.0f, -0.15625f, -0.375f, 0.5f, 0.5f, 0.5f);
    private static final Transform P122 = new Transform(0.0f, 60.0f, 0.0f, 0.6875f, 1.0625f, -0.125f, 1.0f, 1.0f, 1.0f);
    private static final Transform P123 = new Transform(0.0f, 60.0f, 0.0f, 0.1875f, 1.0625f, 0.75f, 1.0f, 1.0f, 1.0f);
    private static final Transform P124 = new Transform(0.0f, -90.0f, 25.0f, -0.125f, 0.75f, 0.0625f, 0.72f, 0.72f, 0.72f);
    private static final Transform P125 = new Transform(0.0f, 90.0f, -25.0f, 0.625f, 0.75f, 0.0625f, 0.72f, 0.72f, 0.72f);
    private static final Transform P126 = new Transform(0.0f, 90.0f, 180.0f, 0.5f, -1.0625f, 0.5625f, 1.0f, 1.0f, 1.0f);
    private static final Transform P127 = new Transform(0.0f, 90.0f, 180.0f, 0.5f, -1.0625f, -0.4375f, 1.0f, 1.0f, 1.0f);
    private static final Transform P128 = new Transform(0.0f, -90.0f, 25.0f, -0.1875f, 0.75f, 0.0625f, 0.72f, 0.72f, 0.72f);

    private static final Map<String, Profile> MODELS = Map.ofEntries(
            Map.entry("block/block", new Profile(Group.BLOCKS, Map.of(GUI, P0, GROUND, P1, FIXED, P2, SHELF, P3, THIRD_RIGHT, P4, FIRST_RIGHT, P5, FIRST_LEFT, P6, THIRD_LEFT, P4))),
            Map.entry("block/heavy_core", new Profile(Group.BLOCKS, Map.of(GUI, P7, GROUND, P8, FIXED, P9, THIRD_RIGHT, P10, FIRST_RIGHT, P11, FIRST_LEFT, P12, THIRD_LEFT, P10))),
            Map.entry("block/lectern", new Profile(Group.BLOCKS, Map.of(FIRST_RIGHT, P13, FIRST_LEFT, P13))),
            Map.entry("block/orientable_with_bottom", new Profile(Group.BLOCKS, Map.of(FIRST_RIGHT, P14, FIRST_LEFT, P14))),
            Map.entry("block/template_glazed_terracotta", new Profile(Group.BLOCKS, Map.of(FIRST_RIGHT, P15, FIRST_LEFT, P15))),
            Map.entry("block/template_shelf_inventory", new Profile(Group.BLOCKS, Map.of(GUI, P16, FIXED, P17, SHELF, P18, FIRST_RIGHT, P19, THIRD_RIGHT, P20, FIRST_LEFT, P19, THIRD_LEFT, P20))),
            Map.entry("block/thin_block", new Profile(Group.BLOCKS, Map.of(THIRD_RIGHT, P21, FIRST_RIGHT, P22, FIRST_LEFT, P23, THIRD_LEFT, P21))),
            Map.entry("item/amethyst_bud", new Profile(Group.ITEMS, Map.of(FIRST_RIGHT, P24, THIRD_RIGHT, P25, HEAD, P26, GUI, P27, FIRST_LEFT, P24, THIRD_LEFT, P25))),
            Map.entry("item/bamboo", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P30, FIRST_RIGHT, P31, FIRST_LEFT, P32, THIRD_LEFT, P33))),
            Map.entry("item/big_dripleaf", new Profile(Group.ITEMS, Map.of(GUI, P34, FIXED, P35, THIRD_RIGHT, P36, FIRST_RIGHT, P37, FIRST_LEFT, P37, THIRD_LEFT, P36))),
            Map.entry("item/blaze_rod", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P30, FIRST_RIGHT, P31, FIRST_LEFT, P32, THIRD_LEFT, P33))),
            Map.entry("item/bone", new Profile(Group.ITEMS, Map.of(HEAD, P38, THIRD_RIGHT, P30, FIRST_RIGHT, P31, FIRST_LEFT, P32, THIRD_LEFT, P33))),
            Map.entry("item/bow", new Profile(Group.TOOLS, Map.of(THIRD_RIGHT, P39, THIRD_LEFT, P40, FIRST_RIGHT, P41, FIRST_LEFT, P42))),
            Map.entry("item/breeze_rod", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P30, FIRST_RIGHT, P31, FIRST_LEFT, P32, THIRD_LEFT, P33))),
            Map.entry("item/conduit", new Profile(Group.ITEMS, Map.of(GUI, P43, GROUND, P8, HEAD, P3, FIXED, P3, THIRD_RIGHT, P44, FIRST_RIGHT, P45, FIRST_LEFT, P45, THIRD_LEFT, P44))),
            Map.entry("item/crossbow", new Profile(Group.TOOLS, Map.of(THIRD_RIGHT, P46, THIRD_LEFT, P47, FIRST_RIGHT, P48, FIRST_LEFT, P49))),
            Map.entry("item/debug_stick", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P30, FIRST_RIGHT, P31, FIRST_LEFT, P32, THIRD_LEFT, P33))),
            Map.entry("item/decorated_pot", new Profile(Group.BLOCKS, Map.of(THIRD_RIGHT, P50, FIRST_RIGHT, P51, GUI, P52, GROUND, P53, HEAD, P54, FIXED, P55, FIRST_LEFT, P51, THIRD_LEFT, P50))),
            Map.entry("item/dragon_head", new Profile(Group.ITEMS, Map.of(GUI, P56, FIRST_RIGHT, P57, FIRST_LEFT, P57))),
            Map.entry("item/generated", new Profile(Group.ITEMS, Map.of(GROUND, P28, HEAD, P29, THIRD_RIGHT, P58, FIRST_RIGHT, P59, FIXED, P3, FIRST_LEFT, P59, THIRD_LEFT, P58))),
            Map.entry("item/goat_horn", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P60, THIRD_LEFT, P58, FIRST_RIGHT, P61, FIRST_LEFT, P62))),
            Map.entry("item/handheld", new Profile(Group.TOOLS, Map.of(THIRD_RIGHT, P30, THIRD_LEFT, P33, FIRST_RIGHT, P63, FIRST_LEFT, P64))),
            Map.entry("item/handheld_mace", new Profile(Group.TOOLS, Map.of(THIRD_RIGHT, P65, THIRD_LEFT, P66, FIRST_RIGHT, P67, FIRST_LEFT, P68))),
            Map.entry("item/handheld_rod", new Profile(Group.TOOLS, Map.of(THIRD_RIGHT, P69, THIRD_LEFT, P70, FIRST_RIGHT, P71, FIRST_LEFT, P72))),
            Map.entry("item/hanging_roots", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P73, FIRST_RIGHT, P74, FIRST_LEFT, P74, THIRD_LEFT, P73))),
            Map.entry("item/pointed_dripstone", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P75, FIRST_RIGHT, P76, FIRST_LEFT, P76, THIRD_LEFT, P75))),
            Map.entry("item/shield", new Profile(Group.TOOLS, Map.of(THIRD_RIGHT, P77, THIRD_LEFT, P78, FIRST_RIGHT, P79, FIRST_LEFT, P80, GUI, P81, FIXED, P82, GROUND, P83))),
            Map.entry("item/shield_blocking", new Profile(Group.TOOLS, Map.of(THIRD_RIGHT, P84, THIRD_LEFT, P85, FIRST_RIGHT, P86, FIRST_LEFT, P87, GUI, P81))),
            Map.entry("item/small_dripleaf", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P25, FIRST_RIGHT, P88, FIRST_LEFT, P88, THIRD_LEFT, P25))),
            Map.entry("item/spear_in_hand", new Profile(Group.TOOLS, Map.of(FIRST_RIGHT, P89, FIRST_LEFT, P90, THIRD_RIGHT, P91, THIRD_LEFT, P92))),
            Map.entry("item/stick", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P30, FIRST_RIGHT, P31, FIRST_LEFT, P32, THIRD_LEFT, P33))),
            Map.entry("item/template_banner", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P93, FIRST_RIGHT, P94, GUI, P95, GROUND, P53, HEAD, P96, FIXED, P55, FIRST_LEFT, P94, THIRD_LEFT, P93))),
            Map.entry("item/template_bed", new Profile(Group.BLOCKS, Map.of(THIRD_RIGHT, P97, FIRST_RIGHT, P98, GUI, P99, GROUND, P100, HEAD, P101, FIXED, P102, FIRST_LEFT, P98, THIRD_LEFT, P97))),
            Map.entry("item/template_chest", new Profile(Group.BLOCKS, Map.of(GUI, P103, GROUND, P1, HEAD, P3, FIXED, P55, THIRD_RIGHT, P104, FIRST_RIGHT, P105, FIRST_LEFT, P105, THIRD_LEFT, P104))),
            Map.entry("item/template_copper_golem_statue", new Profile(Group.ITEMS, Map.of(GUI, P106, GROUND, P107, FIRST_RIGHT, P108, THIRD_RIGHT, P109, FIXED, P110, SHELF, P111, HEAD, P112, FIRST_LEFT, P108, THIRD_LEFT, P109))),
            Map.entry("item/template_shulker_box", new Profile(Group.BLOCKS, Map.of(GUI, P103, GROUND, P1, HEAD, P3, FIXED, P55, THIRD_RIGHT, P104, FIRST_RIGHT, P113, FIRST_LEFT, P113, THIRD_LEFT, P104))),
            Map.entry("item/template_skull", new Profile(Group.ITEMS, Map.of(GUI, P114, FIXED, P9, GROUND, P115, THIRD_RIGHT, P116, FIRST_RIGHT, P117, FIRST_LEFT, P117, THIRD_LEFT, P116))),
            Map.entry("item/tooting_goat_horn", new Profile(Group.ITEMS, Map.of(THIRD_RIGHT, P118, THIRD_LEFT, P119, FIRST_RIGHT, P120, FIRST_LEFT, P121))),
            Map.entry("item/trident_in_hand", new Profile(Group.TOOLS, Map.of(THIRD_RIGHT, P122, THIRD_LEFT, P123, FIRST_RIGHT, P124, FIRST_LEFT, P125, GUI, P81, FIXED, P82, GROUND, P83))),
            Map.entry("item/trident_throwing", new Profile(Group.TOOLS, Map.of(THIRD_RIGHT, P126, THIRD_LEFT, P127, FIRST_RIGHT, P128, FIRST_LEFT, P125, GUI, P81, FIXED, P82, GROUND, P83))),
            Map.entry("block/template_bed_head", new Profile(Group.BLOCKS, Map.of(THIRD_RIGHT, P97, FIRST_RIGHT, P98, GUI, P99, GROUND, P100, HEAD, P101, FIXED, P102, FIRST_LEFT, P98, THIRD_LEFT, P97))),
            Map.entry("block/template_bed_foot", new Profile(Group.BLOCKS, Map.of(THIRD_RIGHT, P97, FIRST_RIGHT, P98, GUI, P99, GROUND, P100, HEAD, P101, FIXED, P102, FIRST_LEFT, P98, THIRD_LEFT, P97))));

    private CompactModelTransforms() { }
    public static Map<Context, Transform> find(String namespace, String path, String owner,
            boolean blocks, boolean items, boolean tools) {
        if (!(blocks || items || tools) || !"minecraft".equals(namespace)
                || !"vanilla".equals(owner) || path == null) return null;
        Profile profile = MODELS.get(path);
        if (profile == null) return null;
        boolean enabled = switch (profile.group()) {
            case BLOCKS -> blocks;
            case ITEMS -> items;
            case TOOLS -> tools;
        };
        return enabled ? profile.transforms() : null;
    }
}
