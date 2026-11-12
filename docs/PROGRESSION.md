# Progression implementation

The combat portion of the [agreed slice](<../Project-Covenant/Progression Vertical Slice.md>)
is playable with the existing skills. The [profession foundation](PROFESSIONS.md)
adds its menu, paid allocations, respec and Logging's Offensive Mastery. Other
profession effects and non-arrow upgrades beyond Rank 1 remain deferred.
Use a fresh world/player save; this changes the
prototype profile and skill-loadout formats without migration.

## Player progression

Class selection starts level 1, 0 Covenant XP and one point. Classless combat
earns nothing and queues nothing. Character levels award one point each, up to
60 by default. Available points are derived from earned level minus committed
ranks, never maintained as a second balance. Covenant XP is separate from orbs.

The seven profession tracks (`mining`, `logging`, `farming`, `crafting`,
`building`, `fishing`, `exploration`) start at
0/0 on login, without requiring a class. Their default cap is 20. Each level
earns one point; Exploration gains levels directly from discovery milestones
and accepts no XP. Availability subtracts the historical costs of committed
allocations. The shipped provisional curve is 100 times the target level per
transition (21,000 total from Level 0 to 20). Natural ore/log/crop harvests,
credited breeding and enchanting-table completion award XP through
`Progression.awardProfession`; operator commands support manual testing. See
[PROFESSIONS.md](PROFESSIONS.md#natural-activity-xp) for eligibility and mappings.

`covenant:character_profile` saves level, raw XP and `wither_claimed` together.
`covenant:profession_progression` saves the independent tracks and paid allocations;
`covenant:skill_loadout` saves a `ranks` map and slot `bindings`.
All three use immutable snapshots, owner sync and NeoForge death/End copying.
`covenant:progression_view` is unsaved owner presentation data, refreshed through
the existing player coordinator. It contains current required XP, caps and
combat status. `covenant:combat_progression` is an unsaved enemy ledger and weak
player contact references, with no sync or death copy.

## Combat XP

Any positive landed damage qualifies once per player, including native
absorption and Covenant Fortitude absorption. Misses, invulnerable, dodged and
fully blocked hits do not qualify as damage. Beneficial aura suppliers, consumed
Fortitude shields, successful control/debuffs and offensive aura contributions
qualify as support. Every valid overlapping aura supplier receives credit even
when the effect applies once. Native bow/crossbow and skill projectiles snapshot
support suppliers at launch; saved native projectile metadata retains those IDs.

Beneficial aura coverage can refresh support on an ally's recent direct combat
contacts. Those references use the shared seven-second combat window; support
never creates a recursive chain of ally contacts. Direct contribution timestamps
live on each enemy for 30 seconds, refreshed by actions and checked at payout.
There are no global enemy scans or per-tick contribution expiry jobs. Unloading,
removal or process restart clears unawarded participation; awarded XP persists.

Accepted native deaths pay online, class-selected contributors equally, rounding
each share upward before caps (10 XP / 3 players gives 4 each). Capped players
still count; blocked shares disappear. Recipient death/respawn and travel do not
erase enemy-owned credit. Offline players receive no queued payout. The enemy's
death dimension supplies the cap regardless of the recipient's current location.
Cancelled deaths pay nothing. The dragon's native final death removal has a
separate payout boundary because its animation bypasses `LivingEntity.die`.

Each eligible player's first Wither grants exactly one level, bypassing only the
dimension gate. It preserves the within-level fraction, rounding XP down, replaces
that recipient's ordinary share and claims the milestone in the same profile
snapshot. Classless/global-capped players do not consume the claim. Repeat Withers
use ordinary rewards/caps. Hardcore remains vanilla.

All classes share one unsaved 140-tick combat deadline for positive combat damage
and qualifying support. Rage decay, Apply and respec read it. Support grants no
Rage; existing native-attack and incoming-damage Rage coefficients remain.
Qualifying support refreshes this timer during PvP too, even though player kills
award no character XP.

## Skills, respec and UI

Left click drafts one rank; right click removes pending ranks only. Removing a
pending prerequisite also removes its pending dependents. Changing tabs/resizing
retains the draft. Closing discards it. Apply sends expected and desired rank
snapshots; the server validates the whole batch before one mutation. Stale,
oversized, unaffordable, wrong-class, level-locked, prerequisite/exclusion-invalid
and in-combat requests spend nothing. At most eight distinct skills may hold
points. Only committed skills can be bound or used. Unimplemented nodes remain
visible and unavailable, including branches behind unimplemented prerequisites.

Piercing Shot and Aimed Shot allow ranks 1–8. Each additional rank adds 2% of
Rank 1 launch velocity (3 blocks/tick), reaching 3.42 at Rank 8. Native velocity
damage remains; no damage coefficient, cost, cooldown or cast-time scaling is
added. Aimed Shot retains its 20% wind-up movement. All other playable skills
allow Rank 1 until upgrades are authored. Planned caps stay 12 for both combos
and 8 for other playable skills. Aura magnitudes retain their baseline values.

Craft `covenant:combat_respec` with four paper plus one lapis lazuli, shapeless.
Hold right click for 40 server ticks out of combat. Early release or combat
interrupts without consumption; successful completion consumes one, refunds all
committed ranks and clears every binding. Existing lifecycle cleanup cancels
owned skill activity; resource balances and committed cooldowns remain.

The HUD draws a one-pixel, unlabeled purple bar immediately below native XP,
above the selected hotbar border, without reserving another HUD row. F1 hides it.
Stats shows level, exact raw/required XP, percentage, caps and unspent points.
Percentages can exceed 100% after a cheaper balance reload: raw XP is preserved
until a real subsequent award processes it. The HUD clamps the visual fill.

`/covenant progression` inspects your level/XP/Wither claim. Operators can use
`/covenant progression xp <positive long>` for normal capped awards, or
`/covenant progression level <1..global cap>` to set a test level and clear its
current XP. The latter deliberately changes testing progress, not the Wither
claim. It does not erase existing allocations; use the respec item when needed.

## Server datapack balance

Override `data/covenant/progression/balance.json` and run `/reload`. Each successful
reload publishes an immutable resource-scoped snapshot; failed reloads retain
the entire previous snapshot. Reload/login never relevels a player or rewrites
raw XP, claims, points or allocations. Harder requirements reduce the displayed
fraction without taking earned XP. Lowered caps block future awards. Only a real
new award can advance against changed requirements; new XP blocked at a cap is
discarded, while pre-existing stored XP is retained.

| Field | Units / default / constraints |
| --- | --- |
| `track_caps` | All six namespaced track IDs required: character plus Mining, Logging, Farming, Crafting and Building. Character 60 (1–1000); professions 20 (0–1000). |
| `character_requirements` | Positive whole XP per transition, indexed by current level minus 1. Must cover the cap. Default `round(50 * 1200^((L-1)/58))`, 50 through 60,000. |
| `profession_requirements` | Positive whole XP indexed by current profession level, starting 0. Provisional default 100–2,000 in steps of 100. A nonempty curve must cover every profession cap; empty explicitly disables awards. |
| `hostile_reward` | Whole XP, default 10, nonnegative signed long. Applies to `Enemy` actors except neutrals, with no spawn-origin penalty. |
| `entity_rewards` | Exact entity ID → nonnegative whole XP. Defaults Enderman 20, Wither 2000, dragon 2000. Exact overrides beat tags; players always give zero. |
| `reward_tags` | `{ "tag": "namespace:tag", "priority": integer, "xp": nonnegative long }`. Uses native entity-type tags; highest priority wins. Priorities must be unique. |
| `fallback_caps` | Per-track fallback; unspecified tracks use their global cap. Values cannot exceed global caps. |
| `dimensions` | `{ "match": "dimension:id" or "#namespace:tag", "priority": integer, "caps": { "track:id": level } }`. Exact IDs win per track; then highest tag priority; then fallback. Duplicate selectors/tag priorities rejected. |

Dimension grouping tags use native **dimension types** under
`data/<namespace>/tags/dimension_type/<tag>.json`. For example, a tag containing
`minecraft:overworld` groups dimensions using that type, including custom
dimensions sharing it. Use explicit dimension IDs to distinguish those worlds.
This avoids depending on tags on generated level-stem definitions, which do not
provide the vanilla dimensions as ordinary registry entries during data loading.

Limits: at most 1000 curve entries, 4096 exact rewards, 256 reward tags and 1024
dimension rules. Fractional/negative XP, unknown track IDs, nonpositive thresholds,
invalid caps, incomplete curves and ambiguous priorities reject the reload.
Rules referencing absent mod content simply have no match. No Apotheosis or pack
dependency is added; its eventual integration remains separate work.

See [VALIDATION.md](VALIDATION.md) for automated evidence and pending client checks.
