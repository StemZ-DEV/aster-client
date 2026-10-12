# Aster 1.0 release plan

## Scope

Ship a client-only Fabric mod for Minecraft 26.3 and Java 25. Keep Aster Flow's frameless command rail, dark charcoal fade, and muted orange accent. Support gameplay, title/options menus, and the pause screen. Include Auto Sprint, Fullbright, Zoom, Coordinates, Performance, and Keystrokes.

Complete profile management with named snapshots, rename, and confirmed deletion. Preserve independent settings and shortcuts. Make module bindings, keyboard navigation, and settings recovery reliable. Package the mod, source archive, licenses, install guide, changelog, screenshots, and SHA-256 checksums.

## Release gates

- Unit tests cover persistence, profile operations, limits, key validation, corrupt recovery, and preservation of future configuration formats.
- Native client tests cover search, toggles, inline adjustment, binding capture, profile management, focus, compact-window scrolling, menus, gameplay, and pause restoration.
- Runtime checks cover zoom and Fullbright activation and deactivation, plus held-key module toggles.
- Build the production JAR with version 1.0.0; verify metadata, licenses, and absence of test code or Minecraft classes.
- Produce a repeatable release bundle and source archive without runtime logs, saves, caches, or credentials.
- Configure CI and a tag-triggered public release workflow. Publish to the agreed GitHub destination if one is available.

## Deferred

Additional modules, remote configuration sharing, account services, a launcher, and compatibility with other Minecraft versions are outside v1. English is the supported interface language. Windows/Linux build verification is delegated to CI; native graphical verification runs on macOS.

## Status

The scoped implementation and local release gates are complete: 13 unit tests, native macOS UI/runtime verification, and archive inspection. The public release is gated by the tagged GitHub workflow's Windows, macOS, and Linux checks. Verification results are available in GitHub Actions.
