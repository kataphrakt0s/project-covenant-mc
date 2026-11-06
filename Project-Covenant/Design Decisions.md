# Design Decisions

| Area | Established decision |
|---|---|
| Design philosophy | Minecraft-native RPG; deepen Minecraft activities rather than replace them |
| Character level cap | 60 |
| Character skill points | 1 at level 1, then 1 per level; 60 total by level 60 |
| Traditional attributes | No primary-attribute point system |
| Classes | Stalker / Focus, Reaver / Rage, Weaver / Mana |
| Combat skill allocation | Maximum 8 distinct skills allocated at once |
| Skill ranks | Individual maximum rank of 8 or 12; Rank 1 unlocks |
| Skill level gates | Character level gates only the first point in a skill |
| Combat respec | Crafted item; refunds allocated combat skill points |
| Professions | Mining, Logging, Farming, Crafting; all open to every class |
| Enchanting | A subset/specialization within Crafting, not a fifth profession |
| Profession levels | Start at 0; default maximum 20 |
| Profession points | 1 per level gained; 20 total at profession level 20 |
| Profession abilities | Level-gated, purchased with variable-cost Profession Points; most should eventually be obtainable |
| Profession recipes | Core recipe rewards may unlock automatically at selected profession milestones |
| Profession respec | Crafted item, more expensive than combat respec; resets one profession while preserving its level and XP |
| XP separation | Covenant character/profession XP is separate from vanilla XP orbs |
| Character XP | Primarily combat, with supplemental significant world/encounter milestones |
| Profession XP | Contextual, activity-specific, meaningful player actions by default |
| Automation XP | Disabled by default; configured exceptions require attribution and anti-exploit safeguards |
| Dimension gates | Data-driven exact IDs and dimension tags, priority, configurable fallback |
| Unmatched dimension fallback | Character 60; each profession 20 |
| XP at a gate | Excess XP is discarded, not banked; existing levels never decrease |
| Cross-profession dependencies | Optional synergies; explicit recipe requirements are allowed |
| Equipment | Existing vanilla/modded items participate |
| Affix Pages | Per-item-stack data that travels with the item; modifiers apply while equipped |
| Crafting | Vanilla crafting remains intact; Covenant workstations planned for advanced affix crafting/salvaging |
| Compatibility | Standalone; ATM11 is a compatibility target, not a dependency |

Examples of dimension caps and profession abilities discussed during design are illustrative, not final balance values. See [[Progression Systems]], [[Open Questions]], and [[Project Covenant - GDD]].
