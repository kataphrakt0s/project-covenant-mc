# Farming

## Identity

Farming combines cultivation and animal husbandry. It enhances existing crop and livestock gameplay rather than replacing vanilla mechanics or introducing a competing animal simulation. Farming progression is organized around cultivation, resource preservation, vitality, and compatibility with existing husbandry systems.

## Cultivation

### Scything

| Rank | Minimum Farming level | Point cost | Harvest area |
|---|---:|---:|---:|
| I | 1 | 1 | 3×3 |
| II | 5 | 1 | 5×5 |
| III | 10 | 1 | 9×9 |

- Scything is used with a hoe.
- Sneaking toggles Scything mode; when disabled, normal single-crop harvesting is used.
- When active, harvesting a supported crop processes the square area centered on the targeted crop.
- Every mature supported crop in the area can be harvested, regardless of crop type.
- Immature crops are left untouched.
- Normal drops and appropriate hoe durability costs are preserved.
- Automatic Replanting can operate alongside Scything.
- Modded crops should be supported through data-driven crop definitions.

Exact activation feedback and edge-case handling remain implementation details.

### Automatic Replanting

- **Unlock:** Farming Level 5
- **Cost:** 2 Profession Points
- **Type:** Single-purchase ability

After a supported mature crop is harvested, the system checks the player's inventory for the appropriate planting item. If one is available and the crop can be replanted, one item is consumed and the crop is replanted. If no planting item is available or planting conditions are invalid, the crop remains harvested and normal drops are preserved.

- Replanting uses the player's inventory; it does not intercept harvest drops for seeds.
- Crop-to-planting-item mappings are data-driven so modded crops can participate.

## Free Meal

Free Meal is one specialization with two independent upgrade branches. Both branches can eventually be fully purchased; neither excludes the other.

| Branch rank | Minimum Farming level | Point cost | Effect |
|---|---:|---:|---|
| Cultivation I | 5 | 1 | Chance to preserve Bone Meal when used on crops |
| Cultivation II | 10 | 1 | Improves the preservation chance |
| Cultivation III | 15 | 1 | Further improves the preservation chance |
| Husbandry I | 5 | 1 | Chance to preserve food when feeding animals |
| Husbandry II | 10 | 1 | Improves the preservation chance |
| Husbandry III | 15 | 1 | Further improves the preservation chance |

Exact chances and rank scaling remain to be balanced.

## Vitality

| Rank | Minimum Farming level | Point cost |
|---|---:|---:|
| I | 10 | 1 |
| II | 15 | 1 |
| III | 20 | 1 |

Vitality improves the player's existing hunger- and saturation-based natural health regeneration. Benefits become stronger when the player is well-fed and has saturation available, with the strongest condition at full hunger. It augments vanilla hunger/saturation mechanics rather than adding a separate hunger system or a custom Well-Fed status effect.

Exact regeneration values and the way each rank scales with hunger and saturation remain open for later tuning.

## Husbandry and mod compatibility

Husbandry focuses on recognizing and rewarding deliberate player-initiated animal interactions, not replacing livestock systems from vanilla or other mods.

Qualifying activity examples include:

- Feeding animals.
- Breeding animals.
- Milking livestock.
- Shearing sheep or compatible animals.
- Meaningful animal-care interactions exposed by supported mods.

The following do not award Husbandry XP by default:

- Killing livestock.
- Passive production or collection, such as eggs appearing and being picked up.
- Automated feeding, breeding, or product collection.

Modded animal interactions should be supported through optional compatibility integrations where tags or generic events are insufficient. Covenant should not introduce its own genetics, animal happiness, sickness, or livestock simulation merely to populate this branch.

Husbandry recipe additions remain open. The profession should avoid duplicating features already provided by established farming and animal mods.

## Point budget

| Ability | Defined investment |
|---|---:|
| Scything (3 ranks) | 3 |
| Automatic Replanting | 2 |
| Free Meal (2 branches × 3 ranks) | 6 |
| Vitality (3 ranks) | 3 |
| **Total currently defined** | **14** |

Six Profession Points remain unallocated at the Level 20 cap.

## XP and compatibility

Farming XP is awarded for deliberate, player-performed activities. Passive production, routine passive collection, and automation do not award XP by default. Crop, tool, livestock, and activity compatibility should use tags, data-driven definitions, and optional integrations wherever possible.

Vanilla crops, animals, farmland, seeds, and ordinary farming remain available without Farming investment.

Related: [[Profession Progression]], [[Experience and Reward Systems]], [[Mining]], [[Logging]], [[Crafting Profession]], [[Mod Compatibility]].
