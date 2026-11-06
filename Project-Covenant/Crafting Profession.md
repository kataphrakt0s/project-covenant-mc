# Crafting Profession

## Identity

Crafting is Covenant's profession for equipment customization and material recovery. Its two dedicated progression branches are:

1. [[Affix System and Equipment Customization]] — creating and modifying Covenant affixes on equipment.
2. [[Salvaging]] — dismantling equipment for chance-based material and affix-component returns.

Enchanting is **not** a dedicated Covenant specialization branch for the current design. It remains a supported Crafting XP activity through vanilla gameplay and optional mod integrations. Covenant does not add new enchantments or replace existing enchanting systems.

## Affix Crafting progression

### Basic Affix Crafting

- **Unlock:** Crafting Level 1
- **Cost:** 1 Profession Point

Purchasing Basic Affix Crafting enables the Covenant Workbench's affix customization operations, subject to the player's Crafting level, item eligibility, materials, affix capacity, affix limits, and the item's remaining Crafting Potential.

Higher affix crafting tiers become accessible at the following Crafting levels:

| Crafting level | Access |
|---:|---|
| 1 | Basic Affix Crafting (purchased ability) |
| 5 | First advanced affix tier |
| 10 | Higher affix tier |
| 15 | Advanced affix tier |
| 20 | Highest affix tier |

These are access milestones; additional point costs for each tier have not been established. Exact tier definitions, eligible affix pools, and material recipes remain open.

See [[Affix System and Equipment Customization]].

## Salvaging progression

### Salvaging

- **Unlock:** Crafting Level 5
- **Cost:** 2 Profession Points
- **Workstation:** Covenant Workbench

Salvaging consumes eligible equipment and makes chance-based rolls for returned materials. It supports vanilla equipment, modded equipment, and equipment carrying Covenant affixes. Eligibility and recovery rules should be configurable, with optional mod integrations where required.

The three specializations are independently upgradeable:

| Specialization | Rank I | Rank II | Rank III | Cost per rank |
|---|---:|---:|---:|---:|
| Recovery | Level 5 | Level 10 | Level 15 | 1 point |
| Refinement | Level 5 | Level 10 | Level 15 | 1 point |
| Focused Recovery | Level 5 | Level 10 | Level 15 | 1 point |

- **Recovery:** Improves the chance of receiving salvage materials.
- **Refinement:** Improves the chance of receiving higher-quality generic crafting materials.
- **Focused Recovery:** Improves the chance of recovering components associated with particular affix families.

All returns remain chance-based. An unsuccessful salvage may return nothing, and the input item is always consumed. When an item has multiple rare affixes, each affix has an independent component-recovery roll.

See [[Salvaging]].

## Enchanting and optional mod integrations

Enchanting activities can contribute Crafting XP when they meet configured criteria, but Covenant does not introduce new enchantments or a dedicated Enchanting skill tree. Vanilla and modded enchanting systems retain ownership of their mechanics.

Apotheosis is a first-class optional compatibility target, not a mandatory dependency. Covenant should coexist with Apotheosis's systems—including its enchanting, affixes, gems, sockets, reforging, salvaging, and spawner mechanics—without duplicating, overriding, or invalidating them. Apotheosis activities such as supported enchanting, reforging, or salvaging interactions may award Crafting XP through optional integration.

Covenant must remain fully functional without Apotheosis. Optional integrations should be modular and should not assume every Apotheosis module is installed. The same compatibility-first principle applies to other enchanting, equipment, and content mods.

## Covenant Workbench

The initial design uses one physical **Covenant Workbench** with separate interfaces for:

- Affix Crafting.
- Salvaging.

Keep the underlying operations, validation, inputs, and outputs separate so they can move into distinct physical workstations later without redesigning the systems. Enchanting improvements, if any are identified in the future, should integrate with existing enchanting stations rather than require a Covenant enchanting station.

## Point budget

| Ability | Defined investment |
|---|---:|
| Basic Affix Crafting | 1 |
| Salvaging unlock | 2 |
| Recovery, Refinement, Focused Recovery (3 ranks each) | 9 |
| **Total currently defined** | **12** |

Eight Profession Points remain unallocated at the Level 20 cap. The Crafting-level affix access milestones do not currently have additional point costs assigned.

## XP and vanilla boundaries

Ordinary vanilla crafting does not award Crafting XP. Meaningful enchanting interactions, completed Covenant Workbench operations, Affix Crafting, Salvaging, and supported activities from optional integrations can be configured XP sources. Rewards should be contextual and should not create trivial repeatable crafting loops. Automated production does not award XP by default.

Vanilla crafting, enchanting, equipment use, and ordinary stations remain available at Crafting Level 0.

Related: [[Profession Progression]], [[Experience and Reward Systems]], [[Affix System and Equipment Customization]], [[Salvaging]], [[Crafting]], [[Mod Compatibility]], [[Mining]], [[Logging]], [[Farming]].
