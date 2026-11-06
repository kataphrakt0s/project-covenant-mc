# Mining Profession

## Identity

Mining develops the player's mastery of prospecting, ore extraction, material processing, and geological interaction. Mining progression should add options to the existing Minecraft mining loop, not gate ordinary mining or resource access.

## Established design directions

### Vein Mining

Vein Mining is one multi-rank ability. It affects **ores only**; it does not apply to ordinary stone, deepslate, logs, or other connected blocks.

- It activates automatically when the player mines an eligible ore without sneaking.
- Sneaking while mining performs normal single-block mining instead.
- Ranks progressively increase the ability's extraction capability (for example, the maximum connected ore blocks affected).
- Modded ores should be able to opt in through tags or equivalent data-driven classification rather than hardcoded block lists.

Exact block limits per rank, search shape, tool/durability/drop handling, enchantment interactions, and edge cases remain to be specified and tested.

### Prospecting

The interaction is established:

- Right-click a suitable stone or deepslate block while holding any valid mining tool.
- Tool eligibility should use tags or data-driven mining-tool classification so modded tools can participate.
- Prospecting should be a deliberate survey action, not continuous detection merely from holding a pickaxe.

The information revealed remains open: possibilities include nearby deposit types/abundance, directional and approximate-distance guidance, or stronger ore detection at advanced ranks. Do not assume X-ray-style block highlighting is approved.

## Additional candidate directions

The following are promising design directions, not finalized ability specifications:

- Automatic smelting or other ore/material processing.
- Other forms of material transfiguration.
- Defensive or resilience benefits that complement combat builds.

Abilities can be passive, conditional, or active. Any combat benefits should be modest and available to all classes.

## Progression integration

Mining starts at Level 0, defaults to a maximum of 20, and earns one Profession Point per level gained. It shares the profession XP curve but maintains independent XP and points. Ability availability is gated by Mining level; abilities are purchased with their authored Profession Point cost. Selected Mining milestones may automatically unlock core recipes.

Mining XP is awarded for meaningful mining/prospecting/extraction actions, with contextual reward values. Repetitive low-value block breaking should not be an efficient progression exploit. Automated mining grants no XP by default; explicit exceptions require attribution and anti-exploit safeguards.

## Cross-profession and vanilla boundaries

Mining materials may support optional Logging, Farming, or Crafting recipes. Ordinary Minecraft mining and tool use are never gated by Mining level. A player who has not invested in Mining can still mine every resource available through normal Minecraft rules.

## Vertical-slice scope

Include the functional profession track and a small playable selection. Vein Mining and Prospecting are leading candidates for initial abilities. Final XP values, point costs, level gates, rank effects, recipe milestones, and the final selection of additional abilities remain open.

Related: [[Profession Progression]], [[Experience and Reward Systems]], [[Dimensional Progression Gates]], [[Crafting Profession]], [[Logging]], [[Farming]].
