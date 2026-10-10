# BlockLens settings UI

Status: settings redesign merged in PR #40; three-version integration is the development target and is not a published release.

## Scope

The native settings screen groups all 40 current BlockLens capabilities into four categories: Decoration (13), Resources (21), Visibility (4), Other (2).
The original 37 source keys and their bit positions stay unchanged. Crying Obsidian,
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
| Save and apply | Save changed edits, install the saved snapshot, invalidate terrain, then return to the parent |
| Discard changes | Return to the parent without saving or applying |
| Esc | Discard changes and return to the parent |
| Tab / Shift+Tab | Move through categories, all options and footer actions; reveal focused rows |
| Mouse wheel / Page Up / Page Down / Home / End | Scroll the selected category |

Esc discarding is an intentional change from the previous close-to-save screen.
Settings keys, existing persisted values, the five enabled defaults and the rendered capability
behavior are unchanged. Closing an unchanged draft performs no write or terrain invalidation.
A save failure retains the screen and draft, leaves runtime settings untouched and displays a
retry/discard message in a reserved area above the footer. No exception details or paths are displayed.

## Architecture

SettingsCatalog, SettingsDraft and SettingsLayout contain Minecraft-independent policies in common.
The Minecraft-facing AbstractBlockLensConfigScreen is compiled from fabric-shared for all three targets.
The version-specific BlockLensConfigScreen adapters keep the distinct 26.1.2 and modern 26.2/26.3 screen APIs.
GameTest scenarios and fixtures remain shared. The sole version-specific test source is a small
SettingsClientAccess bridge for the loading-overlay API, which moved from Minecraft.getOverlay()
to Minecraft.gui.overlay() in 26.2. The repository contract restricts each local GameTest tree to
that one bridge. Screenshot evidence waits for the overlay to clear and uses actual window resizing
with GUI scale 1, checks the complete bounded viewport, and restores the original window/options.
No new required UI dependency, telemetry, startup network call, registry scan or renderer is introduced.

## Verification and limitations

Required verification includes common JUnit/JaCoCo/PIT, three-version build, client GameTests,
settings persistence/failure recovery, keyboard reachability at 320×240 / 640×360 / 854×480 /
1920×1080, actual English/Japanese framebuffer evidence, JAR structure/size and reproducibility.

The reviewed three-version baseline adjustment is recorded in [26.3 integration](minecraft-26-3.md); the newly uploaded ZIP
does not redefine them. CI results must be tied to the current PR head.

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
