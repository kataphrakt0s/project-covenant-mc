# RPG stat foundation

Stats use Minecraft's attribute registry and native equipment modifiers.
`RpgStats` centralizes the initial attributes and skill scaling helper. There is
no parallel stat cache, equipment attachment, or global tick scan.

| Stat | Modifier `stat` ID | Unequipped player base | Meaning |
| --- | --- | --- | --- |
| Health | `minecraft:max_health` | 20 | Health points; two points per heart |
| Damage | `minecraft:attack_damage` | 1 | Vanilla attack damage plus native weapon bonuses |
| Armor | `minecraft:armor` | 0 | Vanilla armor points and mitigation |
| Movement Speed | `minecraft:movement_speed` | approximately 0.1 | Vanilla movement coefficient |
| Attack Speed | `minecraft:attack_speed` | 4 | Fully charged attacks per second |
| Dodge | `covenant:dodge` | 0 | Probability in [0, 1] |
| Resource Cost | `covenant:resource_cost` | 1 | Skill cost multiplier for any resource |
| Cooldown | `covenant:cooldown` | 1 | Skill cooldown multiplier |
| Cast Time | `covenant:cast_time` | 1 | Skill cast-time multiplier |
| Skill Duration | `covenant:skill_duration` | 1 | Skill effect duration multiplier |

Vanilla attributes keep vanilla bounds. Skill multipliers use [0, 1024]: 0.8 means
20% less, 1.2 means 20% more. Cost/cooldown/cast-time reductions get beneficial
tooltip coloring. Increasing maximum health does not heal the player. Damage
follows vanilla attack-damage consumers (principally melee), without rescaling
arrows, fire, explosions, or every third-party damage source. Attack speed affects
attack recharge, not projectile fire rate or skill cooldowns. Armor retains vanilla
toughness, enchantments, nonlinear mitigation, and damage-source bypass rules.

## Calculation

Existing saved operations map directly to Minecraft's operations:

1. `add`: sum flat adjustments with the attribute base.
2. `multiply_base`: add each fraction of that adjusted base (fractions sum).
3. `multiply_total`: multiply by each independent `(1 + amount)`.
4. Clamp to the attribute's supported range.

For base 20, two +4 bonuses, +25% base, and independent +10%/+20% total:
`(20 + 4 + 4) * 1.25 * 1.10 * 1.20 = 46.2`. Vanilla gear/effects and other mods'
modifiers share this calculation. Dodge `add: 0.1` grants ten percentage points;
percentage multipliers alone do not create chance from its zero base.

Skill attributes modify coefficients, not absolute skill units. A cost
`multiply_base: -0.2` changes coefficient 1 to 0.8. Then
`RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, 50)` returns 40; base cost
100 returns 80. The helper also handles cooldown, cast time, and duration. Skill
`add: -0.2` subtracts 0.2 from the coefficient, not 0.2 mana or seconds. There are
no per-skill flat-unit adjustments. Inputs/results must be finite and nonnegative;
zero bases stay zero. Future skills own units, rounding, minimum timers, resource
types, and spending rules.

## Equipment and persistence

`ItemAttributeModifierEvent` reads the stack's immutable page. Every base/affix
entry has a stable modifier ID from page UUID and list position. It adds to native
item attributes without editing item defaults, page data, or player profiles.
Categories select matching vanilla slots: main hand, off hand, head, chest
(`body_armor`), legs, and feet. Chest gear held in hand or inventory grants no page
bonus. Jewelry/custom categories retain data but await explicit slot integration.
Tags gate attachment only; tag reload does not deactivate existing pages.

Minecraft's normal living-entity update removes old transient modifiers and adds
new ones when equipment/components change, including in-place page updates.
Changes take effect after this update, not immediately inside `setItemSlot` or
`AffixPages.update`. Repeated queries/unchanged ticks cannot accumulate bonuses.
Broken gear follows native inactive behavior. Transfers preserve data under the
existing [copy contract](RPG_DATA.md#copy-and-split-contract).

Only genuine character data and ItemStack pages persist. Equipment modifiers are
transient and reconstructed after load/clone, with no profile initialization
requirement. Saved fields, IDs, and operation strings are unchanged; no migration
is required. Previously stored unknown IDs (e.g. placeholder `covenant:power`)
remain intact and inactive, without silent conversion to another stat.

Vanilla loads/clamps current health before player inventory. A narrow required
Mixin at the end of player inventory loading applies native equipped max-health
modifiers and restores the existing saved `Health` field. A clone-event handler
does the same ordering repair for End return and death with retained inventory.
This preserves, for example, 26/26 health across reload instead of losing the six
points above the unequipped maximum. It adds no saved health field or permanent
equipment modifier. The first native equipment update replaces the same IDs.

## Combat and integration boundaries

Dodge rolls on the logical server in `LivingIncomingDamageEvent`, before shield,
armor, and health processing. Direct living-entity/projectile hits qualify;
explosions, environment, self melee, and `bypasses_invulnerability` sources do not.
Cancellation avoids damage, hit knockback, and hit durability costs without
granting a new invulnerability window. Independently applied third-party
projectile effects are outside this guarantee.

Native attribute packets sync effective values; Covenant enables vanilla attack
damage sync too. Pages retain their component sync. There are no client-to-server
stat packets. Future UI reads `player.getAttributeValue(RpgStats.HEALTH)` etc.
Trusted server integrations use normal native attribute modifiers with their own
stable IDs. Use transient modifiers for effects, permanent modifiers only for
state intended to persist. Pages accept any registered attribute ID, provided the
wearer supports it; unknown IDs are ignored without destroying data. Mods add
attributes through `EntityAttributeModificationEvent`. Native page modifiers can
also affect equipped mobs with the corresponding attribute; Covenant's new
attributes and dodge behavior are player-only.

There are no acquisition commands, rolling, stat screen, skills/resources,
attribute points, talents, or class stat bonuses. Author pages through server APIs
or operator item components during development.

## Validation

`scripts/Dev.ps1 test` adds four dedicated GameTests to the existing data checks:
base values/operation order; native equipment activation/removal, independence,
transfer, breakage, and in-place updates; skill scaling/external sources; actual
player-file reload and clone reconstruction; deterministic 0%/100% dodge and
environmental exclusions. Real-client delivery/reconnect, actual dimension
travel/death, full server restart with players, tooltips, and other mods remain
manual checks. APIs were checked against resolved Minecraft 26.1.2 / NeoForge
26.1.2.125 sources and [official 26.1 attribute documentation](https://docs.neoforged.net/docs/entities/attributes/).
