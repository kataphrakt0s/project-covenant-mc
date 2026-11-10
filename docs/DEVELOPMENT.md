# Development

Use PowerShell 7 from the project directory. The scripts select the project JDK
and isolate Gradle's cache, avoiding the default Java 24 installation.

```powershell
.\scripts\Dev.ps1 doctor   # Java, Gradle, Prism, instance pins
.\scripts\Dev.ps1 build    # Compile/check and package build/libs/covenant-0.1.0-dev.jar
.\scripts\Dev.ps1 test     # Dedicated GameTests for RPG data/stats (separate from build)
.\scripts\Dev.ps1 verify   # Build plus dedicated GameTests; recommended before handing off gameplay
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

Connecting clients must use the same Covenant `mod_version` and network wire
revisions as the server. Required payload negotiation rejects mismatches before
entering play; NeoForge's disconnect screen identifies Project Covenant and lists
the client/server versions. Advance `mod_version` for changed release builds;
two different JARs deliberately built under the same version and wire revisions
are not distinguished by this check. Pre-check builds using bare numeric payload
versions are also rejected cleanly. Rendered disconnect UI still needs a real-client check.

The project now targets NeoForge **26.1.2.126**, matching
[ATM11 0.14.0-beta](https://www.curseforge.com/minecraft/modpacks/all-the-mods-11/files/9426887).
The generated mod metadata requires that version or newer. Before deploying a
new build, close the development game and select `26.1.2.126` for NeoForge in
Prism's Edit Instance > Version screen. `doctor`, `deploy`, and `prism` reject an
instance whose loader differs from the project pin; `Setup-Prism.ps1` retains
existing instances and does not upgrade them. Changing the repository pin does
not update Prism or a remote server. Minecraft remains `26.1.2` and Java remains 25.

## Profiling Covenant

Covenant uses Minecraft's native `Profiler.get().zone(...)` scopes with stable
`covenant:` labels. They appear beneath the native caller in the profiler tree;
the same subsystem may appear under several parents. They are not combined into
one global mod total. The scopes also feed Minecraft's Tracy integration when
available. With profiling inactive, Minecraft returns its shared no-op scope.
There is no Covenant polling job, file writer, or profiling dependency.

For the live visual pie, press **F3+1**. Use the numbered entries to descend into
a branch and **0** to go back; follow labels because entry numbers change with
their relative time. Covenant's entries begin with `covenant:`. Useful branches:

- `root > frame > extract > gui > inGameGui`: `covenant:resource_hud` and
  `covenant:progression_hud`, including their eligibility checks.
- `root > frame > extract > gui`: Covenant screen rendering while a menu is open.
- `root > tick > screen`: `covenant:skills_screen_tick`,
  `covenant:professions_screen_tick`, `covenant:stats_screen_tick`, and other open
  Covenant screen updates.
- `root > tick > entities`: named aura/bleed/frost/arrow callback checks, movement
  and freeze checks, plus Covenant projectile ticks when those entities are present.
- Bow rendering callers contain `covenant:bow_pose_enter`, `bow_pose_exit`,
  `bow_pose_check` and `bow_pose_time`; each label uses the `covenant:` prefix.

The F3+1 pie collects the **client thread**, including in singleplayer. Server-owned
skill execution, gathering and combat do not become client slices. Use a server
metrics recording to see those labels. The native pie retains each entry under
its actual caller; it does not provide a single combined Covenant slice at root.
Callback scopes include idle/side/attachment guards so these checks are visible
even when no ability is active. Bow scopes measure Covenant's pose bookkeeping
separately from the vanilla hand/character rendering they surround.

For a client/integrated-server capture, use the native **F3+L** metrics recording
and reproduce the activity. For a dedicated server, an owner/operator can use
`/perf start`, reproduce the activity, then `/perf stop` (or let recording finish
automatically). Read the report location printed by Minecraft and search the
profiling text for `covenant:`. In this Minecraft version `/debug start` and
`/debug stop` measure elapsed ticks/time; use metrics recording for detailed scopes.

| Work | Example labels |
| --- | --- |
| Skill requests, casts and allocations | `covenant:skill_activate`, `covenant:skill_execute`, `covenant:skills_tick`, `covenant:skill_allocation` |
| Auras, statuses and movement | `covenant:auras`, `covenant:bleeding`, `covenant:frost_slows`, `covenant:blood_fever`, `covenant:combos_movement` |
| Damage, combat attribution and rewards | `covenant:damage_incoming`, `covenant:damage_shield`, `covenant:damage_landed`, `covenant:combat_credit`, `covenant:combat_rewards` |
| Gathering and profession allocations | `covenant:gathering_prepare`, `covenant:gathering_harvest`, `covenant:harvest_reward_check`, `covenant:tree_recognition`, `covenant:farming_harvest`, `covenant:profession_allocation` |
| Native Covenant entities | `covenant:trap`, `covenant:rage_storm`, `covenant:arcane_bolt`, `covenant:elemental_bolt` |
| Equipment and Workbench | `covenant:equipment_modifiers`, `covenant:workbench_preview`, `covenant:workbench_action` |
| Client movement, HUD and screens | `covenant:client_dash`, `covenant:resource_hud`, `covenant:progression_hud`, `covenant:stats_refresh`, `covenant:stats_screen`, `covenant:skills_screen`, `covenant:professions_screen`, `covenant:radial_menu` |
| Input, effects and control checks | `covenant:keyboard_input`, `covenant:mouse_input`, `covenant:skill_key_input`, `covenant:arrow_visuals`, `covenant:lightning_visuals`, `covenant:freeze_check`, `covenant:movement_control_check`, `covenant:movement_packet` |
| Screen updates and bow bookkeeping | `covenant:skills_screen_tick`, `covenant:professions_screen_tick`, `covenant:stats_screen_tick`, `covenant:radial_menu_tick`, `covenant:workbench_screen_update`, `covenant:bow_pose_enter`, `covenant:bow_pose_exit`, `covenant:bow_pose_check`, `covenant:bow_pose_time` |

Scopes use try-with-resources so rejection, cancellation and exceptions close
them. Gathering harvest and entity scopes include the native/mod hooks invoked
inside that work: their inclusive time is not solely Covenant Java instructions.
`unspecified` remains the residual time in any parent without a more specific
child scope; these labels cover major runtime boundaries, not every method or
all vanilla/NeoForge work. JVM sampling profilers attribute Java stacks separately;
look for `dev.covenant` there rather than assuming they consume native zone labels.
Client scopes describe CPU state extraction/ticking, not GPU execution time.

## LAN test server

The isolated test server is at `cmm@192.168.0.193`, in
`/home/cmm/project-covenant-test`. Connect in Minecraft to
`192.168.0.193:25565` using Minecraft 26.1.2, NeoForge 26.1.2.126, and the
same Covenant JAR as the server. It uses authenticated accounts, a fresh Survival
world, eight player slots, and a private Temurin Java 25 runtime with a 4 GiB heap
limit. All the Mons `minecraft.service` is stopped and disabled at boot; its files
are retained.

The LAN installation was upgraded to the repository's `.126` loader pin on
2026-11-09 with the latest local Covenant build and a reset world. Connecting
clients need the same loader and Covenant JAR.

The system-wide `covenant.service` runs the server as `cmm`, starts at boot, and
restarts on failure after ten seconds. Its unit is
`/etc/systemd/system/covenant.service`; startup uses the existing `start.sh` and
world. Stops send SIGTERM and allow up to 180 seconds for native world saving.
The earlier `tmux` session is no longer used. Service changes require sudo.

The Java process may exit with status `143` after SIGTERM, leaving systemd's state
as `failed` despite the process having stopped. Before changing files, confirm
`MainPID=0` and that nothing is listening on port 25565. In that specific case,
`sudo systemctl reset-failed covenant.service` clears the status before starting.

```powershell
# Check status and boot configuration.
ssh cmm@192.168.0.193 'systemctl --no-pager status covenant.service; systemctl is-enabled covenant.service'
# Gracefully stop and save Covenant.
ssh -t cmm@192.168.0.193 'sudo systemctl stop covenant.service'
# Start or restart Covenant.
ssh -t cmm@192.168.0.193 'sudo systemctl start covenant.service'
ssh -t cmm@192.168.0.193 'sudo systemctl restart covenant.service'
# Inspect the latest server log.
ssh cmm@192.168.0.193 'tail -n 40 /home/cmm/project-covenant-test/logs/latest.log'
# Follow systemd output (Ctrl+C exits the log viewer).
ssh -t cmm@192.168.0.193 'journalctl -u covenant.service -f'
```

Until the user says otherwise, **reset the LAN server world on every update**.
After gracefully stopping `covenant.service` and confirming it is inactive, remove
the entire active save named by `level-name` (currently `world`), including its
player data, inventories, and progression. An archived copy outside the active
save directory may be retained for recovery. Do not carry old player data into
the newly generated world.

For updates, build and run dedicated GameTests locally, stage any required pinned
loader update, then stop the service as above. Back up the previous installation,
reset the world, and copy `build/libs/covenant-0.1.0-dev.jar` into remote `mods/`.
Compare SHA256 hashes before restarting and update clients to the same build.
Confirm startup reaches `Done` and the LAN Minecraft status query responds.
Keep server worlds, runtime, logs, and local connection details outside source control.

## In-game smoke check

The user performs manual client playtesting. For routine feature work, Codex runs
appropriate automated checks and hands over a short list of relevant in-game
checks. Client launches, UI automation, screenshots, and test-world creation for
validation happen only when the user requests client testing. Pending playtesting
does not block delivery.

1. Launch with `scripts/Dev.ps1 prism` or double-click the development instance.
2. In Mods, confirm **Project Covenant**, mod ID `covenant`, version `0.1.0-dev`.
3. Create a disposable Creative world. Enter it, move, break/place a vanilla block,
   save and quit, then reopen it.
4. Hold **G**: confirm a centered wheel with exactly ten numbered wedges. Slot 1
   is Skills, slot 2 is Stats, and the remaining slots show Option 3-10. Hover Skills and release G or
   left-click to open its window. The center and outside the ring have no selection.
   Other releases close the wheel. Clicking a placeholder has no effect. The world
   continues while open. In Skills, only the current class's tree appears. Left-click
   to allocate/select; right-click removes the node and its learned dependents.
   The whole tree fits the page using compact nodes with full hover tooltips.
   The right-hand Keybindings tab has the learned-skill picker
   and eight slots: click a slot to assign; right-click to clear. See
   `RPG_SKILLS.md` for binding, persistence and cancellation checks.
   Open Stats the same way from slot 2; releasing G must leave it open. On a fresh
   player, hover each class to read its description and select one without operator
   permission. Confirm the header updates and choice buttons disappear. Reopen,
   reconnect, and respawn to check the retained choice. Scroll the stats list through
   its final entry; check wrapping, scrollbar/arrow-key scrolling, and GUI scales.
   Equip a weapon/armor/page and apply a temporary effect or debug adjustment; check
   the corresponding effective value and modifier line. Check no-class and selected
   layouts, Escape/Done, and both click/release handoffs with keyboard/mouse bindings.
5. Check quick taps, repeated holds, Escape while G remains held (no reopening
   until a fresh press), and switching away from the window. Chat, inventory,
   pause, and the title screen must not open the wheel. Rebind Hold Radial Menu
   in Controls > Key Binds > Project Covenant, including a spare mouse button,
   and verify hold/release again. Check different window sizes and GUI scales.
6. Check the instance's `minecraft/logs/latest.log` for
   `Project Covenant loaded (mod id: covenant)` and errors relevant to the mod.

Prism uses your existing launcher account. If Microsoft authentication expires,
sign in through Prism. Project scripts do not read or manage account credentials.

For class selection, use a fresh player and run `/covenant class` in chat to inspect
the localized roster and current choice. Tab-complete and run
`/covenant class select covenant:stalker` (or `covenant:reaver` / `covenant:weaver`)
without operator permissions. Confirm repeated selection is rejected, ordinary
equipment remains usable, and `/covenant class` retains the choice after reconnect,
death, dimension travel, and a full save/restart. Use separate fresh players for
the other choices, or use operator-only `/covenant class switch <id>` to test
resource preservation and class restrictions. Ordinary selection stays one-time.

For resources and skills, use `/covenant resource` and `/covenant skill` for
inspection, `/covenant skill use <id>` for activation, and operator-only
`/covenant resource set <amount>` to seed the active balance. Operator-only
`/covenant resource infinite` toggles resource-free testing; append `true` or
`false` to set it explicitly. It bypasses resource checks/spending while retaining
cooldowns and other activation rules, preserves ordinary balances/HUD values, and
resets on reconnect or player replacement. `/covenant resource` reports its state. See
[RPG_SKILLS.md](RPG_SKILLS.md#in-game-testing) for each skill's test steps,
configuration, modifier IDs, persistence and cancellation expectations.

For stat debugging, use a player with operator permission (normally level 2).
Run `/covenant stats`, tab-complete `/covenant stats get `, then try
`/covenant stats set covenant:dodge 0.2` and inspect it again. Equipment/page
modifiers still apply to the temporary testing base. `/covenant stats reset`
restores normal calculations immediately. Save/reconnect also clears overrides;
normal profile/page data remains intact. See `RPG_STATS.md` for IDs, ranges, and
the command contract.

For affix debugging, hold a single iron sword and use operator level 2:
`/covenant affix set covenant:health covenant:legendary 5 max`. With the shipped
catalogue this initializes Legendary equipment with 60 Potential and +3.45 health.
Check the tooltip and equipped bonus, move it through inventory/chest/drop and
re-equip, then replace the roll with `set covenant:health covenant:common 1 min`.
Equipment rarity/Potential stay unchanged. `potential 0` prepares exhausted gear;
`potential full` refills it. `remove covenant:health` removes one family; `clear`
empties bonuses while keeping quality/page identity; `reset` removes both Covenant
components. `rarity covenant:rare` initializes empty Rare gear or changes existing
rarity, rejecting incompatible affixes, tiers, occupied slots, or Potential.
`component covenant:health covenant:legendary 5` supplies Workbench ingredients
without profession XP; counts are 1–64, default 1, and overflow drops beside you.
Common uses `/give @s minecraft:lapis_lazuli`. All setup edits are free, grant no
XP, and offer family/rarity completion. Tooltips replace `inspect`; raw-stat
`apply` is retired. See [ITEMIZATION.md](ITEMIZATION.md) for the complete contract.

The dedicated `affix_restart` fixture seeds a command-created item in a native
player file on a normal `scripts/Dev.ps1 test` run. After that server exits cleanly,
verify it in a fresh server process:

```powershell
$env:COVENANT_AFFIX_RESTART_CHECK = 'verify'
try { .\scripts\Dev.ps1 test }
finally { Remove-Item Env:COVENANT_AFFIX_RESTART_CHECK }
```

The fixture checks a different server process ID and reloads the saved item/page
with native player storage. Minecraft deletes the GameTest world on each launch;
the fixture therefore retains a byte-for-byte copy of the native player file
outside that world and restores it before loading. This checks cross-process item
persistence, rather than a complete retained-world restart. Both the native file
and expectation metadata live in ignored `runs/gametest/affix-command-restart*`.

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

## Profession foundation checks

Radial slot 3 opens the classless Professions window. Use operator
`/covenant profession level covenant:logging 17` to test the five Offensive
Mastery ranks; `/covenant profession xp covenant:logging 1500` exercises ordinary
XP awards. Native ore/log/mature-crop harvesting, credited breeding and
enchanting-table use now award profession XP. All four Crafting nodes are also
purchasable and drive the Workbench. Vein Mining, Tree Felling, Farming Scything
and Farming Automatic Replanting are purchasable; other gathering nodes remain planned and
unavailable. All tuning is provisional. See [ITEMIZATION.md](ITEMIZATION.md).

Manually check per-tab drafts, right-click undo, Apply's combat gate, close
discard, resized layouts/tooltips, and the two-second Profession Respec hold.
The shapeless respec recipe uses four paper, four lapis and one emerald. Confirm
that all four tracks refund together with XP/levels retained. See
[PROFESSIONS.md](PROFESSIONS.md) for data formats and scope.

For Farming, use `/covenant profession level covenant:farming 10`, then buy
Scything ranks and Automatic Replanting in Professions. Hoe right-click a mixed
mature field to check 3×3/5×5/9×9 areas, sneak for one crop, and verify other
heights/immature crops stay intact. Supply inventory seeds for replanting, then
try an empty seed inventory and a nearly broken hoe. Check one durability use per
harvest (with Unbreaking), native drops/XP, Nether wart on soul sand, and ordinary
left-click/tilling behavior. Check that offhand bone meal is not also used by
the same hoe click. Client animation/input feel and multiplayer latency
still require manual playtesting.

## Validation protocol

For itemization smoke checks, craft or `/give @s covenant:workbench`, then use
`/covenant profession level covenant:crafting 20` for earned test points and buy
the four Crafting nodes in Professions. Check the Crafting tab selectors/costs,
all four operations, affix tooltips and equipped bonuses. Vanilla loot chests
and player-credited hostile kills supply affixed gear. Queue salvage, inspect
the action tooltip, fill results to check batch stopping, and collect to continue
with another explicit click. Leave gear/results inside, close/reopen, and restart
the world. With two clients, check shared contents, actor-specific costs/ranks
and old-preview rejection. These client checks remain manual.

- `scripts/Dev.ps1 verify` runs both the production build/checks and dedicated
  GameTests in one command. Require the final server log's explicit required-test
  pass line; compilation or a successful task with no test sources is insufficient.
- Packet regressions use NeoForge's registered handlers and native connection
  methods with embedded server players. They cover authority and state transitions,
  but do not substitute for two real clients with latency. For skill hardening,
  manually check root/freeze with falling and knockback, delayed dash handoff,
  instant item use and spear attacks while frozen, plus lightning seen by a nearby
  observer. Both clients and server must use the same build (payload protocol 3).
- For server-driven player movement, exercise the full native connection tick,
  not only `ServerPlayer.doTick()` or posted player-tick events. The pinned
  connection wrapper restores cached `firstGood` coordinates after `doTick()`;
  an authoritative position change must update that anchor to survive the tick.
  `combo_connection_movement` covers both dash distances and walking handoff.
- Define observable acceptance criteria before implementing a feature.
- Build with `scripts/Dev.ps1 build`; this includes the dependency-free
  `radialMenuTest` Java task for sector ordering, boundaries, the center dead zone,
  and viewport sizing. `skillTreeLayoutTest` also checks full-tree visibility,
  tier ordering and separate node bounds across GUI sizes. The standard Gradle
  `test` task has no framework tests.
- Run `scripts/Dev.ps1 test` for the RPG lifecycle GameTests on a dedicated test
  server in ignored `runs/gametest`. It exits automatically with a failure code if
  a required test fails. This is separate from `build`; test sources and fixtures
  are excluded from the production JAR and normal client/server runs. See
  `docs/RPG_DATA.md` and `docs/RPG_STATS.md` for coverage and manual lifecycle boundaries.
- For changed resources, registration, or client behavior, hand the relevant Prism
  smoke checks to the user; manual client testing follows the policy above.
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
