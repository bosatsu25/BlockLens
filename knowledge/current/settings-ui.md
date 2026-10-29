# BlockLens settings UI

Status: native settings support all three Minecraft targets; the v0.4.6 change adds Apply without closing and Visibility-OFF defaults. Verification for this change is tracked in Issue #76.

## Scope

The native settings screen groups all 61 current BlockLens capabilities into four categories: Decoration (13), Resources (22), Visibility (9), Other (17).
The original 40 source keys and bit positions stay unchanged. All nine Visibility controls default OFF in new or partial configurations. Explicitly saved values remain unchanged. The historical source preset stays frozen as reference evidence. The six visual/comfort additions, five analyzers, two scene filters and four Builder Assist controls are documented in their current feature specifications. Crying Obsidian,
Nether Gold Ore and Nether Quartz Ore remain independent Resource options after the original 18.

The uploaded UI reference pack defines its option hierarchy in respackopts.json5; it does not
supply custom GUI textures. Its current upload has SHA-256
070bc338936f89320408e7b78bb6f74856917f5944deecc1a1b52390f5363616 and is 3,527,738 bytes.
This is a UI reference, not a replacement for the repository's frozen behavioral/size baseline.
No third-party pack assets or RPO parser are added to the runtime JAR.

## Controls

Open the settings with the existing B key binding or the optional Mod Menu entrypoint.
Select a category to view its option rows. Each row shows a name, a short explanation and
a separate localized Enabled/Disabled button. The tooltip and keyboard narration include
the complete name. Long names are ellipsized; descriptions wrap within the row.
All categories share the same local draft, with a separate bounded scroll position per category.
A dark panel behind the list and status area keeps names and explanations readable on bright
menu/world backgrounds. Framebuffer tests check the complete viewport and a bounded dark backdrop.

| Action | Behavior |
| --- | --- |
| Apply | Save changed edits, install the snapshot, invalidate terrain and reload resources when required; stay on the same settings screen |
| Save and close | Perform the same save/apply, then return to the parent |
| Discard | Return to the parent without saving or applying subsequent edits |
| Esc | Discard edits since the last successful apply and return to the parent |
| Tab / Shift+Tab | Move through categories, all options and footer actions; reveal focused rows |
| Mouse wheel / Page Up / Page Down / Home / End | Scroll the selected category |

Esc discarding is an intentional change from the previous close-to-save screen.
Settings keys, existing persisted values and rendered capability behavior are preserved. Visibility defaults change deliberately from six ON / three OFF to nine OFF. String Tweaks in Other remains ON, so new configurations have one enabled capability. Applying establishes a new saved draft baseline; later discard/Esc does not undo it. Applying or closing an unchanged draft performs no write or terrain invalidation.
A save failure retains the screen and draft, leaves runtime settings untouched and displays a
retry/discard message in a reserved area above the footer. No exception details or paths are displayed.

## Source pack ON

A header action beside the title, on every category tab, enables the source resource pack's 37 options:
13 Orientation & State, 18 Resources, 4 Visibility and 2 Other, the first 37 capabilities
(`CapabilityId.isSourcePackCapability`). Every other capability and option keeps its value. The source
pack itself defaults all 37 options off in its Respackopts configuration, as BlockLens does; this action
is the one-step equivalent of switching all of them on there. Like the shortcuts below, it edits the
shared draft only: Apply/Save and close saves it, Esc/Discard drops it, and repeating it on an
unchanged draft causes no write. The action is one column of the Resources shortcut grid wide, is first
in Tab order, and the settings visual GameTest requires its English and Japanese labels to fit without
touching the title or the tabs at 320 x 240.

## Resources shortcuts

Resources has a separate row below the category tabs with All OFF, Obsidian pair ON, and the existing Ore glow ON.
All OFF disables all 22 Resources controls, including Connected Ore Edges. Obsidian pair ON enables only Obsidian and Crying Obsidian while preserving every other flag and option.
These actions edit the shared local draft only. Apply/Save and close saves the snapshot; Esc/Discard drops edits since the last successful Apply. Repeating an unchanged shortcut does not cause a write.
The row remains visible while the resource list scrolls. Category and scroll are preserved, all three actions are keyboard reachable, and compact English/Japanese labels must fit without overlapping the title, tabs, list or footer.
The 200 KiB release ceiling remains unchanged. Outer JAR packaging omits only empty, uncommented directory records that have retained file descendants; every file payload and debug byte stays exact. Intentional empty directories, comments and directory payloads remain intact. The archive contract checks implicit directory lookup via the JDK ZIP filesystem, payload/metadata equality, deterministic repeated bytes and failure cleanup; all three packaged clients must verify runtime resource access.

Issue #80 tracks the three-version native UI and current-head CI verification for this addition.

## Architecture

SettingsCatalog, SettingsDraft and SettingsLayout contain Minecraft-independent policies in common.
The Minecraft-facing AbstractBlockLensConfigScreen is compiled from fabric-shared for all three targets.
The version-specific BlockLensConfigScreen adapters keep the distinct 26.1.2 and modern 26.2/26.3 screen APIs.
GameTest scenarios and fixtures remain shared. Version-specific test sources include a small
SettingsClientAccess bridge for the loading-overlay API, which moved from Minecraft.getOverlay()
to Minecraft.gui.overlay() in 26.2. The repository contract restricts each local GameTest tree to
the overlay bridge and named narrow render probes. Every local probe is bounded to 35 lines; complete oracles stay shared. Screenshot evidence waits for the overlay to clear and uses actual window resizing
with GUI scale 1, checks the complete bounded viewport, and restores the original window/options.
No new required UI dependency, telemetry, startup network call, registry scan or renderer is introduced.

## Verification and limitations

Required verification includes common JUnit/JaCoCo/PIT, three-version build, client GameTests,
settings persistence/failure recovery, keyboard reachability at 320×240 / 640×360 / 854×480 /
1920×1080, actual English/Japanese framebuffer evidence, JAR structure/size and reproducibility.

The historical M7 five-control visual preset is now constructed explicitly in test-support, independently of product defaults. Its pure regression verifies the exact five source keys; the native oracle retains its independent preset assertion. Test-support classes remain excluded from distributed runtime JARs.

The reviewed three-version baseline adjustment is recorded in [26.3 integration](minecraft-26-3.md); the newly uploaded ZIP
does not redefine them. CI results must be tied to the current PR head.

Historical UI redesign compatibility note (superseded by the scoped delivery evidence in [remaining delivery](remaining-delivery.md)):

Uploaded compatibility references include the UI reference pack, Small Handhelds, NewGlowingOres,
LowOnFire and both Chise packs. Their static resource domains and metadata were inspected,
but those particular uploads have not been exercised together in a real client.
Their pack-format declarations alone do not establish compatibility with Minecraft 26.1.2/26.2/26.3.
Pixel-equivalence with the source options screen, shader support and Vulkan support
remain unverified. Existing synthetic active-pack/render GameTests remain regression gates.

The three-version integration requires passing current-head CI. Pack combinations, shaders and Vulkan are separate open compatibility tracks; do not claim them from passing native UI tests.

Shared input code uses InputConstants so SDL-based 26.3 and earlier GLFW-based targets retain keyboard/mouse semantics. The local visible-window runner and per-version CI screenshot evidence are described in [26.3 integration](minecraft-26-3.md).

## References

- [PR #40](https://github.com/bosatsu25/BlockLens/pull/40)
- [Respackopts official gallery](https://modrinth.com/mod/respackopts/gallery) describes
  Cloth Config's default appearance; its 2022 screenshots are not a capture of the uploaded pack.
- [Source historical baseline](source-baseline.md)
- [Current additive capability scope](chisetweaks-migration.md)

## Numeric detail controls

Low Fire and Handheld Size rows include a separate Details button. The child screen shares the parent draft. Fire offers three presets; held categories use explicit decrease/increase controls bounded to 40–100 percent. Child Back/Esc returns to the parent without writing. Parent Discard/Esc discards all edits, including numeric-only edits. Parent Apply or Save and close persists and installs the complete snapshot; failed saving retains it for retry. Mouse actions revalidate focus after preset widget rebuilds, and keyboard navigation uses current widgets. The real client oracle checks numeric-only edits, compact bounds, mouse preset selection, return/Esc, discard and persistence.

## Builder Assist session panel

The four Builder Assist rows include an Open button while a client world is loaded.
The panel receives the parent screen and navigation callback, never the SettingsDraft.
Inspection uses only saved runtime toggles; enable a tool and Apply first.
Back/Esc returns to the same parent draft without writing settings. The dedicated
Builder Assist key is unbound by default, preserving the existing B settings binding.
Reference selection and clearing affect only the current session. The nonpausing panel
supports wheel, Page Up/Down and Home/End scrolling, with English/Japanese compact
framebuffer evidence. See [builder-assist.md](builder-assist.md).
