# Masa integration and standalone migration decision

Status: #38 evaluation completed by PR #49; exact-head CI #326 passed all four jobs on the first attempt, and the decision is merged.

BlockLens owns client rendering, bounded inspection and advisory assistance. It does not own other mods' automatic item selection, inventory workflows, container collection, shared-server schematic administration or global preference restoration. Installing ChiseTweaks is not required to use BlockLens.

## Capability decisions

The source review is pinned to the ChiseTweaks revision referenced below. These decisions describe behavior; they do not import its reflection framework or implementation.

| Source integration | User value and actual effect | BlockLens decision and maintenance reason |
| --- | --- | --- |
| Litematica pick redirection | Replaces a schematic-required item with an available alternative during external inventory selection; does not itself place a block. | Unsupported. BlockLens's advisory placement scope does not take ownership of automatic inventory selection. |
| Tweakeroo tool-switch guard | Cancels external automatic effective-tool switching according to a target-block ID list. | Unsupported. This requires ownership of another mod's automation hooks and target-block rule policy. |
| Persistent gamma | Restores the configured external gamma override on join by invoking its existing change callback. | Unsupported. Keep external global preference restoration with its owner; this is separate from BlockLens's own visual accents. |
| TweakerMore automatic-pick guard | Cancels automatic schematic-world item picking for configured held items. | Unsupported. An external automation control is outside the advisory boundary even when it only cancels a call. |
| TweakerMore material refresh | Refreshes a Litematica material cache after external automatic container collection. | Unsupported. It does not itself mutate the world, but belongs to that collection/cache workflow and its evolving hooks. |
| Syncmatica disable remove | Cancels REMOVE listener processing when removal is disabled; does not itself send a packet. | Unsupported. Shared-server schematic administration is outside BlockLens's client-only visual product. |
| Syncmatica require Shift | Cancels REMOVE listener processing while Shift is not pressed; disabling removal takes precedence. | Unsupported. This is an external administration guard, with the same ownership boundary as disable remove. |
| MaLiLib Japanese text substitution | Rewrites a limited set of external translations, retaining the original text for unknown entries. | Unsupported in the current product. BlockLens owns English/Japanese translations for its own UI; maintaining another product's translation dictionary and injection hooks adds a separate responsibility. This exclusion does not imply the feature is automation. |
| Read-only Litematica placement comparison | Explains the difference between the locally available schematic state and a placement/world state. | Accepted as a separate optional candidate in #36. It must read one bounded position through a pinned typed API, tolerate absence/unsupported versions and never initiate placement or inventory selection. It is not implemented by this decision document. |

No runtime Masa adapter is introduced by #38. Consequently the absent-mod path has no linkage, hard dependency, background discovery or reflection. The accepted #36 candidate needs its own exact-version and three-client evidence before it can be advertised; a compile-only dependency does not establish runtime compatibility. Unsupported integrations remain with their existing owner.

## Current parity and user path

| Responsibility | Delivered boundary | Retained difference / next owner |
| --- | --- | --- |
| Material highlights, hidden-surface cues, fine thread and Nether palette | P0 in BlockLens; frozen source identities and explicit additive targets. | Source semantics and exceptions remain in [chisetweaks-migration.md](chisetweaks-migration.md). |
| Glass, kelp, bright concrete/chest, low fire and handheld sizing | Six independent controls merged in PR #46 for all three Minecraft targets. | Shader/backend and third-party pack combinations remain #31. |
| Five bounded world analyzers | Implementation and evidence tracked by #34. | Not delivered by #38; client job memory can be absent and range previews do not infer hidden server effects. |
| Block/entity scene filtering | #35. | Not delivered by #38. |
| Inspector, placement preview, pattern consistency and optional schematic comparison | #36. | Not delivered by #38; assistance must remain read-only. |
| Modded ore extensions | #37. | Not delivered by #38. |
| Eight rejected external Masa integration responsibilities | Remain outside BlockLens scope. | Keep using their owner when these behaviors are needed; do not claim complete ChiseTweaks parity. |

Users may select BlockLens for its delivered visual/inspection controls and retain ChiseTweaks for needed external integrations or unported features. Avoid enabling overlapping visual controls in both mods without separate compatibility evidence; coexistence is not established by this source review. The latest BlockLens public release remains distinct from the development branch and does not acquire unshipped capabilities through a documentation change.

## Standalone decision

**Retain ChiseTweaks; do not deprecate, archive or declare it superseded.** Relevant parity is incomplete, intentional differences remain, and a BlockLens release containing all future migrations has not been verified or published. Even completing #34–#37 would not establish parity for the rejected external responsibilities. Any later retirement proposal must first resolve the intended retained user scope, runtime compatibility and release evidence, then receive explicit authorization for changes to that repository. #38 requests evaluation, not repository archival.

The decision is complete when its source matrix, current-delivery claims and independent review are verified on the document PR. It does not close #31, #34–#37 or the allocation investigation #43.

## Source references

Pinned ChiseTweaks revision: `9fc977c5fdc0b23040d152216f60d46724cc65d8`. The source may change later; this matrix refers to these exact paths and behaviors.

- [Pick redirection](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/integration/masa/LitematicaPickRedirect.java#L13-L39)
- [Tool-switch cancellation](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/mixin/masa/TweakerooToolSwitchMixin.java#L13-L25)
- [Gamma restoration and material refresh](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/integration/masa/MasaReflectionSupport.java#L15-L41)
- [Automatic-pick cancellation](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/mixin/masa/TweakerMoreAutoPickMixin.java#L15-L29)
- [Material refresh hook](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/mixin/masa/TweakerMoreMaterialRefreshMixin.java#L16-L32)
- [Syncmatica cancellation hook](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/mixin/masa/SyncmaticaRemoveListenerMixin.java#L16-L37) and [two-condition policy](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/core/policy/SyncmaticaRemoveGuardPolicy.java#L7-L10)
- [Limited translation hook](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/mixin/masa/MaLiLibTranslationMixin.java#L11-L25) and [exact dictionary with unknown-text fallback](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/core/policy/MasaJapaneseTextPolicy.java#L11-L83)
- [Schematic state read](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/integration/masa/LitematicaSchematicAccess.java#L17-L43) and [advisory comparison](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/core/policy/SchematicPlacementComparisonPolicy.java#L17-L28)
