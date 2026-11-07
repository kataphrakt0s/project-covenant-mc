# Milestone 3 — Itemization and Crafting

Affix definitions/generation; rarity and tiers; crafting materials/recipes; affix manipulation and upgrades; salvaging; initial Crafting profession content; tooltips and inventory presentation. Vanilla crafting and enchanting remain fully functional; Covenant adds optional advanced customization through dedicated content and, eventually, specialized workstations.

## First implemented slice

The first prototype implements the complete **find affixed equipment -> salvage
components -> customize equipment** loop for [[Progression Vertical Slice]], with all four
Workbench operations (Add, Upgrade, Reroll, Remove), initial affixed-loot
acquisition, salvaging, and their Crafting profession access and XP. Follow the
settled costs, initial content, rarity/tier/Potential rules, and shared persistent
station storage in the linked system notes. See [Itemization](../docs/ITEMIZATION.md)
for the implemented data format, authority and remaining manual validation.

The Covenant Workbench initially uses Minecraft's **netherite block texture**
as placeholder art, with the already agreed ungated station recipe.

Related: [[Affix Pages]], [[Affixes and Rarity]], [[Crafting]], [[Crafting Profession]], [[Loot and Rewards]].
