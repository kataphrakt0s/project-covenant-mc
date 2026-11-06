# RPG stat foundation

Stats use Minecraft's attribute registry and native equipment modifiers.
`RpgStats` centralizes the initial attributes and skill scaling helper. There is
no parallel stat cache, equipment attachment, or global tick scan.

## Stats page

Hold G, hover radial slot 2 (**Stats**), then release G or left-click. The page
stays open after releasing G and does not pause the world. Escape or Done closes it.
The class appears at the top. Uninitialized characters can click Stalker, Reaver,
or Weaver to select once; hover a choice for its description. The page explains
that selection is permanent. Buttons send the existing `/covenant class select <id>`
command, which requires no operator permission and rejects unknown/repeated choices
on the server. The owner-synced profile updates the header and removes the choices.
Unknown saved class IDs display verbatim and cannot be overwritten by the chooser.

The scrollable text list shows every supported stat below, including inactive-class
resource multipliers. Each entry lists its effective value, native base and active
native modifiers (ID, signed amount and flat/base-percent/total-percent operation).
Dodge totals use percent; skill/resource coefficients use `x`; the other values
use the table's native units. Flat modifier amounts retain raw attribute units.
Mouse wheel/scrollbar and focused arrow keys scroll; long lines wrap to fit.
The page refreshes from existing synced native attributes while open, including
equipment, effects, and debug adjustments. No new packets, permissions, saved fields,
or effective-stat cache are introduced.

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
| Focus Regeneration | `covenant:focus_regeneration` | 1 | Configured recovery multiplier, bounded to [0, 1.25] |
| Rage Capacity | `covenant:rage_capacity` | 1 | Configured maximum multiplier, bounded to [0, 1024] |
| Rage Generation | `covenant:rage_generation` | 1 | Multiplies outgoing and incoming damage-driven Rage gains, bounded to [0, 1024] |
| Mana Capacity | `covenant:mana_capacity` | 1 | Configured maximum multiplier, bounded to [0, 1024] |
| Mana Regeneration | `covenant:mana_regeneration` | 1 | Configured recovery multiplier, bounded to [0, 1024] |

Vanilla attributes keep vanilla bounds. Skill multipliers use [0, 1024]: 0.8 means
20% less, 1.2 means 20% more. Cost/cooldown/cast-time reductions get beneficial
tooltip coloring. Increasing maximum health does not heal the player. Damage
follows vanilla attack-damage consumers (principally melee), without rescaling
arrows, fire, explosions, or every third-party damage source. Attack speed affects
attack recharge, not projectile fire rate or skill cooldowns. Armor retains vanilla
toughness, enchantments, nonlinear mitigation, and damage-source bypass rules.

## Calculation

Resource attributes use the same native equipment/page pipeline; see
[RPG_SKILLS.md](RPG_SKILLS.md) for base configuration and resource rules.
Ordinary effective Dodge chance is capped at 85% across equipment, aura bonuses,
and other sources; the native attribute still retains its original [0, 1] bounds.
Dodge Aura adds ten percentage points. Its active guarantees one dodgeable hit,
bypassing the ordinary cap and consuming all overlapping guarantees together.
Undodgeable damage is unchanged. Owned pets receive the aura chance/guarantee
through the shared handler without enabling Dodge for unrelated mobs.

Focus has no maximum-capacity attribute and always caps at 100. The three initial
skills consume the existing cost, cooldown and cast-time coefficients; they have
no timed effects using the duration coefficient.

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

## Debug commands

Player operators (vanilla gamemaster permission, normally level 2) can use:

- `/covenant stats`: list all fifteen supported stats and effective values.
- `/covenant stats get <stat>`: show the normal base, temporary testing base if
  present, every active modifier's ID/operation/amount, and final clamped value.
- `/covenant stats set <stat> <value>`: substitute a temporary testing base while
  retaining equipment, Affix Pages, and other native modifiers. For example,
  `set covenant:dodge 0.2` with a +0.1 page produces 0.3 effective dodge.
- `/covenant stats reset`: remove every debug adjustment immediately.

`<stat>` uses the full registry IDs in the table above; `get` and `set` provide tab
completion. Unsupported IDs, malformed numbers, non-finite values, and values
outside the attribute's inclusive native range are rejected without changing state.
Commands target the executing player; console execution is rejected. Existing
class commands retain their separate permissions and behavior.

The debug layer is a single transient `covenant:debug/base_override` flat modifier
per affected attribute, calculated as requested base minus the current normal base
at command execution. Repeated `set` replaces that adjustment. It does not edit
native saved bases, permanent modifiers, profiles, or pages. Equipment changes
continue through the normal pipeline; reset removes only the debug ID and uses the
current equipment. A later external base change also affects the calculation;
rerun `set` to substitute a new testing base in that case. No tick polling is added.

Native synchronization delivers changes on the next normal attribute packet;
server calculations update immediately. Overrides disappear on player reload,
reconnect, or player replacement (death/End return), and are never serialized.
Increasing maximum health does not heal. Current health and other ordinary gameplay
consequences of testing still follow vanilla behavior; reset restores stat
calculations, not a prior gameplay snapshot. No saved format changes or migration.

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
attributes remain player-only; owned pets additionally participate in aura Dodge.

There are no gameplay acquisition mechanics, rolling, stat screen,
attribute points, talents, or class stat bonuses. Author pages through server APIs,
operator item components, or the privileged [Affix Page debug commands](RPG_DATA.md#debug-commands).

## Validation

`scripts/Dev.ps1 test` includes dedicated stat GameTests alongside the data/class checks:
base values/operation order; native equipment activation/removal, independence,
transfer, breakage, and in-place updates; skill scaling/external sources; actual
player-file reload and clone reconstruction; zero Dodge, seeded capped ordinary Dodge, guaranteed aura Dodge, and
environmental exclusions; debug commands/completion/validation and unsaved override
isolation across actual player-file reload and cloning. Real-client delivery/reconnect, actual dimension
travel/death, full server restart with players, tooltips, and other mods remain
manual checks. APIs were checked against resolved Minecraft 26.1.2 / NeoForge
26.1.2.125 sources and [official 26.1 attribute documentation](https://docs.neoforged.net/docs/entities/attributes/).
