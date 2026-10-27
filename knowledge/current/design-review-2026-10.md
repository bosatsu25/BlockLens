# Design review against the original resource pack — 2026-10

Status: **current review record**; decisions it made are specified in the linked authoritative documents.

Question reviewed: does BlockLens provide the same function as the source resource pack, with no
display or behavior difference between Minecraft 26.1.2, 26.2 and 26.3, while being lighter than the pack?

## 1. Verdicts

| Area | Verdict | Evidence / action |
| --- | --- | --- |
| Supplied archive identity | Same capability content as the M0 baseline | 36/37 gate path-set hashes identical; `deco.slabs` differs by an orphan sidecar only. Recorded in [`source-baseline.md`](source-baseline.md). |
| Catalog parity (keys, targets, states) | Proven | 37 keys / 323 bindings, `SourceRawParityContractTest`. |
| Information parity (what a player can read) | **Was not met**; now met for single-variant models with listed residuals | Face tints sat on hidden faces and had little contrast. Decal grammar in [`pack-information-parity.md`](pack-information-parity.md). |
| Cross-version behavior | No behavior divergence found; **structural drift risk** | 7 adapter files were identical between the 26.1.2 and 26.2 trees, 2 more nearly so. Now single-sourced and guarded. |
| "Lighter than the pack" | Artifact size: proven. Load/runtime speed: **not measured** against the pack | See §3. |
| Full rewrite | **Not justified** | Architecture (pure `common` policy, thin adapters, model wrapper) fits the goal; 411 unit tests, 98% mutation kill rate and per-version client GameTests would be discarded. |

## 2. Cross-version structure

Before this review, `versions/mc26_1_2` and `versions/mc26_2` each carried their own copy of the state
adapter, model plugin, target index, Nether overlay registration and Mod Menu bridge; the pre-26.3
item-render and shade bridges existed twice. Differences were comments, a version string and one key-code
spelling. Nothing diverged yet, but every future fix had to be applied twice by hand.

Now: one copy in `fabric-shared`, a `pre26_3` root selected by Gradle for 26.1.2/26.2, and
`VersionAdapterSingleSourceContractTest`, which fails on identical code in two version roots, on
shadowing, and on version bridges over 60 lines. The planned 26.1.2 retirement becomes deleting one tree.

## 3. Performance and memory claims

A resource pack's cost is paid at load (parse 3,135 extra JSON files and stitch 1,020 extra sprites into
the block atlas) and is zero per frame afterwards. BlockLens avoids that load work
and atlas memory, but adds work when chunk sections are meshed with a feature enabled: a quad transform
per wrapped block and, for decals, one extra emission of the decorated block's own quads.

So the defensible claims are:

- runtime JAR ~0.1–0.2 MB vs 2.4–3.9 MB archives (measured per version in CI);
- no source-pack JSON parsing or sprite stitching at startup/reload (structural, contract-tested);
- **not** "faster than the pack" at load or while rendering: no head-to-head measurement exists. M8 bounds
  BlockLens against regressions only. Adding a pack-vs-mod M8 scenario is the prerequisite for that claim.

## 4. Open recommendations

1. **Capability mask headroom.** `CapabilityId.bitPosition()` uses bits 0–62 with 46 and 56 reserved; only
   bit 63 remains. The capability after next overflows the 64-bit mask. Move the runtime mask and codec to
   a two-word or `long[]` mask before adding capability 63.
2. **Source-text contract tests.** Many tests assert exact Java source strings. They protected verified
   decisions, but a pure file move required editing eight test files. Prefer behavior-level or
   structural checks (as in `VersionAdapterSingleSourceContractTest`) for new guards.
3. **Anvil arrow.** The source's top-face arrow is not reproduced because its world direction depends on
   an unverified vanilla UV rotation; verify against the vanilla model before adding it.
4. **Scope vs "lightweight".** The product grew from 37 to 61 capabilities and the JAR ceiling from 100 KiB
   to 200 KiB, and on 2026-10-27 to 250 KiB for the decal layer (the compacted layer adds about 6.3 KB; v0.4.7
   left only 1.4 KB under 200 KiB on 26.3). These are explicit product decisions, but the original
   motivation was a lighter pack; keep size and the pack-vs-mod measurement in view when adding capabilities.

## 5. Verification of this change

- `:common:qualityGate` (local and hosted CI on the pull request head): 411 tests, 729 mutations with 711
  (98%) killed, test strength 98%.
- `scripts/` unit suite: 118 tests.
- Three-version build, client GameTests (including `DecorationDecalOracle`), artifact budgets and
  shader/backend matrices: pull request CI on the current head.
