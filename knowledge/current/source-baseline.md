# Source Baseline — AMATERAS Resource Pack

Status: **authoritative migration baseline — M0 complete**

This document pins the exact source files used to define BlockLens M0.

## Files

### Resource-pack ZIP

```text
Name:   AMATERAS_Resourcepack_mc26.1.2.zip
SHA-256: 36e5c5bba4e77f05b8c22d549593dfb95e8aebc7bfebdba57e229257b46640ef
Size:   2,366,865 bytes
```

### RPO preset

```text
Name:   AMATERAS_Resourcepack_mc26.1.2.zip.rpo
SHA-256: 37a429a543d9b5163b8c351ac2b81b5db5c7c7ac08c9d777ccdcf6a45367b949
Size:   1,128 bytes
```

## ZIP structure measurements

```text
ZIP entries:             4,535
JSON files:              3,135
PNG files:               1,020
RPO rule files in ZIP:     336
MCMeta files:               43
JSON5 files:                 1
Uncompressed payload: 2,209,147 bytes
Compressed payload:   1,279,563 bytes
ZIP/container overhead:1,087,302 bytes
```

Approximate container overhead ratio:

```text
1,087,302 / 2,366,865 ~= 45.9%
```

This is a major reason the mod rewrite can plausibly beat the source ZIP by more than 50%: the source representation uses thousands of small files.

## M0 dependency analysis

Automated analysis of all 336 `.rpo` gate files found:

```text
RPO conditions mapped:             37 / 37
RPO rules parsed:                 336 / 336
Non-RPO source assets:          4,196
Assets reachable from gates:    4,101
Reachable ratio:                97.74%
Candidate unreferenced assets:     95
Shared transitive dependencies:     6
```

The complete human-readable mapping is in [`capability-map.md`](capability-map.md).
The machine-readable product contract is in [`capability-contract.tsv`](capability-contract.tsv).

Important findings include:

- `stairs`: 58 gated blockstates and 2,320 source variants.
- `slabs`: 61 gated blockstates.
- `trapdoor`: 32 gated files; 21 current block targets plus item-model gates in the source pack.
- `stringtweaks`: `tripwire` attached/powered/connectivity states are explicitly represented.
- `nethertweaks`: 33 texture gates plus the magma-block model; 27 Nether-oriented target blocks/materials.
- `stainedglass`: source behavior is specifically "Make stained glasses opaque".
- `gaming.*`: source blockstates redirect to dedicated animated highlight textures/models.

## RPO preset contents

```text
{
    deco: {
        anvil: false,
        beehive: false,
        campfire: false,
        glazedterracotta: false,
        grindstone: false,
        fence_gate: false,
        froglight: false,
        slabs: false,
        stainedglass: false,
        stairs: false,
        trapdoor: false,
        wood: false,
        log: false
    },
    gaming: {
        obsidian: false,
        ancient_debris: false,
        diamond_ore: false,
        deepslate_diamond_ore: false,
        gold_ore: false,
        deepslate_gold_ore: false,
        emerald_ore: false,
        deepslate_emerald_ore: false,
        coal_ore: false,
        deepslate_coal_ore: false,
        iron_ore: false,
        deepslate_iron_ore: false,
        copper_ore: false,
        deepslate_copper_ore: false,
        lapis_ore: false,
        deepslate_lapis_ore: false,
        redstone_ore: false,
        deepslate_redstone_ore: false
    },
    outline: {
        blueice: true,
        deadcoral: true,
        powdersnow: true,
        sculk_catalyst: true
    },
    others: {
        nethertweaks: false,
        stringtweaks: true
    }
}
```

## Enabled reference preset

Exactly five capabilities are enabled in this RPO:

1. `blueice`
2. `deadcoral`
3. `powdersnow`
4. `sculk_catalyst`
5. `stringtweaks`

This preset is a **reference configuration**, not the complete capability scope.

## Licensing posture

No explicit license file was found in the supplied resource-pack ZIP.

Therefore the baseline policy is fail-closed:

- source binaries are reference evidence, not automatically redistributable BlockLens assets,
- runtime-generated geometry/markers and independently implemented rendering are preferred,
- any retained source binary requires an explicit redistribution-rights decision before public release.

## M0 exit decision

**PASS.** The source capability set is sufficiently mapped to begin implementation.

M0 does not claim that every final BlockLens pixel must copy the original pack. It freezes the source user value, target/state semantics, gated source roots, and reference configuration so the implementation can be tested for parity while using a smaller and more maintainable runtime design.

## Baseline change policy

If either source hash changes, do not silently overwrite this baseline.

Instead:

1. create a new baseline revision,
2. diff source capabilities/files,
3. update target/state maps,
4. update visual/golden references,
5. re-run parity assessment,
6. document whether the new baseline supersedes this one.
