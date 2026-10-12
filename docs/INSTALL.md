# Install Aster 1.0

## Requirements

- Minecraft Java Edition **26.3**.
- Fabric Loader **0.19.5 or newer** for Minecraft 26.3.
- Fabric API **0.162.0+26.3** or a compatible newer build for the same Minecraft version.
- Java **25**.

## Install

1. Create or select a Minecraft 26.3 Fabric profile in your launcher. The [Fabric installer](https://fabricmc.net/use/installer/) can create one in the official launcher.
2. Download `aster-client-1.0.0.jar` from [Aster's releases](https://github.com/StemZ-DEV/aster-client/releases). The source JAR and ZIP files are for developers; install only the main JAR.
3. Put the Aster JAR and the matching [Fabric API](https://modrinth.com/mod/fabric-api) JAR in that profile's `mods` folder.
4. Launch the Fabric profile. Select **Open Aster** on the title screen, or press **Right Shift** in gameplay or a menu.

The release ZIP includes the same mod JAR, screenshots, licenses, and documentation. It does not include Minecraft or Fabric API.

## First use

Start typing to find a module. Use Up/Down and Enter, or click a row to toggle it. Select the sliders icon to tune its value and bind a toggle key. Hold **C** to zoom. **Escape** clears an active search, cancels a confirmation, or returns to the previous screen.

Use Profiles to save a snapshot, edit its name, and press Enter or Rename. Delete asks for a second click; Escape cancels. At least one profile must remain. Settings offers reduced motion and a confirmed reset of the current profile.

Opening Flow from gameplay leaves the world running. Opening it from the pause menu preserves singleplayer's paused state. Multiplayer continues to run according to Minecraft's normal behavior.

## Settings and updates

Settings are local to the selected game profile at `config/aster/client.json`. Back up that file to move your named profiles to another installation. Replace the mod JAR to update; remove older Aster JARs first. Version 1.0 reads the earlier development config format and clamps obsolete Sprint thresholds to the new whole-point range.

Malformed settings are preserved as `client.json.backup`, with numbered backups when one already exists. A config written by a newer Aster format is protected from overwriting; use the matching newer mod or move the file aside to start with defaults.

## Troubleshooting

- **Incompatible mod set:** check Minecraft 26.3, Java 25, Loader, and Fabric API versions; do not mix Fabric API builds from another Minecraft release.
- **Right Shift does nothing:** check Minecraft's Controls → Aster bindings. The shortcut respects text fields and key-binding capture. Try the title-screen entry.
- **Toggle key does nothing:** toggle keys work while playing with no menu open. Flow's open key and hold-to-zoom key are reserved, including after rebinding them in Controls.
- **Settings are not saved:** the footer shows a write error. Check that the game's config folder is writable. Future-format configs remain protected until resolved.
- **Other mod conflict:** reproduce in a profile with only Aster and Fabric API, then [open an issue](https://github.com/StemZ-DEV/aster-client/issues) with versions, reproduction steps, and a relevant log excerpt.

English is the supported UI language in v1. Other Minecraft versions and third-party mod combinations have not been validated. Choose modules according to the rules of the server you play on.
