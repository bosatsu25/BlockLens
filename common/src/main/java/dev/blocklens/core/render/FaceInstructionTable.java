package dev.blocklens.core.render;

import java.util.Objects;

/**
 * Precompiles semantic face instructions once per baked state; the per-quad path only performs
 * one indexed lookup. Each retained table is immutable and belongs to a baked model lifecycle.
 */
public final class FaceInstructionTable {
    private static final DecorationQuadFace[] FACES = DecorationQuadFace.values();
    private final DecorationQuadInstruction[] instructions;

    private FaceInstructionTable(DecorationQuadInstruction[] instructions) {
        this.instructions = instructions;
    }

    public static FaceInstructionTable compile(DecorationRenderDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        var instructions = new DecorationQuadInstruction[FACES.length];
        for (DecorationQuadFace face : FACES) {
            instructions[face.ordinal()] = DecorationQuadCuePolicy.instruction(descriptor, face);
        }
        return new FaceInstructionTable(instructions);
    }

    public DecorationQuadInstruction instruction(DecorationQuadFace face) {
        return instructions[Objects.requireNonNull(face, "face").ordinal()];
    }
}
