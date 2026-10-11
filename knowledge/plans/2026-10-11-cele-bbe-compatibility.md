# Cele rendering-stack compatibility implementation plan

Goal: repair regular bright chests in Cele-Modpack-v1.1 without disabling BBE or changing working glass/kelp controls.
Architecture: retain both the existing vanilla renderer and BBE terrain meshing. Add an exact-version adapter to BBE sprite selection and final quad material attributes; reuse BlockLens-owned textures in the terrain atlas.
Stack: Java 25, Fabric, Minecraft 26.1.2/26.2/26.3. Optional adapter evidence is scoped to BBE 1.3.9+mc26.3 and Sodium 0.9.2+mc26.3, plus a second stack adding Iris 1.11.7+mc26.3 (shaders OFF) and Continuity 3.0.1+26.3.

## Constraints
- Preserve all 55 capability keys/defaults and the source reference contract. Do not revive Masa integration or the debris analyzer.
- Runtime JAR <= 204800 bytes. Never upload private source-pack configurations/world data or redistribute third-party artwork.
- The supplied pack embeds an older 0.4.0 JAR without the ore-border classes/assets. Displayed version alone is not build identity.
- Four-mod rendering-stack evidence is not proof of whole-pack runtime equivalence.

## Tasks
1. RED: add a packaged-client profile with exact hash-verified public dependencies and synthetic settings. Reuse actual closed single/left/right UI/pixel/animation checks and glass/kelp/ore-border tests; retain the baseline failure.
2. GREEN: add the 26.3-only optional BBE adapter. Select the owned bright texture in the block atlas; mark only that material emissive and avoid BBE's non-emissive lighting path. Prove BBE terrain emission remains active with test-only counters.
3. Test exact/missing/unsupported versions; reload and OFF restoration; negative trapped/ender/Christmas material cases. Retain no world objects or per-frame discovery.
4. Give the repaired artifact an unambiguous version and record the input-JAR identity mismatch and reference-pack boundaries.
5. Run three-version normal CI, the existing external matrix and both new stack jobs on the exact head. Review the diff, merge with an expected-head lease, and verify generated artifact metadata/hash/integrity.

## Review focus
Absent-mod linkage, mixed quad shade flags, cached atlas references, unchanged BBE optimization settings, and accidental disclosure of source-pack personal data.
