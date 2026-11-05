# Project Covenant context

## Game design reference

The foundational [Project Covenant GDD](<Project-Covenant/Project Covenant - GDD.md>)
lives in the **Project-Covenant** Obsidian vault at `Project-Covenant/` in this
repository. Read it before feature work and follow its Obsidian wikilinks to the
relevant system notes. The GDD describes the intended design and roadmap;
the current purpose below and `docs/RPG_DATA.md` describe the implemented scope.

## Current purpose

Develop a Minecraft Java Edition mod for **26.1.2** using **NeoForge**.
ATM11 is the eventual pack target; pack integration is outside the current scope.
The radial menu opens while holding **G** (rebindable under Controls > Key Binds >
Project Covenant) with ten numbered slots. Slot 1 is **Skills**: hover and release G,
or left-click it, to open the non-pausing Skills window. Slot 2 opens Stats with the
same controls; slots 3-10 are placeholders. Stats shows the current class and a
scrollable text list of supported native stats, bases, and active modifiers. Players
without a class can select Stalker, Reaver, or Weaver there through the existing
one-time server command. The page reads synced attributes/profile state; it adds
no saved data or stat calculation system.
Slot 1 starts at the top; slots proceed clockwise. Mouse hover highlights a slot;
release the binding to select or press Escape to cancel. It does not pause the world.
It opens only in a living player's game, outside other screens,
and closes on loss of focus. No custom blocks, items, or recipes exist yet.

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
Classes each have one playable basic skill plus the brainstorm's allocatable tree
placeholders, without passive stat bonuses or global equipment restrictions.
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
builds with time in confirmed combat and depletes after seven seconds without
combat damage; Weaver Mana capacity/recovery scale through native attributes and
Affix Pages. Only the selected class recovers/depletes. Server configuration controls
base rates and Rage/Mana capacities; Focus recovery bonuses cap at 25%. Changed
resource snapshots sync at most four times per second, plus explicit spending/edits.

`/covenant skill` lists definitions; `/covenant skill use <id>` activates Piercing
Shot (bow arrow), Cleave (nearby melee damage), or Arcane Bolt (single-hit magic
projectile). Costs and timers use existing skill multipliers. Server validation
and cast completion own all spending/effects. Cooldowns persist on the saved
overworld game clock; pending casts cancel on logout, death, class/dimension change.
`/covenant resource` inspects balances. Operator-only `resource set <amount>` and
`class switch <id>` support testing; ordinary selection remains one-time.
See `docs/RPG_SKILLS.md` for defaults, APIs, persistence, and test steps.

The Skills window displays only the current class's tree from
`Project-Covenant/Skill Trees Brainstorm.md` (62 total nodes across the three classes).
Left-click allocates/selects for free; right-click removes that node and its learned
dependents, clearing their assignments. The server enforces all 11 prerequisite
links and the Ripping/Fortitude Aura exclusion. Diagram levels are informational;
there are no skill points, level gates, or ranks yet. Planned nodes can be allocated
and bound but have no effects, costs, cooldowns, or passive bonuses. The original
three skills retain their gameplay. The entire current-class tree fits on one page:
compact square nodes resize to the available space, with temporary initials and
full hover/focus tooltips. All eight tiers and dependency/exclusion links stay
visible; empty diagram lanes are compressed while preserving the branch flow.
Right-hand **Tree** / **Keybindings** tabs
separate allocation from the learned-skill picker and eight slot assignments.
Select a learned skill there and click a slot to assign; right-click a slot to clear.
Defaults are **Q, R, F, C**, then **Shift+Q/R/F/C**;
each slot is rebindable in vanilla Controls. Only initial presses activate, with
modified bindings taking priority. `covenant:skill_loadout` persists learned IDs and
slot assignments, copies on death/End return, and syncs only to its owner. Requests
are server-validated; commands and cast completion also require learning. Unlearning
cancels that skill's pending cast without erasing cooldowns. Class switching retains
inactive learning/assignments, but class checks prevent their use. Existing saves
start with no learned skills; resource/cooldown/profile formats are unchanged.
The existing loadout format and stable IDs are retained; no migration is needed.
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
circular Cleave sweep with red hit sparks, and a subtle yellow-white Piercing Shot
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
- Version: `0.1.0-dev`; license remains All Rights Reserved until chosen otherwise.
- Minecraft: `26.1.2`, exact version range.
- NeoForge: `26.1.2.125`; ModDevGradle: `2.0.148`; Gradle wrapper: `9.2.1`.
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
