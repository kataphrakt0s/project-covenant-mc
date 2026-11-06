# Experience and Reward Systems

Covenant has progression XP systems that are separate from Minecraft's vanilla XP-orb/enchanting-level system. Vanilla XP is neither consumed nor converted by Covenant.

## Independent tracks

- **Character XP** advances the shared character level from 1 to a maximum of 60. It is primarily earned through combat.
- **Profession XP** advances one independent profession at a time: Mining, Logging, Farming, or Crafting. Each profession starts at level 0 and defaults to a maximum of 20.
- Each profession uses the same XP curve, but has an independent XP balance and level. Character XP and profession XP never convert into one another.

See [[Character Progression]] and [[Profession Progression]].

## Character XP sources

Combat is the reliable, primary source of character XP. XP should reflect enemy/encounter significance and threat rather than simply rewarding the number of hits or the final blow. Significant accomplishments can supplement combat XP, including:

- Boss victories and major encounter completions.
- Dungeon completion.
- Meaningful discoveries or exploration milestones.
- Other explicitly authored world/encounter achievements.

These milestone awards should be occasional and meaningful, not repeatable substitutes for combat progression or easy farming exploits.

Vanilla XP orbs do not award character XP. Non-combat everyday actions do not automatically award character XP; their progression belongs to their relevant profession track where applicable.

## Multiplayer combat XP

XP from an enemy/encounter is split among contributing players rather than duplicated in full for everyone or given only to the final attacker. Exact contributor eligibility, support credit, participation window, and split formula remain to be tuned. The implementation must avoid kill-stealing incentives and must not reward players solely for being nearby without meaningful participation.

## Profession XP sources

Profession XP is event-driven, contextual, and tied to the relevant Minecraft activity. Rewards should recognize meaningful participation and significance, not grant a fixed efficient reward for every repeated low-value action.

Examples of intended source categories:

- Mining: meaningful ore extraction, prospecting, and designated mining interactions.
- Logging: meaningful tree felling, forestry, and wood-processing interactions.
- Farming: mature crop harvests and designated agricultural/livestock activities.
- Crafting: meaningful enchanting, designated advanced recipes, Covenant workstation interactions, affix crafting/modification, and salvaging.

Ordinary vanilla crafting does **not** award Crafting XP. Vanilla enchanting and other meaningful actions may award configurable Crafting XP, but exact eligibility and values remain to be designed.

## Automation and modded content

Direct player participation is the default requirement for profession XP. Automated production grants no XP by default. A server owner may explicitly configure exceptions for particular machines, recipes, or activity sources. Such exceptions need reliable attribution to a player or machine owner and safeguards so repeatedly collecting the same output cannot generate unlimited XP.

Modded blocks, items, tools, entities, recipes, activities, and machines should participate through tags, identifiers, data definitions, or optional integrations rather than requiring a hardcoded dependency list.

## Anti-exploit and reward principles

- Reward meaningful activity, not repetitive trivial actions.
- Prevent duplicate event processing and repeated output collection from generating duplicate XP.
- Verify the action and eligible source on the server.
- Respect dimensional progression gates before awarding XP.
- Do not bank XP earned beyond a dimension's gate.
- Keep reward values configurable and validate malformed/unknown entries safely.

Exact XP curves, action reward values, diminishing/repeat rules, and contributor math remain open. See [[Open Questions]] and [[Progression Balance Configuration]].

Related: [[Character Progression]], [[Profession Progression]], [[Dimensional Progression Gates]], [[Mining]], [[Logging]], [[Farming]], [[Crafting Profession]], [[Mod Compatibility]].
