# Design Decisions

| Area | Established decision |
|---|---|
| Design philosophy | Minecraft-native RPG; deepen Minecraft activities rather than replace them |
| Character level cap | 60 |
| Character skill points | 1 at level 1, then 1 per level; 60 total by level 60 |
| Traditional attributes | No primary-attribute point system |
| Classes | Stalker / Focus, Reaver / Rage, Weaver / Mana |
| Combat skill allocation | Maximum 8 distinct skills allocated at once |
| Skill ranks | Rank 1 unlocks; current combos cap at 12, other currently playable skills at 8 |
| Skill investment | Left/right-click edits pending upgrades; bottom-right Apply commits out of combat; closing discards unapplied edits |
| Prototype rank effects | Arrow shots gain +2% of Rank 1 velocity per extra rank, up to +14%, with native damage; aura passive strengths stay fixed |
| Skill level gates | Character level gates only the first point in a skill |
| Combat respec | Four paper plus one lapis, shapeless; hold-use two seconds out of combat to refund combat allocations |
| Shared combat state | Seven-second timer for all classes; damage/support refresh it; Rage grace, Apply, and respec use it |
| Professions | Mining, Logging, Farming, Crafting; all open to every class |
| Enchanting | A subset/specialization within Crafting, not a fifth profession |
| Profession levels | Start at 0; default maximum 20 |
| Profession points | 1 per level gained; 20 total at profession level 20 |
| Profession XP curve | Prototype: 100 × target level XP per transition; 21,000 total from 0 to 20, using the same curve independently per track; activity rewards and pacing remain provisional |
| Profession abilities | Level-gated, purchased with variable-cost Profession Points; most should eventually be obtainable |
| Profession recipes | Future core recipe rewards may unlock automatically; additional profession-level recipe milestones are deferred beyond the slice; Workbench and respec recipes remain ungated |
| Profession respec | Prototype: four paper, four lapis, one emerald, shapeless; no profession crafting gate; hold-use two seconds out of combat to reset all four professions' allocations together, preserving XP/levels; consume one item only on success |
| XP separation | Covenant character/profession XP is separate from vanilla XP orbs |
| Character XP | Primarily combat, with supplemental significant world/encounter milestones |
| Character XP sharing | Eligible damage/support contributors split equally, each share rounded up; refreshing 30-second window, online at payout |
| Prototype XP pacing | Five ordinary solo kills for Level 2; 3,000 Endermen or 30 repeat Withers for 59-to-60 where caps allow |
| Profession XP | Contextual, activity-specific, meaningful player actions by default |
| Enchanting Crafting XP | Prototype: 20/50/100 XP once per successful first/second/third enchanting-table option, regardless of displayed requirement; native activity integration implemented |
| Affix-operation Crafting XP | Prototype: 20/40/60/80/100 XP for the affected affix's Common/Uncommon/Rare/Epic/Legendary rarity; shared by Add/Upgrade/Reroll/Remove at every numerical tier; planned activity integration |
| Salvage Crafting XP | Prototype: 10/20/30/40/50 XP for Common/Uncommon/Rare/Epic/Legendary equipment with at least one Covenant affix at consumption; unaffixed gear gives zero XP even if rarity was previously initialized; normal eligible material returns remain available; planned activity integration |
| Profession automation XP | Disabled by default; configured exceptions require attribution and anti-exploit safeguards |
| Spawner combat XP | Eligible spawner mobs use normal rewards and contribution/cap rules |
| Dimension gates | Data-driven exact IDs and dimension tags, priority, configurable fallback |
| Unmatched dimension fallback | Character 60; each profession 20 |
| Initial character dimension caps | Overworld 20, Nether 40, End 60 |
| First Wither milestone | Each eligible first-time contributor gets one full level past dimension gates, preserving percentage progress; replaces normal share; global cap applies |
| Balance definitions | Character XP requirements, enemy rewards, and dimension rules use server datapack JSON |
| Balance changes | Preserve earned levels, points, allocations, and raw XP; future requirements can change the displayed percentage |
| Character XP presentation | Thin unlabeled bar below vanilla XP, within the existing HUD footprint; details on Stats |
| Prototype save compatibility | Not required; validate the new progression format on fresh saves |
| XP at a gate | Excess XP is discarded, not banked; existing levels never decrease |
| Cross-profession dependencies | Optional synergies; explicit recipe requirements are allowed |
| Equipment | Existing vanilla/modded items participate |
| Affix Pages | Per-item-stack data that travels with the item; modifiers apply while equipped |
| Crafting | Vanilla crafting remains intact; Covenant workstations planned for advanced affix crafting/salvaging |
| Compatibility | Standalone; ATM11 is a compatibility target; full Apotheosis compatibility is an eventual target |

These are planned design contracts, not a declaration of implemented gameplay.
The character defaults above are agreed prototype tuning; profession content
and further rank tables remain separate pending design work. See
[[Progression Systems]], [[Open Questions]], [[Project Covenant - GDD]], and
`CONTEXT.md` for implemented scope.
