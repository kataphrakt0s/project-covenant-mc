# Project Covenant context

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

All four independent, classless profession tracks initialize on login at level/XP
zero. The first expanded foundation adds paid-cost allocations, a server-datapack
catalogue, owner-synced four-tab Professions menu, atomic per-tab Apply, and a
Profession Respec item. Held use for two seconds out of combat refunds all four
professions together while preserving XP/levels. Logging's Offensive Mastery is
the first usable ability; the other sixteen authored nodes remain visibly planned
and unavailable. Natural Mining, Logging, Farming and enchanting-table Crafting
XP now use confirmed server outcomes, the provisional shared curve and reward
table, persistent chunk-owned placed-ore exclusions, and bounded tree recognition.
Gathering abilities and Workbench/itemization gameplay remain deferred.
Additional profession-level recipe milestones are outside the
progression slice; Workbench and respec recipes remain ungated. See
[PROFESSIONS.md](docs/PROFESSIONS.md) and
[Profession Progression](<Project-Covenant/Profession Progression.md>).
The planned itemization includes Covenant-owned equipment rarity with server
datapack definitions and
item/tag-based opt-in rules for modpack developers; see
[Affixes and Rarity](<Project-Covenant/Affixes and Rarity.md>).
Use fresh saves: prototype profile/loadout migration is deliberately unsupported.

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
tags for compatibility. No acquisition, random rolling,
or accessory-mod integration exists yet.
See `docs/RPG_DATA.md` for APIs, format contracts, and copy/split semantics.

The initial class identities are **Stalker** (bow/ranged precision and distance),
**Reaver** (dedicated melee), and **Weaver** (elemental offensive magic).
`/covenant class` lists descriptions and the current choice; any player can choose
once with `/covenant class select covenant:stalker` (or `reaver` / `weaver` in that
namespace). Selection initializes the existing profile and class resource balances.
Its saved class ID retains the profile's persistence and owner sync guarantees.
Classes each have a playable basic skill, and Stalker also has Aimed Shot and all
four traps (Bear, Frost, Flame, Blast), plus Disengage, Arcane Arrow and Multishot
at Rank 1. Disengage makes a four-block backward hop over six ticks, works
airborne and breaks falls while keeping stationary attacks usable. Arcane Arrow
replaces its native arrow impact with four magic damage and applies four seconds
of +15% damage taken after health/absorption damage; Shock adds to +25%.
Multishot uses one ammo item for five native arrows over thirty degrees, with
one attempt per target per fan even when blocked/dodged. Shared movement handles
prediction and lifecycle cleanup; saved native arrow payloads and target fan
claims preserve effects across save/load. Existing Stalker tuning and
server-datapack skill configuration remain separate work. See `docs/RPG_SKILLS.md`
for detailed lifetimes and boundaries. Weaver also has Fire Bolt, Ice Bolt, and
Lightning Bolt. Fire ignites through native burning; Ice applies one saved/synced
30% Chilled slow; Lightning is an instant aimed hit after its cast, with no status.
All three prototype bolts cost 20 Mana, cast in 0.5 seconds, have independent
two-second cooldowns, and deal six damage. Fire's four-second burn and Ice's
three-second chill scale with skill duration. Lightning stops at the first eligible
enemy or solid block within 24 blocks. The
Reaver also has Rend and a shared bleed foundation. The remaining brainstorm nodes
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

Operator-only `/covenant affix inspect|apply|remove|clear` commands test the existing
main-hand stack component. Apply uses a loaded compatibility-tag category and one
flat supported-stat modifier; existing pages must be removed first. Remove/clear
are aliases that erase the entire page while preserving all other item components,
including its current stack limit. Attachment does not record the previous stack
limit, so it cannot be reconstructed safely. No new saved fields are introduced.

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
Runtime, gravity suppression, and owner-only HUD rows are never saved or copied.
Saved cooldown/resource/loadout formats are unchanged. Movement feel, latency,
and HUD layout remain for manual playtesting. See `docs/COMBOS_IMPLEMENTATION_PLAN.md`.

The Skills window displays only the current class's tree from
`Project-Covenant/Skill Trees Brainstorm.md` (62 total nodes across the three classes).
Left/right clicks draft upgrades; Apply commits point purchases out of combat.
Committed ranks require a respec item to refund. The server enforces all 11
prerequisite links, unlock levels and the Ripping/Fortitude Aura exclusion.
Unimplemented nodes remain visible and unavailable for points or bindings. Twenty-eight
skills have gameplay, including all eleven auras. Assigned auras grant free
passives; instant activations commit a resource cost and cooldown. Source-aware
transient grants handle allies/pets, overlap, expiry, and loadout/lifecycle cleanup.
Fortitude is a separate six-point shield with an owner HUD row; ordinary Dodge
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
hand swings and bounded particle effects: blue-white Arcane Bolt charge/trail/impact,
forward Cleave sweep with red hit sparks, and a subtle yellow-white Piercing Shot
trail/impact. A transient, tracking-synced arrow marker owns only the shot's visuals.
Piercing Shot uses a six-tick cosmetic vanilla bow charge in first/third person,
with a transient synced deadline and client render hooks rather than actual item use.
Saved resources/loadouts and gameplay rules are unchanged. Rendered HUD/effect
behavior remains subject to user playtesting.

## Identity and toolchain

- Working title: Project Covenant; mod ID: `covenant`.
- Developer: `kataphrakt0s`.
- Mod logo: `src/main/resources/mod-icon.png`, referenced by `logoFile` in mod metadata.
- Java package: `dev.covenant`; entry point: `Covenant.java`.
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

- Skills retain one server activation coordinator, with bow/melee/spell helpers,
  one pending-cast snapshot, and shared control/lifecycle boundaries. Learn/bind/use
  payloads have a transient per-player ingress budget; gameplay validation and
  cooldowns remain independent. Death cleanup follows the final native death-hook
  decision so another mod's revival does not erase active skill state.
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

## Validation status

The user handles manual client playtesting. Routine agent work ends after
implementation and appropriate automated checks, with a short handoff of relevant
in-game checks. Launching/automating the client, screenshots, and test-world
creation for validation require a user request for client testing; pending manual
verification does not block delivery.

See `docs/VALIDATION.md` for verified results and outstanding manual checks.
Use `docs/DEVELOPMENT.md` for the daily test loop.
