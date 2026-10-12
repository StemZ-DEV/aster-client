# v1 verification

Local verification was performed on macOS with Java 25, Minecraft 26.3, Fabric Loader 0.19.5, and Fabric API 0.162.0+26.3.

- **13 unit tests:** persistence, independent profiles, rename and duplicate names, active-profile deletion and last-profile protection, profile limits, corrupted file recovery, retention of older backups, future-format protection, imported key validation, failed writes, numeric limits, and search filters.
- **Native client test:** gameplay/title/options/pause contexts, keyboard selection, scaled pointer input, search, inline sliders, reserved-key rejection, binding cancellation, named profile editing, confirmed deletion/cancellation, Minecraft key-binding capture, smaller-window scrolling, and parent restoration.
- **Runtime checks in a disposable world:** actual camera FOV narrows on zoom and restores on release; Fullbright modifies lightmap extraction and restores it without changing saved gamma or player effects; module keys toggle once per press; Auto Sprint starts and releases its sprint state.
- **Visual inspection:** native 1440×900 and 960×540 screenshots of gameplay, menus, inline settings, named profiles, settings, and the HUD. The previews in `docs/screenshots` come from the running mod.
- **Artifact inspection:** metadata, version, licenses, checksums, and absence of test code, Minecraft binaries, runtime logs, saves, and caches in distributed artifacts. Run `python tools/VerifyRelease.py` after packaging.

GitHub's tagged Release workflow additionally requires build/unit/artifact checks on Windows, macOS, and Linux, plus native client testing under Linux/Xvfb before preparing the release. See [workflow runs](https://github.com/StemZ-DEV/aster-client/actions) for the actual public results.

Compatibility with other mods, other Minecraft releases, multiplayer latency conditions, and translated interface languages is outside this validation scope.
