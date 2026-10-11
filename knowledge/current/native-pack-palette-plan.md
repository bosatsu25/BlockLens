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
measurements, not a performance-improvement claim. The Python evidence suite has 86 passing tests, including execution of the actual CI Bash
collector. Independent review reproduced acceptance of malformed pixel fields; the regression
first failed seven cases, then passed after separating required canonical-integer and ROI bounds.
Hosted current-head Shader/Vulkan, normal and BBE gates remain required before merge.

## Hosted framebuffer diagnosis

The first PR #69 run passes five shader-OFF/Vulkan rows and both BBE rows, but rejects three
shader-ON unchanged-reload frames and two normal restored frames. Exact atlas/decode restoration
passes. Actual 26.3 normal OFF/restored frames differ at 67 RGB-threshold pixels; interior red-blue
wood chroma differs only 0.023/0.000. A shader-ON unchanged reload differs at 1114 RGB-threshold
pixels including temporally varying floor shadows; interior chroma differs only 0.042/0.012.

Retain the positive ON pixel-difference threshold and add a positive warm-to-neutral chroma
change of at least 2 in each of two interior wood faces. Compare unchanged/restored chroma in
those same faces with a strict mean tolerance below 0.5, excluding sky/floor/silhouette noise.
Exact alpha and visible atlas/standalone pixels remain required. A test-only JDK helper, outside
the runtime JAR, reproduces the old false rejection under neutral brightness drift, then verifies
restoration, missing/reversed/one-role-only palette changes and invalid dimensions. This is an
oracle repair tied to actual failing frames; it does not weaken texture restoration or skip shaders.

Independent review identified that mean chroma alone could accept a blank or reshuffled neutral
wood region. Both comparisons now also require each region's three-pixel-cell luminance pattern
variance (standard deviation at least 8 in summed RGB units), variance ratio 0.5..2 and centered
correlation at least 0.9. Observed normal-frame correlations are above 0.999; the failed 26.3 shader
frames retain correlations 0.959/0.995 across ON and 0.961/0.994 across reload. No sky or floor
pixel can replace wood presence. Additional negatives reject black/solid frames, reshuffling and
one-region disappearance. The color and structure guards both apply to positive ON and restoration.

The strengthened metric passes all 329 shared tests, selected JaCoCo/PIT, three build/audit/budget
and native-compilation gates. The same compiled helper accepts the three preserved CI image sets
and rejects their OFF/ON substitution. Actual Windows 26.3 with the pinned active Iris/Sodium shader
profile passes and exits cleanly; its strict independent collector accepts 32 files. Final hosted
current-head all-three/all-eight/BBE evidence is still required before merge.
