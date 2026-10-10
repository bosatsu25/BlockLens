# Bounded ore extension contract

Status: implementation in progress for Issue #37; current-head three-version verification is required before advertising completion.

## Supported inputs

The original Minecraft target catalogs, config keys and independent controls remain unchanged. Extensions only bind exact **non-`minecraft` block identifiers** to existing resource-highlight controls and reuse the active baked model.

Two inputs are supported:

1. Explicit API v1 registration during client initialization, before the first model bake: `OreHighlightExtensions.register(1, "example:ore", "gaming.diamond_ore")`. The return value reports whether the request was accepted. No classpath discovery, reflection, runtime dependency on an ore mod, or automatic feature registration is involved.
2. Synchronized conventional block tags received through Fabric's `CommonLifecycleEvents.TAGS_LOADED` client event. The fixed material tags are `c:ores/coal`, `c:ores/copper`, `c:ores/diamond`, `c:ores/emerald`, `c:ores/gold`, `c:ores/iron`, `c:ores/lapis`, `c:ores/netherite_scrap`, `c:ores/quartz`, and `c:ores/redstone`. The matching deepslate control is selected when the entry belongs to `c:ores_in_ground/deepslate`; gold in `c:ores_in_ground/netherrack` uses Nether Gold Ore. Quartz and netherite scrap use Nether Quartz Ore and Ancient Debris. An ambiguous ground or material assignment is disabled for that target.

Tags supplied by the connected world's existing data packs are sufficient; BlockLens does not require a server installation. An absent tag provides no extra targets. The broad `c:ores` tag alone does not specify a material and is not inferred from names. Locally cached client-tag fallback is not advertised: Fabric's implementation recursively reads installed mods without a caller-controlled work bound. Server data-pack changes take effect when their updated tags reach the client.

## Bounds and isolation

- At most 256 distinct explicit identifiers, each at most 128 characters. Only existing resource capability keys are accepted.
- An identical repeated registration is idempotent. Conflicting explicit registrations disable only that identifier, independent of registration order. Unsupported API versions, invalid identifiers, vanilla identifiers, unsupported capability keys, over-limit requests and late registrations return `false`; previously accepted unrelated registrations survive.
- Explicit decisions take precedence over conventional tags for the same identifier, including an explicitly rejected conflict. This avoids double tinting and does not silently reinterpret the caller's chosen control.
- Tag ingestion inspects only the ten fixed material tags, with at most 512 members per tag and at most 256 distinct non-vanilla candidates in one generation. An over-limit tag generation disables the conventional-tag integration for that generation; explicit registrations and original capabilities continue normally. There is no arbitrary prefix/suffix inference or partial truncation to a registry-dependent subset.
- A target may have at most 64 block states. At most 256 resolved extension targets and 4096 extension state models are retained across both inputs. A combined target overflow disables the conventional generation and preserves explicit targets. Unsupported state counts or missing registered blocks are skipped independently; the shared state-model budget is assigned to explicit targets first and then to conventional targets in identifier order.

## Rendering and lifecycle

Explicit registrations are frozen at the first model bake. Both explicit and conventional bindings are resolved by exact registry lookup on the client tick when a world and its active models are available. They share one bounded retained-model map and the existing resource-highlight wrapper. A compile-time hook selects those wrappers while terrain sections are built; item, inventory and block-entity rendering are outside this ore extension contract. There is no registry scan or tag lookup per frame, and applying a changed tag does not request a resource reload. The original vanilla target index remains separate and unchanged.

Changed effective bindings or a new baked-model generation rebuild the retained extension wrappers and invalidate terrain. Equivalent tag updates leave them intact. Removing a tag restores the base model for its conventional targets while explicit targets remain active; resource reload rebuilds from the new active base; disconnect releases tag and model snapshots. The frozen explicit identifier declarations are session-wide and are resolved again for the next world. Scene-filter suppression remains earlier in section compilation and retains precedence over emitted geometry.

Only the pinned Fabric renderer API on Minecraft 26.1.2, 26.2 and 26.3 is the integration boundary. Named external mods/renderers, shader packs and Vulkan require separate evidence; Issue #31 remains the authority for those combinations.

## Verification contract

Pure tests cover API version/identifier/capability validation, idempotence/conflict order, bounds, immutable freeze, material/ground selection and overflow isolation. Three native clients must prove explicit and conventional custom ore targets, missing/unsupported controls, tag add/remove/overflow/recovery, resource reload, preservation of an active non-cube base model, scene-filter composition and disconnect cleanup. Common quality gates, full native regressions, M8, reproducibility, packaged runtime provenance and the user-authorized 204800-byte release ceiling remain required. Reports must identify the exact tested commit and artifact bytes.

## Primary API sources

- [Fabric synchronized tag lifecycle](https://github.com/FabricMC/fabric-api/blob/0.162.0%2B26.3/fabric-lifecycle-events-v1/src/main/java/net/fabricmc/fabric/api/event/lifecycle/v1/CommonLifecycleEvents.java)
- [Fabric conventional material and ground tag names](https://github.com/FabricMC/fabric-api/blob/0.162.0%2B26.3/fabric-convention-tags-v2/src/main/java/net/fabricmc/fabric/api/tag/convention/v2/ConventionalBlockItemTags.java)
- [Fabric local tag loader bounds and lifecycle](https://github.com/FabricMC/fabric-api/blob/0.162.0%2B26.3/fabric-tag-api-v1/src/client/java/net/fabricmc/fabric/impl/tag/client/ClientTagsLoader.java)
