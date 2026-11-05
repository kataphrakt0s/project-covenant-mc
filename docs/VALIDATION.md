# Environment validation

Verified on **2026-11-03**, America/Winnipeg, on Windows 11 x64.

## Damage-driven Reaver Rage (2026-11-05)

- `scripts/Dev.ps1 build` passed, including radial/skill-tree geometry checks.
  `scripts/Dev.ps1 test` passed all **34 required dedicated GameTests**.
  Evidence: ignored `.local/rage-tests-final.log`. Production JAR inspection
  found no GameTest classes or fixtures; `git diff --check` passed.
- The new `rage_damage` Survival fixture verifies outgoing/incoming damage
  coefficients, armor reductions, absorption, fractions, ordinary versus skill
  arrows, environmental/self/zero/immune/dodged exclusions, incoming attributed
  magic, the existing generation attribute on both gains, Cleave/Rend/bleed
  outgoing exclusions, capacity clamping, and inactive-class preservation.
  The updated recovery fixture verifies no passive combat generation, refreshed
  seven-second grace, zero damage not refreshing grace, and 10/second decay.
- Both damage coefficients are server-configurable. The old time-based
  `rageGeneration` config key is retired; saved balances and the existing
  `covenant:rage_generation` attribute ID remain compatible.
- Manual client handoff: fight with normal attacks and check immediate Rage
  gains; cast Cleave/Rend and confirm they do not refund Rage; stand idle during
  grace and confirm no growth, then decay after seven seconds. Cast frequency
  and gamefeel still need user playtesting. No client was launched or deployed.

## Stalker traps (2026-11-05)

- Range tuning: all traps now use a 1.5 × 1.5-block contact footprint (0.75-block
  half-width, still 0.25 blocks high); Frost/Blast use 3.5-block effect radii.
  Flame retains its three-block effect radius. Build and all 33 required dedicated
  GameTests passed with expanded contact checks for every trap, Frost outer-area
  entry/exit, Blast outer-area damage/knockback and falloff, and Flame's retained
  range. Tooltips, particles, and design notes use the new values. Test evidence:
  ignored `.local/trap-range-tests-final.log`. Rendered boundaries/gamefeel remain
  for user playtesting.
- All four traps are implemented through existing command and saved-slot activation.
  `scripts/Dev.ps1 test` passed **33 required dedicated GameTests**, saved all
  dimensions, and shut down cleanly. Eleven new trap fixtures cover spending,
  cooldowns, rejected entity addition, pending replacement, exact arming/armed
  expiry, walls (including point-blank release), ledges, slab/stair support,
  flooding/support removal, shallow hitbox contact, passive targets, pet/spectator/
  owner/PvP exclusions, saved assignments, dependent unlearning, and owner cleanup.
- Bear checks cover fixed damage, armor, absorption, shields, guaranteed Dodge,
  invulnerability, an eligible Wither, root refresh, input/jump suppression,
  preserved external velocity, and duration snapshots. Native mob serialization
  and an actual player-file save/load preserve root; inactive timers pause, loaded
  mobs count down, and native death/player death replacement clear it.
- Frost checks cover nonstacking multiplayer sources, removal of one source,
  final exit/re-entry, root interaction, armed/active slot separation, triggered
  replacement, prerequisite cleanup, wall/floor cover, expiry, and no saved slow.
  Flame checks cover immediate entry, retained re-entry deadlines, exact six-hit
  lifetimes, two independent owners/credit, ordinary hurt-immunity interference,
  fire immunity/Fire Resistance, shield/Dodge bypass, invulnerability, cover,
  replacement, and class cleanup. Blast checks cover one attributed explosion,
  center/edge/outside damage, armor/Blast Protection, both knockback resistances,
  solid cover, shield/Dodge bypass, invulnerability, root-preserving displacement,
  and retained blocks.
- Native entity saving excludes deployed traps/patches. The fixture invokes the
  native `UNLOADED_TO_CHUNK` removal reason and verifies immediate Frost cleanup;
  it does not perform a full chunk unload/reload or retained-world process restart.
  Dimension cleanup is exercised through its event; live client dimension travel,
  reconnect delivery, and real-client multiplayer remain manual checks.
- The Aimed Shot fixture's firing lane now has explicit temporary test-only chunk
  tickets; its prior failures showed an unticked arrow outside the tiny structure's
  ticking chunks, depending on random grid alignment. Its tickets are released on
  completion. Trap gameplay never creates chunk tickets.
- Evidence: ignored `.local/traps-tests.log` and `.local/traps-build.log`.
  Builds include both geometry checks; standard Gradle `test` remains `NO-SOURCE`.
  No client launch or Prism deployment was performed. Root uses a vanilla Slowness
  sprite alias for its inventory presentation; trap models/particles and movement
  feel remain unobserved. Profiles, Affix Pages, and resource/loadout saved formats
  retain their existing IDs and meanings.
- User playtest: throw while retreating and against obstacles/ledges/slabs/stairs;
  distinguish four models and armed cues; walk/jump/aim/attack/cast while rooted
  and receive knockback; cross/overlap patch boundaries and solid cover; tune Blast's
  low shove (roughly three blocks is a target, not a measured guarantee). Check
  owner death/logout/dimension changes and actual chunk unload/restart with a second
  client. See `TRAPS_IMPLEMENTATION_PLAN.md` for concrete values.

## Rend and shared bleeding (2026-11-05)

- `scripts/Dev.ps1 test` passed all 22 required dedicated GameTests and completed
  native dimension saves and clean shutdown. New fixtures `rend`, `bleed_pulses`,
  and `bleed_persistence` verify native default/custom held-item reach and wall
  clipping; 100% initial damage, 20 Rage, two-second cooldown; application-time
  10% power capture; five-second duration and native cost/cooldown/duration scaling;
  absorption-only hits; actual shield blocking, Dodge, invulnerability and canceled
  damage; shared cap, oldest replacement, duration refresh and attacker credit;
  independent hurt cooldown and native differential damage; armor bypass,
  Resistance/absorption/invulnerability, exact expiry and shared consumption.
- Native mob and player save/load preserve captured damage, UUID ownership and both
  relative timers. Death replacement excludes wounds; non-death replacement retains
  them. An unregistered restored target remains paused while server time advances,
  then resumes without catch-up with its attacker offline. After registration, real
  loaded-entity ticks deliver the remaining pulses and expiry. Native death clears
  the state. These fixtures do not establish live network reconnect delivery or a
  retained-world process restart.
- `scripts/Dev.ps1 build` passed, including radial/skill-tree geometry checks.
  Evidence: ignored `.local/rend-tests.log` and `.local/rend-build.log`. The new
  server-only `covenant:bleed` attachment and data-driven damage type are additive;
  existing saved IDs/formats are retained. Dependent bleed skills remain placeholders.
- No client launch or Prism deployment was performed. Manually check the native
  swing/red bleed particles, normal melee reach, three-stack damage pacing,
  refresh/five-second expiry, weapon swaps, continued melee between pulses,
  shared ownership with another Reaver, and actual reconnect/chunk-unload/restart.

## Foundational attack skills (2026-11-05)

- `scripts/Dev.ps1 test` passed all 19 required dedicated GameTests; the server
  saved all dimensions and shut down successfully. The new `skill_foundations`
  fixture verifies actual three-target piercing with a fourth target unharmed,
  block stopping/trail cleanup, Cleave's forward arc and edge/range/line-of-sight
  exclusions, Aimed Shot's delayed damage from changed position/aim, snapshotted
  costs, single ammunition/durability use, movement slowdown/restoration, and
  cancellation through prerequisite removal, lost equipment, class changes, and
  insufficient completion resources. Existing persistence/loadout tests passed.
- `scripts/Dev.ps1 build` passed, including radial and skill-tree geometry checks.
  `git diff --check` passed. The packaged JAR includes the narrow native arrow
  piercing invoker and excludes GameTest classes/resources. No saved format or
  existing ID changed; the new bow-pose duration attachment is transient.
- Evidence: ignored `.local/skill-foundations-tests.log` and
  `.local/skill-foundations-build.log`. No client launch or Prism deployment was
  performed. Manually check Aimed Shot's bow pose, movement feel, ordinary bow-use
  interaction, interruption/death/logout/dimension travel, and zero/scaled cast
  times; also check Cleave's arc readability and Piercing Shot's successive impacts.
  Client rendering and live multiplayer lifecycle delivery remain unverified.

## Whole-tree page layout (2026-11-04)

- `scripts/Dev.ps1 build` passed, including `radialMenuTest` and the new
  `skillTreeLayoutTest`. Geometry checks cover minimum/common GUI viewports,
  wide and portrait windows, all eight tiers, positive node sizes, containment,
  lane/tier order, and separate click bounds. Every node now fits on the page;
  the tree has no pan/scroll controls or hidden clipped nodes. Empty lanes are
  compressed; initials temporarily stand in for future icons.
- This change affects client layout only. Allocation rules, server authority,
  packets and persistence are unchanged; the dedicated GameTests from the first
  tree pass below were not rerun for this layout change. `git diff --check` passed.
- Evidence: ignored `.local/skill-tree-fit-build.log`. No client launch or Prism
  deployment was performed. The user should check full-tree visibility, initials,
  hover/focus tooltips, connection readability and click targets at their GUI scale,
  plus switching between Tree and Keybindings.

## First skill tree pass (2026-11-04)

- `scripts/Dev.ps1 build` passed, including radial geometry checks.
  `scripts/Dev.ps1 test` passed all 18 required dedicated GameTests. The new
  `skill_tree` fixture checks the 62-node catalog and exact 11 prerequisite links,
  both aura exclusion directions, free learning at level one, dependent removal
  with binding cleanup, unrelated-state preservation, wrong-class/spectator
  rejection, placeholder activation without spending/cooldowns, full-tree native
  player-file save/load, and death/End copying. Existing skill/loadout tests pass.
- All 62 IDs and diagram tiers match the brainstorm text, and every catalog node
  has a name and description translation. No executable skill was added; the
  original three definitions retain their existing effects. Attachment and packet
  formats remain unchanged.
- The dedicated fixture uses ordinary server players with embedded connections:
  the deprecated GameTest mock forces Creative in its `gameMode()` override and
  cannot verify spectator behavior.
- Logs are ignored `.local/skill-tree-build.log` and `.local/skill-tree-tests.log`.
  No client launch or Prism deployment was performed. The user should check that
  only their current class tree is visible, prerequisite/exclusion tooltips and
  node colors update, removing
  prerequisites clears dependents, and the separate right-hand Keybindings tab
  scrolls/selects/assigns/clears correctly. Live reconnect, dimension travel, and
  retained-world restart remain manual checks.

## Piercing Shot bow charge (2026-11-04)

- `scripts/Dev.ps1 build` passed, including radial geometry checks. All 17
  required dedicated GameTests passed and the server saved/shut down cleanly.
  The effects fixture verifies immediate firing, the six-tick cosmetic deadline,
  no forced melee swing or actual item use, single-arrow consumption, and no
  animation start/restart on rejected requests. Existing damage and lifecycle
  checks continue to pass.
- Pinned renderer signatures and LocalPlayer's item-use overrides were inspected.
  The packaged JAR includes the four client-only bow Mixins and excludes GameTest
  classes. `git diff --check` passed. Logs are ignored
  `.local/bow-animation-build.log` and `.local/bow-animation-tests.log`.
- Client rendering and Mixin application have not been exercised in a live client.
  Manually check first/third-person bow charging, left-handed use, nearby viewers,
  movement, item switching, normal right-click drawing/release, and no extra shot
  or stuck pose. No client launch or Prism deployment was performed.

## Resource HUD and skill visuals (2026-11-04)

- `scripts/Dev.ps1 build` passed, including the radial geometry checks. The
  dedicated `scripts/Dev.ps1 test` run passed all 17 required tests and completed
  a clean server save/shutdown. The skill-effects fixture now checks native
  main-hand animation on success, no animation on rejected requests, the
  Piercing Shot arrow marker, ordinary-arrow exclusion, and marker removal on
  an actual native impact. Existing resource/cast/damage/lifecycle fixtures passed.
- The HUD reads the existing owner-synced balance/capacity. Its row is registered
  immediately after food and reserves space before native air/mount indicators.
  Common skill/particle code has no client-class dependencies. Projectile trails
  use client-local particles; cast/sweep/impact events use native server particles.
- `git diff --check` passed. Evidence is in ignored
  `.local/skill-polish-build.log` and `.local/skill-polish-tests.log`.
- No client was launched or Prism instance deployed for this pass. Rendered
  placement, text/GUI scales, Creative/F1, bubbles/mount health, each skill's
  animation/particles, and visibility to other players still require user
  playtesting. See `RPG_SKILLS.md` for the focused checklist.

## Stats page and class chooser (2026-11-04)

- `scripts/Dev.ps1 build` passed, including radial geometry checks. The dedicated
  `scripts/Dev.ps1 test` run passed all 17 required tests and saved/shut down cleanly.
  Existing class command tests cover non-operator one-time selection, invalid and
  repeated selection, native player-file reload, and death/End copying. The new
  chooser uses that command rather than introducing a separate mutation path.
- Rendered smoke checks in a new disposable Gradle client world, **Stats UI Smoke**:
  radial slot 2 opened Stats on both release and left-click; releasing G after a
  click left Stats open. The no-class layout and description tooltip rendered.
  Selecting Reaver updated the header and removed the chooser after server sync;
  reopening retained the choice. Mouse-wheel scrolling reached Mana Regeneration.
- The page displayed health 26/base 20 with the +6 debug modifier, and iron sword
  damage 6/base 1 with its +5 native modifier. Speed showed 0.12/base 0.1 and
  `+20% of total`, then updated to 0.1/no modifier while the page stayed open after
  the effect expired. These checks used the final built-in scrolling text widget.
- JSON/translation checks and `git diff --check` passed. Logs and screenshots are
  ignored `.local/stats-page-*.log` and `.local/stats-*.png`. Prism was not deployed.
  Alternate GUI scales, mouse-bound radial activation, keyboard scrolling, live
  multiplayer/reconnect, and retained-world restart remain manual checks for this UI.

## Skills window and eight-slot loadout (2026-11-04)

- `scripts/Dev.ps1 build` passed with the final client configuration, including
  radial geometry checks. `scripts/Dev.ps1 test` passed all 17 required dedicated
  tests and completed a clean server save/shutdown. Existing combat fixtures now
  explicitly learn their skills before activation. JSON parsing, new Java source
  whitespace checks and `git diff --check` passed.
- New `skill_loadout` fixture checks class-required/free/idempotent learning,
  unknown IDs, unlearned assignment and command rejection, all eight independent
  assignments, invalid indices, clearing one slot, inactive-class rejection and
  preservation, NBT and owner-sync codecs, all three request codecs, actual native
  player-file save/load, death/End copying, unlearning every matching assignment,
  cooldown retention across relearning, slot activation and pending-cast cancellation.
- Development client startup initialized the renderer/resources without a relevant
  mod error. Its saved `run/options.txt` contains all eight default bindings, with
  SHIFT modifiers for slots 5-8. Final startup wrote both Drop Item and Swap Hands
  to `key.keyboard.unknown` with the default client config enabled. These are actual
  startup/options observations; they do not establish in-world key dispatch.
- Rendered GUI/mouse interaction could not be observed: the desktop probe found no
  accessible Minecraft window, including after elevation and a window-enumeration
  attempt. A thread dump showed the client render loop running. The validation
  clients were stopped after startup checks; no world was opened. No Prism instance
  was deployed or changed. Logs are ignored `.local/skill-menu-build.log`,
  `.local/skill-menu-tests.log`, and `.local/skill-menu-client-final.log`.
- Manual checks remain: radial release/click handoff, window layout/tooltips at GUI
  scales, actual left/right clicks and attachment updates, every base/Shift key and
  rebinding, no double activation on Shift, config opt-outs/alternate vanilla keys,
  multiplayer reconnect, dimension travel, and retained-world restart. See
  `RPG_SKILLS.md` for the smoke sequence. Compilation and codec/server tests do not
  replace those rendered/network lifecycle checks.

## Basic class resources and skills

- `scripts/Dev.ps1 test`: all 16 required dedicated-server tests passed in the
  final run, followed by a clean save/shutdown and successful Gradle exit.
  `scripts/Dev.ps1 build` passed, including radial geometry checks. Resource JSON
  parsing, added-source whitespace checks and `git diff --check` passed.
  Logs: ignored `.local/skills-acceptance.log` and `.local/skills-build.log`.
- Three dedicated fixtures cover the new foundation: `skill_lifecycle`,
  `skill_recovery`, and `skill_effects`. The suite now contains 16 required tests
  (15 Covenant tests and the existing vanilla test).
- Actual native player-file save/load retains all independent class balances and
  cooldown deadlines. Death/End cloning, owner snapshot codecs, one-time legacy
  balance import, class switching without refill, and loading equipped Mana above
  100 are covered. Effective capacities remain native/configured values, not saved
  equipment totals. Focus capacity and regeneration bonus bounds are exercised
  through actual Affix Pages alongside increasing Mana capacity/recovery.
- Time-based Rage generation, a real hit refreshing seven-second combat grace,
  zero damage failing to refresh it, out-of-combat depletion, inactive balance
  preservation, and Focus/Mana recovery run on a dedicated server. Observations
  at pulse boundaries allow the documented single five-tick resource pulse.
- Real native arrow damage, survival ammunition/bow durability, Infinity without
  recoverable duplicate arrows, multi-target Cleave damage, and delayed single-hit
  Arcane Bolt damage are checked. Tests cover resource cost scaling, prerequisites,
  wrong-class/insufficient-resource rejection, duplicate activation, cast overlap,
  class-change cancellation and completion-time resource rejection without cooldown.
- Resource/skill discovery, skill completion suggestions, operator-only balance
  edits/class switching, non-player rejection, and invalid resource input are covered.
  Existing stat command tests now use the expanded supported-stat list and respect
  Focus's 1.25 regeneration bound.
- No Prism deployment or rendered playtest was performed. Real-client packet
  delivery, multiplayer reconnect, actual dimension travel and retained-world restart
  remain manual checks. Native player-file reload and codecs do not establish those
  observations; no other-mod compatibility/performance measurements are claimed.
  See [RPG_SKILLS.md](RPG_SKILLS.md#in-game-testing) for the manual procedure.

## Affix Page debug commands

- `scripts/Dev.ps1 test`: all 13 required dedicated GameTests passed (twelve
  Covenant tests plus the existing vanilla test), both in the final seed run and
  the subsequent `COVENANT_AFFIX_RESTART_CHECK=verify` run. Existing class/stat
  command tests passed; their implementations were not changed for this task.
- Registered affix commands validate privileges, main-hand presence, loaded
  compatibility categories, supported stats, finite amounts, singleton stacks,
  and existing pages before mutation. Category/stat completion and inspection
  of every independent base/nested affix modifier are exercised.
- Command-created vanilla equipment retains its individual page through inventory
  moves, a real chest block entity's save/load and retrieval, and native player
  drop/pickup. Identical item types have different page identities/values. Native
  equip/unequip, repeated queries/ticks, and removing an equipped page update stats
  without compounding; normal sword damage remains. Remove/clear preserve the
  exact underlying sword components, damage, custom name, count, and stack object.
- Cross-process persistence verifies the command-created page UUID/category/value,
  underlying sword/damage, unchanged profile, and reconstructed stat contribution.
  Minecraft's GameTest launcher deletes its world each time; the fixture retains
  and restores an unchanged native player file outside that world. This does not
  establish a complete retained-world restart or rendered client behavior.
- Existing limitation: attachment overwrites max-stack size without preserving its
  prior value. Remove/clear retain that component rather than guessing, so a
  normally stackable item can remain limited to one. The custom paper category
  regression covers this behavior. Custom/accessory categories still have no
  implicit equipment-slot integration. No new page fields or format were added.
- `scripts/Dev.ps1 build`, JSON/added-source whitespace checks, and production JAR
  inspection passed; test classes/resources are excluded. No Prism deployment or
  rendered/manual playtest was performed. Evidence is in ignored
  `.local/affix-seed-test.log` and `.local/affix-restart-test.log`.

## Stat debug commands

- `scripts/Dev.ps1 test`: all 11 required dedicated GameTests passed (ten Covenant
  tests plus the existing vanilla test). `stat_commands` exercises the registered
  tree, all ten supported IDs, tab completion for get/set, inspection feedback,
  immediate calculations, repeated set/reset, operator permission enforcement,
  non-player rejection, and invalid identifiers/numbers/ranges.
- A testing base combines with flat, base-percentage, and total-percentage page
  modifiers and an independent permanent native modifier. Ticks, unequip/reequip,
  and in-place page updates do not duplicate or corrupt contributions. Reset
  restores the calculation from current equipment and preserves other modifiers.
- Saving an overridden player to an actual player file and reloading into a fresh
  player restores the normal value and exact non-debug modifiers, character profile,
  and Affix Page. Native packed bases/permanent modifiers remain identical while
  overridden. Death/End-return clones do not inherit the debug modifier. Existing
  class command/lifecycle tests also passed without changes to their implementation.
- `scripts/Dev.ps1 build` passed with radial geometry checks. Rendered command
  presentation and live client attribute delivery were not manually observed;
  actual reconnect/travel/restart remain manual checks. Prism was not deployed.

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
