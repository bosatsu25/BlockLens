# Native compact model transforms

Issue #64 remains open; this implementation depends on the reviewed palette PR #69.
Preserve existing 56 identities/config/defaults and keep removed bits 46/56 reserved.
Append three independent default-off controls at bits 58..60: compact blocks, items and tools.
They replace the relevant vanilla-owned model display transforms using a static compiled policy,
while the existing configurable first-person percentage multiplier remains independent.

The supplied Small Handhelds input has 44 model definitions and 30 explicit display groups.
The behavioral reference includes both hands, third person, GUI/ground/head/fixed/shelf contexts,
shield blocking, bow/crossbow, spear, trident and item-specific exceptions. Numeric transforms
are functional reference facts; original model geometry, textures, JSON and RPO metadata are not
bundled or parsed at runtime. Source helper-parent changes are resolved into bounded native
transform profiles, keeping original geometry/textures/parents from Minecraft's ordinary loader.
Use typed CuboidModel/ItemTransforms records after normal decode, with no registry/resource scans.
Skip external-owned model definitions and disabled controls before allocating native vectors.

All three versions expose the same model-parser and transform APIs. Minecraft 26.1.2 retains
item/template_bed; 26.2/26.3 use block/template_bed_head and foot. Modern aliases must preserve
composite geometry and be checked with actual rendered whole-bed frames rather than assuming
legacy template availability. Source-only helper names must never produce missing-model warnings.

Successful save reloads resources only when one of these model controls or the palette changed.
Discard and failed save retain active config/textures/models. The flags are never mutually exclusive.
Verify independent groups, actual loaded transforms, inherited/special-state transforms, left/right
and third-person frames, all-enabled composition, active external priority, reload and restoration
on every target. Add fail-closed collectors, all common/coverage/PIT/build/audit/size gates, exact-head
hosted normal/Shader/Vulkan/BBE CI and independent review before merge.
Do not claim original pixels or an FPS improvement; palette boat/entity/GUI frames and other pack
behaviors remain distinct Issue #64 work until independently established.

The bounded policy contains 42 concrete profiles (40 legacy display roles plus two modern bed
aliases), reusing 129 immutable numeric poses. Six vanilla children that formerly selected a
source-only helper receive only the helper's explicit hand poses plus their own exceptions.
Inherited head/ground/fixed contexts stay unset so Minecraft resolves the current active parent;
freezing those inherited source-default values would override external parent-pack choices.
An actual regression reproduced two failures before correcting this boundary. Native observation
must check both direct external definitions and effective inherited external contexts.

Actual native validation exposed Fabric model API cancelling the vanilla CuboidModel.fromStream call and substituting its own deserializer. The transform now runs on the typed successful parser return, after Fabric deserialization; null/failure and custom non-cuboid models pass through. The test-only observation runs after the production return modification. The saved-model count regression must retain all native roles.

The native pose probe is scoped to ItemStackRenderState.submit and its real display context, excluding parser/bounds/HUD false positives. Capture both original first-hand poses before enabling; reject an ON frame claiming to match those originals; after disabling require both original applied poses again. OFF restoration therefore covers parsed resources and actual renderer submissions. Failed diagnostic directories cannot supply successful compact evidence; a regression reproduces and rejects that path.

## Local verification before PR

339 JUnit tests, selected JaCoCo, PIT 496/513 (97%), 93 Python tests, all three builds/runtime audits/budgets and native test compilation pass. Final packaged 0.4.3 native tests pass and exit cleanly on 26.1.2, 26.2 and 26.3. Each strict collector accepts 28 PNG frames and the 14-control manifest; 26.1.2 observes 40 parsed model roles, modern targets 41. Runtime JARs measure 188777/188891/191708 B. Current-head hosted normal, external and BBE gates remain mandatory before merge. A separate post-palette-main scene-filter rebuilding defect is being repaired independently; it is not hidden by lowering the visual thresholds.
