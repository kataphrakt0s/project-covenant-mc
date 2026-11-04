# Development

Use PowerShell 7 from the project directory. The scripts select the project JDK
and isolate Gradle's cache, avoiding the default Java 24 installation.

```powershell
.\scripts\Dev.ps1 doctor   # Java, Gradle, Prism, instance pins
.\scripts\Dev.ps1 build    # Compile/check and package build/libs/covenant-0.1.0-dev.jar
.\scripts\Dev.ps1 test     # Dedicated GameTests for RPG data (separate from build)
.\scripts\Dev.ps1 deploy   # Build and copy the JAR to the isolated Prism instance
.\scripts\Dev.ps1 prism    # Build, deploy, and launch through Prism
.\scripts\Dev.ps1 client   # Launch the source-based Gradle development client
.\scripts\Dev.ps1 server   # Launch the development dedicated server
.\scripts\Dev.ps1 data     # Run data generation once providers are added
```

Close the development game before deploying another build. Prism's instance is
`Project Covenant - Dev (26.1.2)`; it contains only NeoForge and this mod. The
deployment script replaces only `covenant-*.jar` in that marker-owned instance.
Other mods and worlds are retained. New dependencies require explicit setup.

## In-game smoke check

1. Launch with `scripts/Dev.ps1 prism` or double-click the development instance.
2. In Mods, confirm **Project Covenant**, mod ID `covenant`, version `0.1.0-dev`.
3. Create a disposable Creative world. Enter it, move, break/place a vanilla block,
   save and quit, then reopen it.
4. Hold **G**: confirm a centered wheel with exactly ten numbered wedges. Hover
   each wedge and confirm the matching Option 1-10 label. The center and outside
   the ring have no selection. Release G: the wheel closes and mouse look resumes.
   Clicking a placeholder must have no effect. The world continues while open.
5. Check quick taps, repeated holds, Escape while G remains held (no reopening
   until a fresh press), and switching away from the window. Chat, inventory,
   pause, and the title screen must not open the wheel. Rebind Hold Radial Menu
   in Controls > Key Binds > Project Covenant, including a spare mouse button,
   and verify hold/release again. Check different window sizes and GUI scales.
6. Check the instance's `minecraft/logs/latest.log` for
   `Project Covenant loaded (mod id: covenant)` and errors relevant to the mod.

Prism uses your existing launcher account. If Microsoft authentication expires,
sign in through Prism. Project scripts do not read or manage account credentials.

## New machine or moved checkout

Install Prism and a **64-bit JDK 25**. Java 24 and Java 21 are unsuitable for this
target. A portable JDK is fine. The Gradle wrapper supplies Gradle.
The official MDK toolchain resolver also provisions Java 21 for NeoForge's asset
tooling inside the local Gradle cache; the mod and game continue to use Java 25.
Create `.local/environment.json` with real absolute paths, for example:

```json
{
  "javaHome": "C:/Tools/jdk-25",
  "prismExe": "D:/PrismLauncher/prismlauncher.exe",
  "prismInstances": "D:/PrismLauncher/instances",
  "prismInstanceId": "Covenant-26_1_2-Dev"
}
```

Run `scripts/Setup-Prism.ps1`, then `scripts/Dev.ps1 doctor` and
`scripts/Dev.ps1 prism`. Creating/deploying the instance requires write access to
Prism's instance directory. The first build and launch download dependencies and
assets. If the new instance is not listed by an already running Prism, restart
Prism or launch using the script. Do not edit instance metadata while it is running.
When moving the checkout, update the local JSON and the owned instance's Java path
and ownership marker deliberately; scripts reject a marker for another checkout.

## IDE

Import `build.gradle` as a Gradle project in IntelliJ IDEA. Select the same JDK 25
for both Project SDK and Gradle JVM. For a PowerShell session with matching Java
and Gradle cache, dot-source `scripts/Environment.ps1` before invoking `gradlew.bat`
or launching the IDE. NeoForge sources are available through the resolved Gradle
dependencies. VS Code is usable with Java and Gradle tooling; the command-line
build and Prism workflow do not require any editor extension.
The repository includes VS Code extension recommendations and build/Prism tasks.
This machine's ignored `.vscode/settings.json` selects the installed project JDK;
update it too when moving the project or changing JDK installations. ModDevGradle
generates `.vscode/launch.json`; it is ignored because it contains absolute paths.

## Validation protocol

- Define observable acceptance criteria before implementing a feature.
- Build with `scripts/Dev.ps1 build`; this includes the dependency-free
  `radialMenuTest` Java task for sector ordering, boundaries, the center dead zone,
  and viewport sizing. The standard Gradle `test` task has no framework tests.
- Run `scripts/Dev.ps1 test` for the RPG lifecycle GameTests on a dedicated test
  server in ignored `runs/gametest`. It exits automatically with a failure code if
  a required test fails. This is separate from `build`; test sources and fixtures
  are excluded from the production JAR and normal client/server runs. See
  `docs/RPG_DATA.md` for coverage and manual lifecycle boundaries.
- Run the Prism smoke check for changed resources, registration, or client behavior.
- For common gameplay changes, check dedicated-server startup and the relevant
  multiplayer/save lifecycle. Do not import client classes into common code.
- NeoForge's development server can bypass the production EULA gate. For a
  production server, the user must read and accept Mojang's EULA themselves.
  The generated development server uses offline mode; keep local testing bound
  to `127.0.0.1` in `run/server.properties`. Use an interactive terminal for server
  runs so you can enter `stop` when finished.
- Inspect relevant logs and `git diff --check`; report automated and manual checks
  separately. A task with no test sources does not represent a passing test suite.
- Commit only when requested. Do not publish builds, create a remote, or push
  automatically.

## Authoritative references

- [NeoForge getting started and Java requirements](https://docs.neoforged.net/docs/gettingstarted/)
- [Official 26.1.2 MDK](https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle)
- [NeoForge concepts](https://docs.neoforged.net/docs/concepts/)
- [Prism command-line launch](https://prismlauncher.org/wiki/getting-started/command-line-interface/)
- [Microsoft OpenJDK downloads](https://learn.microsoft.com/en-us/java/openjdk/download)

NeoForge docs may default to a newer target in future. Select the 26.1 documentation
and confirm details against this project's resolved sources.
