# Environment validation

Verified on **2026-11-03**, America/Winnipeg, on Windows 11 x64.

## Trap family and area clarity - visual polish Phase 2 (2026-11-10)

- Bear: 14 cuboids / 168 triangles, opposing toothed jaws, pressure plate,
  actual-trigger snap streaks/sound. Flame: 11 cuboids / 132 triangles, vented
  ember vessel, ignition-to-patch handoff and natural-expiry-only extinguish.
  Blast: 5 cuboids / 60 triangles, bound charge/solid unlit primer and brief
  pressure burst. All share Frost's native item-model path, iron ramp, 32x32
  opaque atlas density (16 texels/block), and matching 16x16 tree icons.
- Headless geometry reported zero errors/warnings on all three. Mirror naming
  is not applicable; interpenetration gate self-tests cannot discriminate in
  these single-group sources. Native/enlarged atlases/icons and iso/front/back/
  left/right/top/three-quarter mapped previews were inspected. The packaged
  resource audit checks exact source/export PNG agreement, proportional UVs at
  one texel/model unit, dimensions, alpha/palettes and each definition/model/
  texture/icon chain. Sources/briefs stay ignored under models/<skill_id>/;
  previews/evidence stay .local/blockbench/phase2 and .local/phase2.
- Essential supported contact squares now survive Minimal for every trap;
  Frost/Flame switch to their actual circular patch footprints. One merged
  owner-only broken aura outline replaces dust, preserves self-only exclusions,
  and omits trap cover rays because aura gameplay ignores cover. Both use dry
  same-elevation support sampling without downward projection. Aura extraction
  snapshots geometry/position into native level render state. Root/freeze/chill
  target hints use existing synced state; no new gameplay or saved IDs.
- scripts/Dev.ps1 build passed. Final scripts/Dev.ps1 verify passed:
  **All 173 required tests passed :)** and **BUILD SUCCESSFUL**. This exercises
  dedicated-server loading and existing trap/aura/control/lifecycle suites;
  it does not exercise Minecraft client rendering. No gameplay assertions or
  fixtures were weakened. One compile error in the new aura's local-variable
  declaration was corrected before the successful build and final verification.
- Pending user client review: all silhouettes and throw/arming/readiness states,
  square versus patch-circle semantics, snap/ignition/burst/expiry timing, visible
  targets, grass/snow/pale/dark ground, walls/slabs/stairs/ledges, Minimal, nearby
  observers, shared/self-only auras, root/freeze/chill, reload and lifecycle
  removal. Overlap-scene frame cost is unmeasured; no performance improvement
  is claimed. No client automation/launch, Prism deployment, retained-world
  restart, remote deployment or ATM11 compatibility validation was performed.

## Frost Trap startup resource reload fix (2026-11-10)

- User-reported black main menu was traced to the installed Prism latest.log:
  Failed to create model for covenant:trap, caused by Components not bound yet
  in ItemStack construction inside TrapRenderer. Startup resource reload invokes
  renderer factories before Minecraft 26.1 binds item components.
- Deferred the visual ItemStack allocation to the first in-world extraction.
  Added the exported model particle texture alias #0 to fix its separate warning.
- scripts/Dev.ps1 build passed. Packaged model/texture/UV/PNG audit passed;
  compiled renderer constructor/static initializer inspection confirms no startup
  ItemStack allocation. No client launch or rendered main-menu recheck performed.
- Corrected JAR is in build/libs; Prism still needs the rebuilt artifact deployed.

## Frost Trap visual polish Phase 1 (2026-11-10)

- Original low iron/crystal device: 12 cuboids, 72 faces / 144 triangles, one
  opaque 32x32 atlas at 16 texels/block. A 16x16 cutout skill-tree icon retains
  text fallbacks for other skills and very small nodes. Native-size, enlarged
  nearest-neighbor and mapped isometric/top headless previews were inspected.
- Headless geometry reported zero errors/warnings; mirror matching is not
  applicable to these node names. Interpenetration self-test cannot discriminate
  across groups in this single-group device; mapped overlaps were inspected.
  A separate audit checked all exported face UV spans at one texel/model unit,
  palette/alpha values and the packaged definition -> model -> texture chain.
- Frost's depth-tested square/patch footprint remains visible on Minimal, with
  sampled gaps where same-elevation support or low cover rays fail. Other trap
  and aura presentation remains as before. Trigger/expiry use bounded cosmetic
  cues; no gameplay values, saved state or gameplay lifecycle rules changed.
- `scripts/Dev.ps1 build` passed. Final elevated `scripts/Dev.ps1 verify` passed:
  **All 173 required tests passed :)** and **BUILD SUCCESSFUL**. Logs and the
  resource/UV audit are in ignored `.local/frost-polish/`; mapped model previews
  are in `.local/blockbench/frost-trap/`.
- Earlier runs had one failure each: Control fixture chunk warmup, then Combo's
  native freeze setup beyond the template. Test-only corrections give Control
  independently owned simulation tickets and make Combo movement await its lane
  chunks. Production chunk loading and gameplay assertions were preserved. The
  first sandboxed run also logged a SavedData atomic-rename access error; final
  verification was run outside that sandbox.
- Pending user Minecraft review: first/third-person throw and readiness, trigger
  square versus patch circle, grass/snow/pale/dark ground, slabs/stairs/ledges,
  All/Decreased/Minimal, nearby observers, resource reload, expiry and lifecycle
  removal. No client launch/automation, Prism deployment, retained-world restart,
  remote deployment, ATM11 compatibility or runtime performance measurement.

## Wolf leg surfaces and walk selection (2026-11-10)

- All four shin cubes in the animated source and runtime geometry use a -0.02
  model-unit inset. Front shins sit behind the ruff; rear shins sit behind the
  body/hip surfaces. Body, hips, paws, UVs, atlas, pivots and animation keys are
  preserved. The source audit confirms only those four approved adjustments,
  with 20 cubes, 11 bones and all 1,964 numeric animation keys retained.
- Headless front/rear idle and intermediate walk previews show the corrected
  surface ordering in .local/blockbench/wolf-leg-fix/. Geometry has zero errors
  and eight inherited volume-overlap warnings for rigid joints; GeckoLib and
  animation validation have zero errors/warnings. No new geometry was added.
- The registered controller now recognizes native cached horizontal displacement
  when GeckoLib's native movement flag is absent. It returns to idle on stop,
  ignores vertical-only travel and recall-sized jumps, and preserves hurt/death
  priority. pet_locomotion exercises the actual registered controller handler.
- Animated GameTests use fully compatible NeoForge mock channels and explicitly
  in-memory embedded connections, in an isolated pet environment. Earlier full
  runs exposed missing GeckoLib channels and config-sync setup in the fixtures;
  those fixture errors are corrected without changing production networking.
- Final scripts/Dev.ps1 verify succeeded. Explicit final line:
  All 173 required tests passed :)
  Evidence: .local/wolf-visual-fixes-verify-complete.log. Wolf asset checks passed
  all five clips and 34,926 runtime samples. Packaged model/clip/atlas bytes match
  the source exports; test fixtures remain excluded from the production JAR.
- The final log includes four Windows SavedData atomic-rename AccessDenied
  warnings during early reload checks. The run subsequently completed all tests
  and reported all dimensions saved; it is not proof of a retained-world restart.
- Minecraft client appearance and observed walk transitions remain manual checks.
  No client launch, Prism deployment, commit or push was performed.

## Hunter's Pet (2026-11-10)

- Rank 1 now summons the original animated wolf through a native Wolf subclass:
  35 Focus, 40 health, 3 damage, 30-tick melee interval; defeat/dismissal starts
  600 ticks of cooldown. Existing skill cost/cooldown modifiers apply. Native
  feeding heals; no passive regeneration, sitting, breeding or wolf armor.
- Owner-local snapshots use native entity serialization in the ordinary player
  save path. Pets are not saved in chunks, do not tick offline, and do not force
  chunks. Logout/restart, actual Nether travel and chunk unloading preserve the
  wounded companion; unsafe restore waits on a bounded one-second retry.
- Twelve focused pet tests passed, including native ranged targeting, hostile
  retaliation/following, attack cadence, ally protection, resource/slot authority,
  failed joins and reentrancy, native feeding, cancelled/confirmed deaths,
  health/effects/fire save/load, stored placement, class cleanup and valid/invalid
  datapack reloads with existing summon snapshots preserved.
- Final validation: scripts/Dev.ps1 verify with COVENANT_PET_RESTART_CHECK=verify
  completed successfully. Explicit final line: All 172 required tests passed :)
  Log: .local/pet-verify-final.log. The fresh JVM loaded the previously seeded
  native player file and restored exactly one original pet at 11 health, with
  no extra Focus cost. The GameTest world is disposable; this is a cross-process
  native player-file check, not a retained-world restart or real client reconnect.
- The build's wolfAnimationAssetTest passed: 11 bones, 5 clips, 34,926 runtime
  samples and the 128x64 atlas. JAR inspection found the pet runtime classes and
  exact shipped model/animation/texture/tuning bytes, without tests/examples.
  Language JSON parses; git diff --check passes.
- Manual checks remain wolf facing/scale, idle/walk/attack/hurt/death transitions,
  late tracking and two-client timing, and real client reconnect/retained-world
  restart. Aggregate multiplayer performance has not been benchmarked. No client
  launch, Prism/LAN deployment, publishing, commit or push was performed.

## Status icons, aura outlines and native traps (2026-11-10)

- `scripts/Dev.ps1 build` and final `scripts/Dev.ps1 verify`: compilation,
  all three UI geometry suites, production packaging and dedicated NeoForge
  26.1.2.126 checks passed. Explicit final result:
  **All 160 required tests passed :)**. Evidence:
  `.local/status-polish-build.log` and `.local/status-polish-verify-validated.log`.
- Three new required status tests cover classless allied passive/active receipts,
  unchanged passive refreshes, refreshed deadlines, expiry, source removal,
  spectator visibility boundaries, immediate Dodge/shield consumption, lifecycle
  cleanup, bounded wire round trips, owner-only sync and omitted save data.
  Authoritative area radii and self-only exclusions are also checked.
- The new unsaved `covenant:buff_display` attachment supplies top-left native
  icons. Skills channel revision 6 requires matching clients; saved gameplay
  data is unchanged. Aura outlines run locally with 48/32/0 live native dust
  points for All/Decreased/Minimal; shared radii merge into one six-block ring.
- Native trap block rendering compiles against the pinned model API. Production
  JAR inspection confirms StatusHud, AuraRangeVisuals, TrapRenderer and BuffDisplay,
  with no GameTest classes/resources. Language JSON parses and whitespace checks pass.
- Earlier runs exposed existing test-fixture chunk assumptions. Blood Fever's
  expiry target now leaves range vertically while remaining in its ticking chunk.
  Sliding-combo and control fixtures now await native entity-chunk readiness and
  assert target query visibility; fixture-owned tickets are released on completion
  or readiness timeout. No production chunk forcing or combat tuning was added.
  The original sliding failure did not log query visibility; the new precondition
  addresses that missing guarantee. All affected tests passed on the final run.
- Manual checks remain top-left layout at normal/high GUI scale, open-screen
  tooltips, combo/Threefold countdowns, consumed buffs, aura movement/unslotting,
  particle settings and slopes, plus trap models on full blocks/slabs and in
  darkness. No client launch, Prism/LAN deployment, publishing or remote push.

## Combat feedback and native audio (2026-11-09)

- `scripts/Dev.ps1 verify`: build, all three UI geometry suites, production
  packaging and dedicated NeoForge 26.1.2.126 checks passed. Explicit result:
  **All 157 required tests passed :)**. Evidence:
  `.local/combat-polish-verify.log` and `runs/gametest/logs/latest.log`.
- Four new required feedback tests observe native positional sound events:
  rejected requests stay silent; Multishot emits one release while preserving
  five arrows, ammo and durability; casts emit one start/completion cue and
  cancellation prevents release; Arcane/Fire/Ice wall collisions emit one
  terminal impact; melee whiffs and prevented damage have no contact accent,
  while two damaged targets produce one aggregate contact accent.
- Native arrow save/load omits both cosmetic attachments while retaining
  saved skill provenance. The new visual style is transient and tracking-synced.
  Skills channel revision 5 requires matching clients for the added attachment.
- Pinned Minecraft sound/particle APIs were checked against resolved sources.
  Production JAR contains skill audio and excludes test classes; changed common
  feedback helpers have no client imports. `git diff --check` passed.
- Source review caught and fixed Rage Storm's resolved-definition identity
  comparison before validation. It now selects its cast sound by effect kind.
- Sound balance, first-person aim visibility, effect readability, reduced-particle
  settings and nearby-player perception remain manual client checks. No client
  launch, Prism/LAN deployment, publishing or remote push was performed.
- This completes the first combat feedback pass. Native model/pose improvements
  and progression/Workbench feedback remain subsequent polish work.

## Farming Scything and Automatic Replanting (2026-11-08)

- `scripts/Dev.ps1 verify`: build, all three UI geometry suites, JAR packaging
  and the dedicated NeoForge 26.1.2.126 GameTest suite passed. Explicit result:
  **All 153 required tests passed :)**. Final evidence is in ignored
  `.local/farming-complete-verify.log` and `runs/gametest/logs/latest.log`.
- Five new required Farming tests cover all three mixed-crop area sizes,
  maturity/height boundaries, classless XP, baseline/sneaking single harvests,
  ordinary left-click/tilling, wrong tools/Creative, durability/Unbreaking,
  all five crop replant mappings, inventory/offhand seed use, missing seed,
  right-click/break/placement cancellation, invalid soil, cancelled loot and
  finishing the last replant on hoe break without using a replacement hoe.
- Profession content checks validate Scything widths, planting mappings and
  legacy XP-only crops. Owner-view codec and native wire round trips retain
  rank areas and crop target metadata; live views recognize configured mature
  crops and reject immature ones. Crop targets support client consumption of
  the interaction; no terrain or inventory mutations are predicted.
- The breakage fixture initially counted consumed planting items as broken hoes;
  its replacement listener now filters the original hoe item. Final tests pass.
- `git diff --check` and new-source whitespace checks passed. New/updated Farming
  documentation is UTF-8; this file's preexisting bytes were preserved.
- Client input/animation, same-click offhand suppression, latency/multiplayer,
  retained-world restart and protection/modpack interoperability remain manual.
  No client launch, Prism/LAN deployment, publishing or remote push was performed.

## F3+1 pie visibility (2026-11-08)

- `scripts/Dev.ps1 build`: compilation, all three UI geometry checks and JAR
  packaging passed; evidence: `.local/pie-visibility-build.log`.
- `scripts/Dev.ps1 test`: explicit **All 148 required tests passed :)**;
  evidence: `.local/pie-visibility-tests.log`.
- Extended native scopes cover input, screen updates, Workbench presentation,
  lightning/arrow visuals, bow pose bookkeeping, and movement/freeze checks.
  Existing entity/player callback and HUD scopes now include their early-return
  guards, including checks made on the client without an active effect.
- The native recording test asserts named idle movement/freeze checks and
  aura/bleed/frost/arrow callback guards alongside harvesting, rejection and
  exception cleanup. Gameplay and persistence checks remain in the full suite.
- Bow enter/exit/query scopes close independently of the wrapped vanilla render
  body. No synthetic client slice or combined server/client timer is introduced.
- `docs/DEVELOPMENT.md` documents live F3+1 navigation and its client-thread
  boundary. Rendered pie navigation, client labels and input/visual effects
  remain user playtesting checks. The new JAR has not been deployed by this task.

## Class tooltips, gathering caps and profiler visibility (2026-11-08)

- `scripts/Dev.ps1 build`: compilation, all three UI geometry checks and JAR
  packaging passed. Final build evidence: `.local/profiler-build.log`.
- `scripts/Dev.ps1 test`: explicit **All 148 required tests passed :)** on the
  final run; evidence: `.local/profiler-validation-rerun.log`.
- Native Survival rank tests verify Vein Mining's new 4/8/16/32 total-block caps,
  per-block durability/exhaustion/drops/XP and parity with Tree Felling. Fortune
  and Unbreaking coverage uses the reduced Rank I cap. Other gathering rules,
  rank gates, catalogue validation bounds and custom datapack support are retained.
- `profiler_activity` records actual server metrics around native harvesting,
  classless skill rejection and a throwing extra-harvest callback. The completed
  native profile contains Covenant's named scopes as siblings after early return
  and exception cleanup. An ignored sample is `.local/covenant-profiler-sample.txt`.
  This establishes visibility and balanced scopes, not representative timings.
- The first full run reported a sliding-combo fixture contact failure. The same
  test passed on the full rerun without combat or fixture changes; the initial
  server log is retained in `.local/profiler-validation-first.log`.
- Weaver's description now reflects implemented elemental skills. All class
  tooltips omit the duplicated permanent-choice warning; the shared warning
  remains above the buttons. Rendered tooltips, client profiler/HUD/screen scopes,
  Tracy, third-party profiler displays and multiplayer captures remain manual.

## Gathering drops, tooltips, Weaver tuning and connection versions (2026-11-08)

- `scripts/Dev.ps1 verify`: build/layout checks and dedicated NeoForge server
  validation; explicit **All 147 required tests passed :)**. Evidence is ignored
  `.local/gathering-weaver-network-verify.log`.
- Gathering rank tests now check that every ore/log item drop shares the first
  block's actual native drop point. A new native-drop-event fixture verifies
  added loot, cancelled origin/extra loot, center fallback, ordinary-harvest
  cleanup and untouched nearby loose items. Native durability, enchantments,
  profession XP, stop conditions and partial-tree continuation remain covered.
- Discharge tests cover the expanded five-block spherical radius, its boundary,
  explosion falloff and external-only chains, plus updated Threefold accounting.
- Teleport now continues the horizontal aim component after floor/ceiling contact
  and only terrain obstructs its route. A new Survival fixture checks down-aimed
  floor travel, floor-then-wall blocking, unsupported upward/downward eight-block
  endpoints and an entity in the route. Existing thin-wall, diagonal, ceiling,
  build-height, blocked-cost and native acknowledgement checks also pass.
- Network tests inspect the real registered required channels, accept matching
  releases, reject missing/different releases and verify both version labels in
  NeoForge's mod-specific failure detail. The native server negotiation path is
  exercised with an embedded connection and configuration-listener test double:
  it sends mismatch details and disconnects before any play listener is created.
  Version equality uses `mod_version` plus wire revisions; changed release builds
  must advance their version label.
- Packaged resources retain Teleport range 8 and Discharge radius 5, changed JSON
  has no duplicate keys, and the production JAR excludes test code/data.
- Manual: read profession tooltips at the user's GUI scale, check teleport feel
  against floors/walls and into open air, observe gathered drops, and connect a
  real mismatched client to inspect the disconnect screen. Client/LAN testing,
  deployment, retained-world restart and modpack compatibility were not performed.

## Weaver Discharge, Threefold System and Teleport (2026-11-08)

- Implemented all three at Rank 1 with the accepted provisional defaults,
  server datapack tuning in `data/covenant/skills/weaver.json`, owner-only
  charge/reserve/buff HUD rows and existing authoritative purchase/cast/spend flow.
  See `RPG_SKILLS.md` for the numerical and lifecycle contract.
- `scripts/Dev.ps1 verify`: **BUILD SUCCESSFUL** and the dedicated server's
  explicit **All 144 required tests passed :)**. This includes fourteen new
  Weaver tests, the existing 130 tests and all three geometry/layout checks.
  Final evidence is ignored `.local/weaver-verify-final.log`.
- Survival fixtures cover empty/unlearned/class/resource/control rejection,
  scaled costs/cooldowns, bolt completion versus cancellation, independent charge
  caps, paid empty-area Discharge completion, all elemental damage components,
  max-charge freeze/ignite/explosion/chains, external chain bounds, cover, armor,
  actual invulnerability and preservation of native hurt-window state. Native
  slows use the shared strongest-only calculation.
- Threefold tests cover assignment gates, actual health/absorption/Fortitude
  accounting without overkill, lethal hits, excluded allies/invulnerable hits,
  reserve caps, per-contribution rolling expiry, linear buff strengths, paused
  banking and resumption, additive Battle Cry interaction, projectile snapshots,
  Discharge banking/buff integration, and immediate assignment/lifecycle cleanup.
- Teleport fixtures cover full-range airborne movement, momentum/fall reset,
  diagonal collision without sliding, body/head clearance, thin iron bars,
  build-height bounds, blocked-start rejection without spending, short paid travel,
  native connection ticks and native teleport acknowledgement. Source inspection
  confirms collision lookups use non-loading chunk access; skill routes reject
  unloaded collision-query bounds and never add chunk tickets.
- Real successful/invalid datapack reloads test new tuning, preservation of live
  data after rejection, old active snapshots, charges and player progression,
  resources, cooldowns and loadouts. Native player-save inspection excludes the
  unsaved owner view. The production JAR contains all three skill implementations
  and their tuning resource, excludes test code/data, and changed JSON parses
  without duplicate keys. `git diff --check` passes.
- Manual checks remain: purchase/binding and HUD layout at different GUI scales,
  elemental rotations and full-charge riders, slow/freeze feel, airborne/wall
  blinks, real-client latency/multiplayer movement, retained-world restart and
  modpack/protection interoperability. No client was launched or deployment made.

## Reaver Battle Cry, Rage Storm and Blood Fever (2026-11-08)

- Rank 1 implementations follow the Obsidian Reaver balance proposal. Blood Fever
  is passive while slotted and requires Rend. Rage Storm follows full release-time
  aim, stops at solid terrain, and cannot damage through cover. Higher ranks and
  other proposed Reaver changes remain deferred. See `RPG_SKILLS.md` for the
  implementation contract and `data/covenant/skills/reaver.json` tuning schema.
- `scripts/Dev.ps1 verify`: dedicated server reported **All 130 required tests
  passed :)**, including ten new Reaver tests, existing RPG/profession/itemization
  coverage and all three geometry/layout checks. Evidence: ignored
  `.local/reaver-verify-final.log`. The final packaged build is also checked with
  `scripts/Dev.ps1 build` after updating the command's live tuning readout.
- Native Survival fixtures cover point purchases/prerequisites, free slotted
  passive behavior, five-enemy cap, shared bleed counting, local updates/expiry,
  immediate unbind/class cleanup, shout team/pet eligibility, overlap, duration
  scaling, captured bleed/projectile power, remote source cleanup, scaled Storm
  spending/cast/cooldown, completion-time aim, immediate/repeated pulses, armor,
  actual invulnerability, defended-attempt caps, solid cover/terrain, no outgoing
  skill Rage, unsaved Storm state, unlearning/dimension cleanup, canceled casts
  and rejected entity admission. Real successful/failed reloads preserve live
  balance, player progress, allocations, resources, cooldowns and effect snapshots.
- Blood Fever's final local measurement was **11.1352 microseconds/refresh** for
  1,000 refreshes after 100 warm-ups with 25 nearby bleeding enemies. Earlier runs
  measured roughly 19-28 microseconds. This measures the local fixture/query path,
  not whole-server tick cost or multiplayer scalability. Queries run once per
  five ticks only for eligible slotted Reavers; unchanged modifiers are retained.
- An existing sliding-combo fixture failed once at a different world placement;
  it passed subsequent full runs without gameplay or combo-test edits.
- Client visuals, Storm aiming/feel, real-client allied bursts, latency, reconnect
  and retained-world behavior remain manual checks. No client was launched and no
  Prism deployment occurred. New temporary state is intentionally unsaved; stable
  resource/loadout formats and existing cooldown persistence remain unchanged.

## Vein Mining and Tree Felling (2026-11-08)

- Both gathering abilities are purchasable and use bounded server-owned native
  harvest batches. Shipped rank caps are 8/16/32/64 ore and 4/8/16/32 logs,
  including the initiating block. Sneaking bypasses expansion. Blocks use
  native durability, exhaustion, loot, enchantments and original-block XP;
  cancellation/tool break stops the whole activation. Partial-tree continuation
  uses disposable recognition evidence, with no saved job or terrain loading.
- `scripts/Dev.ps1 verify`: **BUILD SUCCESSFUL**, **All 120 required tests
  passed :)**, including all three geometry/layout checks and six new gathering
  GameTests. Evidence: ignored `.local/gathering-verify.log`.
- Native Survival coverage includes all rank caps and no recursive expansion,
  classless purchases, unpurchased/sneaking/wrong-tool/Creative/FakePlayer
  fallback, face/edge/corner ore-family grouping, stone/deepslate variants,
  placed-origin/extra-ore XP exclusions, Silk Touch/Fortune/Unbreaking,
  per-block drops/durability/exhaustion/XP, initial/extra cancellation, initial
  and extra tool break with one native destroy-item event, respec, conservative
  canopy checks, upper-log exclusion, partial-tree continuation, edited-log
  invalidation and simulated loss of disposable chunk evidence.
- Catalogue checks reject missing/zero/fractional/oversized gathering limits and
  verify owner-view codec round trips. The real isolated resource-reload test
  checks changed batch limits on future harvests, unchanged paid allocations,
  and removed-definition disabling without erasing the paid reservation.
- Production JAR inspection includes the gathering implementation and shipped
  limits and excludes test/local files. SHA256:
  `0542A5D2FBE0C22567FC743AC25824EF3FAACF3F85F6CE897B4D1708C4C6A5D5`.
  `git diff --check` passes.
- Manual checks remain: rendered menu/tooltips, actual mining/chopping feel,
  unusual trees/axes, real-client reconnects, a retained-world restart,
  simultaneous real clients and protection/modpack interoperability. Fixed
  bounds are not a performance benchmark. No client launch, deployment or
  remote update was performed. See [PROFESSIONS.md](PROFESSIONS.md).

## Affix debug commands (2026-11-07)

- Replaced raw-stat apply/inspect with operator-only main-hand `set`, `rarity`,
  `potential`, family-specific `remove`, `clear`, `reset`, and non-Common
  `component` supply. Setup is free and grants no XP, while preserving valid
  itemization limits and unrelated item components. See
  [ITEMIZATION.md](ITEMIZATION.md#debug-commands) for syntax and cleanup rules.
- `scripts/Dev.ps1 verify`: **BUILD SUCCESSFUL**, **All 114 required tests passed :)**. A second
  verification with `COVENANT_AFFIX_RESTART_CHECK=verify` also passed all 114,
  checking the native player file from the first process for unchanged page
  identity, affix family/rarity/tier/amount, equipment rarity and spent Potential.
  Evidence: ignored `.local/affix-debug-verify.log` and
  `.local/affix-debug-restart-verify.log`.
- Refreshed command tests cover permissions/non-player rejection, empty/stacked
  inputs, live definition completion, normal random sampling and exact min/max
  values including negative reductions, add/replace behavior, independent prefix
  and suffix capacities, rarity/tier/Potential downgrade rejection, zero-Potential
  edits, no ingredient/XP changes, cleanup and legacy/unknown definitions,
  enchantment/name/durability preservation, native chest serialization and
  drop/pickup, equipment recalculation, and component identity/count/overflow.
  Command-created gear passes through real Workbench operations with ordinary
  profession checks, ingredients, Potential spending and XP. The isolated reload
  fixture verifies new families/allowlists, explicit item participation without
  category tags, changed roll bounds/Potential, and singleton preservation on reset.
- Production JAR inspection includes the new command class and excludes test/local
  files; feedback translations resolve and `git diff --check` passes. Manual
  checks remain: chat completion/feedback, rendered tooltips and actual Workbench
  interaction, real-client reconnects and a retained-world restart. No client
  launch, deployment, remote update, or pack compatibility test was performed.

## Itemization and Covenant Workbench (2026-11-07)

- Implemented the complete find/salvage/customize prototype: ten affix families,
  five datapack-defined rarities, per-stack Potential, fresh affixed loot and
  bonus equipment, family/rarity components, four Workbench operations, ten
  queued salvage inputs/twelve result slots, shared persistent storage and four
  Crafting nodes with operation/salvage XP. Generic refunds initially cover
  iron/diamond gear. Station art references Minecraft's netherite block.
  See [ITEMIZATION.md](ITEMIZATION.md) for exact boundaries.
- `scripts/Dev.ps1 verify`: **BUILD SUCCESSFUL**, **All 114 required tests
  passed :)**, including three geometry/layout checks and eight new itemization
  GameTests. Evidence: ignored `.local/itemization-verify.log`.
- New tests cover costs, failed initialization/duplicate/capacity rejection,
  all operations and numerical tier access, material identity, native equipped
  bonuses, irreversible Potential/rarity, classless operation XP, specialization
  yields, maximum-output batch stopping/continuation, zero-output/empty-affix
  rewards, two embedded players' stale/replayed menus through native packet
  dispatch, output/hidden-slot restrictions, block-entity/item serialization
  and single removal drops, registered loot generation and chest unpack/reopen.
  An isolated native reload batch checks prospective costs, unchanged saved
  rolls/Potential/progression, explicit item opt-in without a tag, old-preview
  rejection and retention of the live catalogue after invalid reload.
- Dedicated startup/resource loading passed without loot-modifier, recipe or
  itemization errors. NeoForge 26.1.2 loads modifier definitions directly; the
  obsolete global-modifier-list manifest is absent. Production JAR inspection
  confirms itemization classes/data/assets and excludes test/local files.
- Manual checks remain: rendered layout/tooltips/selectors across GUI scales,
  mixed-plank station recipe, loot/crafting pacing, two real clients with
  latency, and retained-world unload/reconnect/restart. Serialization tests do
  not establish a retained-world restart. No client launch, deployment, remote
  update or pack compatibility test was performed. `git diff --check` passes.

## Pending tree rank undo (2026-11-07)

- Skills and Professions now keep nodes active while they have unapplied ranks,
  even when another purchase is blocked by the point budget, rank cap or unlock
  requirements. Resolved 26.1.2.126 `AbstractWidget` source confirms that inactive
  widgets fail `isMouseOver` and request the blocked cursor, which prevented both
  screens' right-click undo handlers from finding these nodes. Left-click
  purchases still use their existing eligibility checks; committed ranks retain
  their respec requirement and pending prerequisite cleanup is unchanged.
- `scripts/Dev.ps1 build` passes, including radial, skill-tree and profession-tree
  layout checks. Evidence: ignored `.local/tree-undo-build.log`.
  `git diff --check` passes. Dedicated GameTests were not rerun for this client-only
  widget change; rendered interaction has not been observed.
- Manual checks: spend the last available skill point and right-click it before
  Apply; repeat at a skill's rank cap and Logging Offensive Mastery's next-rank
  level gate or rank cap. Confirm each click removes one pending rank, restores
  its points, and cannot remove committed ranks after Apply. Also check undoing
  a pending prerequisite removes its pending dependents. No client launch or
  deployment was performed.

## Stalker mobility and arrow skills (2026-11-06)

- Added playable Rank 1 Disengage, Arcane Arrow and Multishot using the existing
  skill tree, bindings, Focus, cooldowns, native arrows and server-owned movement.
  Higher ranks, existing Stalker balance conversions and server-datapack skill
  tuning remain deferred. See [RPG_SKILLS.md](RPG_SKILLS.md#stalker-mobility-and-arrow-skills)
  for the agreed values, defense rules, snapshots and persistence boundaries.
- `scripts/Dev.ps1 build` passes, including radial, skill-tree and profession-tree
  layout checks. Final evidence: ignored `.local/stalker-build-final.log`.
- `scripts/Dev.ps1 test` reports **All 106 required tests passed :)**, including
  ten new Stalker tests, then saves all dimensions and exits with `BUILD SUCCESSFUL`.
  Final evidence: ignored `.local/stalker-tests-final.log`.
  Coverage includes activation/cost/cooldown gates, Rank 1 availability,
  pitch-independent airborne movement, exact arc and endpoint, fall breaking,
  native collision, owner prediction/packet roundtrip, attacks during travel,
  root/knockback/unlearning/lifecycle cleanup, autosave gravity restoration,
  fixed magic damage, absorption and immunity gates, additive Shock vulnerability,
  tipped/spectral effects, Infinity and special-ammunition consumption, fan spread,
  pickup limits, damage attempts across siblings and independent fans, actual
  flight/cover, native arrow/target serialization, expiry, Dodge, shield behavior,
  Fortitude-only absorption and pet exclusion.
- Native source/tag inspection confirms armor-bypassing magic inherits shield
  bypass through Minecraft's `bypasses_shield` tag. Arcane Arrow preserves that
  native behavior and remains dodgeable; Multishot remains shield-blockable.
  The root-persistence fixture now acquires/releases a test-only chunk ticket
  for its off-template loaded target. The new flight fixture also waits for its
  test-only chunk tickets to make its lanes visible. Gameplay forces no chunks.
- `git diff --check` passes. Production JAR inspection includes the new gameplay
  classes and damage resource, and excludes tests/fixtures/local files.
  `build/libs/covenant-0.1.0-dev.jar` SHA256:
  `B501C46BF1F351375C889C18A56C6129967E4C7B1458B7F69D89BB329BA44F2C`.
- No client launch or deployment. Manual checks remain for leap feel, rendered
  bow/projectile/status feedback, multiplayer latency and a retained-world
  restart; the short checklist is in [RPG_SKILLS.md](RPG_SKILLS.md#in-game-testing).

## NeoForge 26.1.2.126 update (2026-11-06)

- Updated the project loader pin from `.125` to `.126`, matching
  [ATM11 0.14.0-beta](https://www.curseforge.com/minecraft/modpacks/all-the-mods-11/files/9426887).
  Minecraft `26.1.2`, Java 25, ModDevGradle and Gradle are unchanged.
- The [official changelog](https://maven.neoforged.net/releases/net/neoforged/neoforge/26.1.2.126/neoforge-26.1.2.126-changelog.txt)
  lists one change since `.125`: a missing damage-container fallback when
  blocking a goat's attack. Reviewed the
  [upstream patch](https://github.com/neoforged/NeoForge/pull/3715);
  no Covenant API adaptations were needed. The resolved POM's dependencies
  are unchanged between these releases.
- `scripts/Dev.ps1 build` passes, including radial, skill-tree and profession-tree
  geometry checks. Evidence: ignored `.local/neoforge-126-build.log`.
- `scripts/Dev.ps1 test` loads the dedicated server on NeoForge `26.1.2.126`
  and reports **All 96 required tests passed :)** at 20:43:37.
  Evidence: ignored `.local/neoforge-126-tests.log`.
- Inspected `build/libs/covenant-0.1.0-dev.jar`: the generated NeoForge minimum
  is `[26.1.2.126,)`, Minecraft remains `[26.1.2]`, and test classes/fixtures
  are excluded. `git diff --check` passes.
- Prism's development instance was inspected and still uses `.125`; it has
  not been upgraded or deployed to. The documented LAN installation was not
  inspected or changed. Update those loaders before deploying the new build;
  the existing Prism version guard will reject `.125`.
- Pending manual checks: launch the isolated instance on `.126`, verify the
  Skills/Professions menus and ordinary combat/dash behavior, and block a goat's
  ram with a shield. Full ATM11 compatibility and real-client behavior remain
  unvalidated; matching the loader alone does not establish pack compatibility.

## Natural profession activity XP (2026-11-06)

- Implemented classless, server-owned XP from native ore/log/mature-crop
  harvesting, credited breeding and enchanting-table completion. Reuses saved
  profession tracks, the provisional curve, caps, paid allocations and owner
  synchronization. Gathering abilities and Workbench/itemization remain deferred.
- `scripts/Dev.ps1 build` passes, including all three geometry/layout checks.
  Final evidence: ignored `.local/activity-build-final.log`.
- `scripts/Dev.ps1 test` reports **All 96 required tests passed :)**, including
  seven new activity tests, followed by clean shutdown and `BUILD SUCCESSFUL`.
  Final evidence: ignored `.local/activity-tests-final.log`, with the required-pass
  line at 20:10:17. Coverage includes real Survival harvesting, every initial ore
  family, deepslate/Nether variants, Fortune/Silk Touch, incorrect tools, late
  break and placement cancellation, Creative/FakePlayer exclusions, native chunk
  serialization/load with another miner, piston extension and sticky retraction,
  crop maturity, sequential tree chopping, persistent-leaf/structure rejection,
  cached-log replacement, native breeding attribution/cancellation and native
  enchanting costs/option rewards/replays. The isolated profession reload test
  also verifies unchanged earned XP and new rewards on future native harvests.
- Retraction validation found and fixed an ordering issue: provenance must be
  captured from vanilla's resolved movement list after its old head removal,
  not from a second resolver before that removal. Two existing long-lane
  fixtures (combo sliding and lifecycle cancellation) now acquire/release
  test-only chunk tickets and allow native entity visibility to settle before
  their queries. Gameplay does not force chunks.
- `git diff --check` passes. Production JAR inspection includes the activity
  hooks, harvesting definitions and ground tag, and excludes test/local files.
  `build/libs/covenant-0.1.0-dev.jar` SHA256:
  `6B2A41A79FE468CD913B2E71D3E6FBA95A061B11ABC2CD825F283B33C861BB34`.
- No client launch or deployment. Actual leveling time, rendered menu updates,
  broader natural/sapling-grown tree coverage and a manual saved-world restart
  remain user playtesting. Automated persistence coverage uses native chunk
  serialization/load, not a retained-world restart in a separate process. See
  [PROFESSIONS.md](PROFESSIONS.md#manual-client-checks) for the checklist.

## Expanded profession foundation (2026-11-06)

- Implemented the classless four-tab Professions entry in radial slot 3,
  immutable paid-cost allocations, bounded server-validated Apply, owner-only
  catalogue presentation, provisional XP/reward data, operator testing commands,
  and all-four native held-use Profession Respec. Logging's Offensive Mastery is
  functional; the other sixteen nodes and natural activity XP remain deferred.
  All tuning remains provisional.
- Seven new dedicated GameTests cover legacy-empty/paid-cost codecs, native
  player files and death copying, classless independent XP/caps, atomic invalid
  and stale purchases, forged costs/refunds, malformed packets, native melee,
  arrow and skill-source damage, summon/tag exclusions, respec interruption and
  completion, and real successful/failed datapack reloads including offline and
  removed-definition reservations. The reload test caught listener replacement
  resetting the catalogue revision; revision tokens now span listener instances.
- Final combined verification passed all three geometry checks (radial, combat
  tree, profession tree) and **All 89 required tests passed** on the dedicated
  server. The profession geometry check covers 0-32 nodes across four viewport
  sizes. Evidence: ignored `.local/professions-foundation-final.log`, including
  the explicit native required-test pass line. An earlier post-fix full server
  run also passed 89/89.
- JAR inspection found the profession runtime/client classes, authored data,
  damage tag and respec recipe/assets; test namespaces/classes and local artifacts
  were excluded. English localization JSON parses. Production artifact:
  `build/libs/covenant-0.1.0-dev.jar` (SHA256
  `85F7A99E3342E95666EC656CB3B75391296B3F408866289871B46D9FEADCD494`).
- No client was launched or deployed. Manual radial input, tab/draft/undo/Apply,
  resized labels/tooltips and held-use feel remain for the user; see
  [PROFESSIONS.md](PROFESSIONS.md). Automated geometry and server tests do not
  establish rendered behavior. Crafting XP amounts and extra recipe milestones
  remain design questions rather than guessed implementation defaults.

## Progression combat slice (2026-11-06)

- Implemented combat XP/levels, equal rounded contributor shares, source-owned
  support, dimension/global caps, atomic first-Wither levels, points/ranks,
  staged Apply and crafted held-use respec. Four independent profession tracks
  had saved state at this stage; the profession foundation above adds a curve and first ability.
  Non-arrow playable skills allow Rank 1 until upgrades are authored.
- Dedicated checks cover native damage/death and cancellation, support-only
  participation and overlapping suppliers, full Fortitude absorption, capped,
  offline and dead-player payouts, contribution/combat expiry, actual player
  files and death/End copying, Wither claims and mixed group rewards, source
  dimensions and native tag precedence, atomic purchases and bounded packets,
  two-second item use/interruption and native arrow launch velocity.
- An isolated fixture creates a disposable datapack, performs real successful
  and rejected resource reloads, verifies raw XP/level/claim/allocation
  preservation for online and offline data, then restores the original packs.
  An early test exposed that generated vanilla dimensions are not ordinary
  level-stem tag entries; grouping now uses native dimension-type tags.
- Fresh saves are required. Both sides use payload protocol 3. No client was
  launched or deployed. Manual: XP bar positioning/F1/GUI scales, Stats values,
  draft clicks/tabs/close/Apply, respec recipe/animation, arrow feel, live
  reconnect/dimension travel, retained-world restart and leveling pace.
  Apotheosis/ATM11 integration remains deferred.
- Final automated run: `scripts/Dev.ps1 verify` built the production JAR,
  passed both geometry checks, and reported **All 82 required tests passed**.
  `git diff --check` passed. JAR inspection confirmed progression classes,
  balance data, respec recipe/models and HUD, with test/local artifacts excluded.
  Evidence: ignored `.local/progression-final-2.log` and
  `build/libs/covenant-0.1.0-dev.jar`.

## Skills architecture hardening (2026-11-06)

- Added shared server control and lifecycle boundaries, family execution helpers,
  a pending-cast snapshot, saved native-projectile combat metadata, a separate
  request ingress budget, Frost membership reconciliation and endpoint-based
  lightning visuals. Existing player save IDs and gameplay tuning are retained.
- `scripts/Dev.ps1 verify` builds the production JAR and runs dedicated GameTests.
  Final run: **all 71 required tests passed**, including eight new fixtures.
  Native packet/connection tests cover controlled movement, delayed corrections,
  external impulses, teleport handoff, invalid movement, instant item use and
  spear attacks. Other new coverage exercises registered request handlers,
  actual arrow save/load/impact, late death cancellation, accepted death before
  player copying, lifecycle cancellation, and lightning payload encoding.
- Build geometry checks passed. The production JAR contains the new runtime and
  client mixins and excludes GameTest classes/resources. `git diff --check` passed.
  An earlier run exposed the existing arrow fixture's dependence on randomized
  chunk placement; all arrow lanes now receive fixture-only ticking tickets.
  Production skills still do not force chunks.
- No client was launched or deployed. Live two-client latency, observer visuals,
  native teleport during dash prediction and control feel remain manual checks.
  The new client teleport hook was checked against pinned sources and compiled;
  dedicated tests exercise its shared prediction model, not rendered client code.
  Broader modpack compatibility and retained-world process restart remain manual.
- Both sides require the same build (skill payload protocol 2). Existing arrows
  without the newly saved metadata cannot recover their old provenance/snapshots.
  Mounted vehicle steering and mining retain their existing policies.

## Dash direction on wall contact (2026-11-05)

- Fixed both dashes converting blocked forward motion into full-speed sideways
  movement. Launch aim remains normalized and captured at movement start. Wall
  collisions now clip the blocked horizontal components without renormalizing the
  surviving direction. Floor/ceiling contact still flattens pitch while preserving
  horizontal heading and speed; simultaneous contacts flatten before wall clipping.
  Distances, base times, costs, damage, and combo progression are retained.
- `scripts/Dev.ps1 test` passed all 63 required dedicated-server GameTests. Expanded
  sliding checks cover both skills hitting walls with tiny left/right aim offsets,
  owner prediction, unchanged floor travel, original tangential wall speed, ceiling
  contact, and Rush's actual swept hit path. Earlier runs exposed a contact-fixture
  placement issue after adding lanes; appending the new lanes preserved its original
  chunk placement and the complete final suite passed.
- `scripts/Dev.ps1 build` and `git diff --check` passed. No client was launched.
  Manual check: dash almost straight into a wall with small left/right aim offsets,
  then compare a deliberate diagonal wall approach and downward floor aim.

## Ignore skill cooldowns debug toggle (2026-11-05)

- Added operator-only, self-targeted `/covenant skill ignorecooldowns` with optional
  `true|false`. This independent unsaved runtime flag bypasses cooldown validation
  for instant skills, cast start/completion, and combo starts. Saved cooldowns still
  commit and elapse; disabling restores remaining timers. Skill inspection reports
  the mode and actual timers. Class changes preserve the flag on the same entity;
  native save/load and replacement/respawn omit it.
- `scripts/Dev.ps1 test` passed all 63 required dedicated-server GameTests. The new
  fixture checks command permissions, console rejection, per-player scope, toggle
  and explicit modes, instant/cast/combo reuse, normal spending/casting/throttling,
  saved timer preservation, mid-cast disabling, native save/load, and death copying.
  The first run had a corrected new-test cost expectation and an existing arrow
  collision fixture failure; both passed in the complete final rerun.
- `scripts/Dev.ps1 build` passed, including the geometry checks. `git diff --check`
  passed. Client testing remains manual: enable, repeat a learned skill during its
  cooldown, disable, and confirm the remaining cooldown is enforced.

## Faster dashes and surface sliding (2026-11-05)

- User playtesting confirmed the preceding connection-anchor fix worked, then
  requested twice-faster travel and smooth movement along floors/walls. Lunging
  Combo now travels four blocks over three ticks (0.15 seconds); Rip and Rush
  travels six blocks over four ticks (0.2 seconds). Their distances, costs,
  cooldowns, follow-up windows, and status/damage tuning are retained.
- The shared server/owner collision solver now moves to first contact, removes
  blocked normal components, and spends the remaining tick fraction along the
  surface tangent at unchanged speed. Floors, walls, and ceilings permit glancing
  slides without prematurely completing the stage. Full blockage still stops
  movement. Steering stays captured, gravity stays suspended, and normal momentum
  cleanup/interruption rules remain. Sliding direction persists through the stage.
- Rush checks hits per actual movement segment, so a bent path does not become
  one broad diagonal hit sweep. Its existing per-enemy attempt limit, defenses,
  shred application, and authoritative ownership are preserved.
- `scripts/Dev.ps1 test` passed all **62 required dedicated GameTests**, including
  the new `combo_sliding` fixture. It checks both faster speeds/distances through
  full connection ticks with downward ground aim, floor/wall corners, glancing
  ceiling contact, actual sliding-path Rush hits, untouched off-path enemies,
  and owner prediction using the same collision solver. Existing direct-impact,
  3D movement, control/knockback, packet handoff, lifecycle, and resource tests pass.
  Evidence: ignored `.local/dash-sliding-tests-final.log`.
- `scripts/Dev.ps1 build`, both geometry checks, and `git diff --check` passed.
  Build evidence: ignored `.local/dash-sliding-build-final.log`. The built JAR
  includes updated movement/description resources and excludes GameTest fixtures.
- No client was launched. Manual check: compare both faster dashes on a flat
  floor with level/downward aim, glance along walls/corners, aim upward/downward
  in open space, and verify direct head-on stops plus root/freeze/knockback
  interruptions. Retest rendered feel and owner/observer latency in-client.

## Dash connection-tick rewind (2026-11-05)

- User playtesting found smooth predicted motion followed by a return toward the
  starting position. The new `combo_connection_movement` regression reproduced
  the server-side cause through the full native `connection.tick()`: the first
  lunge step should travel 0.6667 blocks but ended at zero displacement. Evidence:
  ignored `.local/dash-connection-repro.log`; the prior 60 fixtures still passed.
- In pinned 26.1.2, `connection.tickPlayer()` caches `firstGood`, calls
  `ServerPlayer.doTick()`, then restores the cached coordinates. Each
  authoritative dash step now calls `connection.resetPosition()` after native
  movement, committing the new position to that cache without sending a packet.
  The existing smooth client path and dash gameplay rules are retained.
- The regression passes for both the four-block/six-tick lunge and
  six-block/eight-tick dash, checking each step after a complete connection tick,
  matching cached coordinates, no increment in native teleport IDs, retention of
  the endpoint through a later connection tick, rejection of late starting-point
  packets, and ordinary walking after endpoint acknowledgement.
- `scripts/Dev.ps1 test` passed all **61 required dedicated GameTests**. The
  earlier tests invoked `doTick()` or posted player-tick events directly, which
  omitted the outer restore; this coverage gap is now documented in the workflow.
  Evidence: `.local/dash-connection-tests-final.log` (ignored).
- `scripts/Dev.ps1 build`, both geometry checks, and `git diff --check` passed.
  Build evidence: `.local/dash-connection-build-final.log` (ignored).
- No client was launched. Manual check: repeat the same open-ground dashes that
  returned toward their origin, then walk immediately afterward; also recheck
  upward/downward travel, terrain stops, and interruptions. The reproduced server
  rewind is fixed; the resulting rendered behavior still needs user verification.

## Predicted dash movement (2026-11-05)

- User playtesting reported choppy Lunging Combo and Rip and Rush movement with
  the initial per-tick teleport implementation. That path is replaced by
  owner-only start/stop trajectories, native client collision movement with
  ordinary frame interpolation, and separate authoritative server collision/hits.
  Distances, travel times, costs, follow-up windows, and interruptions retain
  their existing tuning and server ownership.
- Normal dash travel sends no native teleport stream. The owner acknowledges
  the completed sequence after reaching the server endpoint. Late predicted
  position packets cannot rewind the server; matching acknowledgements reset
  vanilla's movement anchor. A mismatched endpoint or missing acknowledgement
  after 40 ticks uses one correction preserving velocity. Client prediction is
  scoped to the player/dimension; removed entities release the server packet hold.
- `scripts/Dev.ps1 build` passed with both geometry checks; all **60 required
  dedicated GameTests** passed through `scripts/Dev.ps1 test`; `git diff --check`
  passed. Evidence: ignored `.local/dash-prediction-build.log` and
  `.local/dash-prediction-tests.log`. Production JAR inspection confirms the new
  trajectory/prediction/client mixin classes and excludes test fixtures.
- New coverage exercises trajectory/acknowledgement codecs, distributing starting
  error over a constant predicted path, endpoint convergence, immediate knockback
  handoff, stale/duplicate transitions and acknowledgements, no native teleport
  increments through normal travel, rejection of late rewind packets, ordinary
  walking after acknowledgement, mismatched endpoint recovery retaining impulse,
  and acknowledgement timeout recovery. Existing movement, hit/bleed/shred,
  lifecycle, gravity-save, combo, and infinite-resource fixtures remain passing.
- The prediction model and server path were tested without a renderer. Client
  code compiled, but physical-client mixin application, rendered smoothness,
  camera feel, remote-player interpolation, and real latency are unverified.
  No client was launched. Manual checks: compare both dashes on open ground,
  upward/downward aim and terrain stops, turn/hold movement during travel, resume
  walking immediately afterward, and interrupt with root/freeze/native knockback
  or Blast Trap. Check owner and observer views under actual network latency.

## Infinite class-resource debug command (2026-11-05)

- Operator-only, self-targeted `/covenant resource infinite` toggles resource
  requirements/spending off; append `true` or `false` for an explicit setting.
  `/covenant resource` reports the state. Class selection and all non-resource
  activation checks remain authoritative. Stored/HUD balances and ordinary
  recovery/decay continue normally.
- The flag lives only in `SkillRuntime`, survives class switches on the same
  entity, and is neither saved nor copied on replacement/respawn. Disabling it
  during a pending cast restores the resource check at completion.
- `scripts/Dev.ps1 build`, including both geometry checks, and `git diff --check`
  passed. `scripts/Dev.ps1 test` passed all 59 required dedicated GameTests.
  Evidence: `.local/infinite-resource-build.log` and
  `.local/infinite-resource-tests-final.log` (ignored).
- The new fixture checks permission/console rejection, toggle and explicit
  on/off semantics, all three classes at zero balance and above-capacity cost,
  unchanged saved balances, player isolation, save/load and death-copy reset,
  instant cooldown retention, zero-resource cast completion and staged combos,
  and restoring resource rejection when disabled during a cast.
- Manual client smoke check: toggle on, activate a learned skill at zero resource,
  inspect the state with `/covenant resource`, then toggle off and retry after its
  cooldown. No client was launched for this command change.

## Combo skills and paid whiffs (2026-11-05)

- Lunging Combo and Rip and Rush now execute their three-stage prototypes.
  Cleave, Rend, and all combo attacks accept empty targets/areas as paid whiffs;
  normal resource spending, progression, and cooldown rules still apply.
- `scripts/Dev.ps1 test` passed **58 required dedicated GameTests**, including
  seven new combo fixtures and updated Cleave/Rend regressions. Evidence:
  ignored `.local/combo-tests-final.log`. This loads the new common mixins and
  native status on a dedicated server; client rendering is not exercised.
- `scripts/Dev.ps1 build` passed with JDK 25, including both geometry checks;
  `git diff --check` passed. Evidence: ignored `.local/combo-build-final.log`.
  Production JAR inspection confirmed combo classes/mixins and excluded all
  test classes and fixtures. Saved profile, item-page, resource, cooldown, and
  loadout formats and existing IDs are retained; the new native harmful-status
  ID is `covenant:armor_shred`. No migration of existing saves is required.
- Combo fixtures cover independent interleaving and windows, current per-stage
  costs, stage-one cooldown snapshots, empty attack completion, resource/root
  rejection without deadline refresh, exact timeout boundaries, expiration during
  real aura freeze, native player ticking, movement-packet position rejection,
  captured 3D direction, horizontal/upward/downward travel, short wall stops,
  restored gravity/fall tracking, effective/resisted native knockback, and the
  actual Blast Trap effect path preserving its explicit impulse.
- Native damage checks cover circular Rend and normal shared stacks, dash and
  attack damage coefficients, shared shred without toughness changes or strength
  stacking, retaining a longer refresh, cleansing and native immunity hooks,
  all-owner bleed consumption, absorption, lethal-hit credit, and preservation
  of bleeds on invulnerable finishers. Speed checks cover the +30% cap, duration
  snapshots, weaker/shorter replacement, no refresh on zero consumption, expiry,
  and binding cleanup after combo completion. A damaging callback cannot re-enter
  and double-spend/advance the combo stage.
- Lifecycle fixtures cover dependent unlearning, duplicate/unchanged/rejected
  assignments, class changes, logout/dimension events, actual death and native
  replacement copying, persistent cooldown serialization, paused target-owned
  shred serialization, target death clearing, and autosaving during movement
  without persisting the temporary native gravity flag. These are controlled
  server fixtures, not a live multi-client or cross-process movement test.
- Manual checks: bind both combos and interleave them, try empty attacks, test
  wall/floor/ceiling travel and interruptions, and check the independent HUD
  countdown/action lines with Fortitude, air/mount indicators, GUI scales, and
  F1. Assess movement feel and reconciliation under actual client latency.
  Icons/circular cooldown displays remain deferred; no client was launched.

## Eleven auras (2026-11-05)

- All eleven aura skills are executable: Dodge, Swiftness, Ripping, Fortitude,
  Raging, Icy Veins, Flame Cloak, Freezing, Shock, Immolation, and Enchanting.
  Existing class, learning, prerequisite/exclusion, and saved-slot rules apply;
  aura commands also require a slot assignment.
- `scripts/Dev.ps1 build` passed with the project JDK 25, including both geometry
  checks. `scripts/Dev.ps1 test` passed **51 required dedicated GameTests**,
  including fourteen new aura fixtures and the existing bleed/trap/bolt/Rage suite.
  Evidence: ignored `.local/aura-build-final.log` and `.local/aura-tests-final.log`.
- Coverage includes source/teammate/pet/outsider membership, spherical edges and
  ignored cover, free upkeep, duplicate assignments, lingering coverage, fixed
  active recipients, independent source expiries, team changes, source class and
  slot cleanup, exact native modifiers, ordinary Dodge's 85% cap, shared guaranteed
  Dodge consumption, pets, and Swiftness's additive speed and slow cleanse.
- Native damage fixtures cover separate Fortitude mitigation/overflow/replacement,
  ordinary absorption coexistence, exact shield-only Rage accounting and Rend
  bleed setup without outgoing skill Rage; Ripping pair limits and additive
  Ripping/Raging bleed snapshots; one Rend stack; retained bleed power; Raging
  decay, combat grace and spending; Icy Veins cooldowns and repeated instant spells;
  freeze interruption/input/jump/native mob and player attack denial, damage and
  external velocity, successive freeze and expiry; shared Immolation cadence and
  source handoff; Flame Cloak fire reduction/retaliation; and exact Lightning chains
  with cover and target limits. Covenant slow suppression/resumption preserves
  Chilled's existing serialization/expiry regression coverage.
- Enchanting covers charged and weak native player melee, partial native bow
  firing, full crossbow multishot snapshots, committed-shot source cleanup,
  per-projectile/per-target gates, piercing targets, inactive-class Mana, both
  damage portions, and preserved initial hurt immunity. Aura lifecycle fixtures
  exercise native save/load, death replacement, actual caster death, logout and
  dimension lifecycle hooks, cooldown retention, shield expiry/display sync, and
  absence of saved temporary modifiers or capacity.
- Production JAR inspection found aura classes and damage definitions and no
  GameTest classes/fixtures. `git diff --check` passed. No registry versions or
  saved IDs changed. No client launch, Prism deployment, or performance benchmark
  was performed.
- Manual client checklist: cross fields quickly and check 0.5–1s exit grace and
  10% slow feel; verify teammate/pet benefits through cover; watch Fortitude's
  separate HUD row deplete, refresh and disappear; check freeze controls with
  knockback and consecutive casters; inspect Lightning chain/activation feedback;
  remove/change aura assignments and verify immediate cleanup. Check real-network
  reconnect, death/dimension travel, multiplayer visuals, and representative
  third-party AI. Native action hooks do not establish universal modded AI support.

## Weaver elemental bolts (2026-11-05)

- `scripts/Dev.ps1 build` passed, including both geometry checks.
  `scripts/Dev.ps1 test` passed all **37 required dedicated GameTests** and the
  server saved and exited normally. Evidence: ignored
  `.local/elemental-build-final.log` and `.local/elemental-tests-diagnostic.log`.
- Three new fixtures exercise Fire/Ice projectile collision and removal, first-hit
  behavior, Mana/cooldowns, duration scaling, Chilled refresh/expiry/native
  serialization, burning refresh, Fire Resistance, absorption, invulnerability,
  Dodge and canceled-hit status gating. Lightning covers its wind-up, first-target
  damage, absence of status, and solid-cover blocking. Existing skill-tree coverage
  now expects twelve implemented skills. Bleed fixtures have separate heights to
  avoid overlap between their wide test areas.
- Production JAR inspection found the new classes and all three damage definitions,
  with no GameTest classes/fixtures. `git diff --check` passed.
- Manual client checks remain: learn/bind all three Weaver bolts; check Fire/Ice
  travel, impacts, burning/extinguishing and Chilled movement; check Lightning's
  cast, current aim, 24-block limit and wall blocking. Check Mana/cooldown feedback,
  status removal/reconnect, and multiplayer visuals. Client rendering and feel
  were not observed. No Prism deployment was requested or performed.

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

- Range tuning: all traps now use a 1.5 Ã— 1.5-block contact footprint (0.75-block
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
