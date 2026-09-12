# Source Baseline — AMATERAS Resource Pack

Status: **authoritative migration baseline**

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

## Baseline change policy

If either source hash changes, do not silently overwrite this baseline.

Instead:

1. create a new baseline revision,
2. diff source capabilities/files,
3. update target/state maps,
4. update visual/golden references,
5. re-run parity assessment,
6. document whether the new baseline supersedes this one.
