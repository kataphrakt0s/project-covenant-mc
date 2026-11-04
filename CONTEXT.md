# Project Covenant context

## Current purpose

Develop a Minecraft Java Edition mod for **26.1.2** using **NeoForge**.
ATM11 is the eventual pack target; pack integration is outside the current scope.
The first UI foundation is a client-only radial menu: hold **G** (rebindable under
Controls > Key Binds > Project Covenant) to open ten numbered placeholder slots.
Slot 1 starts at the top; slots proceed clockwise. Mouse hover highlights a slot;
release the binding or press Escape to close. It does not pause the world or
execute actions. It opens only in a living player's game, outside other screens,
and closes on loss of focus. No blocks, items, recipes, or world changes exist yet.

## Identity and toolchain

- Working title: Project Covenant; mod ID: `covenant`.
- Developer: `kataphrakt0s`.
- Mod logo: `src/main/resources/mod-icon.png`, referenced by `logoFile` in mod metadata.
- Java package: `dev.covenant`; entry point: `Covenant.java`.
- Version: `0.1.0-dev`; license remains All Rights Reserved until chosen otherwise.
- Minecraft: `26.1.2`, exact version range.
- NeoForge: `26.1.2.125`; ModDevGradle: `2.0.148`; Gradle wrapper: `9.2.1`.
- JDK: 64-bit Java 25. Machine-specific locations are in ignored
  `.local/environment.json`. Scripts set Java and Gradle home for their process;
  the Windows default Java is not the project's toolchain.
- The MDK's Foojay resolver provisions Java 21 for asset tooling; compilation
  and game launches still use Java 25.
- Initialized from the official
  [26.1.2 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle),
  upstream revision `5861593c1f4ff5f1b181ba37118a4db0e0c3b0a1` observed during setup.
  The example gameplay content was replaced with a minimal mod entry point.
  `TEMPLATE_LICENSE.txt` preserves the upstream template license.

## Ownership and boundaries

- `src/main/java`: common mod entry point, with input/screens isolated in
  `dev.covenant.client` behind a physical-client-only mod entry point.
- `src/geometryTest/java`: dependency-free wheel geometry checks, run by
  `radialMenuTest` as part of `check` and `build`.
- `src/main/resources`: authored assets/data.
- `src/main/templates/META-INF/neoforge.mods.toml`: build-expanded metadata.
- `src/generated/resources`: data generator output when generators are introduced.
- `scripts`: local build, deploy, and diagnostics.
- `skills/covenant-development`: reusable workflow for future Codex sessions.
- `.local`: machine configuration and local validation artifacts; ignored.
- `.tools`: downloaded JDK and setup artifacts; ignored.
- `run`: Gradle development game files; Prism has a separate game directory.

Prism development instance ID is `Covenant-26_1_2-Dev`, displayed as
`Project Covenant - Dev (26.1.2)`. It is separate from the existing vanilla
`26.1.2` and ATM11 instances. Deploy scripts check the instance ownership marker
and pinned versions before copying the built JAR.

## Validation status

See `docs/VALIDATION.md` for verified results and outstanding manual checks.
Use `docs/DEVELOPMENT.md` for the daily test loop.
