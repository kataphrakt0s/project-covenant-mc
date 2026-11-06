# Salvaging

## Purpose

Salvaging is a Crafting profession specialization for dismantling equipment into a chance-based return of generic materials and affix-family components. It supports a player-driven equipment economy without guaranteeing recovery of invested value.

## Access and workstation

- **Profession:** [[Crafting Profession]]
- **Unlock:** Crafting Level 5
- **Cost:** 2 Profession Points
- **Workstation:** Physical Covenant Workbench

The initial Covenant Workbench contains separate Affix Crafting and Salvaging interfaces. Keep their underlying operations and validation separate so they can become individual workstations later if needed.

## Eligible equipment

Covenant Salvaging is intended to support:

- Vanilla equipment.
- Modded equipment.
- Equipment carrying Covenant affixes.

Eligibility should be configurable using item identifiers, tags, and optional mod integrations. Reasonable default equipment categories should be provided rather than treating every arbitrary item as salvageable.

Apotheosis and other mods retain their own salvaging systems. Covenant Salvaging is an additional system and must not override or interfere with those mechanics.

## Salvage outcome rules

- The input equipment is always consumed when a salvage operation is performed.
- Every material return is chance-based; a salvage can return nothing.
- Generic crafting materials may be returned according to the item's properties, such as equipment category, rarity, and power.
- Affix-family components may be returned from equipment carrying eligible Covenant rare affixes.
- Each rare affix on an item receives an independent component-recovery roll.
- Salvaging never returns intact Affix Pages. Affix Pages are not standalone items and the old terminology is retired.
- Salvaging does not guarantee recovery of the item's crafting inputs or value.

The exact tables, probabilities, yields, and influence of item properties remain to be tuned.

## Specializations

Each specialization has three independently purchasable ranks. All three branches can eventually be mastered; none excludes another.

| Specialization | Rank I | Rank II | Rank III | Cost per rank |
|---|---:|---:|---:|---:|
| Recovery | Level 5 | Level 10 | Level 15 | 1 Profession Point |
| Refinement | Level 5 | Level 10 | Level 15 | 1 Profession Point |
| Focused Recovery | Level 5 | Level 10 | Level 15 | 1 Profession Point |

### Recovery

Improves the chance of receiving materials from a salvage operation.

### Refinement

Improves the chance of receiving higher-quality generic crafting materials.

### Focused Recovery

Improves the chance of recovering components associated with particular Covenant affix families.

Exact rank values and how the specializations interact with individual salvage rolls are deferred for balancing.

## Component economy

Salvaging can return two broad types of materials:

1. **Generic crafting materials**, determined by item properties and usable across general Covenant crafting recipes.
2. **Affix-family components**, associated with particular affixes or affix families and used when crafting/applying those rare affixes.

Rare affix components are consumable, not permanent unlocks. Applying a rare affix consumes its required component. Generic materials are also required as specified by the relevant recipe.

The equipment itself is destroyed; its Covenant affixes are not returned as intact items or pages.

## Multiplayer and economy considerations

Salvaging should be a server-authoritative workstation operation. The server validates the input, eligibility, player access, and required state; consumes the item exactly once; performs recovery rolls; and grants outputs. Repeated or duplicated requests must not duplicate returns.

The chance-based model is intentional: failed crafting and salvaging must not become a route to duplicate equipment or guarantee recovery of invested materials.

Related: [[Crafting Profession]], [[Affix System and Equipment Customization]], [[Profession Progression]], [[Mod Compatibility]].
