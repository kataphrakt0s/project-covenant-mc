# Progression Vertical Slice

## Goal

Deliver a complete, playable progression framework that demonstrates both Covenant combat progression and Minecraft activity mastery while remaining vanilla-friendly. The slice includes all four initial professions, but only a deliberately modest first selection of actual profession content.

## Included systems

### Save compatibility scope

Existing prototype-save compatibility is not required for this progression
slice. Plan and validate against fresh saves using the new progression format.
Do not add legacy decoders, free-skill conversion, a one-time allocation reset,
or another migration system solely to carry prototype saves forward.

Progression created under the new format must still persist correctly across
save/load, restart, reconnect, death/respawn, and dimension changes. This scope
decision removes legacy migration work, not normal persistence requirements.

### Character progression

- Covenant character level 1–60.
- One starting skill point at level 1, then one additional skill point per character level (60 total).
- Character progression begins on first combat class selection at Level 1 with
  zero character XP and one skill point. Classless players cannot earn or bank
  character XP; earlier combat grants no retroactive XP. Profession progress is
  preserved when a class is selected.
- Combat XP earned primarily from hostile combat, supplemented by major milestones.
- The default character dimension route is Overworld 20, Nether 40, End 60.
  A player's first qualifying Wither kill grants one full character level past
  the dimension gate, once per player. Later Wither kills use normal XP and caps.
  Every eligible unclaimed contributor, including support-only players, gets
  their own full level; group size and the final blow do not divide or reserve
  this milestone. Class selection and the global level cap still apply. Persist
  each player's claim. Preserve percentage XP progress through the level, with
  integer XP rounded down as needed; reaching the global cap banks no excess.
  The first-time milestone replaces that player's ordinary Wither XP share;
  repeat recipients receive normal shares under the usual caps. Forgone ordinary
  shares are not redistributed. Mark the claim only when the level is granted,
  committing it together with level, preserved progress, and the skill point.
  Classless/global-capped players do not consume it, and no ungrantable reward
  is queued or paid retroactively.
- Initial opening tuning targets roughly five ordinary hostile kills while solo
  to reach character Level 2. The starting Level 59-to-60 benchmark is 3,000
  ordinary vanilla Enderman kills or 30 reference boss kills at full solo rewards
  where the dimension gate allows Level 60. Include vanilla Endermen explicitly
  as XP sources. Use the agreed accelerating curve of roughly 13% more required
  XP per level, with reference rewards of 10 XP per ordinary mob, 20 per Enderman,
  and 2,000 per reference boss. Requirements run from 50 XP for Level 1-to-2 to
  60,000 for Level 59-to-60; see [[Character Progression]] for the exact formula
  and table. Classic WoW is the pacing-feel reference, subject to playtesting.
  The reference boss is a repeat Wither kill at 2,000 base character XP before
  sharing/caps, with the first-time level milestone replacing that player's
  ordinary share. The 30-kill final-level benchmark requires those repeat kills
  in a dimension allowing Level 60 (the End by default). Other enemy/boss rewards
  remain tunable; profession pacing is separate.
- Hostile mobs are the default combat XP sources. Other combat mobs, including
  neutral mobs, require explicit inclusion through entity IDs or tags. Ordinary
  livestock and player kills grant no character XP in the default slice.
- Eligible spawner-produced combat mobs award the same configured XP as
  equivalent naturally spawned mobs, using normal contribution and cap rules.
  No spawn-origin penalty or provenance tracking is required. Full Apotheosis compatibility is an eventual
  project target; this slice remains standalone without an integration dependency
  or a claim of tested compatibility. See [[Mod Compatibility]].
- Separate from vanilla XP orbs.
- Persistent, server-authoritative profiles.

### Combat skill progression

- Existing Stalker, Reaver, and Weaver trees.
- Diagram level requirements gate initial skill investment only.
- First point unlocks Rank 1; subsequent points increase rank.
- Rank 1 preserves current prototype behavior and values. Further ranks provide
  minor, flavorful, skill-specific upgrades, avoiding flat damage per rank or
  routine damage scaling. Total growth through maximum rank must remain modest
  enough to preserve vanilla balance; see [[Combat Skill Progression]].
- Aura passive strengths remain at Rank 1 values across ranks. Their rank
  upgrades may modestly improve activations or coverage, with existing stacking
  rules retained; exact tables remain to be authored.
- Most arrow-shot rank upgrades use arrow velocity for the prototype, including
  current Piercing Shot and Aimed Shot. More varied rank progression is deferred.
  Each extra rank adds 2% of Rank 1 launch velocity, reaching +14% at Rank 8.
  Retain native speed-based arrow damage, accepting its indirect damage growth;
  do not add damage compensation. Aimed Shot retains its current wind-up
  movement behavior. Other skills' rank tables remain a later content dependency.
- Until their upgrades are authored, non-arrow playable skills allow only Rank 1.
  Keep their planned 8/12 maximums as metadata; never sell a rank with no benefit.
- Per-skill maximum rank 8 or 12.
- Initial playable caps: Lunging Combo and Rip and Rush each cap at 12; all
  other currently playable combat skills cap at 8. Define caps per skill;
  unfinished skills' future caps remain open.
- Maximum eight distinct skills with points allocated at once.
- Server-enforced prerequisites and mutual exclusions.
- Skills tree clicks stage upgrades: left-click adds one pending rank and
  right-click removes one pending rank, never below the committed rank. A simple
  Apply button at the bottom right submits the complete draft for server
  validation and commit. Pending upgrades grant no gameplay effects or binding
  access. Invalid/stale requests cannot partially spend points or allocate ranks.
  Closing without Apply discards unapplied edits with no confirmation dialog;
  reopening reads committed server state. Drafts are not saved or restored.
  Editing can happen in combat, but Apply requires being out of combat using
  the shared seven-second timer, checked on the server at commit. A blocked
  Apply spends nothing and retains the local draft while the screen is open.
- Crafted respec item that refunds combat skill allocations.
- Prototype combat-respec recipe: four paper plus one lapis lazuli, shapeless
  at a vanilla crafting table, producing one item. Hold right-click for two
  seconds (40 server ticks) to use it; releasing early cancels without spending
  it. Successful server-validated completion consumes one and refunds combat
  allocations. Respec requires being out of combat at start and completion;
  entering combat during the hold cancels without consuming the item. Use one
  seven-second (140-tick) combat timer for all classes and Rage grace, refreshed
  by confirmed combat damage dealt/taken and qualifying support. The timer and
  support alone do not generate Rage; see [[Combat]].

#### Agreed combat-content boundary (planned)

No new combat skills are required for this slice. Add progression and meaningful
rank upgrades to existing playable skills that are reachable through implemented
prerequisites. Unfinished nodes remain visible but cannot receive points. A
playable skill behind an unfinished prerequisite also remains unavailable for
investment until that prerequisite is implemented; do not bypass or rewrite the
intended prerequisite chain to make it available in this slice.

This limits initial build variety while allowing the progression loop to be
demonstrated across Stalker, Reaver, and Weaver. Completing the remaining combat
skill roster is separate work. These are planned allocation rules; the current
free-allocation prototype has not yet changed.

### Professions

#### Content dependency

Profession abilities and their requirements are being designed separately by
the user. Keep the initial roster and its costs, gates, prerequisites,
specializations, rank details, and associated recipes open until those
requirements arrive. The Mining Vein Mining/Prospecting pair was proposed, not
selected as the slice roster. Existing notes remain context for that separate
work. See [[Profession Progression]].

The shared progression foundation can be planned independently. Profession
ability implementation and playable-content acceptance remain dependent on
the supplied requirements; the full slice's content criteria are not removed
or considered satisfied by framework-only work.

All four are included:

- Mining
- Logging
- Farming
- Crafting (Enchanting is a subset)

Profession progression starts on first login and does not require a combat class
or character combat level. Players can earn XP, spend Profession Points, and use
earned profession benefits before selecting a class. Later class selection
preserves all profession progress and allocations. See [[Profession Progression]].

Each starts at level 0, defaults to level 20, shares a profession XP curve, and has independent XP/points. One Profession Point is earned per level. The slice implements a modest playable selection of level-gated abilities, variable-cost investments, a few genuine competing choices, and automatic recipe unlocks at selected profession milestones.

The slice does not require all 20 levels to be fully populated or every future ability to be designed. Most eventual profession options should be obtainable, so the initial selection should demonstrate broad mastery rather than artificially restrictive builds.

### Profession respec

A crafted item resets one selected profession's ability/specialization allocations, refunds that profession's points, and preserves its XP/level. Its cost is higher than combat respec. Exact recipe remains to be determined.

### XP and dimensions

- Separate Covenant character XP and profession XP tracks.
- Contextual action-based profession rewards; ordinary vanilla crafting excluded from Crafting XP.
- Direct player activity is default; automation disabled unless specifically configured with attribution/anti-exploit safeguards.
- Multiplayer combat XP is split equally among eligible contributors, including support-only
  participants. One confirmed positive damaging hit qualifies without an amount
  or percentage threshold; misses and zero-damage attempts do not. Personal
  damage is not required for support qualification. Overlapping copies of the
  same qualifying passive aura credit all supplying players, while its bonus
  still applies once and each player counts once. Passive aura benefits supplied
  to fighting allies qualify, as do supplied shields absorbing enemy damage and
  successful enemy control/debuffs during combat. Presence alone is insufficient.
  Each player counts once, with no larger share for more damage or the final
  blow. A 30-second (600-server-tick) window per player per enemy refreshes on
  qualifying damage or support. Store contribution timestamps and check expiry
  at death/payout, without recurring expiry scans. Contribution lists are
  server-only runtime state, reset on enemy unload or server restart; awarded
  progression remains saved. Contributor death/respawn
  preserves recorded credit within the remaining window; Minecraft handles
  Hardcore without a separate Covenant system. Contributors must be online at
  payout; offline players are excluded before splitting, with no deferred award
  or payout on reconnect. No distance/radius check applies to recorded
  contributors at payout; action and effect ranges still apply normally.
  Dimension changes also preserve credit within the remaining window; apply
  the character XP cap of the dimension where the enemy dies, regardless of
  the recipient's location. Split before applying individual caps; capped
  contributors still count, and blocked portions are discarded without banking
  or redistribution. Detailed attribution,
  Round each ordinary share up to whole XP before applying caps: 10 XP split
  three ways grants 4 XP each. The small total rounding bonus is intentional;
  keep no fractional sharing balance. Remaining source-attribution details
  remain open; see
  [[Experience and Reward Systems]].
- Dimension gates use exact IDs or tags, configurable priority, and fallback (character 60, profession 20).
- XP beyond a dimension cap is discarded, not banked; existing levels never decrease.

### Balance/configuration

- Configurable XP curves/rewards, track caps, and dimensional rules.
- Character XP requirements, enemy rewards, and dimension-cap rules use server
  datapack JSON with built-in defaults and datapack overrides. Validate on load
  and support the server datapack reload workflow. Balance changes preserve
  earned levels, points, allocations, and raw XP; future awards use the new
  requirements, so percentages may change. A harder curve never subtracts
  earned progression. Exact schema and validation/failure handling remain open.
- Data-driven identifiers/tags for modded content and dimensions.
- All classes can access all professions.
- No primary attribute point system.
- Profession effects are class-agnostic and modest when they overlap with combat.
- Profession abilities do not consume combat keybinds.

### Character XP presentation (agreed, planned)

Squeeze a thin Covenant XP bar directly below vanilla XP in the existing bottom
HUD footprint. It has no text or labels, including no HUD character-level
number. Do not add a row above the existing HUD stack or move the hotbar.
Detailed level, current/required XP, percentage, and unspent combat points belong
on the existing Stats page, reading owner-synced authoritative progression.
See [[Character Progression]]. Pixel fit and native bar variants remain manual
client validation work, not verified rendering.

## Explicit boundaries

- Do not replace or gate vanilla Minecraft XP, enchanting levels, baseline crafting, mining, logging, farming, tools, blocks, resources, or ordinary recipes.
- Do not create a mandatory profession choice or profession slot system.
- Do not introduce a second character attribute-point economy.
- Do not hardcode a dimension compatibility list.
- Do not require every future profession ability, recipe, or custom workstation to exist in this slice.
- Specialized Covenant workstations for advanced affix crafting and salvaging are planned; exact roster and delivery milestone remain open.

## Acceptance criteria

1. Character XP advances character level independently of vanilla XP orbs and awards the correct skill-point schedule.
2. Character level gates the first skill point only; further ranks are available without additional character-level gates.
3. The server prevents allocation in more than eight distinct skills and enforces rank caps, prerequisites, and exclusions.
4. Combat respec refunds invested points without reducing character level/XP.
5. Each of the four professions independently gains XP, levels, and Profession Points from configured meaningful activities.
6. Profession rewards distinguish automatic recipe unlocks from purchased abilities.
7. Each profession has enough playable initial content to demonstrate its interaction style and at least one meaningful investment decision.
8. Profession respec resets one profession only and retains its XP/level.
9. Dimension gates match exact identifiers and tags, resolve priorities deterministically, use fallback for unmatched dimensions, and never reduce existing levels.
10. XP past a dimension gate is not banked for later dimensions.
11. Modded tools/ores/dimensions and configured activity sources can participate without hardcoded mod support.
12. Vanilla XP and baseline vanilla gameplay remain unaffected.
13. Character and profession progression persist across logout/restart and remain server-authoritative in multiplayer.
14. Unfinished combat skills and skills blocked by unfinished prerequisites remain visible but reject point investment on the server; the slice does not require new combat skill implementations.
15. Rank 1 matches current prototype behavior and values; authored rank upgrades follow the modest, flavorful progression rules in [[Combat Skill Progression]], including review of cumulative power at maximum rank.
16. A player can progress all four professions and use earned profession benefits before choosing a combat class; later class selection preserves that profession state.
17. Before first class selection, combat grants no character XP and banks nothing for later. Selecting a class starts character progression at Level 1, zero character XP, and one skill point without changing profession progress.
18. A class-selected player can qualify for shared combat XP through support alone, including passive aura support tied to actual combat, without personally dealing damage or activating the aura. Mere presence grants no credit. Qualification follows [[Experience and Reward Systems]]; detailed attribution rules remain to be specified before implementation.
19. Divide the enemy's base XP reward equally among eligible contributors, counting each player once across damage and support, and round each share up before applying caps. A 10-XP reward split three ways grants 4 XP each; the total rounding bonus is intentional. Higher damage and the final blow grant no larger share.
20. Qualifying damage and support refresh only that player's 30-second (600-server-tick) contribution window for that enemy. At death/payout, contributions older than the window are excluded using timestamps, without recurring expiry scans or per-contributor tick timers.
21. Contributor death/respawn does not erase or duplicate recorded participation credit or reset/pause its window. XP awarded before or across respawn persists correctly. Hardcore behavior remains Minecraft's responsibility.
22. Contributors must be online at enemy death/payout. Offline contributors are excluded before equal shares are calculated, and reconnecting does not grant a missed payout. No offline award queue or deferred player-file writes are required.
23. Recorded contributors have no payout distance/radius restriction. Retreat, teleportation, and respawning far away preserve credit within the original window; skill and support-effect range rules are unchanged.
24. Recorded credit survives dimension changes and cross-dimension respawn within the original window. Combat awards use the character XP cap of the enemy's death dimension, not the recipient's destination dimension.
25. Otherwise-eligible contributors at dimension/global character caps still count in the equal split. Apply each player's cap afterward, accepting only what fits and discarding the rest without banking or redistribution.
26. One confirmed positive damaging hit qualifies the attributed player without a minimum amount, percentage, or hit count. Misses and zero-damage attempts do not grant damage credit; successful support can still qualify independently.
27. All owners supplying the same qualifying passive aura during combat can receive credit, even though the bonus applies once. Multiple auras and damage/support participation never give a player multiple shares.
28. Enemy contributor records are never saved or synced and reset on enemy unload/removal or server restart. A reloaded enemy requires fresh participation; already-awarded player progression remains persistent.
29. Default combat XP eligibility covers hostile mobs, with explicit entity-ID/tag inclusion for other combat mobs. Ordinary livestock and player kills give no character XP; ordinary hit eligibility and vanilla rewards remain unchanged.
30. Eligible spawner-produced mobs award normal configured XP under the same contribution and cap rules as equivalent naturally spawned mobs. Spawn origin alone neither excludes nor reduces rewards. Apotheosis-specific integration remains future work.
31. Each eligible unclaimed Wither contributor, including support-only participants, receives a one-time full level past dimension gates, respecting the global cap and class requirement. Preserve percentage XP progress across that level, rounding integer XP down as needed and retaining no post-cap excess. The milestone replaces that player's ordinary Wither XP share without redistributing it; repeat recipients receive ordinary shares under normal caps. Track each claim persistently and independently; the milestone is neither split nor reserved for the final blow.

## Dependency-ordered implementation plan

Stages 1–5 are implemented in the combat/shared foundation. See
[PROGRESSION.md](../docs/PROGRESSION.md) for the shipped scope and data format,
and [VALIDATION.md](../docs/VALIDATION.md) for evidence and manual boundaries.
Profession content remains dependent on the user's separate requirements;
non-arrow ranks above 1 remain unavailable until their upgrades are designed.

1. **Definitions and saved foundation.** Load server datapack XP requirements,
   enemy rewards, and dimension rules; validate and prepare lookup data. Extend
   fresh-save player progression with points/ranks and the Wither claim.
   Initialize the four profession tracks independently of combat class selection.
2. **XP and level engine.** Centralize server awards, multi-level advancement,
   point grants, dimension/global caps, and no-banking behavior. Preserve earned
   state across balance changes. Keep vanilla XP independent.
3. **Combat acquisition.** Add eligible enemy rewards, event-driven damage and
   source-owned support credit, runtime contributor timestamps, equal-share
   payouts, online checks, and death/respawn/dimension handling. Add the atomic
   per-player first-Wither milestone using the same eligibility boundary.
   Broaden the existing seven-second combat timer to all classes and qualifying
   support here, before Apply and respec depend on it. Keep it distinct from
   the 30-second per-enemy XP contribution window.
4. **Combat skill investment.** Replace free allocation with point/rank spending,
   initial level gates, the eight-skill ceiling, prerequisites, and exclusions.
   Stage left/right-click rank edits in the UI and commit them through the
   bottom-right Apply button using one validated server request, requiring the
   shared seven-second combat state to have expired at commit time.
   Keep unfinished nodes unavailable. Resolve rank parameters through existing
   skill execution boundaries, with current values at Rank 1 and small authored
   upgrades. Retain planned cap 12 for Lunging Combo and Rip and Rush and cap 8 for other
   currently playable skills. Apply the specified additive +2%-of-base velocity
   per extra rank to Piercing Shot and Aimed Shot, retaining native damage
   behavior. Other playable skills allow only Rank 1 for now. Their individual rank-effect tables remain separate design work;
   do not describe their ranked gameplay as complete before those tables exist.
5. **Playable presentation and combat respec.** Extend owner-synced screens/HUD
   to show progression and point/rank availability while preserving the compact
   current-class tree. Add the crafted combat respec and its server-side refund,
   assignment, and active-skill cleanup. Use the agreed four-paper/one-lapis
   shapeless crafting-table recipe and two-second native held-item use, with
   no consumption on early release. Enforce out-of-combat use through the shared
   seven-second timer used by Rage grace. Fit the thin unlabeled Covenant XP
   bar below vanilla XP without growing the HUD upward; show detailed progress
   on the existing Stats page.
6. **Profession content integration.** Integrate the supplied profession ability,
   reward, recipe, and respec requirements when ready. Shared foundation work
   alone does not satisfy the full slice's playable profession-content criteria.

Validate each stage with focused checks, then run the required project build and
dedicated-server checks against the final implementation. Verify new-format
persistence, network validation, payout attribution, caps, Wither claims, and
allocation/refund accounting. Hand off client presentation and representative
leveling-time checks for manual playtesting under the development workflow.

## Deferred beyond the slice

Fully populated profession trees; final numerical XP/ability balance; all possible modded dimension/activity integrations; complete future workstation roster; every affix manipulation/salvage recipe; all future profession branches; and complete endgame content.

Related: [[Progression Systems]], [[Project Covenant - GDD]], [[Character Progression]], [[Combat Skill Progression]], [[Experience and Reward Systems]], [[Profession Progression]], [[Dimensional Progression Gates]], [[Progression Balance Configuration]].
