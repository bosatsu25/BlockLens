# ADR-001: One audited owned runtime container

**Status:** Implemented; local validation recorded below, exact-head hosted CI required before merge
**Date:** 2026-10-10
**Deciders:** BlockLens implementation and independent review, within the authorized issue implementation scope

## Context

The five #34 analyzers preserve the previous 46 capabilities and add bounded loaded-client inspection. Initial ordinary flat JARs measured 176470 / 177177 / 177262 bytes for 26.1.2 / 26.2 / 26.3, above the unchanged 153600-byte product ceiling. Feature removal and stripping debug information are not acceptable size reductions.

An outside-tree, unpublished 26.3 packing prototype retained the existing class bytes in a STORED inner ZIP and compressed that complete container in the outer ZIP. It measured 119356 bytes with a 338534-byte inner container. This is packaging evidence only, not a supported artifact or a startup/performance claim.

## Decision

Keep the BlockLens outer ID, entrypoints, mixin configuration, assets and resource-pack identity. Place all owned native/shared/common bytecode into exactly `META-INF/jars/blocklens-runtime.jar`. The inner archive uses STORED entries; the outer JAR uses ordinary DEFLATE, allowing compression across repeated class headers and constant-pool strings. Preserve every compiled byte, including source/debug information.

The inner archive is an owned client-only Fabric library, ID `blocklens-runtime`, the same artifact version and exact Minecraft target, with no entrypoints, assets, third-party code or additional nested archives. The outer metadata declares its fixed `jars` path and requires the same library version. Fabric Loader performs the standard loading; introduce no custom class loader or runtime reflection/discovery/network behavior.

Only this exact owned container is allowed. Inspect both archive layers for required classes, retired policies, test/residue/private markers, duplicate or unsafe entry paths and bounded decoded sizes. Verify every packaged class byte against normal compile output. No extra dependency JAR is allowed, and all existing size limits remain unchanged.

## Options considered

- Keep individual compressed class entries: simplest structure, but measured artifacts exceed the product ceiling.
- Strip debug data or remove functionality: would harm diagnosis or capability parity; rejected.
- Consolidate repeated code alone: still useful for maintainability, but does not address repeated ZIP/class headers across the existing product.
- One owned STORED container inside standard Fabric nested-JAR packaging: adds packaging/audit obligations while preserving all compiled behavior and debug information; selected for verification.

## Consequences and validation

Fabric Loader extracts/loads a larger uncompressed internal archive through its usual nested-mod cache. The smaller download is not evidence of faster startup, lower heap use or improved rendering. Record packaged launch and M8 observations separately; retain the current coarse performance guards.

Real-client GameTests must remove development main/common directories and common JARs from their classpath, add the audited outer artifact, and retain only test and Minecraft/Fabric dependencies. Prove outer origin matches that artifact, inner origin belongs to BlockLens, and representative native/common classes actually load from the inner archive. This remains a Fabric development-environment GameTest of packaged contents, not a claim about production remapping.

Required before acceptance: three-target builds and byte/parity/privacy audits; unchanged size budgets; reproducible outer/inner artifacts; all three packaged UI/visual/lifecycle/performance suites; independent review; exact-head hosted CI. Keep shader/third-party-pack/Vulkan verification in #31.

## Local verification record

The legacy 46-capability product passes clean `ciGate`: 172 common tests, selected JaCoCo line coverage 761/774, PIT 265/274 killed mutations, all three builds, unchanged byte budgets and byte-for-byte class audits. Archive contracts include 25 negative controls for unsafe paths, directory payloads, private/test residue, decoded entry/count/total bounds, unexpected compression/containers, metadata mismatches and missing/changed classes. The independent reviewer also compared every class against compile output: 68/69/69 classes, all equal.

| Minecraft | Outer bytes | Inner bytes | Outer SHA-256 |
| --- | ---: | ---: | --- |
| 26.1.2 | 99811 | 266076 | `cf03679c95dfb5a5ea9147b2238b210c70bb71a3a2e80405e0d9f7a28a0dbb04` |
| 26.2 | 100061 | 267453 | `31804ba5121d136fd7414b7087d1e1129f59d3943a4ecd8b0fb08a8c215a9fc8` |
| 26.3 | 100072 | 267776 | `063e33b087a5c0080cfdd3ff627ce722bfed48bc3231e538b5222b75cb683d97` |

Both outer and inner SHA-256 values match before and after clean rebuilds. These are Windows observations; hosted artifact hashes and sizes are separate evidence. All three visible packaged-client suites completed with origin checks, ten settings screenshots, 32 lightweight visual screenshots, reload/lifecycle/parity checks and unchanged M8 guards. No startup or rendering improvement is claimed from the smaller archive. Current-head hosted results must be checked on the associated PR before merge; this prerequisite does not complete #34's new analyzers or close reopened #43.

## References

- [Fabric nested JAR metadata specification](https://wiki.fabricmc.net/documentation:fabric_mod_json_spec)
- [Pinned Loader 0.19.5 nested discovery](https://github.com/FabricMC/fabric-loader/blob/0.19.5/src/main/java/net/fabricmc/loader/impl/discovery/ModDiscoverer.java)
- [Fabric classpath group boundaries](https://docs.fabricmc.net/develop/loom/classpath-groups)
- [Fabric production-run API](https://docs.fabricmc.net/develop/loom/production-run-tasks)
