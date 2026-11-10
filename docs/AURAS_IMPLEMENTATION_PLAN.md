# Auras: implementation plan

Status: all eleven aura skills implemented, 2026-11-05.
Automated validation and remaining manual checks are recorded in `VALIDATION.md`.

The requested 2026-11-09 presentation follow-up adds native top-left buff icons
and a persistent local-owner outline for slotted area auras. Shared six-block
fields merge into one bounded client particle ring; self-only auras have no ring.
This supersedes the original temporary-icon/no-persistent-outline presentation
scope below. Gameplay, coverage queries and saved-state boundaries are unchanged.
See [Status presentation](RPG_SKILLS.md#status-icons-aura-outlines-and-trap-models).

The behavior contract lives in [RPG_SKILLS.md](RPG_SKILLS.md#aura-contract)
and [Skill Trees Brainstorm](<../Project-Covenant/Skill Trees Brainstorm.md>).
This checklist organizes implementation of all 11 aura skills, including Icy
Veins and Flame Cloak. Threefold System, companions, banners, and other non-aura
skills are outside this batch. Preserve unrelated work already in the checkout.

## Base tuning

All activations are instant. Costs, cooldowns, and active durations use the
existing corresponding skill multipliers. Nearby aura effects have a six-block
spherical radius measured between entity positions, ignoring solid cover.
Range-based passives refresh every 10 game ticks with 20 ticks of coverage;
neither interval scales with skill duration. Self-only effects need no area query.

| Aura | Passive | Active | Base active duration | Cost | Base cooldown |
|---|---|---|---:|---:|---:|
| Dodge | Caster/allies: +10 percentage points Dodge; ordinary chance capped globally at 85% | One guaranteed dodgeable hit per recipient; consumes all overlapping guarantees | 3 s | 25 Focus | 20 s |
| Swiftness | Caster/allies: +10% movement speed | One-time slow cleanse, additional +20% movement speed, +0.5 blocks step height | 4 s | 25 Focus | 20 s |
| Ripping | Caster/allies: +20% bleed damage, captured on stack application | Eligible ordinary/melee-skill hits apply shared bleeding; Rend still applies one stack | 5 s | 40 Rage | 20 s |
| Fortitude | Caster/allies: +4 Armor, +2 Armor Toughness | Separate six-point shield (three hearts); recasts replace capacity and refresh full duration | 7 s | 40 Rage | 20 s |
| Raging | Self: decay at 3 Rage/s after existing seven-second grace | +20% personally dealt damage; stop passive Rage decay, retain skill spending | 6 s | 40 Rage | 30 s |
| Icy Veins | Self: 10% reduced newly calculated skill cooldowns | Remove spell wind-ups; retain costs, cooldowns, channel firing rates; no cast-count limit | 4 s | 30 Mana | 30 s |
| Flame Cloak | Self: 20% incoming fire-tagged damage reduction | Damaging eligible melee hits ignite attacker for 4 s base | 8 s | 30 Mana | 20 s |
| Freezing | Enemies: 10% movement slow | Freeze voluntary movement, jumps, attacks, and casting; cancel pending Covenant casts | 1.5 s | 40 Mana | 25 s |
| Shock | Enemies: 10% increased damage taken from all sources | Caster's Lightning Bolt chains to two additional targets, four blocks per jump, 50% base damage on each jump | 7 s | 30 Mana | 25 s |
| Immolation | Enemies: 1 fire damage per second; one shared pulse schedule | Self: +30% attributed fire damage; native burning ticks unchanged | 6 s | 40 Mana | 30 s |
| Enchanting | Caster/allies: +20% Mana regeneration; no inactive-class recovery | Add up to 2 magic damage to ordinary melee/bow/crossbow hits; exclude skills | 6 s | 40 Mana | 25 s |

Flame Cloak's eight-second base duration is derived from 40% of its 20-second
base cooldown. Duration and cooldown modifiers apply independently afterward.
Icy Veins' own passive reduces its 30-second cooldown to 27 seconds before other
modifiers. Ripping/Fortitude remain mutually exclusive; Freezing requires Icy
Veins and Immolation requires Flame Cloak. Preserve all stable node IDs.

## 1. Shared aura ownership and recipients

- [x] Reuse `SkillDefinition`, `Skills`, `SkillLoadouts`, and the existing saved
  cooldown/loadout state. Activation requires learning, current class, and at
  least one slot assignment. Commands cannot bypass the slot commitment.
- [x] Add only the small transient source/recipient state needed for aura
  coverage, active expiries, source cleanup, and Fortitude capacity. Derive
  passives from unique current-class assignments; duplicates never multiply them.
  Do not store derived modifiers in profiles or Affix Pages.
- [x] Centralize aura ally resolution: caster, scoreboard teammates, and pets
  owned by caster/teammates. Unteamed casters benefit themselves and their pets.
  Keep one place to add future FTB chunk-team support; add no FTB dependency now.
- [x] Offensive eligibility excludes these allies and pets, respects PvP, and
  includes eligible passive animals and modded living entities.
- [x] Query only nearby entities around eligible aura sources every 10 ticks;
  share queries where practical. Maintain expiries on affected entities without
  scanning every loaded entity, forcing chunks, or doing coverage raycasts.
- [x] Refresh range passives to 20 ticks; let them linger after field exit.
  Select active recipients immediately at activation; do not grant active buffs
  to later arrivals. Moving out of range does not remove active buffs.
- [x] Use strongest-copy resolution for identical passives and ordinary timed
  active effects, retaining source expiries. Keep different auras independent.
  Implement Dodge consumption and Fortitude replacement as explicit exceptions.
- [x] On team changes, stop new allied refreshes/activations but let existing
  coverage and active buffs expire naturally. Do not revoke those buffs.
- [x] Removing the last assignment, unlearning, caster death/logout, or caster
  class/dimension changes immediately remove that source's effects on everyone.
  Keep cooldowns and other sources intact. Duplicate-slot removal alone does not
  cancel effects. Recipient death/logout/dimension changes clear recipient buffs.
- [x] Preserve committed bleed and projectile snapshots when source buffs end.
  Do not save or death-copy temporary aura buffs or shield capacity. Discard
  stale recipient/source references when entities leave the level.
- [x] Keep passive upkeep free and working at zero resource. Reject activations
  while another skill is casting; otherwise commit resource and cooldown once.
  Rejections spend nothing. Freezing spends even when no target receives freeze.

Acceptance: owner, teammate, teammate pet, outsider, and PvP fixtures demonstrate
correct membership. Test duplicate assignments, radius edges/elevation/cover,
0.5-second refresh/one-second grace, active recipient selection, independent
expiries, team changes, and source/recipient cleanup without erasing cooldowns.

## 2. Native stats, Dodge, and shared slows

- [x] Apply flat native Armor/Toughness modifiers and native speed, step-height,
  regeneration, and cooldown modifiers where applicable. Swiftness's passive and
  active speed modifiers must add to +30%, rather than multiply to +32%.
- [x] Extend Dodge support narrowly to eligible pet recipients; the current
  `RpgStats` Dodge handler is player-only. Cap ordinary chance at 85% across
  sources. Guaranteed Dodge precedes the ordinary roll, bypasses the chance cap,
  and consumes all overlapping guarantees only on a dodgeable hit.
- [x] Preserve Dodge's existing damage eligibility. Do not make explosions or
  other undodgeable sources dodgeable. Update obsolete deterministic 100%-Dodge
  fixtures; test the cap without flaky statistical assertions.
- [x] Reconcile `Chilled`, `FrostSlows`, and Freezing's slow so only the strongest
  Covenant slow applies. Keep weaker timers/source membership and Chilled's
  existing save/load behavior. Do not give Frost Trap the aura's exit grace.
- [x] Swiftness clears vanilla Slowness and Covenant slows once, without clearing
  roots/freezes or arbitrary third-party modifiers. Fields may reapply a slow.
- [x] Integrate Icy Veins at the normal cast/cooldown calculation boundaries and
  Raging at the existing resource-decay boundary. Never rewrite running cooldowns
  or alter channel rates; retain the current resource recovery/sync cadence.

Acceptance: verify exact stat values, 85% cap and guaranteed consumption, pet
benefits, additive Swiftness bonuses, slow suppression/resumption/cleanse,
unchanged Chilled persistence, new versus existing cooldowns, unlimited eligible
instant casts during Icy Veins, and Raging's grace/decay/spending behavior.

## 3. Fortitude and confirmed damage

- [x] Track Fortitude separately from native absorption. Consume it after
  applicable mitigation and before ordinary absorption/health. Never remove
  unrelated golden-apple absorption on expiry or cleanup.
- [x] Fresh activations replace each in-range recipient's existing shield with
  six points and a full new duration. Record the fresh source for cleanup; retain
  no backup shield and never add capacity. Area entry alone grants nothing.
- [x] Capture the actual Fortitude damage absorbed in the native damage flow,
  once per hit, including shield-only hits. Feed landed-damage confirmation to
  Rage/combat grace and bleed application, plus Flame Cloak and Enchanting.
- [x] Preserve no outgoing Rage from skills and existing combat attribution
  restrictions. Do not count prevented damage or count shield absorption twice.
  Review existing `Skills`, `Bleeding`, `ClassResources`, `ElementalBolts`, and
  trap hit-confirmation paths for the new separate damage pool.

Acceptance: exercise mitigation, shield-only and overflow hits, depletion,
expiry, source replacement/cleanup, ordinary absorption coexistence, Rage and
bleed setup from shield-only hits, and no damage credit on dodge/blocked hits.

## 4. Ripping, Raging, and Flame Cloak damage hooks

- [x] Reuse shared bleed stacks, ownership, relative timers, and defenses. New
  Ripping stacks use 10% captured native attack damage per one-second pulse for
  five seconds base, with normal bleed-duration handling. Limit aura-triggered
  application/refresh to once per 20 ticks per attacker/target pair, shared across
  overlapping buffs. Rend retains its own single application.
- [x] Capture bleed bonuses once on application. Ripping and Raging add to +40%
  (a 10% base pulse becomes 14%); never apply Raging again on the bleed pulse.
  Retained stacks keep captured power on refresh; replacements capture anew.
- [x] Apply Raging's direct bonus at each effect's existing damage-calculation
  point. Exclude pet damage and prevent recursive bonus/retaliation triggers.
- [x] Flame Cloak reduces native fire-tagged damage and retaliates only against
  eligible damaging melee attackers. Reuse native burning and burning-time
  modifiers; do not add burn ownership or custom burn stacks for this batch.

Acceptance: test attack attribution, bleed snapshot/refresh/replacement,
attacker/target application limits, one Rend stack, retained boosted stacks,
retaliation through Fortitude/absorption, and no retaliation after negated hits.

## 5. Freezing, Shock, and Immolation

- [x] Add source-aware temporary freeze using native effects/attachments and
  narrow action hooks. Block voluntary movement/jumps, attacks, and Covenant
  casting; retain external velocity and damage. Cancel pending Covenant casts
  without charging them. Keep client presentation separate from authority.
- [x] Freeze survives damage. No blanket boss immunity, post-freeze immunity, or
  diminishing returns. Test successive casters and ordinary non-stacking overlaps.
  Inspect resolved APIs for mob/player action suppression; document limitations
  of third-party entity AI rather than claiming universal mod compatibility.
- [x] Add Shock's Lightning Bolt chains to the existing hit flow: nearest unhit
  eligible target within four blocks of the previous target with clear cover
  ray; stop without a qualifying target. At most three targets, power 6/3/3
  before bonuses/defenses. Do not derive jump power from previous damage received.
- [x] Apply Shock's vulnerability to damage received, including ongoing damage,
  without snapshotting it into bleed power. Preserve actual invulnerability.
- [x] Immolation uses one target pulse schedule across overlapping sources, not
  independent per-owner damage. Preserve attribution to the selected strongest
  source and avoid resetting pulse deadlines through coverage refresh or source
  handoff. Resolve equal-strength sources deterministically.
- [x] Immolation bypasses armor/block/Dodge and normal hurt immunity; preserve
  other defenses and other attacks' immunity state. Its +30% active affects owned
  attributed fire damage at normal calculation points, not native burning ticks.

Acceptance: freeze players/mobs, jump/attack/cast denial, interrupted casts,
knockback/damage and staggered re-freeze; chain range/cover/no repeat targets and
exact power; Shock on ally/ongoing damage; overlapping Immolation, attribution,
pulse cadence, hurt-immunity independence, and fire immunity/protections.

## 6. Enchanting and presentation

- [x] Gate the added magic portion on positive original health/absorption/
  Fortitude damage. Exclude skills, retaliation, and damage-over-time recursion.
- [x] Player melee bonus is two times pre-hit vanilla attack strength. Bow bonus
  scales with draw strength; ordinary crossbows get two points. Snapshot shots
  at firing. Permit one bonus per projectile per successfully damaged enemy,
  including ordinary piercing and multishot; exclude skill-created arrows.
- [x] Apply the magic portion with its own vanilla magic defenses: bypass
  armor/toughness/blocking, respect Protection/Resistance/absorption/invulnerability
  and Fortitude. Do not reroll Dodge, lose the portion to the initial hit's hurt
  immunity, or extend that immunity. Preserve original damage attribution.
- [x] Update descriptions/tooltips with costs and effects, and provide bounded
  activation/status feedback through existing native presentation. Keep current
  placeholder icons unless separately requested. Show Fortitude capacity without
  presenting it as ordinary absorption; ensure the chosen UI reflects depletion
  and source cleanup. Avoid permanent particle emitters or repeated packet floods.

Acceptance: charged/weak melee, partial/full bow draws, crossbow shots,
skills exclusion, shield-only hits, original-hit negation, projectile capture,
piercing/multishot, vanilla magic mitigation, both portions landing, and no
recursive triggers or hurt-immunity changes.

## Validation and delivery

- [x] Inspect the pinned 26.1.2 / NeoForge sources at each damage, action, and
  synchronization integration point. Preserve versions and saved registry IDs.
- [x] Add focused dedicated-server GameTests as stages acquire gameplay. Include
  load/reconnect/death/dimension/class/loadout lifecycles and native modifier
  cleanup, plus regression coverage for affected bleed, trap, bolt, and Rage paths.
- [x] Run `scripts/Dev.ps1 build`, `scripts/Dev.ps1 test`, and `git diff --check`
  with the project toolchain. Record actual results in `docs/VALIDATION.md` and
  update implemented-scope docs only as gameplay is delivered.
- [x] Hand the user a concise client checklist: high-speed field crossing and
  grace, 10% slow noticeability, team/pet coverage through cover, shield display
  and refresh, freeze controls, chaining feedback, and active/passive loadout
  changes. Do not launch or automate client playtesting without a request.

Implementation uses the pinned native damage/action/sync hooks. See
[RPG_SKILLS.md](RPG_SKILLS.md#aura-implementation-boundaries) for transient state,
pet Dodge, and third-party AI boundaries. Client rendering, movement feel,
real-network reconnect/travel, and performance remain manual/unbenchmarked.
