# Loot and Rewards

Rewards support character progression, profession mastery, and crafting agency. Materials/components are central; direct equipment drops remain appropriate for distinctive rewards, unique items, and special encounters. Tie rewards to dimensions, regions, enemy roles, dungeons, bosses, professions, and endgame challenges. Avoid excessive currencies and grind.

Character XP is primarily combat-earned, with occasional rewards for significant discoveries and encounter milestones. Profession XP is awarded separately for meaningful activity-specific actions. Vanilla XP orbs do not contribute to either Covenant progression track.

## Affixed equipment prototype (planned)

For the prototype, **100% of eligible equipment generated in mob drops and
chest loot receives Covenant affixes**. This supplies the initial non-Common
affixes and family components recovered through salvaging. Apply this to
existing vanilla and opted-in modded equipment using the planned Covenant
rarity system.

Also **increase the amount of equipment that appears**, through additional
equipment loot rolls. Bonus equipment uses the same 100% prototype affix rate.
Make the affix rate and additional-equipment rules configurable, including the
ability to disable bonus equipment. The intended first pass favors finding
enough items to exercise the Crafting/Salvaging loop, with later tuning toward
vanilla balance.

The provisional mob bonus is a **25% chance to add one equipment piece per
player-killed hostile mob**. Roll once per qualifying mob death, in addition
to ordinary loot, with at most one bonus piece from this rule. The bonus piece
receives Covenant affixes under the 100% prototype rate. Keep the chance and
quantity configurable.

The provisional vanilla chest bonus is **one guaranteed additional affixed
equipment piece when the chest's loot is first generated**. Apply it once per
loot generation, not on later reopenings. Keep this bonus configurable and
disableable, alongside the mob bonus.

New affixed loot uses **equal prototype rarity weights: 20% each** for Common,
Uncommon, Rare, Epic, and Legendary. Apply these configurable weights to both
ordinary equipment loot and bonus equipment. This intentionally makes every
rarity accessible for testing; revisit the weights for normal progression.

Each generated piece starts with **exactly one random affix matching its rolled
Covenant rarity**, leaving the remaining prefix/suffix slots available for
crafting. This is a provisional first-pass loot setup, not a permanent limit
on generated affix counts.

For now, roll that affix's numerical tier **uniformly from Tier 1 through the
equipment rarity's ceiling**, inclusive: Common 1, Uncommon 1-2, Rare 1-3,
Epic 1-4, and Legendary 1-5. This loot roll is independent of the recipient's
Crafting level, so found gear may exceed what they can currently craft.
This tier distribution is provisional prototype tuning.

Freshly generated affixed equipment receives the **full starting Crafting
Potential budget for its rarity**, without charging for the natural starting
affix or its generated numerical tier. Later Workbench operations spend
Potential normally. This initializes fresh loot once; later drops or ownership
changes do not refill an already initialized item's remaining Potential.

The configurable bonus-equipment pool initially contains **iron sword, iron
axe, iron pickaxe, iron shovel, iron hoe, iron helmet, iron chestplate, iron
leggings, iron boots, bow, crossbow, and shield**. This twelve-item pool applies
to additional equipment rolls; existing eligible mob/chest loot retains its
base item when affixed. Pack authors can change the pool through data.

Remaining source conditions and detailed pool weighting remain under design.
These are planned prototype defaults, not implemented loot generation or final
balance.

See [[Affixes and Rarity]], [[Affix System and Equipment Customization]], and
[[Salvaging]].

Related: [[Crafting]], [[Enemy Progression]], [[Dungeons]], [[Endgame]], [[Experience and Reward Systems]], [[Profession Progression]].
