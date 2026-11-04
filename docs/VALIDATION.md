# Environment validation

Verified on **2026-11-03**, America/Winnipeg, on Windows 11 x64.

## Basic class definitions

- `scripts/Dev.ps1 test`: all 10 required dedicated GameTests passed (nine authored
  Covenant tests and the existing vanilla test). The new `class_foundation` test
  exercises Stalker, Reaver, and Weaver through the registered command with no
  operator permissions: discovery/completion, selection, invalid input, repeated
  selection rejection, and rejection of non-player sources.
- Each choice retains its exact existing-profile snapshot through actual player-file
  save/load, the profile network codec, death cloning, and End-return cloning.
  Unknown saved IDs remain unchanged and resolve without a fallback definition.
  Bow, sword, and axe equipment remains usable for every class; selection adds no
  resource or stat bonus. All class code loaded on the dedicated test server.
- `scripts/Dev.ps1 build`: passed, including radial geometry checks. The production
  JAR contains class definitions/commands and excludes test classes/resources.
  JSON parsing, added-source whitespace checks, and `git diff --check` passed.
- Real-client command presentation/translation and attachment delivery, actual
  disconnect/reconnect, death, dimension travel, and full server restart with
  players remain manual checks. The Prism JAR was not replaced.

## RPG stat foundation

- `scripts/Dev.ps1 test`: all 9 required dedicated GameTests passed (eight authored
  Covenant tests plus the existing vanilla test). Four new tests exercise native
  base values, flat/base-percent/total-percent order, independent affix entries,
  equipment removal/replacement, in-place page updates, inventory/wrong-slot
  exclusion, weapon attributes/recharge, repeated queries, transfers, breakage,
  neutral/modified skill coefficients, external native modifier coexistence,
  actual player-file stat reconstruction, and death/End clone fixtures.
- Current health above the unequipped maximum initially failed a focused reload
  regression (26 became 20). The narrow player-load Mixin repairs the vanilla
  inventory/health load order. Tests now preserve both full and injured health
  above base maximum, preserve End-return current health, remove stale bonuses on
  death without gear, and restore full effective health with `keep_inventory`.
- Deterministic dodge fixtures verify 0% and clamped 100%, actual melee/projectile
  damage cancellation before armor durability/immunity, and fall/explosion/void
  exclusions. Native sync flags include all new attributes and attack damage.
- All new gameplay code loaded on the dedicated test server. Real-client delivery,
  rendered tooltips, disconnect/reconnect, actual travel/death, complete server
  restart with players, and unrelated mod compatibility remain manual checks.
  Damage uses vanilla attack damage; arbitrary projectile/skill damage is not
  rescaled. Jewelry/custom slot integration and acquisition/UI remain deferred.
- `scripts/Dev.ps1 build`: passed, including radial geometry checks. Production
  JAR inspection confirmed the stat classes, health-load Mixin, and its metadata
  registration are present, with test classes/resources excluded. `git diff
  --check`, new-source whitespace, and modified JSON resource checks passed.
- The Prism instance was not replaced. See `RPG_STATS.md` for the complete stat
  contract and API boundaries.

## RPG data foundation

- `scripts/Dev.ps1 test`: passed. The dedicated GameTest server reported all
  5 required tests passing, including the four authored Covenant lifecycle tests.
  Covenant checks cover default/initialized profiles, full entity serialization
  and actual player-file save/load (including a paged inventory item), repeat
  creation rejection, death and End-return cloning, profile/item network codec
  round trips, independent pages on identical items, inventory/container movement,
  equip/unequip, dropped-item serialization/pickup, custom tag category compatibility,
  singleton enforcement, copy/split behavior, immutable nested lists, progression
  updates retaining page identity, and invalid data. No GameTest errors were logged.
- `scripts/Dev.ps1 build`: passed, including the existing radial geometry checks.
  The standard Gradle `test` task is still `NO-SOURCE`; GameTests run separately
  through the new script task. The production JAR was inspected: RPG classes and
  compatibility tags are included; test mod/classes/fixtures are excluded.
- `scripts/Dev.ps1 server`: loaded only Minecraft, NeoForge, and Covenant, loaded
  the compatibility tags, and reached `Done (0.500s)!` on `127.0.0.1:25565`.
  No Covenant loading or serialization errors occurred. The environment blocked
  Mojang public-key retrieval and hardware WMI queries; the isolated offline
  development server continued to its ready state. Console `stop` input did not
  reach the Gradle child despite a PTY, so its specifically identified Java
  process was terminated after startup and initial chunk saving. This server task
  is a startup check, not a successful Gradle task exit. GameTests shut down and
  saved normally.
- `git diff --check` and whitespace inspection of added sources passed.
- Real-client attachment delivery, multiplayer transfers, disconnect/reconnect,
  actual dimension travel, and a complete server restart with players remain
  manual checks. Codec round trips and clone fixtures do not establish these
  interactive behaviors. The Prism JAR was not replaced. No creation UI,
  acquisition/rolling, combat stat aggregation, or accessory integration was added.

See `RPG_DATA.md` for format contracts, server APIs, compatibility tags, and the
distinction between safe transfer snapshots and deliberate creative item cloning.

## Radial menu foundation

- `scripts/Dev.ps1 build`: passed with the ten-slot client menu and localized
  rebindable control. `radialMenuTest` passed sector centers/edges, clockwise
  wrapping, dead zone, outer bounds, and viewport sizing. The standard `test`
  task remains `NO-SOURCE`; these checks use a separate Java source set.
- Gradle development client loaded Covenant and entered a local world without
  a mod loading error. The user took over interactive testing. The final GUI
  quad-winding correction was rebuilt after that launch and requires a client
  restart. The agent has not visually verified the final wheel or hold/release
  behavior; use the radial smoke checks in `DEVELOPMENT.md`.
- The menu has no packets, persistent state, or gameplay actions. Common code
  does not reference client classes. Dedicated-server loading was not rerun for
  this client-only change. The Prism JAR has not been replaced in this task.
- `git diff --check` passed, but the repository still has no tracked files, so
  authored menu files were also inspected directly for trailing whitespace.

## Completed

- Microsoft OpenJDK `25.0.5.0.2+1` installed inside `.tools/java`; downloaded ZIP
  SHA256 matched Microsoft's checksum. No global Java installation was changed.
- `scripts/Dev.ps1 doctor`: JDK 25, compiler, Gradle 9.2.1, Prism executable,
  owned development instance, and Minecraft/NeoForge version checks passed.
- Gradle wrapper distribution is pinned with the official SHA256.
- `scripts/Dev.ps1 build`: successful. Gradle's `test` task reports `NO-SOURCE`;
  the starting project has no unit tests or GameTests.
- Packaged JAR inspected: correct `covenant` mod ID and `0.1.0-dev` version,
  entry-point class present, metadata placeholders expanded, no example-mod assets.
- Minecraft's downloaded `version.json` confirms Java 25 and resource format 84.
- Prism `11.1.1` launched `Covenant-26_1_2-Dev` with the packaged JAR. Its log
  lists Minecraft `26.1.2`, NeoForge `26.1.2.125`, and Project Covenant `0.1.0-dev`,
  and records `Project Covenant loaded (mod id: covenant)`. Resource loading,
  renderer, texture atlases, and audio initialized, then the client exited normally.
- Deployed JAR SHA256 matched the build output.
- Dedicated development server discovered and constructed Covenant, loaded data,
  generated its disposable world, and reached `Done (...)! For help, type "help"`.
  The server saved its initial chunks. The automation session's stdin was closed,
  so its specific Java process was terminated after startup verification. The
  interrupted `runServer` task is not recorded as a successful Gradle task exit.
- Skill validated with the bundled skill-creator validator. PowerShell scripts
  parsed successfully. Source files checked for whitespace and unresolved scaffold
  placeholders.
- Local Git repository initialized on `main`; no commit, remote, or push created.

## Observed tool behavior

NeoForge's asset tooling asks for Java 21 independently of the Java 25 mod target.
The official MDK Foojay resolver is enabled and provisioned that toolchain into
the local Gradle cache. Client assets are downloaded for future Gradle launches.

The first server launch logged a missing `server.properties` before creating it.
The client logged two NeoForge shader sampler warnings. Neither prevented startup.

## Manual checks still available

Use the smoke-check sequence in `DEVELOPMENT.md` to inspect the Mods screen and
create, play, save, and reopen a client world. Those interactions were not manually
verified during environment setup. Multiplayer behavior and ATM11 compatibility
have not been tested; the radial UI currently exposes placeholders only.

Local evidence lives in ignored build artifacts and game logs:

- `build/libs/covenant-0.1.0-dev.jar`
- `run/logs/latest.log` for the development server
- `D:/PrismLauncher/instances/Covenant-26_1_2-Dev/minecraft/logs/latest.log`
  for the Prism client
