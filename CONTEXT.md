# Project Covenant context

## Summon polish acceptance and current scope

Hunter's Pet is user-accepted and Phase 7 complete. Elemental Golem now has its
Weaver level-25 Rank 1 skill, approved four-form assets, five body animations,
slow dynamic cores and restrained form particles. The skill summons Arcane for
40 base Mana; active presses cycle Arcane → Fire → Ice → Lightning → Arcane for
free, with a fixed five-tick switch delay. Other spells do not select the form.
Remove its final slot assignment to dismiss. Defeat/dismissal starts a 30-second
base cooldown, scaled and captured at summon. All forms share 60 HP, 3 native
physical melee damage and a 40-tick attack interval. Iron ingots repair 25 HP.

Owner-only benefits use a 12-block spherical range: Fire +10% spell damage,
Ice 20% shorter cast times / 10% shorter cooldowns, Lightning +20% duration /
maximum Mana, Arcane +20% Mana regeneration / 10% lower costs. Capacity never
grants Mana; losing Lightning permanently clamps excess. Native transient
attributes, spell snapshots and the existing owner-only status HUD provide
integration; the fixed cycle delay is separate from skill cooldown modifiers.

The golem reuses Hunter's Pet targeting, native movement/follow/melee, loaded
body-safe placement/recall and owner-saved native snapshot policy. Injuries,
status, form and remaining attack delay survive logout/restart/dimension travel;
no offline ticking, chunk forcing, summon payment or healing on restore. Owner
death/class change dismisses and starts cooldown. Datapack tuning lives in
`data/covenant/skills/elemental_golem.json`; combat/cooldown snapshot at summon,
benefits/range update on reload. Registry IDs are unchanged; the former unsaved
operator prototype has no saved data to migrate. Operator summon now respects
skill ownership, assignment, Mana and cooldown; explicit form overrides remain
for testing.

Animated sources remain local in `models/elemental_golem_animated/`, with approved
static sources preserved in `models/elemental_golem/`. [Motion review](.local/blockbench/golem-animations/review.html)
provides fixed-camera playback/scrubbing. Client playback, form-change feedback,
range feel, reconnect/restart and multiplayer still need manual playtesting.
See the [roadmap](<Project-Covenant/Visual Polish Roadmap.md>) and
[skill notes](docs/RPG_SKILLS.md) for details and validation boundaries.

## Promotional art benchmark

The user approved the final lit, halo-free hybrid combat-class banner on 2026-11-10.
Use [the benchmark and visual rules](docs/promotional-art/README.md) for future
promotional artwork: finished Minecraft-adjacent voxel forms, restrained neutral
lighting and floor shadows, quiet dark background, gothic border and serif type.
The canonical PNG is `docs/promotional-art/benchmark.png`; that folder is local-only,
Git-ignored, and absent from fresh checkouts. This is promotional-art
approval; runtime visual acceptance remains separate.

## Game design reference

The foundational [Project Covenant GDD](<Project-Covenant/Project Covenant - GDD.md>)
lives in the **Project-Covenant** Obsidian vault at `Project-Covenant/` in this
repository. Read it before feature work and follow its Obsidian wikilinks to the
relevant system notes. The GDD describes the intended design and roadmap;
the current purpose below and `docs/RPG_DATA.md` describe the implemented scope.

The [progression slice](<Project-Covenant/Progression Vertical Slice.md>) now has
its combat and shared saved foundation. Character XP requires a selected class;
first selection starts level 1, zero XP, and one skill point. Positive damage
and qualifying support earn equal, rounded-up shares from eligible enemy deaths.
Default dimension caps are Overworld 20, Nether 40, End 60; the first qualifying
Wither grants each unclaimed contributor one full level past the dimension gate.
Server datapack balance reloads preserve earned levels, allocations and raw XP.
Dimension grouping uses native dimension-type tags; explicit dimension IDs win.
See [PROGRESSION.md](docs/PROGRESSION.md) for format, APIs and boundaries.

Five independent activity tracks, the Building meta-profession and Exploration initialize
on login at level/XP zero. The foundation adds paid-cost allocations, a server-datapack
catalogue, owner-synced seven-tab Professions menu, atomic per-tab Apply, and a
Profession Respec item. Held use for two seconds out of combat refunds all seven
professions together while preserving XP/levels. Logging's Offensive Mastery is
usable alongside Vein Mining, Tree Felling, Farming Scything/Automatic Replanting
and all four Crafting nodes; eleven authored nodes (including three Building placeholders) remain visibly planned and unavailable. Natural Mining, Logging, Farming and enchanting-table Crafting
XP now use confirmed server outcomes, the provisional shared curve and reward
table, persistent chunk-owned placed-ore exclusions, and bounded tree recognition.
Vein Mining and Tree Felling use bounded native per-block harvests, sneaking to
bypass, normal tool costs/drops/XP, and stop at tool break or the first blocked
harvest. Both default to 4/8/16/32 total blocks at ranks I-IV, including the
initiating block. Lowest remaining recognized logs can continue partial trees using
disposable tree evidence; no unfinished harvesting job is saved.
Item drops from each batch gather at the initiating block's native drop point;
cancelled loot remains cancelled and vanilla XP stays on its native per-block path.
Farming hoe right-click harvests one mature crop, or a learned 3×3/5×5/9×9
same-height square. Sneaking keeps one crop; each successful harvest costs one
Unbreaking-aware durability use. Learned Farming replanting spends inventory
planting items through native placement, including the last crop when the hoe
breaks. Crop maturity/planting items and Scything widths are datapack-defined.
Owner-synced crop targets let the client consume the hoe action without a second
offhand use; gameplay remains server-owned (profession protocol revision 5).
Other gathering abilities remain deferred. The shared, persistent Covenant Workbench
now supports Add/Upgrade/Reroll/Remove and ten-input/twelve-output salvage,
Crafting access/XP, and Recovery/Focused Recovery. It uses an original stone
ritual-station model and the agreed ungated recipe. See [ITEMIZATION.md](docs/ITEMIZATION.md).
Additional profession-level recipe milestones are outside the
progression slice; Workbench and respec recipes remain ungated. See
[PROFESSIONS.md](docs/PROFESSIONS.md) and
[Profession Progression](<Project-Covenant/Profession Progression.md>).
Itemization includes Covenant-owned equipment rarity with server datapack definitions and
item/tag-based opt-in rules for modpack developers; see
[Affixes and Rarity](<Project-Covenant/Affixes and Rarity.md>).
Use fresh saves: prototype profile/loadout migration is deliberately unsupported.

Building's [profession foundation](<Project-Covenant/Building.md>) is
implemented: a fifth class-independent tab, 65% of XP actually
credited to Mining/Logging/Farming/Crafting/Fishing, no contribution from capped sources
or discarded overflow, shared Level 0-20 curve and one Building point per level.
Fractional credit persists as hundredths; source and Building awards commit
together. Dimension caps default to 20 everywhere. This work
uses fresh saves without migration or catch-up. Reach, cosmetic wood/stone
swapping and a further undecided skill remain unimplemented design directions.

Fishing and Exploration now complete the seven-profession foundation. Fishing
earns 50 XP once per native loot-bearing reel-in, including fish, junk and
treasure regardless of item count. Cancelled/empty catches and entity hooks earn
nothing; native loot, rod behavior and vanilla XP remain unchanged. Fishing uses
the shared Level 0-20 curve and contributes 65% of accepted XP to Building.

Exploration is the explicit no-XP exception: 22 one-time default milestones
(10 Overworld, 8 Nether, 4 End) provide levels and points up to Level 20. Available
rewards require both later dimensions without extra visit gates or dimension XP
caps. Its paged owner checklist shows collection progress, with skills deferred.
`data/covenant/progression/exploration.json` is a server-datapack replacement
catalogue with biome/structure tag or ID targets and grouped structure families.
The server-only saved `covenant:exploration` ledger retains discoveries and claims
through native lifecycles. Reloads may reward new goals from recorded discoveries
without revoking earned levels or repeating claims. Position changes check only
loaded chunks and structure starts; stationary retries handle newly loaded starts.
There is no chunk forcing or individual structure-instance tracking. Raw history
stays on the server; the owner receives a transient checklist view. Protocol is
`professions/6`. Both new professions' skills remain deferred, and this format
requires fresh saves without migration. See [PROFESSIONS.md](docs/PROFESSIONS.md)
and [Profession Progression](<Project-Covenant/Profession Progression.md>).

All classes share the seven-second combat timer for damage/support, Rage grace,
Apply and respec. Support/timer refresh alone grants no Rage. Skills use earned
points, unlock levels, prerequisites, exclusions, and an eight-distinct-skill cap.
Tree clicks draft ranks locally; bottom-right Apply commits out of combat.
Piercing/Aimed Shot support ranks 1–8 (+2% base velocity per extra rank, native
impact damage retained). Other playable skills allow rank 1 until upgrades are
authored; planned 8/12 caps remain metadata. The crafted Combat Respec consumes
four paper plus one lapis to craft, then requires two seconds of held use out
of combat to refund all skills and clear assignments without resetting resources.
A thin unlabeled XP bar fits below vanilla XP; Stats shows exact progress/points.
Client rendering and gameplay feel still require manual playtesting.

## Current purpose

Develop a Minecraft Java Edition mod for **26.1.2** using **NeoForge**.
ATM11 is the eventual pack target; pack integration is outside the current scope.
Full Apotheosis compatibility is an eventual target, including spawners in the
progression model. Apotheosis integration is not implemented or validated; the
progression slice remains standalone. See
[Mod Compatibility](<Project-Covenant/Mod Compatibility.md>).
The radial menu opens while holding **G** (rebindable under Controls > Key Binds >
Project Covenant) with ten numbered slots. Slot 1 is **Skills**: hover and release G,
or left-click it, to open the non-pausing Skills window. Slot 2 opens Stats with the
same controls. Slot 3 opens Professions even before class selection; slots 4-10
are placeholders. Stats shows the current class and a
scrollable text list of supported native stats, bases, and active modifiers. Players
without a class can select Stalker, Reaver, or Weaver there through the existing
one-time server command. The page reads synced attributes/profile state; it adds
no saved data or stat calculation system.
Slot 1 starts at the top; slots proceed clockwise. Mouse hover highlights a slot;
release the binding to select or press Escape to cancel. It does not pause the world.
It opens only in a living player's game, outside other screens,
and closes on loss of focus. Combat Respec and Profession Respec have native
held-use interactions and shapeless recipes.

The RPG storage foundation is server-authoritative: `covenant:character_profile` is
an immutable, persistent, owner-synced NeoForge player attachment with death copying.
`covenant:affix_page` is an immutable persistent/synced item-stack data component;
each page-bearing stack has maximum size 1. Equipment modifiers stay on items,
separate from permanent character progression. Open namespaced categories use item
tags for compatibility. Fresh entity-table equipment and vanilla chest loot now
receive rolled affixes, with configurable bonus equipment; previously owned
equipment is never initialized on ordinary drops. Accessory-mod integration remains deferred.
See `docs/RPG_DATA.md` for APIs, format contracts, and copy/split semantics.

The initial class identities are **Stalker** (bow/ranged precision and distance),
**Reaver** (dedicated melee), and **Weaver** (elemental offensive magic).
`/covenant class` lists descriptions and the current choice; any player can choose
once with `/covenant class select covenant:stalker` (or `reaver` / `weaver` in that
namespace). Selection initializes the existing profile and class resource balances.
Its saved class ID retains the profile's persistence and owner sync guarantees.
Classes each have a playable basic skill, and Stalker also has Aimed Shot and all
four traps (Bear, Frost, Flame, Blast), plus Disengage, Arcane Arrow, Multishot and
Hunter's Pet at Rank 1. Disengage makes a four-block backward hop over six ticks, works
airborne and breaks falls while keeping stationary attacks usable. Arcane Arrow
replaces its native arrow impact with four magic damage and applies four seconds
of +15% damage taken after health/absorption damage; Shock adds to +25%.
Multishot uses one ammo item for five native arrows over thirty degrees, with
one attempt per target per fan even when blocked/dodged. Shared movement handles
prediction and lifecycle cleanup; saved native arrow payloads and target fan
claims preserve effects across save/load. Hunter's Pet reuses wolf movement and
melee AI, with owner-directed targeting and ordinary retaliation. It costs 35 Focus
for a 40-health, 3-damage companion biting every 30 ticks; defeat/dismissal starts
a 600-tick cooldown. Native food heals it; no passive regeneration, sitting,
breeding or armor. The skill must stay assigned. Owner-saved native snapshots
retain wounded health/effects across logout, restart and dimension changes;
offline pets do not tick and no pet forces chunks. Owner death, class change or
last unassignment dismisses it; pet damage grants owner Covenant combat credit.
Its tuning lives in `data/covenant/skills/hunters_pet.json`. Other existing Stalker
tuning and server-datapack skill configuration remain separate work. See `docs/RPG_SKILLS.md`
for detailed lifetimes and boundaries. Weaver also has Fire Bolt, Ice Bolt, and
Lightning Bolt. Fire ignites through native burning; Ice applies one saved/synced
30% Chilled slow; Lightning is an instant aimed hit after its cast, with no status.
All three prototype bolts cost 20 Mana, cast in 0.5 seconds, have independent
two-second cooldowns, and deal six damage. Fire's four-second burn and Ice's
three-second chill scale with skill duration. Lightning stops at the first eligible
enemy or solid block within 24 blocks. Weaver also has Rank 1 Discharge, Threefold
System and Teleport. Completed
fire/ice/lightning bolt casts bank up to five charges each for Discharge: a
five-block elemental burst with full-charge freeze, explosion and external-chain
riders. Assigned Threefold banks a fraction of actual damage for the other two
elements in a ten-second rolling window, then consumes its reserves for fixed
percentage bonuses while pausing banking. Teleport blinks up to eight blocks along
full aim, checking the entire body path before spending and resetting momentum
and fall distance on success. Floor/ceiling contact clips vertical travel while
allowing remaining forward movement; walls stop the blink, and airborne endpoints
need no landing support. Entities do not obstruct the blink. Owner-only top-left
icons show elemental charges and reserves/buffs.
The three skills use `data/covenant/skills/weaver.json` server tuning; charges,
reserves and buffs are unsaved and clear on lifecycle changes. Threefold also
clears on removal of its last assignment. See `docs/RPG_SKILLS.md` for numerical
defaults, damage snapshots, collision and cleanup contracts. The
Reaver also has Rend and a shared bleed foundation, Battle Cry, Rage Storm,
and the slotted Blood Fever passive. Their Rank 1 defaults follow the Reaver balance
proposal. Battle Cry grants nearby allies +15% outgoing damage for six seconds;
new projectiles and bleeds capture the bonus once. Rage Storm winds up for sixteen
ticks, then travels eight blocks along full release-time aim, stopping at solid
terrain; its two-block area pulses for three armor-mitigated damage every ten ticks.
Blood Fever counts up to five bleeding enemies within four blocks every five ticks
for up to +10% base movement and +15% base attack speed. It unlocks at level 5
without a skill prerequisite; Rend is not required.
Server datapacks tune these three skills through `data/covenant/skills/reaver.json`;
higher ranks and converting other Reaver tuning remain separate work. See
`docs/RPG_SKILLS.md` for snapshots, cleanup and resource bounds. The remaining brainstorm nodes
remain visible but unavailable for point purchases, without passive stat
bonuses or global equipment restrictions.
`PlayerProfiles.getSelectedClass` resolves built-in definitions;
unknown saved IDs are preserved without fallback. See `docs/RPG_DATA.md`.

The deliberately small stat foundation uses native attributes for health, vanilla
attack damage, armor, movement speed, and attack speed, plus player-only Dodge and
four neutral skill multipliers (cost, cooldown, cast time, duration). Equipped
pages contribute transient native modifiers in their matching vanilla slot;
inventory pages do not. No effective equipment values are saved in profiles.
The server handles dodge and equipment calculation; native attribute packets sync
values. A narrow player-load Mixin and clone handler restore equipped max health
before restoring current health, avoiding vanilla's early load clamp without
duplicating saved health. There are no primary attributes or stat points;
earlier broader design notes are roadmap material.
See `docs/RPG_STATS.md` for IDs, units, formulas, extension APIs, and boundaries.

Operator-only `/covenant stats` debug commands list effective values, inspect bases
and modifiers, temporarily substitute a testing base with `set <stat> <value>`, and
remove all debug adjustments with `reset`. They use supported attribute registry
IDs with completion. A native transient flat adjustment preserves equipment/page
calculations without editing saved bases, profiles, or pages; it disappears on
reload/reconnect or player replacement. Existing class commands are unchanged.

Operator-only `/covenant affix` commands prepare main-hand equipment and supply
non-Common components. `set <family> <rarity> <tier> <random|min|max>` adds or
replaces a family, initializing fresh equipment with matching rarity/full Potential.
`rarity`, `potential <amount>|full`, `remove <family>`, `clear`, and `reset` support
further setup. Edits bypass costs/profession gates, award no XP, and enforce current
equipment limits. Clear keeps rarity/Potential and page identity; reset removes
both Covenant components while preserving other data and the current stack limit.
Tooltips replace the retired inspect command. See `docs/ITEMIZATION.md` for syntax.

The basic skill foundation uses a persistent, death-copied, owner-synced
`covenant:skill_state` attachment for independent class resource balances and skill
cooldowns. Stalker Focus has a hard 100 capacity and fast recovery; Reaver Rage
gains 1 Rage per normal-attack damage dealt and 2 per combat damage taken, after
defenses including absorption. Skill damage grants no outgoing Rage; combat grace
only prevents decay, with no passive generation. Rage depletes after seven seconds without
combat damage; Weaver Mana capacity/recovery scale through native attributes and
Affix Pages. Only the selected class recovers/depletes. Server configuration controls
base rates and Rage/Mana capacities; Focus recovery bonuses cap at 25%. Changed
resource snapshots sync at most four times per second, plus explicit damage gains/spending/edits.

`/covenant skill` lists definitions; `/covenant skill use <id>` activates Piercing
Shot (up to three enemies in a line), Aimed Shot (one stronger arrow after a wind-up),
Cleave (120-degree frontal melee sweep), Rend (native-reach melee strike and bleed),
or Arcane Bolt (single-hit magic
projectile), or throws Bear/Frost/Flame/Blast Trap. Costs and timers use existing skill multipliers. Server validation
and cast completion own all spending/effects. Cooldowns persist on the saved
overworld game clock; pending casts cancel on logout, death, class/dimension change.
`/covenant resource` inspects balances. Operator-only `resource set <amount>` and
`class switch <id>` support testing; ordinary selection remains one-time.
Operator-only `/covenant resource infinite` toggles a per-player debug bypass of
class-resource requirements and spending; `infinite true|false` sets it explicitly.
It applies to whichever known class is selected, including combo stages and cast
completion. Balances/HUD values and ordinary recovery/decay remain unchanged;
cooldowns, casting, learning, equipment, and control checks still apply. The flag
is unsaved and does not copy to replacement players, so reconnect/respawn resets it.
`/covenant resource` reports its state.
Operator-only `/covenant skill ignorecooldowns` toggles a self-only cooldown-check
bypass; an optional `true|false` sets it explicitly. `/covenant skill` reports the
mode alongside actual remaining timers. Timers still commit and elapse normally,
including combo completion; disabling restores any remaining cooldown immediately,
including revalidation at cast completion. Other activation rules and the one-tick
request throttle remain enforced. This independent unsaved flag resets on
reconnect/respawn and survives class switches on the same player.
See `docs/RPG_SKILLS.md` for defaults, APIs, persistence, and test steps.

Rend hits the crosshair target using native held-item melee reach (normally three
blocks in Survival), costs 20 Rage, and has a two-second cooldown. Positive health
or absorption damage applies one five-second bleed at 10% of captured attack
damage per second. Targets share a three-stack cap across attackers; reapplication
refreshes all durations and replaces the oldest stack at the cap. The server-only
`covenant:bleed` attachment saves per-stack ownership/power and relative timers,
pauses while the target is not ticking, and clears on death. One shared pulse per
second applies each stack independently of native hurt cooldown, bypassing armor,
blocking, and Dodge while retaining general mitigation and absorption. Other
bleed-dependent skills remain placeholders except Rip and Rush below. See `docs/RPG_SKILLS.md`.

Reaver's Lunging Combo and Rip and Rush are playable three-press sequences with
independent progression and fixed 40/60-tick follow-up windows. Lunging Combo
lunges four blocks over three ticks, strikes for 125% attack damage, then cleaves
for 100% in the existing three-block/120-degree area. Costs are 20/10/20 Rage;
its base cooldown is twelve seconds. Rip and Rush opens with three-block circular
Rend, dashes six blocks over four ticks for 50% damage per contacted enemy, then
performs a three-block circular 100% finisher consuming shared bleeds on damaging
hits (including kills). Costs are 30/20/20 Rage; its base cooldown is twenty
seconds. Cooldown duration snapshots at stage one and starts at completion,
timeout, or lifecycle/loadout cancellation. All skill attacks may whiff: no target
never rejects activation; resource spending, progression, and cooldowns still occur.
This also applies to Cleave and Rend. Cover/range/allies gate hits, not activation.

Both combos share one native harmful 20% armor-shred status for five base seconds,
with unchanged toughness, damage-gated application, native immunity/cleansing,
and a target-owned saved relative timer paused while inactive. It clears on target
death and survives caster departure; refresh retains the later expiry without
stacking strength. Rip and Rush grants +10% base speed per consumed enemy, capped
at +30%, for four base seconds, using a transient modifier cleared on expiry and
lifecycle/loadout edits. Duration scaling snapshots at the applying stage.
Movement captures full 3D aim, replaces momentum, suppresses gravity, breaks falls,
flattens pitch on floor/ceiling contact while preserving horizontal heading,
and clips blocked wall components without boosting the surviving sideways speed.
Movement completes early on full blockage
or root/freeze/effective knockback including
Blast pushes. One server-owned movement may run at a time, while other stationary
skills/attacks remain usable. The server advances native collision steps while an
owner-only start/stop trajectory lets the client simulate native movement with
normal frame interpolation. Normal dash travel sends no teleport corrections.
Each authoritative step updates the native connection's cached position anchor,
so its enclosing tick cannot restore the pre-step position.
Late predicted position packets are held through an endpoint acknowledgement;
the server resets the native movement anchor on a matching endpoint, with one
impulse-preserving correction on disagreement or a 40-tick acknowledgement timeout.
Costs, hits, movement completion, and follow-up windows remain server-owned.
Runtime, gravity suppression, and owner-only combo displays are never saved or copied.
Saved cooldown/resource/loadout formats are unchanged. Movement feel, latency,
and HUD layout remain for manual playtesting. See `docs/COMBOS_IMPLEMENTATION_PLAN.md`.

The Skills window displays only the current class's tree from
`Project-Covenant/Skill Trees Brainstorm.md` (62 total nodes across the three classes).
Left/right clicks draft upgrades; Apply commits point purchases out of combat.
Committed ranks require a respec item to refund. The server enforces all 12
prerequisite links, unlock levels and the Ripping/Fortitude Aura exclusion.
Unimplemented nodes remain visible and unavailable for points or bindings. Thirty-four
skills have gameplay, including all eleven auras. Assigned auras grant free
passives; instant activations commit a resource cost and cooldown. Source-aware
transient grants handle allies/pets, overlap, expiry, and loadout/lifecycle cleanup.
Fortitude is a separate six-point shield with an owner status icon; ordinary Dodge
caps at 85%, and Covenant slows use only their strongest active value. Freeze
suppresses native movement/actions and interrupts Covenant casts. No temporary
aura state is saved. See `docs/AURAS_IMPLEMENTATION_PLAN.md` and `docs/RPG_SKILLS.md`.
The entire current-class tree fits on one page:
compact square nodes resize to the available space, with temporary initials and
full hover/focus tooltips. All eight tiers and dependency/exclusion links stay
visible; empty diagram lanes are compressed while preserving the branch flow.
Right-hand **Tree** / **Keybindings** tabs
separate allocation from the learned-skill picker and eight slot assignments.
Select a learned skill there and click a slot to assign; right-click a slot to clear.
Defaults are **Q, R, F, C**, then **Shift+Q/R/F/C**;
each slot is rebindable in vanilla Controls. Only initial presses activate, with
modified bindings taking priority. `covenant:skill_loadout` persists committed ranks and
slot assignments, copies on death/End return, and syncs only to its owner. Requests
are server-validated; commands and cast completion also require purchased skills. Respec
cancels owned activity without erasing cooldowns. Class switching retains
inactive learning/assignments, but class checks prevent their use. Fresh saves
start without purchased skills. Stable attachment IDs remain, but profile/loadout
formats changed for progression; old prototype saves are unsupported.
Skill behavior is being added in dependency order, starting with basic attacks,
then targeting/defense and movement, statuses, buffs/auras, persistent areas/traps,
and channels/companions/compound mechanics. See `docs/RPG_SKILLS.md` for the order
and prototype tuning. The four Stalker traps share a transient native entity,
three-block horizontal toss, terrain collision, a 1.5-block-square contact trigger,
ten-tick landed arming, and
600-tick armed expiry. Each owner has one pending/armed trap per type plus separate
Frost/Flame active-patch slots. Death, logout, dimension/class change, dependent
unlearning, fluid/support loss, and chunk removal clean deployments without refunds.
Bear's saved/synced native root blocks movement input/jumps without freezing external
velocity; positive snap health/absorption damage is required. Frost applies one
source-aware transient 50% slow; Flame has independent per-owner twenty-tick damage
deadlines; Blast uses attributed native explosion damage tags and a custom impulse
with distance falloff, without block destruction. Frost/Blast have a 3.5-block
effect radius; Flame has a three-block radius. Solid cover blocks areas; all trap damage bypasses
shields, only Bear participates in Dodge. Root/patch duration snapshots on throw.
Profiles, Affix Pages, saved resource/loadout formats, and stable skill IDs are retained.
See `docs/TRAPS_IMPLEMENTATION_PLAN.md` for concrete tuning and the acceptance guide.
Aimed Shot reuses existing casts, slowing movement to 20%
during its wind-up with transient native state cleared on completion/cancellation.
Client config
`config/covenant-client.toml` defaults `unbindDropItem` to true: startup clears vanilla
Drop Item if it uses Q, retains other drop keys, and saves Controls. Set false to
keep Q for dropping and rebind skill slots. `unbindSwapHands` also defaults true and
applies the same startup policy to vanilla Swap Hands on F. Other swap keys are
retained. C's creative hotbar binding is retained.

The current class's resource HUD stays visible above hunger, including at full or
empty resource and outside combat: red Rage, blue Mana, yellow Focus, with centered
current/maximum values. It follows normal F1 visibility, reads existing synced state,
and reserves space for air/mount indicators above it. Existing skills have native
hand swings and bounded original pixel effects: pale/violet Arcane, ember Fire,
fractured Ice, real-link ivory/gold Lightning, a frontal Cleave crescent and
distinct precision/magical/fan arrow streaks. Collision and accepted-damage marks
are separate. A transient, tracking-synced arrow marker owns only shot visuals.
Piercing Shot uses a six-tick cosmetic vanilla bow charge in first/third person,
with a transient synced deadline and client render hooks rather than actual item use.
Saved resources/loadouts and gameplay rules are unchanged. Rendered HUD/effect
behavior remains subject to user playtesting.

Polish prioritizes player feel and useful feedback without crowding the visual
space. The first combat pass adds native positional cast/release/collision sounds,
aggregate melee contact cues, trap transition sounds, elemental cast/impact
particles, distinct arrow accents and accurate circular/square attack/deployment
outlines. Multishot and recurring effects keep restrained feedback. Arrow visual
styles are transient and tracking-synced. A second pass adds top-left native
icons for combos, Discharge, Threefold and received buffs, replacing the old
status text above hunger. Buff receipts/timers are unsaved owner-only snapshots
updated at existing transitions (skills network revision 6). Slotted area auras
show one merged, local-owner six-block supported outline; self-only auras have no
false radius. The bounded ring follows movement, cleans up on unslotting and
lifecycle changes, and remains visible on Minimal. Traps now use grounded native
block models with a small tumble during flight; spent cores disappear when an
area patch triggers. Saved state and combat tuning are unchanged. Listening and
rendered readability remain user playtesting work; progression/Workbench feedback
remains a later pass. See
[Abilities](<Project-Covenant/Abilities.md#presentation-and-feedback>) and
[RPG_SKILLS.md](docs/RPG_SKILLS.md#combat-feedback-pass).

## Identity and toolchain

- Working title: Project Covenant; mod ID: `covenant`.
- Developer: `kataphrakt0s`.
- Art correction (2026-11-10): the user finds the overall art too desaturated
  and realistic and wants bolder, more cohesive dark fantasy. Wolf and traps
  are accepted anchors; all other art is eligible for eventual redesign.
  [Art Reference Studies](<Project-Covenant/Art Reference Studies.md>) tracks
  reference comparisons. FurfSky's strong silhouettes are approved as a reference;
  darker black/purple/red/grey treatments are favoured, while much of its flashy
  neo-fantasy styling is unsuitable. Use these qualities selectively for a dark
  gothic identity. Combine forged equipment/cathedral ironwork with cursed
  relics/ritual objects/occult symbols according to asset context; neither
  vocabulary is mandatory everywhere. Exact palettes and family designs remain
  open. This supersedes earlier subtle/restrained aesthetic wording below.
- Icon follow-up: review individually for descriptive player meaning; keep
  silhouettes close to vanilla Minecraft items/icons. Recognizability takes
  priority over unique forged/relic shapes. Gothic colour and interior detail
  remain; the workbench is acceptable for now. See Art Direction's icon correction.
- The user subsequently authorized the remaining catalogue in small batches,
  asking for direction on unclear icons. A 60-texture pass is implemented for
  review at `.local/catalogue-review/index.html`; authoritative source is
  `models/gothic_rework/catalogue.py`, integrated into the usual icon/material
  rebuild. Six approved skills and all other resources are preserved. Icy Veins
  uses the user-selected pale-blue hourglass. The prior single Movement Speed
  boot is preserved and reused for movement effects. New drafts are not approved.
  Review corrections restore the prior Ice/Arcane Bolt icons and cores, use the
  classic circular bear trap for Rooted, and assign hourglass to cooldown and clock to
  duration. Rooted's icon need not match the trap model or skill icon.
  Flame Cloak is a cloth cloak with crimson flames along its hem.
- Runtime handoff (2026-11-10): the user requested packaging all implemented
  polish, including the latest catalogue corrections, into the built JAR before
  continuing the roadmap. These assets already live in runtime resources; no
  separate preview-to-runtime copy is required. No design decision blocks this
  build. Packaging does not imply final visual acceptance of the catalogue drafts.
  Phase 6 onward remains follow-up work; see `docs/VALIDATION.md` for build evidence.
- Runtime gothic rework: 129 textures, distinct relic components, crimson/violet
  menu framing and an eight-cuboid workbench altar are implemented for client
  review. User-selected elemental colours are occult crimson fire, pale blue
  ice and electric blue lightning. Wolf/trap device assets are preserved; Flame
  Trap feedback follows the crimson palette. Native burning remains vanilla.
  Pixel sources: ignored `models/gothic_rework/` plus existing combat sprite
  sources; altar: `models/ritual_workbench/workbench.bbmodel`. Local review:
  `.local/gothic-rework/review.html`. All existing gameplay/ID contracts remain.
- Visual direction accents vanilla Minecraft with dark fantasy/gothic-lite
  character, occult imagery and magical spectacle: Minecraft with an ancient,
  forgotten supernatural history. Stalker traps are crafted hunting devices
  containing occult power. The existing original wolf is the visual foundation
  for Hunter's Pet. Before art-direction or asset-brief work, consult
  the user's local reference index and HTML board in ignored
  `docs/art/references/covenant_gothic_lite_references/`. The board uses remote
  previews and requires internet; the collection is absent from fresh checkouts. See
  [Art Direction](<Project-Covenant/Art Direction.md>) for the current visual audit
  and proposed effects, trap and summon designs; specific asset concepts remain
  under review and are not runtime implementation claims.
  [Visual Polish Roadmap](<Project-Covenant/Visual Polish Roadmap.md>) owns the
  proposed delivery sequence: Frost Trap first, then combat/GUI/UX/items, advanced
  effects and gameplay-dependent companions, with per-slice validation.
- Mod logo: `src/main/resources/mod-icon.png`, referenced by `logoFile` in mod metadata.
  The approved cleaned 400x400 pixel-art logo is also the visual style reference:
  uniform square pixels, clean silhouettes and shading clusters, charcoal/gothic
  forms, stone/lavender tones and restrained violet accents. Adapt its treatment
  to each material and asset family; its C/sword motif, palette and 4x pixel grid
  are not universal requirements for gameplay assets. Local art guidelines,
  references and working exports live in Git-ignored `docs/art/`; the approved
  icon is tracked and packaged independently of that folder.
- Java package: `dev.covenant`; entry point: `Covenant.java`.
- Local-only model tooling (Git-ignored; absent from fresh checkouts):
  `covenant-bbmodel` runs the pinned Blockbench headless MCP over
  stdio without the editor. `scripts/Blockbench.ps1` installs/runs/checks it;
  `skills/covenant-modeling/SKILL.md` adapts the installed upstream skills to
  headless tools and the Java 26.1.2 consumer. Editable sources live in ignored `models/`,
  runtime exports in Covenant assets, previews in ignored `.local/blockbench/`,
  and portable Node/Bun/render dependencies in ignored `.tools/blockbench/`.
  Use the explicit `1.21.11` Java export profile for the compatible schema rather
  than upstream's `26.3` default. See `docs/BLOCKBENCH.md`; headless previews do
  not establish Minecraft client appearance or entity-export API compatibility.
- GeckoLib 5.5.2 is the required animation runtime for the pinned NeoForge 26.1.2
  target. `docs/GECKOLIB.md` defines the headless authoring/export contract for
  entities, block entities and items, including server-owned gameplay and
  client-only rendering. The original wolf now has exported idle/walk/attack/hurt/
  death clips and a separate editable GeckoLib source in ignored
  `models/original_wolf_animated/`; its atlas, UVs and rig remain intact, with four
  tiny shin insets to prevent leg/body surface fighting. The
  `wolfAnimationAssetTest` build check bakes and samples the shipped assets with
  the pinned runtime. Hunter's Pet now uses these assets through a client-only
  GeckoLib renderer/controller with native wolf movement and melee AI. Locomotion
  recognizes actual cached horizontal travel when the native movement flag is absent. Local
  tooling rejects animation features its exporter cannot preserve.
- Version: `0.1.0-dev`; original Project Covenant source code is MIT licensed
  (`LICENSE`). The logo is excluded with no reuse permission granted by this
  repository (`ASSET-LICENSE.md`); Minecraft and third-party content retain their
  respective owners' licenses and terms. See `NOTICE` for attribution and upstream notices.
- Minecraft: `26.1.2`, exact version range.
- NeoForge: `26.1.2.126` (matches ATM11 `0.14.0-beta`); ModDevGradle: `2.0.148`;
  Gradle wrapper: `9.2.1`. This pins the loader only; ATM11 integration is unvalidated.
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

- Native profiler scopes identify major runtime work with stable `covenant:`
  labels: skills, auras/statuses, damage/rewards, gathering, equipment, Workbench,
  client movement, HUD, input, visuals and screens. The live F3+1 pie exposes
  client scopes beneath their native callers, including callback guards and
  control checks; bow bookkeeping excludes the wrapped vanilla render body.
  Server work remains in server captures. Scopes close on returns and exceptions and
  use Minecraft's inactive no-op when profiling is off. See the profiling guide
  in `docs/DEVELOPMENT.md` for capture commands and attribution boundaries.
- Skills retain one server activation coordinator, with bow/melee/spell helpers,
  one pending-cast snapshot, and shared control/lifecycle boundaries. Learn/bind/use
  payloads have a transient per-player ingress budget; gameplay validation and
  cooldowns remain independent. Death cleanup follows the final native death-hook
  decision so another mod's revival does not erase active skill state.
- Required network channels negotiate the exact Covenant release version plus
  their wire format revisions before play. NeoForge's mismatch screen reports
  client/server versions; release builds must advance `mod_version` when changed.
- Native arrows now save skill provenance, captured aura bonuses and Enchanting
  hit history independently of cosmetic trails. Existing saved IDs are retained;
  metadata absent from older arrows cannot be recovered. See `docs/RPG_SKILLS.md`.
- `src/main/java`: common mod entry point, with input/screens isolated in
  `dev.covenant.client` behind a physical-client-only mod entry point.
- `src/geometryTest/java`: dependency-free wheel and skill-tree geometry checks,
  run by `radialMenuTest` / `skillTreeLayoutTest` as part of `check` and `build`.
- `src/gameTest`: isolated test mod and lifecycle fixtures, run by
  `scripts/Dev.ps1 test`; excluded from normal client/server runs and the JAR.
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

For the LAN test server, reset the entire world, including player data and
progression, on every update until the user says otherwise. See
`docs/DEVELOPMENT.md` for the shutdown, backup, reset, and validation workflow.

## Validation status

Visual polish Phase 1 now delivers an original low iron/crystal Frost Trap, a
native-size skill-tree icon, and supported same-elevation square/circular range
marks. Essential Frost boundaries survive Minimal particles and use ordinary
depth testing; decoration respects particle settings. Unsupported/covered samples
leave gaps rather than projecting onto lower floors. The device disappears on
trigger; natural patch expiry has a quiet cosmetic event. Gameplay geometry,
timings, ownership and cleanup remain unchanged. See the Visual Polish Roadmap
and `docs/RPG_SKILLS.md`; Minecraft appearance still needs user playtesting.
Client renderer factories run before item components are bound in 26.1: defer
presentation ItemStack creation until an active-world render/state extraction.

Visual polish Phase 2 extends that native model/footprint path through Bear's
toothed jaws/pressure plate, Flame's vented ember cage and Blast's bound charge
with a solid unlit primer, each with a 16x16 skill-tree icon. All four essential
contact outlines survive Minimal; Frost/Flame switch to their real patch radii.
One quiet broken owner-only aura footprint now uses depth-tested ground marks,
preserving self-only exclusions and the aura's ignored-cover behavior. Like trap
marks, it samples support at the owner's elevation instead of projecting onto
other floors. Modest root/freeze/chill cues reuse synced target state. Flame has
a natural-expiry-only extinguish cue. These are presentation changes; gameplay
dimensions, damage, timing, ownership and cleanup contracts stay unchanged.
Minecraft appearance and overlap-scene performance remain unverified.

Phase 3's reference batch now establishes an original pale-core,
lavender/violet Arcane Bolt (palette approved by the user), a steel/crimson
120-degree Cleave crescent and a narrow motion-aligned Piercing Shot streak, with
three original tree icons. Collision and accepted-damage marks are distinct;
the latter requires positive damage including absorption/shields. Five short-lived
native particle types established that reference batch. Phase 3 now extends through
original Fire/Ice cores and cast/trail/collision sequences, bounded jagged Lightning
links, Aimed Shot gathering during its real wind-up, violet Arcane Arrow contact,
restrained five-arrow Multishot and single-target Rend/aggregate bleed-pulse marks.
Ten Phase 3 skill icons join the four trap icons. The user rejected floating-item
bolt cores on 2026-11-10: particles now supply crimson Fire, pale-blue Ice and
violet Arcane bodies, with only narrow travel-aligned geometric filaments for
Minimal visibility. Lightning flickers and sheds electric-blue sparks; elemental
collisions disperse into energy particles. Twenty-three short-lived native
particle types respect decorative settings. The old item art remains independent
of projectile rendering. No new custom payload, saved IDs or gameplay rules.
The existing lightning payload supplies exact endpoints; ornament never invents
chain targets. Cosmetic arrow styles survive the pre-damage entity collision hook
until native removal, and clear immediately on block impact. Accepted bleed stacks
share one quiet cue per target/pulse. Minecraft appearance review remains pending.

Phase 4 shares original icons across tree, bindings and top-left status, plus
stone/iron menu framing, profession emblems/current-next costs, visible class
previews with explicit permanent choice, grouped Stats with an optional precise
breakdown, and radial emblems/discovery hints. The first abstract icon study was
rejected; the recognizable-object redraw awaits user art acceptance. Locked and
planned nodes remain inspectable only. Apply confirms matching synced ranks,
with honest conflict/timeout states and independent profession-tab submissions.
Existing native server action-bar messages replace repeated skill-key/cast/Apply
chat; commands keep native routing. Root/chill tiles read hidden native effects;
freeze reads synced state without inventing an expiry or duplicating native
potion tiles. Resource/XP positions and F1 remain. No new custom payload, saved
IDs, skill bar or gameplay rules. Sources are ignored models/ui_phase4; preview
is ignored .local/phase4/icons-v2.png. Minecraft appearance remains unverified.

Phase 5 replaces the Workbench placeholder with a stone ritual station, the
user's chosen direction, retaining full-block collision and no facing property.
Direct operation buttons and paged native family/rarity/target choices use
token-bound server intent. Structured costs, Potential before/after, possible
roll ranges and rejection reasons are separate from full paged details; the
client does not parse previews or roll outcomes. Workbench protocol is now 4.
Only the acting player's accepted transaction gets a receipt and one quiet sound;
stale/failed requests show no success. Shared storage and native slots remain.
Original respec icons and ten component family marks use existing item IDs.
Component-driven rarity pips and generic unknown-entry fallbacks require no new
saved data. Equipment tooltips group rarity/Potential and compact tier/value lines.
Actual earned character/profession levels and the first-Wither award emit one
owner overlay/sound per award, including multi-level awards; sync/reload does not.
Crafting hints describe enchanting then Basic Affix Crafting. Gathering keeps
native cues; replant supply shortages get one accurate aggregate overlay, without
mislabeling denied placement or failed harvests. Thin XP/resource HUDs are intact.
Sources/previews are ignored `models/ritual_workbench/` and `.local/phase5/`.
Minecraft rendering, usability, audio balance and new art acceptance remain manual.

Phase 6's first movement-polish batch adds green Disengage departure/actual-segment
trail/completion cues and violet open Teleport endpoint brackets. Interruptions
never claim a Disengage arrival; Teleport shows no invented travel path. Native
particles reuse Minecraft glitter sprites, fade within four/six ticks and respect
particle settings. Existing sounds, icons, movement, spending and cooldowns remain.
No new saved data or custom payload. Phase 6's combo batch adds steel/crimson
actual-segment trails, a short Lunging Combo jab, distinct circular Rip and Rush
opening/finisher effects, accepted-hit chips and qualifying bleed-consumption
marks. Lunging Combo's finisher retains the frontal Cleave crescent; its first
stage still deals no damage. Presentation preserves current stage timing,
targeting, native status outcomes and resource rules. Existing icons and sounds
are retained. Reaver signatures now have a Battle Cry snapshot pulse/receipt,
Rage Storm hollow depth-tested sphere bands with release-synced radius and native
entity lifetime, and a quiet Blood Fever bleeding-enemy count label/accent.
Storm geometry survives Minimal; optional decoration is client-local. Accepted
pulse contacts and natural/terrain breakup use existing authoritative boundaries.
Weaver signatures now show Discharge's actual consumed elements/strength with
separate real chains and accepted contacts. Threefold has reserve-proportional
consumption and one natural-expiry cue from an unsaved receipt on the existing
owner refresh; banking, unassignment and lifecycle cleanup stay quiet.
Existing elemental energy/shards are reused; gameplay and icons remain.
All eleven aura activations now use compact family-colored release/recipient
cues. Last-supplier expiry is distinct from silent cleanup; guaranteed Dodge
consumption and Fortitude absorption/depletion have event-driven feedback.
Passive refresh, overlap bookkeeping and shield replacement never claim a
false consumption or end. No new visual state, timers or coverage queries.
Phase 6 implementation is complete;
Minecraft appearance is unverified. See the Visual Polish Roadmap and validation.

The user handles manual client playtesting. Routine agent work ends after
implementation and appropriate automated checks, with a short handoff of relevant
in-game checks. Launching/automating the client, screenshots, and test-world
creation for validation require a user request for client testing; pending manual
verification does not block delivery.

See `docs/VALIDATION.md` for verified results and outstanding manual checks.
Use `docs/DEVELOPMENT.md` for the daily test loop.
