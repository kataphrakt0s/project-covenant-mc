---
project: Project Covenant
type: design-document
version: 0.1
status: foundational
---

# Project Covenant — Foundational GDD

## Vision
A comprehensive, modular action-RPG experience within Minecraft. Covenant adds deep character progression, meaningful itemization, class abilities, dangerous world progression, and repeatable endgame while preserving Minecraft's sandbox identity and supporting modded content.

> Minecraft mods should contribute to the RPG rather than be replaced by it.

See [[Project Vision]] and [[Design Pillars]].

## Core gameplay loop
1. Explore regions and discover enemies, resources, dungeons, and activities.
2. Fight using familiar Minecraft interactions augmented by RPG systems.
3. Earn experience, crafting materials, equipment, and special rewards.
4. Develop the character through levels, attributes, classes, specializations, talents, and gear.
5. Craft and customize equipment.
6. Attempt increasingly difficult content.
7. Repeat with new challenges and build goals.

See [[Core Gameplay Loop]].

## World and progression
The world receives a comprehensive RPG progression overhaul. Regions have distinct identities, recommended difficulty ranges, enemy populations, resources, equipment opportunities, dungeons, and bosses. Exploration remains open-ended: players may discover dangerous content early, but its threat should be legible and its rewards meaningful.

Enemy difficulty reflects region, encounter role, and progression context—not indiscriminate player-level scaling.

See [[World Progression]], [[Regions and Biomes]], [[Enemy Progression]].

## Character progression
- Level cap: **100**.
- Configurable experience curve.
- Progression is persistent and server-authoritative.
- Character development includes attributes, class, specialization, talents, abilities, and equipment.
- Equipment modifiers are derived from currently equipped items, not permanently copied into character progression.

See [[Character Progression]], [[Attributes]], [[Classes and Specializations]], [[Class Resources]].

## Statistics
The implemented foundation uses Health, Damage, Armor, Movement Speed, Attack Speed, and Dodge, with neutral multipliers for future skill costs, cooldowns, cast times, and durations. Native Minecraft attributes combine equipment and other native modifier sources. The current scope has no primary attributes, attribute points, talent system, or conditional modifier framework; broader progression notes remain roadmap material.

See [[Statistics]], [[Stat Calculation Pipeline]].

## Itemization and equipment
Covenant enhances existing Minecraft equipment rather than requiring replacement items. RPG data attaches to item instances through [[Affix Pages]].

Affix Pages belong to individual item stacks and travel with the item through inventories, containers, drops, pickup, trading, player transfers, and save/reload. Unequipping preserves the page; modifiers apply only while equipped.

Future itemization includes rarity tiers, multiple affixes, affix tiers, and exceptional properties. Crafting is the primary equipment acquisition and improvement philosophy. Direct drops remain suitable for unique rewards and special encounters.

See [[Equipment]], [[Affix Pages]], [[Equipment Categories]], [[Affixes and Rarity]], [[Crafting]], [[Loot and Rewards]].

## Combat
Hybrid combat retains recognizable Minecraft attacks, weapons, and blocking while adding class abilities, resources, cooldowns, statistics, status effects, and specialized mechanics. Enemies eventually include ordinary, elite, champion, and boss roles with differentiated behaviors. Modded entities should participate without requiring their source mods to depend on Covenant.

See [[Combat]], [[Abilities]], [[Encounter Design]].

## Endgame
Endgame combines escalating repeatable challenges, structured dungeons and bosses, and open-world progression such as rare encounters, events, exploration, and high-level gathering. Level 100 begins a broader endgame rather than ending character development.

See [[Endgame]], [[Challenge Tiers]], [[Dungeons]], [[Bosses]], [[World Events]].

## Mod compatibility
Covenant is standalone. ATM11 is a compatibility test target, not a mandatory dependency. Support vanilla and modded equipment/entities, optional integrations, and extension points without unnecessarily overriding vanilla combat.

See [[Mod Compatibility]].

## Technical principles
Server-authoritative gameplay; persistent profiles; item-instance RPG data stored with stacks; data-driven definitions where useful; multiplayer/dedicated-server safety; optional integrations; automated tests; separation of character progression, item data, and derived statistics.

See [[Technical Architecture]], [[Player Profiles]], [[Item Data and Persistence]], [[Networking and Authority]], [[Testing Strategy]].

## Development sequence
[[Milestone 1 - RPG Foundation]] → [[Milestone 2 - Character Gameplay]] → [[Milestone 3 - Itemization and Crafting]] → [[Milestone 4 - World Progression]] → [[Milestone 5 - Class Depth and Content]] → [[Milestone 6 - Endgame]].

See [[Development Roadmap]] and [[Implementation Priorities]].
