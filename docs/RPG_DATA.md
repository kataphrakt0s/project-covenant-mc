# RPG data architecture

The server-authoritative foundation now includes class selection, combat XP and
skill-point progression. See [PROGRESSION.md](PROGRESSION.md) for the progression
format, APIs and data balance. Profession tracks have saved state; their activity
sources and ability content await the user's requirements. Page acquisition,
rolling/loot and accessory integrations remain deferred.
Effective stats use native equipment attributes; see [RPG_STATS.md](RPG_STATS.md).
Skills use bounded intent packets and event-driven combat; there are no global
enemy progression scans.

## Permanent character state

The progression format requires fresh saves. No legacy prototype profile/loadout
migration or free-skill conversion is provided. New-format persistence and normal
lifecycle guarantees remain required.

`covenant:character_profile` is a serialized NeoForge entity attachment. Its `profile` field holds
an immutable `CharacterProfile`: creation flag, level, current RPG experience balance (long),
optional class/specialization/resource identifiers, current/maximum resource (double units),
and the required boolean `wither_claimed`. Level, XP and the claim commit together.
These identifiers describe future RPG content, not item registry entries. A resource-less class
has no resource type and both resource values are zero. Player-facing class selection validates
against the built-in definitions on the logical server.

New players get the uninitialized snapshot on login: false, level/XP/resource zero, no selections.
`PlayerProfiles.initialize(ServerPlayer, ...)` creates level 1, XP 0, no specialization, and a full
resource balance. Repeat initialization fails. Trusted server code uses `PlayerProfiles.update`
to replace initialized state. Constructors and codecs reject inconsistent creation state,
negative progression, non-finite numbers, and resources outside [0, maximum].

The attachment uses `copyOnDeath()` and NeoForge's default serialization copy handler. NeoForge
copies on death and End return; there is deliberately no second Clone listener to add or reset
progression. Vanilla player save/load stores the attachment. NeoForge sends owner-only snapshots
on initial login, respawn, dimension transitions, and attachment replacement. Client UI can read
`PlayerProfiles.get(player)`; it has no authority to submit progression changes. Mutation APIs
require a logical server player and the server thread.

## Class identities and selection

The small built-in `PlayerClass` catalog defines stable IDs, localized names, and descriptions:

| Saved ID | Name | Intended identity and equipment |
| --- | --- | --- |
| `covenant:stalker` | Stalker | Precision ranged damage and distance; bows and other ranged weapons. |
| `covenant:reaver` | Reaver | Direct close-quarters damage; swords, axes, and other melee weapons. |
| `covenant:weaver` | Weaver | Simple elemental offensive magic; magical equipment where supported. |

Players use `/covenant class` to see their current class, descriptions, and the selection command.
`/covenant class select covenant:stalker` (or `covenant:reaver` / `covenant:weaver`) selects once.
Tab completion suggests IDs. Operator permission is unnecessary. Commands act only on the
executing player; console calls, unknown IDs, and selection after character creation are rejected.
Operator-only `/covenant class switch <id>` now supports resource/skill testing;
ordinary selection remains one-time. The Stats screen offers the same one-time selection.

`PlayerProfiles.selectClass(ServerPlayer, PlayerClass)` creates the existing profile at level 1,
XP 0, no specialization, and empty legacy resource fields, and initializes the separate
built-in resource balances. There is no separate class XP or
level. Class IDs retain the `class` field; the progression format requires fresh saves. Save/load,
reconnect, death, and dimension changes use the same attachment persistence/copy/owner sync path.
Nothing writes class identity or progression into Affix Pages.

Other systems call `PlayerProfiles.getSelectedClass(player)` for an `Optional<PlayerClass>`;
`PlayerClass.find(id)` resolves an ID, and `PlayerClass.all()` exposes immutable definitions.
Uninitialized profiles and unknown saved IDs resolve empty. The raw ID remains available through
`PlayerProfiles.get(player).selectedClass()`. Existing unknown IDs are neither rewritten nor
treated as uninitialized; no automatic rename from earlier placeholder class IDs is performed.
The low-level trusted `initialize`/`update` APIs still support open IDs for server integrations.

Definitions are code-backed, with display text in `assets/covenant/lang/en_us.json`; there is no
new datapack registry or reload framework. Add a catalog entry and translations to expose a future
class in discovery/selection. Existing saved IDs must remain stable or receive an explicit migration.
Classes provide query hooks without passive stat modifiers, global equipment gates,
or specialization mechanics. Basic resources and skills are documented in
[RPG_SKILLS.md](RPG_SKILLS.md). Native attack damage
does not provide a general ranged/elemental damage stat; no class bonus bridge is added here.

## Resource and skill state

Built-in resource balances now live in the additive `covenant:skill_state` player
attachment, alongside persistent cooldown deadlines. Existing profile resource fields
remain unchanged for compatibility and seed a matching built-in balance once.
Selection initializes the independent balances. See [RPG_SKILLS.md](RPG_SKILLS.md)
for authority, migration, class switching and timer lifecycles; effective resource
capacities still come from configuration/native attributes, separate from saved balances.

The separate additive `covenant:skill_loadout` attachment saves learned skill IDs and
eight logical slot assignments. It is immutable, copied on death/End return, and synced
only to its owner. Missing state defaults empty, including for existing characters.
Unlearning clears matching assignments and cancels that skill's cast without erasing
resources or cooldowns. Client requests go through `SkillLoadouts` on the logical
server; profile/class data remains behind its existing server API. Keyboard mappings
are local vanilla options, separate from the saved per-player assignments. See
`RPG_SKILLS.md` for the GUI, packet validation and migration contract.

## Per-item Affix Pages

`covenant:affix_page` is a persistent, network-synchronized Minecraft item data component. It is
set on individual `ItemStack` instances; item defaults and other mods' item implementations are
untouched. The component contains:

- A UUID minted on attachment, retained on transfers and updates.
- A namespaced equipment category.
- An ordered list of independent base RPG modifiers.
- An ordered list of independent affixes, each with definition ID, rarity ID, positive tier,
  and its own modifier list. IDs/rarities are open identifiers; no random generation exists yet.
- Progression metadata: nonnegative upgrade count and experience, both defaulting to zero.
  No upgrade mechanics or costs are defined yet.

Each modifier has a stat identifier, operation (`add`, `multiply_base`, `multiply_total`), and
finite double amount. Multiplicative amounts are fractions: 0.10 represents 10%. Repeated stat
entries are preserved; they are not collapsed into one bonus. Format limits are 64 affixes per
page, 64 base modifiers, and 64 modifiers per affix, bounding serialized lists. Lists are copied
and immutable, including nested affix lists. Native equipment aggregation uses the page UUID
and affix/modifier position as stable source identity. Stat IDs resolve directly to attribute
registry IDs; unknown IDs remain intact but inactive.

`AffixPages.attach(ServerLevel, stack, category, modifiers, affixes)` checks the server thread,
category compatibility, absence of a page, and item count exactly 1 before changing anything.
It sets that stack's `minecraft:max_stack_size` component to 1. `AffixPages.update` preserves
identity/category and reasserts the same singleton rule. Callers must notify the containing
inventory/block entity after mutation, using its standard change notification.
`AffixPages.remove` removes the complete page in place on the logical server and returns whether
one was present. It preserves all other stack components, including the max-stack override:
attachment does not retain that component's previous value. A naturally stackable item may
therefore remain limited to one after removal. There is no extraction/replacement acquisition
mechanic or change to the saved page format.

Moving slots, putting gear in containers, dropping/picking it up, player transfers, equipping,
and unequipping use Minecraft's existing component-bearing stacks. Item persistence and inventory
network synchronization include both the page and max-stack override automatically. No owner,
slot, or item-type lookup is used to find a page. No equipment modifiers are written into a
permanent character profile. Native equipment updates remove and reconstruct transient attribute
contributions when equipment or its components change; no custom aggregation cache exists.

### Debug commands

These commands require vanilla gamemaster permission (normally operator level 2), target the
executing player's main hand, and reject an empty hand or non-player source:

- `/covenant affix inspect`: shows category, page UUID, every independent base and nested affix
  modifier (stat, operation, amount), and existing progression metadata. Missing/empty pages
  receive distinct feedback. Unknown saved stat/category IDs are displayed without alteration.
- `/covenant affix apply <category> <stat> <value>`: calls the existing attachment API to create
  a page with one flat `add` modifier, no affixes, and zero progression. Category IDs come from
  loaded `affix_compatible/` item tags, including datapack additions. Stat IDs use the fifteen
  supported entries in `RPG_STATS.md`. Both arguments offer tab completion. The item must match
  the category tag, have count one, and have no page. Values are finite signed doubles in the
  stat's native units; negative reductions are valid and effective values retain native clamps.
- `/covenant affix remove` and `/covenant affix clear`: aliases that erase the whole page,
  including identity, base modifiers, affixes, and progression. No page is a harmless no-op.
  Item type/count, damage, names, enchantments, other components, and current stack limit remain.

Commands notify the native inventory/menu after mutation. Equipment contributions refresh on
the normal living-entity equipment update, including removing a page from a held weapon. Armor
pages held in the main hand do not activate until worn in their matching slot; accessory/custom
categories retain their existing lack of slot integration. These are persistent item edits, unlike
temporary stat overrides. No player profile is read or changed, and class/stat commands are separate.

### Copy and split contract

For a legal page-bearing stack, `split(n)` can only consume/move its sole item (or move nothing
for n=0). Two differently paged items cannot merge, and even identical component snapshots have
maximum count 1. The attachment API rejects multi-item stacks instead of distributing or silently
discarding items.

`ItemStack.copy()` intentionally retains the exact page and UUID. Minecraft uses this for inventory
snapshots, network comparison, persistence, and transfer intermediates; regenerating/removing the
page on copy would corrupt those operations. Immutable values prevent copies from sharing mutable
RPG state. A snapshot must not be inserted as an additional live item while retaining its source.
The APIs never do so. Deliberate creative cloning, operator item commands, and third-party code
which manufactures copies or changes counts/components can duplicate an item and its page just
as they can duplicate vanilla equipment. The UUID is provenance, not a global anti-duplication
registry. A future supported duplication mechanic must explicitly mint new identities. Normal
survival transfers need neither a global identity scan nor ownership tracking.

## Category compatibility

Categories are open `EquipmentCategory(Identifier)` values, not a closed enum or equipment-slot
keys. Category `namespace:path` checks item tag `namespace:affix_compatible/path`. Unknown or empty
tags deny compatibility; no universal fallback permits arbitrary gear. Multiple category tags may
include an item, but the attached page has one chosen category. A tag reload never rewrites existing
page data. Compatibility gates attachment; the page category selects its active equipment slot
as documented in `RPG_STATS.md`. Custom/jewelry categories have no active slot mapping yet.

Built-ins are `covenant:main_hand`, `off_hand`, `head_armor`, `body_armor`, `leg_armor`, `foot_armor`,
`ring`, `necklace`, `accessory`, and `trinket`. Initial tags support vanilla tools/weapons, shields,
and armor; jewelry/accessory tags are empty until content explicitly opts in. The vanilla equipable
component is not a blanket compatibility rule.

For example, a datapack can opt a foreign ring into `covenant:ring` with
`data/covenant/tags/item/affix_compatible/ring.json`:

```json
{
  "replace": false,
  "values": [{ "id": "another_mod:silver_ring", "required": false }]
}
```

Optional entries allow the datapack to load without that mod. A new category `example:charm` just
uses tag `example:affix_compatible/charm` and an `EquipmentCategory` with that ID. No required mod
dependency, class check, custom registry, or accessory-slot integration is needed.

## Persistence compatibility and validation

This is the first saved RPG format. Registry/component IDs, serialized field names, operation
strings, category identifiers, and units are now persistence contracts. Future renames, field
removals, or semantic changes require an explicit migration. Additive optional metadata should
retain defaults; unknown future fields are not preserved by an older mod build. Downgrading a
world with newer page data is not a supported migration path.

Run `scripts/Dev.ps1 test` for the isolated dedicated GameTest server. It checks player entity and
actual player-file serialization, death/End cloning, profile and item network codecs, inventory
and container movement, equip/unequip, dropped-entity persistence/pickup, page independence,
custom-category opt-in, stack copying/splitting, and invalid data. Test code/resources live in
`src/gameTest`, load only in that run configuration, and are excluded from the production JAR.
The empty 5x5x5 NBT structure is a test fixture.

The `class_foundation` GameTest exercises all three registered choices without operator permissions,
command discovery/completion, invalid and repeated selections, non-player rejection, ordinary
equipment use, actual player-file reload, death/End cloning, network codecs, and unknown saved IDs.

These tests do not replace real-client login/reconnect, dimension travel, restart, GUI inventory,
multiplayer trade, or creative-mode observations. Additional stat checks and combat/integration
boundaries are documented in `RPG_STATS.md`; unrelated mods' custom inventories remain untested.

`affix_commands` exercises command-created pages, category/stat completion, invalid/unauthorized
calls, exact underlying-item preservation on remove/clear, a real chest block entity and its
serialization, native drop/pickup, individual stack ownership, and equipment recalculation.
`affix_restart` additionally checks an unchanged native player file across separate server
processes; see the two-run procedure in `DEVELOPMENT.md`. GameTest deletes its world on launch,
so this fixture retains/restores that file outside the disposable world. A complete retained-world
restart and rendered client command/inventory behavior still require manual observation.

API references: [NeoForge attachments](https://docs.neoforged.net/docs/datastorage/attachments/),
[item components](https://docs.neoforged.net/docs/items/datacomponents/),
[commands](https://docs.neoforged.net/docs/misc/commands/), and
[GameTests](https://docs.neoforged.net/docs/misc/gametest/), checked against the resolved
Minecraft 26.1.2 / NeoForge 26.1.2.125 sources.
