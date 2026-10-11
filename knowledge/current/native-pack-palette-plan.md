# Native supplied-pack palette implementation

Issue #64 remains open. Preserve the first 55 capabilities, saved keys, default preset,
original 37-capability baseline and reserved removed bits 46 and 56.

Append an independent, default-off `outline.pale_oak_neutral` at bit 57. Shared pure policy
selects exactly the 18 source pale-oak texture roles plus three modern sign aliases.
Recolor only low-chroma warm wood using integer luminance, preserving alpha, grain and
colored hardware. Operate only on vanilla-owned Minecraft textures, after ordinary decode,
before atlas/standalone upload. No copied assets, pack scans, dynamic packs or material hashes.
The bounded image policy allows at most 256 by 256 pixels and does no work when disabled.

Successful settings save installs the config and reloads resources only when this bit changed.
Esc/discard or a failed save must leave the active textures and runtime configuration unchanged.
Native tests must exercise actual atlas sprites and standalone decoded textures, OFF/ON/OFF,
reload, external priority, modern signs and rendered screenshots on all three versions.
Run full common/coverage/mutation, three build/audit/budget gates, native clients and hosted
exact-head compatibility gates before merge. Do not claim complete Issue #64 source parity.

The five LowOnFire world model overrides equal vanilla geometry after replacing the auxiliary
texture alias. Its fire_0 and auxiliary fire_2 have identical alpha and visible colors to vanilla
fire_0/fire_1; the animation metadata agrees. Those aliases preserve ordinary world fire while
its fire_1 modifies the first-person overlay. Existing first-person geometry lowering preserves
ordinary world fire without introducing a separate duplicate world-fire feature.

## Local verification evidence — 2026-10-12 JST

The initial six policy tests reproduced four failures before implementation. Seven policy tests
now pass. Shared JUnit, selected JaCoCo/PIT and all three runtime build/budget/audit gates pass;
PIT observed 481/498 killed mutations (97%). Native compilation uses Java 25 with warnings as errors.

Packaged visible clients pass on all three targets and terminate cleanly. Each produces six PNGs:
OFF, ON, unchanged reload, OFF restored, all 56 capabilities and external warm-color priority.
Within the fixed wood ROI x220..410/y180..255, ON changed 377 / 389 / 385 pixels respectively.
Retained atlas pixels and standalone decodes are checked for 18 old-path roles on 26.1.2 and
19 current roles on 26.2/26.3. Transparent RGB filled by vanilla mipmap generation is excluded
from visible-color comparisons; alpha is always checked. UI save/discard/failure and external
priority checks pass. These are representative block frames and texture-loader/atlas proofs;
separate boat/entity/GUI screenshot coverage and the other Issue #64 behaviors remain outstanding.
The added mandatory CI collector requires all six frames and each native manifest control.
The full packaged 26.3 graph also passes and exits normally (8m21s), including both languages,
all categories, compact layout, analyzers, scene filters, builder tools, extensions and cleanup.
Its palette ON frame changes 412 pixels in the fixed ROI. The full test clears the previous
dark-fixture ceiling before observing wood. Allocation/reload observations are retained as
measurements, not a performance-improvement claim. The Python evidence suite has 84 passing tests.
Hosted current-head Shader/Vulkan, normal and BBE gates remain required before merge.
