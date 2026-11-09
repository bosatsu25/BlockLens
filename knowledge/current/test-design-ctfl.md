# Technique-driven test design (ISTQB/JSTQB CTFL v4.0, chapter 4)

Status: **authoritative record of the technique-driven unit test designs**; complements
[`quality-strategy.md`](quality-strategy.md) §3 Layer A.

## 1. Scope and method

The Foundation Level syllabus (JSTQB Version2023V4.0.J02, chapter 4) names four black-box techniques
(equivalence partitioning 4.2.1, boundary value analysis 4.2.2, decision table testing 4.2.3, state
transition testing 4.2.4), the two white-box coverage measures (statement 4.3.1, branch 4.3.2) and
three experience-based techniques (error guessing 4.4.1, exploratory 4.4.2, checklist-based 4.4.3).
This document applies them to the deterministic `common` policies whose inputs have declared ranges,
rule tables or state, and records the coverage items each design reaches.

The targets were not chosen by taste. The PIT gate already measured 98 % mutation coverage over the
`common` policy classes, and its surviving mutants named the inputs no test had probed: **every
"changed conditional boundary" survivor is a boundary without a boundary-value test**. The designs
below start from that list, so each one either kills a survivor or shows it to be equivalent (§9).

Coverage per technique is measured as the syllabus defines it: partitions covered / partitions
identified; boundary coverage items (three-value BVA: the boundary and both neighbours) covered /
identified; feasible decision-table rules covered / identified; transitions covered / valid
transitions (0-switch) plus invalid transitions attempted. Statement and branch coverage come from
JaCoCo; mutation coverage from PIT. Each test case carries a `TD-*` identifier in its display name
so a failure traces back to its row here.

## 2. Test basis

| Item | Test basis | Test object |
| --- | --- | --- |
| Settings list geometry | [`settings-ui.md`](settings-ui.md) (bounded scroll per category, resource action row, rows reachable by keyboard) | `dev.blocklens.core.ui.SettingsLayout` |
| Configuration file decoding | [`chisetweaks-migration.md`](chisetweaks-migration.md), [`scene-filter.md`](scene-filter.md) ("invalid file entries retain the last valid rule"), `BlockLensConfigCodec` contract ("unknown or malformed keys fail soft") | `dev.blocklens.core.BlockLensConfigCodec` |
| Settings draft lifecycle | [`settings-ui.md`](settings-ui.md) §Details ("Discard/Esc discards all edits", "failed saving retains it for retry") | `dev.blocklens.core.ui.SettingsDraft` |
| Pale oak neutral palette | [`native-pack-palette-plan.md`](native-pack-palette-plan.md) (keep transparent, keep saturated hardware, neutralize warm wood) | `dev.blocklens.core.render.PaleOakPalette` |
| Scene filter bounds | [`scene-filter.md`](scene-filter.md) (512 IDs × 256 chars per list, 48 KiB encoded over four lists) | `dev.blocklens.core.scene.SceneFilterOptions` |

## 3. SettingsLayout — BVA and decision table (`SettingsLayoutBoundaryTest`)

Equivalence partitions of `create(width, height, count, scroll, rowHeight, resourceActions)`:
width < 160 (invalid) / ≥ 160; height < 140 / ≥ 140; count < 0 / 0..64 / > 64; requested row
height < 42 / 42..span / > span; requested scroll < 0 / 0..maxScroll / > maxScroll. All ordered, so
three-value BVA applies.

| ID | Coverage items (boundary, neighbours) | Expected | Test |
| --- | --- | --- | --- |
| TD-L1 | width 159/160/161; height 139/140/141; count −1/0/1 and 63/64/65 | reject below, accept at and above (inclusive) | `acceptedDimensionsAndCountsAreInclusiveAtEveryDeclaredBoundary` |
| TD-L2 | width 643/644/645 (content width cap 620) | 619/620/620, centered | `contentWidthIsCappedAtSixHundredTwentyAndCentered` |
| TD-L3 | requested row height 41/42/43 and 125/126/127 (span 126) | 42/42/43 and 125/126/126 | `rowHeightIsClampedBetweenFortyTwoAndTheListSpan` |
| TD-L4 | requested scroll −1/0/1 and 881/882/883 (maxScroll 882), int extremes | clamped inclusive | `scrollIsClampedToZeroAndMaxScrollInclusive` |
| TD-L5 | decision table of `visible` (C1 index ≥ 0, C2 index < count, C3 rowY ≥ listTop, C4 rowY + rowHeight ≤ listBottom): all-true; each single false; both equalities | 8 rules, 7 feasible (C2 false alone is infeasible: a row past the last is always below the list) | `visibilityDecisionTable` |
| TD-L6 | `scrollToReveal`: already visible; one pixel above; one pixel below; far away; index −1/0/count−1/count | smallest move, clamped; reject outside | `scrollToRevealMovesByTheSmallestAmountAndClamps`, `scrollToRevealRejectsIndexesOutsideTheCategoryAtBothEnds` |

Coverage: 12 of 12 boundary items (TD-L1), 3/3, 6/6, 6/6, 7/7 feasible rules, 9/9 reveal items.

## 4. BlockLensConfigCodec — EP, BVA and error guessing (`BlockLensConfigCodecPartitionTest`)

Partitions of one input line (each-choice coverage):

| ID | Partition | Representative | Expected |
| --- | --- | --- | --- |
| TD-C1 | capability key, `true`/`false` in any case, surrounding spaces | `deco.anvil=TRUE`, `  deco.anvil = true  ` | applied |
| TD-C1 | capability key, non-boolean value | `yes`, `1`, empty, `true=false` | default kept |
| TD-C2 | comment, blank, no separator, unknown key, empty key, wrong key case | `# …`, `   `, `deco.anvil`, `unknown.key=true`, `=true`, `DECO.ANVIL=true` | whole config equals defaults |
| TD-C3 | bounded comfort option, three-value BVA at both bounds | fire −1/0/1, 1/2/3; percents 39/40/41, 99/100/101 for all three fields | accepted inclusive, default outside |
| TD-C4 | numeric text forms | `+70`, `070`, ` 70`, full-width `７０` (accepted by `Integer.parseInt`), `70.0`, `7e1`, words, overflow, negative, empty | only decimal integers inside the range are applied; the rest keep the default |
| TD-C5 | scene filter mode case, invalid mode, valid list (trim/lower/sort/dedupe), list with an invalid member | see test | last valid rule kept |
| TD-C6 | error guessing: CRLF and bare CR line endings, duplicate keys, one bad line among good ones, encode→decode identity at the bounds | see tests | later duplicate wins; bad lines isolated |

Analyzer option lines (same `bounded` path) are covered by `AnalyzerConfigurationTest`
(`everyNumericLimitRoundTripsWhileLegacyBitsAndComfortRemainIntact`,
`MalformedOrOutOfRangeValuesPreserveTheLastValidField`).

Coverage: 9/9 capability-line partitions, 6/6 inert partitions, 20/20 comfort boundary items,
12/12 numeric text partitions.

## 5. SettingsDraft — state transition testing (`SettingsDraftStateTransitionTest`)

States: **CLEAN** (`config` equals the saved baseline, `changed()` false) and **DIRTY**. Events:
`toggle`, `discard`, `save(writable)`, `save(unwritable)`, shortcut actions. State table (target
state / effect; `—` is an invalid or no-op transition that must change nothing):

| From \ Event | toggle | discard | save ok | save fail | no-op shortcut |
| --- | --- | --- | --- | --- | --- |
| CLEAN | DIRTY | CLEAN | CLEAN, returns false, **no I/O at all** | CLEAN, returns false, no exception | CLEAN |
| DIRTY | DIRTY, or CLEAN when the last edit is reverted | CLEAN (original baseline) | CLEAN, baseline advanced, file written | DIRTY, baseline unchanged | — |

| ID | Transitions | Test |
| --- | --- | --- |
| TD-S1 | CLEAN→DIRTY→DIRTY→DIRTY→CLEAN by toggles | `cleanToggleDirtyToggleBackClean` |
| TD-S2 | CLEAN→CLEAN and DIRTY→CLEAN by discard | `discardFromEitherStateIsClean` |
| TD-S3 | CLEAN + save (both paths): invalid transitions refused before I/O | `saveWhileCleanIsANoOpEvenIntoAnUnwritablePath` |
| TD-S4 | DIRTY→CLEAN by save ok; the new baseline is what discard returns to | `successfulSaveAdvancesTheBaselineSoDiscardKeepsTheSavedEdits` |
| TD-S5 | DIRTY→DIRTY by save fail; discard returns to the old baseline | `failedSaveKeepsTheDraftDirtyAndTheBaselineUnchanged` |
| TD-S6 | CLEAN + shortcuts with and without effect | `shortcutsThatChangeNothingKeepTheDraftClean` |
| TD-S7 | decision table of `changed()`: C1 capabilities, C2 comfort, C3 analyzer, C4 scene filter; any true → changed | `changedIsTrueWhenAnySingleDimensionDiffers` |

Coverage: all 9 valid transitions (0-switch, 100 %), 2 invalid transitions attempted, 5/5
decision-table rules (the 16-row table collapses to any-true plus all-false).

## 6. PaleOakPalette — BVA and decision table (`PaleOakPaletteBoundaryTest`)

| ID | Coverage items | Expected | Test |
| --- | --- | --- | --- |
| TD-P1 | `neutralWood`: red − blue 47/48/49; red = green − 1 / green / green + 1; green = blue − 1 / blue / blue + 1; alpha 0/1 | kept when red < green, green < blue or red − blue > 48; gray otherwise; alpha 0 untouched | `neutralWoodBoundaries` |
| TD-P2 | `applies`: six conditions, collapsed table (all-true + one false each) with width/height 0/1/256/257 | applies only when all hold | `appliesDecisionTable` |

Coverage: 11/11 boundary items; 11/11 rules. Before this design the red-vs-green and
green-vs-blue equalities had no test, and PIT reported both boundary mutants as survivors.

## 7. SceneFilterOptions — BVA and partitions (`SceneFilterOptionsBoundaryTest`)

| ID | Coverage items | Expected | Test |
| --- | --- | --- | --- |
| TD-F1 | IDs per list 511/512/513 | 512 inclusive | `idCountBoundaryIsFiveHundredTwelveInclusive` |
| TD-F2 | raw ID length 255/256/257, and 256/257 with leading spaces (the raw length is checked before trimming) | 256 inclusive | `idLengthBoundaryIsTwoHundredFiftySixRawCharactersInclusive` |
| TD-F3 | encoded text over the four lists at exactly 49,152 chars, 49,153, and the same characters without a separator | separators count once per list | `encodedTextBoundaryCountsSeparatorsAcrossAllFourLists` |
| TD-F4/F5 | path-segment partitions: `.`/`..`/empty segments rejected; dots inside segments and namespaces accepted | per `scene-filter.md` | `pathSegmentsMustBeNonEmptyAndNotDots`, `dotsInsideSegmentsAndNamespacesAreAllowed` |

Coverage: 3/3, 5/5, 3/3 boundary items; 9 invalid and 7 valid segment partitions.

## 8. Measured effect (white-box coverage, CTFL 4.3)

PIT over the five target classes with their previously existing tests, then with the designs
above added (same build, same mutators):

| | Mutations | Killed | Mutation coverage | Line coverage | Survivors |
| --- | ---: | ---: | ---: | ---: | ---: |
| Existing tests | 185 | 161 | 87 % | 94 % | 24 (incl. 4 uncovered) |
| With the designs | 185 | 179 | 97 % | 98 % | 6, all equivalent or covered elsewhere (§9) |

The five uncovered lines that remain are the ones §9 lists as equivalent or covered by a test outside
this measurement. The whole `common` gate (JUnit + JaCoCo + PIT, thresholds 96 %) stays green;
`SettingsLayout` joined the JaCoCo and PIT target lists with this change.

## 9. Equivalent mutants and residual survivors

Mutants that no test can kill because the mutated program is observationally identical:

| Class : line | Mutant | Why equivalent |
| --- | --- | --- |
| `SettingsLayout` : 51, 53 | `y < listTop` → `<=`, `y + rowHeight > listBottom` → `>=` | At equality the recomputed scroll equals the current scroll, so both branches return the same value. |
| `ResourceBorderPolicy` : 66 | `nx + ny + nz > 0` → `>= 0` | The sum of one face normal's components is ±1, never 0. |
| `DecorationLines` : 129 | `nx > 0` → `>= 0` inside `nx != 0` | Guarded: `nx` is ±1 there. |
| `CapabilityId` : 164 | `bitPosition() < 64` → `<=` | No capability sits at position 64; the enum has 61 entries. The guard exists for future additions and is exercised by `CapabilityRenderBitContractTest` structurally. |
| `OreExtensionCatalog` : 22 | `capabilityKey.length() > 128` → `>=` | A 128-character key never names a capability, so the call returns false either way. |
| `BlockLensConfigCodec` : 88 | negated loop conditions | Covered by `AnalyzerConfigurationTest`, which is in the gate but not in the five-class measurement above. |

Residual real gaps left for a later design (not equivalent): `AnalyzerEngine` lines 123, 165 and
223 (`CELL_LIMIT - cells >= limit`, `steps < STEP_LIMIT`, `cells >= CELL_LIMIT`) need a world
fixture whose cell budget lands exactly on the limit; the current fixtures reach the limit but not
its neighbours.

## 10. Findings (defect candidates and open questions)

Designing the boundaries surfaced behavior the tests do not pin because the test basis does not
decide it. Each needs a product decision before a test can assert it.

1. **Smallest accepted window shows no row (BVA, TD-L1).** `SettingsLayout.create` accepts
   height 140, but at 140 the list span is 26 px (2 px with the Resources action row) while the row
   height is clamped up to 42 px, so `visible(i)` is false for every row and `scrollToReveal`
   cannot help. Rows become visible only from height 156 (180 with the action row). Below 140 the
   constructor throws, and `AbstractBlockLensConfigScreen.init` calls it with the scaled window
   size without catching, so a scaled height under 140 would fail the screen. Minecraft's automatic
   GUI scale keeps the scaled size at or above 320×240 whenever the window is at least that large;
   smaller physical windows are the only way to reach this. Options: raise the minimum height to
   156/180 and clamp instead of throwing, or let the row height shrink below 42 at small heights.
2. **A trailing comma rejects the whole list (error guessing, TD-C5).** `scene.block_filter.blacklist=minecraft:stone,`
   yields an empty final element, which fails the ID grammar, so the line is dropped and the previous
   list is kept. This follows `scene-filter.md` ("invalid file entries retain the last valid rule"),
   but a hand-edited file loses a whole list over one character. Ignoring empty elements would be a
   one-line change in `parseList`; whether to make it is a specification decision.
3. **A UTF-8 byte-order mark hides the first line (error guessing).** `﻿deco.anvil=true` is read
   as the key `﻿deco.anvil`, which is unknown, so the line is ignored. Files written by BlockLens
   never carry a BOM; editors on Windows may add one. Stripping a leading U+FEFF before parsing is a
   one-line change.
4. **Full-width digits are accepted (TD-C4).** `others.handheld_size.blocks_percent=７０` decodes to
   70 because `Integer.parseInt` accepts any Unicode digit. Harmless; recorded so the behavior is
   known, not because it should change.

Items 1–3 are not changed by this document's tests; the tests assert the current behavior only where
the specification already states it (item 2) and leave items 1 and 3 unasserted.
