# Environment validation

Verified on **2026-11-03**, America/Winnipeg, on Windows 11 x64.

## Elemental Golem full Rank 1 skill (2026-11-10)

- `scripts/Dev.ps1 verify`: **All 202 required tests passed :)** and
  **BUILD SUCCESSFUL**. Four new skill cases join the five existing golem cases
  and the Hunter's Pet/other gameplay regressions. The skill is now available
  through the ordinary Weaver level-25 tree/assignment path.
- `golem_skill`: paid summon, initial Arcane, free same-entity cycling at zero
  Mana, the fixed five-tick switch delay despite cooldown scaling, last-slot
  dismissal, duplicate assignments and summon-time dismissal cooldown snapshots.
- `golem_benefits`: all form stat transitions, owner-only spell power, actual
  Arcane spell release/impact retaining exactly one Fire bonus after switching,
  no physical-attack bonus, spherical 12-block boundary, Lightning capacity
  without Mana gain, permanent excess clamping, no healing on switches and
  the existing owner-only buff display.
- `golem_persistence`: native player attachment save/load with registry-aware
  problem reporting, wounded health, selected form, effects and UUID retention,
  no duplicate pre-spawn or summon payment/cooldown, plus a stored attachment
  codec round trip and safe restoration through the real player cadence.
- `golem_repair_defeat`: 25-HP native ingot repair/cap, no item consumption at
  full health or on cancelled healing, cancelled death preservation, confirmed
  defeat/cooldown/benefit removal, owner XP credit and owner combat marking.
- Existing golem cases now exercise the 60-HP / 3-damage / 40-tick profile,
  shared native navigation/retaliation, tall-body placement rejection, unload
  storage, lifecycle boundaries and existing body/core animation controllers.
  Fixture-only tickets wait for actual entity readiness, allow a bounded
  1000-tick setup/execution window and clean up on timeout. Production does not
  force chunks or scan world entities.
- `golemAssetTest` and the wolf asset/runtime tests pass. The local JAR audit
  (`.local/audit-golem-skill.py`) byte-matches eleven golem resources, checks
  balance/form labels and the original 16×16 binary-alpha skill icon, and
  excludes all test fixtures. `git diff --check` passes.
- Pending user playtest: buy/slot/use at Weaver level 25; form feedback, status
  tooltips and range feel; walk/attack/hurt/death playback; iron repair; actual
  reconnect, fresh-server restart, dimension travel and multiplayer observers.
  Native serialization automation does not establish those client/live-server
  scenarios. No client launch, deployment or remote publication was performed.

## Elemental Golem first animation set and form accents (2026-11-10)

- Integrated Idle/Walk/Attack/Hurt/Death plus an independent slow core loop, with
  four cosmetic appearances and minor family particles. `/covenant golem summon
  <form>` and `/covenant golem form <form>` are operator-only appearance controls.
  Automatic form mechanics, benefits, full skill and persistence remain deferred.
- `scripts/Dev.ps1 verify` passed **All 198 required tests passed :)** and
  **BUILD SUCCESSFUL**. The new `golem_animations` test exercises registered
  body/core controllers, idle/walk selection, completed attack/hurt release,
  negative-finished-timeline recovery, death interruption/hold, bounded core
  speed/death stop and appearance-only attribute preservation. It reuses the
  generic companion fixture and keeps the existing wolf regressions. Native
  movement/melee/targeting/placement/lifecycle tests remain passing.
- All six clips in each animated source pass 60 Hz headless checks: zero
  errors/warnings, skipped channels, Bezier approximations or capped clips.
  Ground tolerance is 0.05 model units; detachment tolerance is 0.5. Static
  geometry retains eighteen documented intentional rigid-joint overlap warnings.
- `golemAssetTest` bakes all four 17-bone/34-cube geometries and native PNGs with
  GeckoLib 5.5.2. Dense runtime samples cover finite motion, body/core bone
  ownership, loop seams, opposed stride, contact-first/recovering attack,
  kneeling death and slow turn/tilt. Core corner-radius bounds prove full depth
  clearance after the half-unit pivot/geometry adjustment; no texture changes.
- Source/export audit passes: all approved static sources are unchanged, PNG
  pixels and UVs match, only the explicit core clearance adjustment differs in
  animated copies, four exports agree on one shared animation file, and staged
  runtime resources match. The packaged JAR audit passes all nine resources,
  appearance localization, common/client separation and exclusion of test fixtures.
- Fixed-camera preview uses the pinned headless render engine, nearest texture
  sampling and the same camera/floor across clips: 486 frames, two views,
  play/pause, replay, scrubbing and slow motion. Representative stride, strike,
  kneel and core poses were visually inspected. Body/core are shown separately;
  runtime combines them. Minecraft particles are not rendered by this preview.
- Evidence: ignored `.local/blockbench/golem-animations/{verify.log,preview.json,
  source-audit.json,*-animation-validation.json,export-*-report.json,render-review.log,
  audit_runtime.py,review.html}`; recipes/sources in `models/elemental_golem_animated/`.
  The original static sources remain in `models/elemental_golem/`.
- Particle limits are code bounds: two locally requested children per eight entity
  ticks, existing two-to-six-tick lifetimes, native limiting, no server packets or
  global scan. This does not measure GPU/CPU performance or establish appearance.
  Minecraft playback/blending, all four particle families, particle settings,
  resource reload, actual multiplayer/latency and crowded scenes remain unverified.
  No Minecraft client launch or deployment was performed.

## Elemental Golem movement/follow/melee slice (2026-11-10)

- User-approved model/textures; scope is the static arcane golem with movement,
  following and native melee. Operators use `/covenant golem summon` and
  `/covenant golem dismiss`. Full skill, elemental benefits/form switching,
  persistence and animation remain deferred. See `docs/RPG_SKILLS.md`.
- `scripts/Dev.ps1 verify` passed **All 197 required tests passed :)** and
  **BUILD SUCCESSFUL**. Four new native tests cover canceled/reentrant/duplicate
  summons, full-body low-ceiling rejection, shared landed-damage/retaliation
  targeting and ally protection, walking follow, pursuit and repeated native
  melee with attack spacing, hostile retaliation, far-owner recall, dismissal,
  owner lifecycle cancellation and chunk-unload reference cleanup. Fixtures
  wait for entity-ticking chunks and release their own temporary tickets.
- Existing Hunter's Pet tests pass after extracting shared targeting, placement
  and recall into `Companions`. The wolf keeps its native goals, animation
  controllers, balance and saved lifecycle. The full existing suite also passes.
- `golemAssetTest` bakes the shipped geometry through GeckoLib 5.5.2: 17 bones,
  34 cubes and the 128×128 PNG. There are zero animation controllers and no
  animation resource. Pinned source inspection established that the runtime
  skips controller evaluation when none exist, but rejects empty animation files
  in development. No dummy clip was authored.
- Local `gecko-smoke` passed seven checks, including explicit static-export
  behavior and preservation of animated/unsupported-feature rejection checks.
  Static mode exports geometry/PNG only. The packaged JAR audit confirms source
  geometry/PNG equality, the approved arcane atlas, common/client class separation,
  localized commands, and absence of placeholder animations/test fixtures.
- Evidence: `.local/blockbench/elemental_golem/implementation-verify.log`,
  `static-mode-smoke.log`, `static-export-report.json`, `audit_runtime.py`, and
  `runs/gametest/logs/latest.log`. Sources/preview studies remain ignored in
  `models/elemental_golem/` and `.local/blockbench/elemental_golem/`.
- Not performed: Minecraft client appearance, actual walk/attack readability,
  tight passages, real multiplayer/latency or comparative performance checks.
  This testing golem is deliberately unsaved; retained-world golem restoration
  is not implemented or claimed. No deployment or client launch was performed.

## Hunter's Pet walking after combat (2026-11-10)

- Fixed a pinned GeckoLib 5.5.2 controller lockout: completed triggered bite/hurt
  clips retained control and skipped the idle/walk handler. The controller now
  receives those callbacks, preserves unfinished actions and explicitly releases
  completed actions. Hurt/death priority and the existing movement fallback remain.
- Expanded `pet_locomotion` to advance the actual pinned controller with the
  shipped clips. It covers repeated bite/hurt recovery, stopping, the negative
  finished-timeline sentinel, death interruption and held death. The fixture
  seeds initialized client-trigger state without a physical-client clock.
- The focused regression failed with `Walk must resume after triggered attack`
  before enabling the fix, then passed with it. Evidence:
  `.local/pet-animation-regression-red.log` and
  `.local/pet-animation-regression-green.log`.
- `scripts/Dev.ps1 verify` passed build checks, all five wolf clips with 34,926
  runtime samples, and **All 193 required tests passed :)**. Evidence:
  `.local/pet-animation-verify.log`. The production JAR contains `HunterPet` and
  excludes GameTest classes/data.
- Manual client check: follow/wander, bite an enemy, take damage, then continue
  moving and stop. Confirm walking resumes after each action and idle resumes
  on stopping. No client was launched or deployment performed.

## Blood Fever prerequisite removal (2026-11-10)

- Removed Rend from Blood Fever's shared tree metadata. Server purchases and
  bindings, client prerequisite text/connections, and dependent removal now treat
  Blood Fever as independent. Its level-5 gate and slotted passive remain.
- `scripts/Dev.ps1 verify` passed the build checks and **All 193 required tests
  passed :)**. Updated fixtures cover rejection below level 5, purchase and
  binding without Rend, shared-bleed bonuses and passive HUD feedback without
  Rend. Evidence: `.local/blood-fever-prerequisite-verify.log`.
- Manual client check: the Reaver tree shows no Rend connection or requirement
  for Blood Fever, and allows its purchase at level 5 without learning Rend.
  No client was launched or deployment performed.

## Phase 6 aura activation effects (2026-11-10)

- `scripts/Dev.ps1 build` passed. Final `scripts/Dev.ps1 verify` reported
  **All 193 required tests passed :)**. Evidence:
  `.local/phase6/aura-build.log`, `.local/phase6/aura-final-verify.log` and
  `runs/gametest/logs/latest.log`. `git diff --check` passed.
- All eleven instant aura activations now have compact family-colored release
  and actual-recipient cues. Generic cloud/enchant/dust receipts are replaced
  with open green, crimson, steel, pale-blue, electric-blue and violet shapes.
  Real entity dimensions size recipients within fixed limits. Current icons,
  native sounds, merged owner-only footprint and gameplay remain unchanged.
- Natural expiry emits only when the final overlapping active supplier ends.
  Passive refresh/expiry and lifecycle/loadout cleanup remain quiet. Guaranteed
  Dodge consumption emits once; Fortitude distinguishes positive partial
  absorption from depletion, without false breaks on replacement or expiry
  after depletion. Hooks use existing authoritative transitions and expiry work;
  no new visual state, scans, tick handlers or custom payloads.
- Three native feedback tests cover every aura identity, payment rejection,
  freeze immunity, self versus hostile recipients, teammates/pets, late entrants,
  overlapping expiry, actual guaranteed Dodge, rejected shield damage, partial
  absorption, depletion, replacement, unassignment and lifecycle cleanup.
  Expiry fixtures advance existing deadlines; existing aura tests retain
  real-duration, damage, ownership, projectile and lifecycle coverage.
- Packaged audit passed **52 particle definitions / 322 sprite references**,
  byte-matching sources and resolving native/Covenant textures. Aura emitter and
  common helper classes are present; test classes are absent. Evidence:
  `.local/phase6/aura-asset-audit.txt`. Brief/source map:
  ignored `models/aura_activations/BRIEF.md`.
- Budgets per event: 12 release, eight receipt/consumption, four partial-shield
  or natural-expiry children. Existing two-to-seven-tick particles use native
  depth and limiting. Event counts scale with actual recipients/hits, not
  passive refreshes. These are code limits, not measured performance gains.
- Output: `build/libs/covenant-0.1.0-dev.jar`; no deployment or client launch.
  Manual checks: all eleven identities in first/third person, pet scaling,
  observers, overlapping sources, shield damage/depletion versus replacement,
  expiry versus cleanup, Minimal, terrain contrast and resource reload. Minecraft
  rendering, latency/multiplayer and performance acceptance remain pending.

## Phase 6 Weaver signature effects (2026-11-10)

- `scripts/Dev.ps1 build` passed. Final `scripts/Dev.ps1 verify` reported
  **All 190 required tests passed :)**. Evidence:
  `.local/phase6/weaver-final-verify.log` and `runs/gametest/logs/latest.log`.
  `git diff --check` passed.
- Discharge emits only its actual consumed elements, with density based on
  charge fraction and a full-bank accent. Main targets receive one aggregated
  accepted-hit mark; external lightning chains retain real endpoints and accepted
  contacts. Wind-up motes show held elements only. Old enchant/explosion sprites
  are removed. Damage, riders, targeting, costs and timings remain unchanged.
- Threefold has inward consumption cues proportional to actual reserves and one
  small natural-expiry breakup retaining consumed element identity. An unsaved
  receipt runs on the existing owner refresh, with no additional tick handler.
  Reserve banking, passive refresh, unassignment and lifecycle cleanup remain
  quiet. Existing icons, energy/shards and authoritative HUD are retained.
- Two new native feedback tests use real casts/damage to check partial/full/absent
  banks, payment/cancellation, one-time consumption, actual chain endpoints,
  accepted versus protected contacts, reserve fractions, natural expiry once,
  unassignment and logout. Expiry feedback advances only the fixture deadline;
  existing Weaver tests cover normal duration, resource, damage, reload and
  lifecycle behavior. These tests do not establish client appearance.
- The first full run exposed an existing aura ownership fixture that queried
  newly spawned pets without waiting for entity-ticking chunks. That test now
  holds fixture-only loading tickets and waits for readiness before setup.
  The second run exposed a similar lifecycle-route fixture using a fixed
  two-tick delay for remote trap lanes; it now waits for simulation readiness
  with its own tickets. Both release resources on completion, with idempotent
  timeout fallback cleanup. No production chunk forcing was added. Earlier
  logs: `.local/phase6/weaver-initial-verify.log` and
  `.local/phase6/weaver-second-verify.log`; both new Weaver tests passed in each.
- Packaged audit passed **47 particle definitions / 282 sprite references**,
  byte-matching source files and resolving native/Covenant textures. Weaver
  emitter/helper classes are packaged; test classes are absent. Evidence:
  `.local/phase6/weaver-asset-audit.txt`; brief/source mapping:
  ignored `models/weaver_signatures/BRIEF.md`.
- Limits: three wind-up motes every four owner ticks, at most 91 Discharge
  children across all-full banks, 24 Threefold activation and nine expiry motes.
  Native limiting/depth behavior and short energy/shard lifetimes apply. These
  are code limits, not measured performance improvements.
- Output: `build/libs/covenant-0.1.0-dev.jar`; no deployment or client launch.
  Manual checks: first/third-person aim, partial/mixed/full banks, real chains,
  protected/covered targets, Threefold expiry versus cancellation, Minimal,
  terrain contrast, overlapping casts, observers and resource reload. Rendering,
  real latency/multiplayer and comparative performance acceptance remain pending.

## Phase 6 Reaver signature effects (2026-11-10)

- Battle Cry now emits one radius-aware shout pulse and a brief receipt at each
  actual beneficiary. Rage Storm has a real-cast hand cue, hollow rotating bands
  using its release-synced radius, accepted-hit marks and one natural/terrain
  breakup. Blood Fever retains its quiet synced count with a crimson accent and
  a specific bleeding-enemy label. Current icons, sounds, timings, recipient/
  target rules, resource costs, cooldowns and gameplay remain unchanged.
- `scripts/Dev.ps1 build` passed. Final `scripts/Dev.ps1 verify` reported
  **All 188 required tests passed :)**. Three new native feedback tests cover
  shout recipients/late entrants/rejections, real cast cancellation and rejected
  storm admission, native radius tracking/distance culling, accepted versus
  immune pulse damage, terrain/lifecycle termination, and quiet Blood Fever
  count refresh. Fixtures wait for entity-ticking chunks, use distinct player
  names for scoreboard teams, and originally learned Rend before binding Blood
  Fever (that prerequisite was removed on 2026-11-10).
  Existing Reaver gameplay, reload, damage and lifecycle coverage remains passing.
  Final evidence: `.local/phase6/reaver-final-verify.log` and
  `runs/gametest/logs/latest.log`.
- The production mesh check is part of `build`: spherical vertex bounds,
  hollow center, thin nondegenerate bands, all-axis extent, bounded mesh size,
  tiny/large legal radii, rotating phases and invalid-input rejection passed.
  Five broken bands use 80 quads. Native `debugQuads` was checked in resolved
  26.1.2.126 sources: translucent blending, depth test enabled, no depth writes,
  no culling. The renderer accounts for visual radius in frustum and distance
  culling; the tiny gameplay body stays unchanged.
- Packaged audit passed **42 definitions / 242 sprite references**, all
  byte-matching sources and resolving native/Covenant textures. New renderer,
  geometry, emitter and common helper classes plus the HUD label are packaged;
  test classes are absent. Evidence: `.local/phase6/reaver-asset-audit.txt`.
  `git diff --check` passed. No new bitmap assets or custom payloads.
- Budgets: 32 shout motes, six per actual recipient, six local storm motes every
  other client tick and twelve at natural/terrain termination. Decoration fades
  within four ticks and respects native limiting; the shell survives Minimal.
  No world scan, chunk forcing, persistent visual state, per-refresh passive
  spectacle or recurring server storm-particle stream. These are implementation
  limits, not measured performance gains. Brief/source mapping:
  ignored `models/reaver_signatures/BRIEF.md`.
- Output: `build/libs/covenant-0.1.0-dev.jar`; no deployment or client launch.
  Manual checks: shout recipients/observers, real cast cancellation, near/steep/
  vertical storm motion, hollow volume and targets, walls and lifecycle removal,
  Minimal, distant/frustum culling, pale/dark terrain, multiple storms, Blood
  Fever count readability, F1 and resource reload. Minecraft rendering,
  latency/multiplayer and comparative performance acceptance remain pending.

## Phase 6 Reaver combo effects (2026-11-10)

- Lunging Combo has a steel trail on actual movement, a small hand-level strike
  and the existing frontal Cleave crescent. Its first stage remains harmless.
  Rip and Rush has separate circular opening/finisher bursts, a crimson trail,
  accepted-hit chips and inward marks on qualifying bleed consumption. Damage
  marks do not claim successful native armor-shred application. The existing
  icons, stage HUD, sounds, windows, costs, targeting and cooldowns are retained.
- `scripts/Dev.ps1 build` passed and `scripts/Dev.ps1 verify` reported
  **All 185 required tests passed :)**. Two new tests use native activations and
  captured particle packets to verify harmless movement, actual trail coordinates,
  completed/interrupted trail silence, native picked strikes, frontal versus
  circular damage, defended/empty attacks, one dash contact per target, and
  qualifying lethal bleed consumption versus dry/prevented hits and repeats.
  Existing movement/prediction, combo lifecycle, damage and progression tests
  remain passing. Evidence: `.local/phase6/combo-build.log`,
  `.local/phase6/combo-verify.log` and `runs/gametest/logs/latest.log`.
- Packaged audit passed **39 definitions / 218 sprite references**, including
  nine new combo definitions using native glitter frames. Sources byte-match
  the JAR, all native/Covenant sprites resolve, movement/combo runtime classes
  are packaged and test classes are absent. Evidence:
  `.local/phase6/combo-asset-audit.txt`. `git diff --check` passed.
- Local effects expire in four ticks: three motes per actual movement segment,
  24 per circular release, five per jab, four per contact and six per qualifying
  bleed consumption. These are explicit budgets, not measured performance gains.
  The old repeated-packet circular dust/sweep helper is removed. No new custom
  networking, saved state or recurring stage emitter. Brief/source mapping:
  ignored `models/combo_effects/BRIEF.md`.
- Output: `build/libs/covenant-0.1.0-dev.jar`; no deployment or client launch.
  Manual checks remain: first/third person, vertical/sliding/clipped movement,
  root/knockback, frontal versus circular recognition, whiffs and defended hits,
  consumption versus ordinary contact, crowded enemies, pale/dark terrain,
  nearby observers, particle settings and resource reload. Minecraft appearance,
  real-network observer timing and comparative performance are unverified.

## Phase 6 movement effects (2026-11-10)

- Disengage now emits one green departure, low flecks along actual native
  collision segments and one arrival at natural/terrain completion. Arrival
  retains the departure heading. Root/lifecycle cancellation stops further
  emission without an arrival; existing motes fade within four ticks.
- Teleport replaces the old portal clouds with two open violet bracket effects
  at the confirmed endpoints, lasting six ticks. No invented travel line,
  preparation delay, new sound, saved state or custom payload. Native particle
  limiting and distance/depth checks remain; the small breakup uses collision.
- `scripts/Dev.ps1 build` passed, followed by `scripts/Dev.ps1 verify` with
  **All 183 required tests passed :)**. The two new production-activation tests
  verify actual trail segment coordinates, heading capture, wall clipping,
  interruption/lifecycle silence, ceiling-clipped blink endpoints, rejected
  destinations/cooldowns, unchanged rejected spending and native limiter flags.
  Existing movement prediction, control, lifecycle and combo tests also pass.
  Evidence: `.local/phase6/verify.log` and `runs/gametest/logs/latest.log`.
- Packaged audit passed for **30 particle definitions / 146 sprite references**,
  including seven new definitions reusing native glitter frames. Definitions
  byte-match sources; all sprites resolve in Covenant or the pinned Minecraft
  client JAR; both new runtime classes are packaged and test classes are absent.
  Evidence: `.local/phase6/asset-audit.txt`. Native nullable particle-provider
  and engine APIs were checked in the resolved 26.1.2.126 sources.
- Unobstructed Disengage creates at most 28 local motes, with a solver-bound
  maximum of 64; Teleport creates 24 from two endpoint packets. These are
  emission budgets, not measured performance improvements. Sources/brief:
  ignored `models/movement_effects/BRIEF.md`; runtime output:
  `build/libs/covenant-0.1.0-dev.jar`. No deployment or client launch.
- Manual checks remain: first/third-person aim, turning, airborne/vertical
  Teleport, walls/slabs/low ceilings, root and knockback, nearby observers,
  pale/dark terrain, All/Decreased/Minimal, overlap and resource reload. No
  Minecraft visual, audio-balance or real-network acceptance is claimed.

## Particle-driven bolt correction (2026-11-10)

- Replaced Arcane/Fire/Ice generated-item rendering with thin travel-aligned
  geometric energy filaments, crossed ribbons and a small end cap. Moving native
  particles supply the main body. Fire rises/fades crimson, Ice disperses pale
  blue with sparse fractures, and Arcane flows violet. Lightning retains its
  exact server endpoints while flickering and emitting small electric-blue sparks.
  Elemental collision providers now emit bounded breakup bursts from the same
  authoritative packets; accepted-damage marks remain independent.
- `scripts/Dev.ps1 verify` passed: **All 181 required tests passed :)**,
  including existing elemental contact, lightning-link and projectile lifecycle
  checks. Build checks include lightning endpoint/vertical/degenerate geometry,
  UI geometry/Apply feedback and wolf animation assets. No new cosmetic-constant
  tests were added. Server evidence: `runs/gametest/logs/latest.log`.
- Packaged audit passed for **23 particle definitions / 90 sprite references**:
  definitions byte-match sources, Covenant sprites exist in the mod JAR, native
  sprites exist in the pinned Minecraft 26.1.2 client JAR, and the energy provider
  is packaged. The packaged bolt renderer has no ItemStack/ItemModelResolver
  dependency. Native particle/geometry APIs were checked against resolved
  `minecraft-patched-26.1.2.126-sources.jar`. `git diff --check` passed.
- Output: `build/libs/covenant-0.1.0-dev.jar`. No deployment or client launch.
  Manual appearance, first/third-person and steep/end-on aim, overlapping casts,
  pale/dark backgrounds, All/Decreased/Minimal, resource reload and impact
  readability remain pending. No rendered or comparative performance claim.

## Runtime polish JAR handoff (2026-11-10)

- Packaged all currently implemented polish: Phases 1–5, gothic runtime art,
  six approved skill-icon revisions and the latest remaining-catalogue corrections.
  The catalogue already references runtime textures; no asset copy or further
  design decision was required. Inclusion does not imply final art acceptance.
- `scripts/Dev.ps1 verify` passed: **All 181 required tests passed :)**,
  **BUILD SUCCESSFUL**, exit code 0. Geometry, authoritative Apply feedback and
  pinned GeckoLib wolf-animation checks also passed. Evidence:
  `.local/catalogue-review/integration-verify.log`.
- Packaged catalogue, gothic, Workbench/relic and compatible Phase 4/combat
  resource audits passed. All **204 Covenant resource files** byte-match the
  JAR; 60 catalogue textures differ from the catalogue baseline and the other
  144 files are unchanged. Checks cover all 35 skill icons, 19 particle families,
  50 family/rarity combinations, GUI atlas/language references, runtime presentation
  classes and exclusion of test classes. Approved icons, movement boot and
  20 protected wolf/trap resources remain intact.
- Updated the ignored legacy Phase 4 audit to accept the approved Battle Cry
  close-up's fully opaque square. Every other icon retains that audit's existing
  transparent/opaque requirement; runtime artwork was unchanged by this handoff.
- Output: `build/libs/covenant-0.1.0-dev.jar` (833,488 bytes).
  SHA256: `548C48D2C21360B19E8222F84E642BC7934A7671D68CE7C719360772E5F3832E`.
- No client launch or Prism/LAN deployment. Pending user checks: native-size
  icons across GUI scales/backgrounds, combat effects at particle settings,
  Workbench/item readability and audio balance. Phase 6 and later roadmap work,
  Enduring Cry's badge and golem design remain separate follow-ups.

## Gothic runtime art rework (2026-11-10)

### Remaining icon catalogue follow-up

- Rooted circular follow-up: a classic circular steel jaw, six inward teeth
  and a separate pressure plate replace the rejected side-view draft. The
  catalogue preview was regenerated and inspected at native/enlarged sizes.
  Build evidence: `.local/catalogue-review/rooted-circular-build.log`; the packaged
  catalogue audit passed. No client rendering check was performed.
- `scripts/Dev.ps1 build` passed after review corrections. Evidence:
  `.local/catalogue-review/corrections-build.log`.
  This pass changes resource textures only; GameTests were not rerun.
- `models/gothic_rework/catalogue_audit.py --packaged` passed: 60 changed
  textures, all other 144 Covenant resources byte-identical to the starting
  tree, six approved skill icons and the accepted Movement Speed boot preserved,
  all 35 skill mappings, 16px/binary-alpha contracts, reserved rarity rows and
  matching skill/projectile-core images. Every Covenant resource matches the JAR.
  The previous Ice/Arcane Bolt icons and item cores are byte-identical to their
  baseline; cooldown/duration images are swapped
  exactly against the snapshot before these corrections.
- Existing gothic and Workbench/relic audits also passed, including 20 protected
  wolf/trap resources and all 50 family/rarity combinations. Full icon/material
  source rebuilds preserve the same result. The earlier audit log is at
  `.local/catalogue-review/audit.log`; all three audits were rerun successfully
  after these corrections.
- Before/after and native-size pale/dark samples were inspected, then revised
  for tool/feather readability, elemental shape differentiation and distinct time
  affix symbols. User-selected Icy Veins hourglass and preserved movement boot
  are included. Latest revisions clarify the Dodge feather, Attack Speed sword,
  Combat Respec ink sword, Cast Time charge arc and Flame Cloak garment/flame hem.
  Review: `.local/catalogue-review/index.html`.
- Pending: user art approval, in-game GUI/background readability and projectile
  appearance. No client launch or deployment was performed.

### Original runtime art pass

- `scripts/Dev.ps1 build` passed. Final `scripts/Dev.ps1 verify`, including
  the crimson Flame Trap feedback changes, passed: **All 181 required tests
  passed :)** and **BUILD SUCCESSFUL**. Evidence is in ignored
  `.local/gothic-rework/verify-final.log` and `assets-final.log`.
- Resource audits verify all Covenant resources byte-match the packaged JAR,
  valid JSON/PNG, native dimensions and binary alpha. The 129 revised textures
  cover icons, relics, projectile cores, 19 effect families and the altar atlas.
  All 20 protected wolf/trap resources are byte-identical to the starting tree.
  Relic layers remain separate from rarity pips across all 50 combinations.
- The revised workbench supersedes the single-cube Phase 5 visual below:
  eight cuboids, 48 faces, 96 triangles, full-block bounds, one texel/unit,
  boundary-only culling and numeric symmetry about X=8. Headless geometry
  validation reports zero errors/warnings. Mapped front/back previews and
  native/enlarged icon/effect studies were visually inspected.
- Review gallery: `.local/gothic-rework/review.html`. Editable sources and
  rebuild order: `models/gothic_rework/BRIEF.md`. No gameplay dimensions,
  particle counts, lifetimes or attack/endpoint geometry changed.
- Pending manual client review: crimson fire, pale blue ice and electric blue
  lightning in motion against bright/dark terrain; icons at GUI scales; relic
  recognition and rarity pips; altar lighting and adjacent-block culling.
  Headless previews are not Minecraft rendering. No client launch or deployment
  was performed.

## Workbench, inventory and progression - visual polish Phase 5 (2026-11-10)

- `scripts/Dev.ps1 verify` passed: **All 181 required tests passed :)** and
  **BUILD SUCCESSFUL**. Final friendly choice-label/resource packaging changes
  then passed `scripts/Dev.ps1 build` and the complete Phase 5 asset audit.
  Evidence: ignored `.local/phase5/verify-final.log`, `build-final.log` and
  `manifest.json`; authoring/audit scripts are in ignored `models/ritual_workbench/`.
- New native packet tests cover structured Workbench preview codec round trips,
  possible costs/ranges without initialization or RNG consumption, stale/replayed
  choices, invalid indices/menu IDs, accepted commits, rejection with no success
  sound, owner-only receipts and one sound for multi-item salvage. Existing
  Workbench tests still cover shared storage, native inventory handling, native
  block removal/drop behavior, saved components, profession access and reloads.
- Progression packet tests cover ordinary XP and class initialization silence,
  one cue for a real level, aggregate points for a multi-level award, classless
  professions, owner-only delivery, the first-Wither milestone, capped/zero awards,
  admin snapshots and repeated `refreshView` calls. These establish that the
  synchronization path cannot replay cues; no new real-client reconnect/restart
  or dimension-travel acceptance run was performed.
- Replant feedback tests exercise actual native hoe harvesting: nine missing
  supplies produce one count-accurate notice, sneaking reports one crop, denied
  placement is not called missing supplies, denied harvesting produces no false
  notice, and the final successful crop can report a shortage after tool break.
  Existing gathering/protection/drop/durability tests remain passing.
- The headless Java block model has zero geometry errors/warnings, one cuboid,
  six faces and full-block bounds. Floating/mirror gates are inapplicable to a
  single cube. The mapped isometric preview was inspected. The runtime adapter
  adds native parent, namespaced texture IDs and boundary culling without altering
  exported geometry/UVs. All six faces retain one texel/unit.
- The asset audit verifies 43 byte-current packaged resources, 19 original PNGs,
  0/255 alpha, native dimensions, all texture/model/item/blockstate references,
  ten family selections by five rarity bands, neutral unknown-entry fallbacks
  and disjoint generated-model layers. Native/enlarged pale/dark/grayscale item
  studies are `.local/phase5/items.png`; mapped block preview is
  `.local/blockbench/phase5-workbench.png`. This is not Minecraft rendering.
- Manual: approve station/item art at gameplay and inventory size; inspect all
  GUI scales/small windows, focus, choices/full details and carried/shift-click
  items; shared-station latency/stale clicks and partial salvage; resource reload,
  unknown datapack entries, quiet cue/audio balance, reconnect/dimension silence
  and actionable shortage messages. No client launch, Prism/LAN deployment or
  comparative performance claim is included.

## HUD, menus and navigation - visual polish Phase 4 (2026-11-10)

- Shared stone/iron framing retains native Button input/focus/narration. Original
  icons cover all 35 playable skills across tree, bindings and status, plus 22
  navigation/class/profession emblems and three native control-effect sprites.
  The user rejected the first thin abstract study; the redraw uses recognizable
  equipment, tools, wolf, horn, book and elemental forms. Four original trap
  miniatures remain. Ten Phase 3 tree icons are redrawn; projectile-core/particle
  art is unchanged. Native/enlarged samples on pale/dark patches were inspected;
  the revised artwork still awaits user acceptance.
- Tree shapes/badges, rank/assignment focus tooltips and pending totals distinguish
  real build states. Locked/planned nodes remain inspectable with guarded
  purchases. Eight binding cards prioritize actual rebound keys. Professions
  keep independent drafts and show current/draft/next rank, costs and requirements.
  Stats has visible class previews before explicit permanent choice, grouped
  summaries, optional precise native breakdown and known equipment/aura labels
  with raw ID fallback. The radial keeps its controls/order with three emblems,
  dormant wedges and discovery guidance. Resource/XP positions and F1 remain.
- ApplyFeedback checks passed for initial sync, unchanged/rejected state, matching
  authoritative changes, conflicts, five-second timeouts, late snapshots, editing
  guards and independent tabs. No speculative success, message parsing, new custom
  payload, saved data, skill bar or gameplay tuning. Hidden native Rooted/Chilled
  effects supply real owner timers; synced freeze gets no invented countdown.
  Visible native potion tiles are not duplicated. Solid/hollow marks supplement
  active/passive/reserve colors.
- `scripts/Dev.ps1 build` and `scripts/Dev.ps1 verify` passed: **All 178 required
  tests passed :)** and **BUILD SUCCESSFUL**. Existing casting fixtures additionally
  check native overlay packets for real cast completion and equipment cancellation,
  with no success/spending/ammo consumption after interruption. Three older fixture
  helpers now check the same stable translatable outcome keys instead of the
  English word `Used`; shorter success copy no longer invalidates gameplay
  assertions. An initial pet-AI failure did not recur in the full passing run;
  no pet gameplay or assertion was changed. Final client-only refinements also
  passed a separate build and packaged audit. One pre-existing CP1252 dash was normalized to UTF-8 to edit
  this otherwise UTF-8 document safely.
- Packaged audit passed: 60 native-grid binary-alpha icon exports, all 35 playable
  IDs, controlled palettes, 62 exact JAR resource matches, native effect atlas,
  language keys/encoding and test-class exclusion. The compatible combat audit
  passed for 120 resources / 19 particle families, core/model chains, complete beam
  strips and frontal Cleave cell bounds. This supersedes Phase 3's historical
  14-entry icon-map count. Sources/briefs are ignored `models/ui_phase4`; previews,
  audits and logs are ignored `.local/phase4`.
- Pending user Minecraft review: revised icon recognition/acceptance, small
  windows/GUI scales, hover/keyboard tooltips and narration, build states, rebound
  keys, assignment/clearing, class preview/permanent choice, independent drafts,
  combat/rejected/stale Apply and action-bar visibility. Also review crowded
  status timers/counts, controls/expiry, resource reload, F1 and hunger/air/mount
  stacking. No client launch/automation, Prism/LAN deployment, latency/multiplayer
  acceptance, retained-world restart, ATM11 compatibility or comparative
  performance measurement. Phases 5-9 remain pending.

## Everyday combat - remaining visual polish Phase 3 batches (2026-11-10)

- Phase 3 is implemented with Minecraft review pending. Fire/Ice now have
  original 16x16 generated-item cores and cast/trail/collision sprites: rising
  embers versus motion-aligned angular shards. Their existing native burning,
  Chilled status, hitboxes, costs and lifetimes remain unchanged. All three
  spell cores use lazy client-only item stacks and native full-bright rendering.
- Lightning reuses its existing dimension-scoped real-link endpoints. Its
  jagged centerline preserves endpoints with <.079-block interior deviations
  and <=64 four-tick 3D strip segments per link. Viewer-facing strip geometry
  follows the actual 3D segments; the texture spans both exact endpoints.
  Only server-selected Shock links branch to enemies. Actual collisions/chain
  endpoints have brief collision glyphs; range misses do not invent impacts.
- Aimed Shot gathers only during its real cast, then uses a paired narrow
  streak. Arcane Arrow shares the approved pale/violet family and accepted-hit
  cross. Multishot retains five real arrows, one small two-tick streak per
  arrow/tick, a quiet collision mote and one native volley release sound.
  Entity-impact events precede native arrow damage: cosmetic style now remains
  through that call, with block impact/native removal/age guard ending visuals.
  Saved provenance, fan claims, vulnerability and damage rules are unchanged.
- Rend has a distinct compact single-target slash, including shared circular
  Rend applications. Bleeding emits one sparse cue per target/pulse when any
  stack deals accepted damage; fully prevented pulses stay quiet. There is no
  new bleed audio or damage/status framework. Native weapon poses remain.
- Original 16x16 icons complete all ten Phase 3 skills. Across Phases 1-3 the
  current icon map has 14 original icons; Phase 3 uses 19 native decorative
  particle families. Native/enlarged sources on pale/dark backgrounds were
  inspected. The packaged audit passed for 99 resource files, including sprite
  references, cutout dimensions/palettes, three generated-item chains, complete
  beam strips, Cleave cell bounds, exact JAR bytes and exclusion of test classes.
  Sources/briefs: ignored models/elemental_spells and models/bow_melee_effects;
  previews/logs/audit: ignored .local/phase3. No new custom payload or saved IDs.
- scripts/Dev.ps1 build passed for both incremental runtime batches. Final
  scripts/Dev.ps1 verify passed: **All 178 required tests passed :)** and
  **BUILD SUCCESSFUL**. Production lightning geometry checks cover exact
  endpoints, finite segment budgets, zero-length/vertical aim, random 3D paths
  and bounded ornament. Native GameTests cover elemental immune/absorption-only
  hits, real Shock endpoints and cover-clipped links, range misses, native bow
  collisions retaining contact style, one Multishot attempt per target, Aimed
  completion/cancellation, Rend and aggregated/prevented bleed-pulse cues.
  One initial compile error in the new generic fixture target type was corrected
  to Mob before the final run; no gameplay assertions were weakened.
- Pending user Minecraft review: first/third-person silhouettes and aim space,
  reduced particles/Minimal, pale/dark terrain, vertical/near-view Lightning,
  cover and real chain links, Aimed wind-up/cancellation, Arcane collision versus
  accepted damage, whole-volley brightness/sound, quiet Rend/bleed, resource reload
  and lifecycle removal. No client launch/automation, Prism or LAN deployment,
  retained-world restart, multiplayer/latency acceptance, ATM11 compatibility or
  comparative performance measurement. Phases 4-9 remain pending.

## Everyday combat reference batch - visual polish Phase 3 (2026-11-10)

- Arcane Bolt: original pale/violet 16x16 generated-item core with full-bright
  native rendering, ordered five-tick motes, six-tick broken-seal collision and
  separate accepted-damage contact. Palette approved by the user. Cleave: one
  four-tick, three-frame 32x32 steel/crimson crescent at the real release position,
  yaw and range. Both faces render because native particle pipelines cull
  backfaces. Piercing Shot: narrow motion-aligned two-tick ivory/gold streaks,
  preserving the native arrow and six-tick cosmetic bow pose. Three original
  16x16 tree icons reuse these identities. Existing native sounds remain.
- Five native decorative particle types use normal distance/settings/limiter
  behavior. No new custom network payload, saved state, targeting, costs or
  timings. Arcane's entity core remains visible on Minimal; decorative cues may
  disappear. The shared melee contact helper also updates existing Rend/combo
  hit marks without authoring their later skill/stage effects.
- Native/enlarged sprite sheets on pale/dark backgrounds were inspected.
  Packaged audit passed for 26 resource files: five sprite-list chains, PNG
  dimensions/cutout alpha, occupied Cleave cell corners within 120 degrees and
  range, generated-item definition/model/texture references, exact JAR bytes and
  exclusion of test classes. Sources/brief: ignored models/combat_examples/;
  previews/logs/audit: ignored .local/phase3/.
- Initial scripts/Dev.ps1 build and final scripts/Dev.ps1 verify passed:
  **All 174 required tests passed :)** and **BUILD SUCCESSFUL**. Native packet
  checks cover accepted yaw/range, normal limiter flags, melee whiffs versus
  damaging contacts, prevented hits, cancelled charge, wall collision,
  absorption-only projectile damage, marked Piercing Shot and ordinary arrows.
  First run passed 173/174: the new absorption fixture lacked a positive native
  MAX_ABSORPTION attribute, so Minecraft clamped absorption to zero. Correcting
  that setup retained all gameplay assertions. Client code compiled after the
  final backface/animation fixes. Dedicated tests establish server loading and
  emission, not rendered art. git diff --check passed.
- Pending user Minecraft review: first/third-person readability, crosshair/target
  visibility, cardinal/diagonal crescent direction, frontal versus circular cues,
  steep/vertical arrow streaks, collision versus accepted damage, pale/dark terrain,
  overlapping skills, particle settings, resource reload and lifecycle removal.
  No client launch/automation, Prism or remote deployment, retained-world restart,
  multiplayer observer/latency acceptance, ATM11 compatibility or comparative
  performance measurement. Remaining Phase 3 batches stay pending.

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
  is passive while slotted; its original Rend prerequisite was removed on
  2026-11-10. Rage Storm follows full release-time
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
