# Farming

Natural mature-crop/credited-breeding XP, Scything and Farming Automatic
Replanting are implemented. Free Meal and Vitality remain planned. Runtime contracts:
[Profession activities](../docs/PROFESSIONS.md#natural-activity-xp).

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
- Right-click a targeted mature supported crop with a hoe to harvest. When
  Scything is purchased and the player is not sneaking, process the square area
  centered on that crop. Holding sneak limits the right-click harvest to the
  targeted crop; there is no persistent Scything-mode toggle.
- The square lies in the horizontal X/Z plane at the targeted crop's Y level.
  Only that crop layer is processed; terraced fields need a separate action at
  each height, and stacked farm floors are not included.
- This uses the right-click interaction, not left-click breaking. When no
  mature supported crop is targeted, leave ordinary hoe interactions in place.
- Every mature supported crop in the area can be harvested, regardless of crop type.
- Immature crops are left untouched.
- Preserve normal crop drops. Each successfully harvested crop incurs **one
  point of hoe durability use**, with Unbreaking applying normally. This also
  applies to the single-crop right-click harvest while sneaking.
- Stop harvesting when the hoe breaks, leaving remaining crops untouched.
  Already harvested crops retain their drops and can complete their learned
  replanting step. Replanting adds no separate durability charge to that
  crop's harvest.
- If Automatic Replanting is learned, the right-click harvest also attempts to
  replant each harvested crop using planting items from inventory.
- Modded crops should be supported through data-driven crop definitions.

Without purchased Scything, hoe right-click harvests just the targeted crop.
Only main-hand hoes activate it; left-click breaking stays ordinary and does not
replant. The target is processed first, then the remaining square in X/Z order.
The first protected, cancelled, changed or failed harvest stops the action.
Native placement cancellation preserves planting items; invalid soil or missing
items skips that replant while preserving the harvest. Drops stay at their native
per-crop positions. No job or unfinished action is saved.

### Initial supported crops

The first harvest-and-replant set is **wheat, carrots, potatoes, beetroot, and
Nether wart**. Define maturity and planting items through data-driven crop
definitions so modded crops can participate where their behavior fits.

| Crop | Planting item |
|---|---|
| Wheat | Wheat seeds |
| Carrots | Carrot |
| Potatoes | Potato |
| Beetroot | Beetroot seeds |
| Nether wart | Nether wart |

Cocoa, berries, and multi-block crops require additional handling and are
deferred from this initial set. All five initial crops support harvesting and replanting.

### Automatic Replanting

- **Unlock:** Farming Level 5
- **Cost:** 2 Profession Points
- **Type:** Single-purchase ability

During the right-click crop harvest interaction, if this ability is learned,
the system checks the player's inventory for the appropriate planting item
after each supported mature crop is harvested. If one is available and the crop
can be replanted, one item is consumed and the crop is replanted. If no planting
item is available or planting conditions are invalid, the crop remains
harvested and normal drops are preserved. This also applies to the single-crop
harvest while sneaking.

- Replanting uses the player's inventory; it does not intercept harvest drops for seeds.
- Crop-to-planting-item mappings are data-driven so modded crops can participate.

## Free Meal

Free Meal is one specialization with two independent upgrade branches. Both branches can eventually be fully purchased; neither excludes the other.

| Branch rank | Minimum Farming level | Point cost | Effect |
|---|---:|---:|---|
| Cultivation I | 5 | 1 | 20% chance to preserve Bone Meal when used on crops |
| Cultivation II | 10 | 1 | 30% total preservation chance |
| Cultivation III | 15 | 1 | 40% total preservation chance |
| Husbandry I | 5 | 1 | 20% chance to preserve food when feeding animals |
| Husbandry II | 10 | 1 | 30% total preservation chance |
| Husbandry III | 15 | 1 | 40% total preservation chance |

Both branches use configurable **20% / 30% / 40%** prototype preservation
chances. Each rank's chance replaces the previous chance rather than adding to
it. These are planned effects, not implemented item preservation.

Husbandry preservation applies to normal feeding interactions that would
consume food, including breeding preparation, baby growth, and animal healing.
The feeding effect still occurs when the item is preserved. Invalid feeding
attempts provide no preservation roll. Feeding alone remains ineligible for
Farming XP; only successful breeding earns the husbandry XP reward.

## Vitality

| Rank | Minimum Farming level | Point cost | Saturation-loss reduction |
|---|---:|---:|---:|
| I | 10 | 1 | 10% |
| II | 15 | 1 | 20% |
| III | 20 | 1 | 30% |

Vitality **reduces saturation lost to normal exhaustion**, with the reduction
determined by rank. This applies across exhaustion sources, including natural
regeneration, sprinting, and harvesting. Keep native exhaustion thresholds and
regeneration rules, including healing amount and pulse timing.

Saturation remains finite and can reach zero. Once it is depleted, normal
hunger depletion resumes. There is no permanent positive saturation floor or
additional saturation-reserve resource.

These are configurable prototype reductions, with each rank replacing the
previous value. At Rank III, a normal one-point saturation drain becomes 0.7
points, clamped at zero saturation. This is planned behavior, not implemented
saturation handling.

## Husbandry and mod compatibility

Husbandry focuses on recognizing and rewarding deliberate player-initiated animal interactions, not replacing livestock systems from vanilla or other mods.

For the initial slice, the husbandry XP source is **successful breeding**
attributed to player participation. Feeding an animal or putting it into love
mode is not itself an XP payout; failed or cancelled breeding grants none.

Pay one breeding XP reward to **the player vanilla credits as the breeder**,
reusing native attribution. If two players feed different parents, only the
native credited breeder receives the reward; do not split or duplicate it
between the parents' feeders.

The **provisional successful-breeding reward is 100 Farming XP per event**,
shared across supported animals and paid once to the native credited breeder.
It is not a reward per parent or per feeding interaction. One successful event
reaches Level 1 from zero under the shared curve. No purchased ability is
required; these are starting values for later gameplay balance work.

The following do not award Husbandry XP by default:

- Feeding alone, milking, shearing, or other animal-care interactions.
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

For now, Farming XP comes **only from harvesting mature supported crops and
successful player-attributed breeding**. Award crop XP once per mature crop
successfully harvested, including ordinary harvesting and each crop harvested
by Scything. Scything adds no XP multiplier, and drop quantity does not multiply
XP. Ordinary harvesting can earn XP before purchasing Scything.

The **provisional crop reward is 5 XP per mature supported crop harvested**,
shared by the initial wheat, carrot, potato, beetroot, and Nether wart crops.
Ordinary harvesting and Scything use the same per-crop value. Twenty mature
harvests reach Level 1 under the shared 100-XP first-level requirement. Keep
crop reward mappings data-driven for modpack tuning and supported modded crops;
actual leveling-time balance still needs validation.

Successful player-attributed breeding awards **100 XP once per event** to
the native credited breeder, under the Husbandry rules above. Both initial
Farming sources now have provisional reward values; actual pacing still
requires validation and tuning.

Planting, replanting, and Bone Meal use grant no Farming XP. Feeding alone,
milking, shearing, passive production/collection, and automation likewise grant
none in the initial slice. Free Meal can still preserve items on its eligible
uses; that benefit does not make those actions XP sources.

Crop, tool, livestock, and activity compatibility should use tags, data-driven
definitions, and optional integrations wherever possible. These XP rules are
implemented for ordinary mature harvests and native credited breeding. Scything
and the other purchased Farming effects remain planned.

Vanilla crops, animals, farmland, seeds, and ordinary farming remain available without Farming investment.

Related: [[Profession Progression]], [[Experience and Reward Systems]], [[Mining]], [[Logging]], [[Crafting Profession]], [[Mod Compatibility]].
