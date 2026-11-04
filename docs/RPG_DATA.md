# RPG data architecture

This is storage and server API infrastructure. Character creation screens, class definitions,
XP curves, page acquisition, rolling/loot, accessory integrations, and effective stat evaluation
are deferred. There are no gameplay packets, commands, tick scans, or combat attribute changes.

## Permanent character state

`covenant:character_profile` is a serialized NeoForge entity attachment. Its `profile` field holds
an immutable `CharacterProfile`: creation flag, level, current RPG experience balance (long),
optional class/specialization/resource identifiers, and current/maximum resource (double units).
These identifiers describe future RPG content, not item registry entries. A resource-less class
has no resource type and both resource values are zero. Class selection must be validated against
future class definitions by the server creation flow.

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
and immutable, including nested affix lists. Future aggregation should use the equipped page UUID
and affix/modifier position as source identity and evaluate operations consistently.

`AffixPages.attach(ServerLevel, stack, category, modifiers, affixes)` checks the server thread,
category compatibility, absence of a page, and item count exactly 1 before changing anything.
It sets that stack's `minecraft:max_stack_size` component to 1. `AffixPages.update` preserves
identity/category and reasserts the same singleton rule. Callers must notify the containing
inventory/block entity after mutation, using its standard change notification. There is no page
removal or replacement acquisition mechanic in this stage.

Moving slots, putting gear in containers, dropping/picking it up, player transfers, equipping,
and unequipping use Minecraft's existing component-bearing stacks. Item persistence and inventory
network synchronization include both the page and max-stack override automatically. No owner,
slot, or item-type lookup is used to find a page. No equipment modifiers are written into a
permanent character profile. A future effective-stat evaluator must read current equipment and
remove contributions when that equipment changes; no aggregation cache exists yet.

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
page data. Compatibility gates attachment; future equipment rules decide whether a page contributes.

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

These tests do not replace real-client login/reconnect, dimension travel, restart, GUI inventory,
multiplayer trade, or creative-mode observations. Full combat/stat aggregation and unrelated mods'
custom inventory implementations remain outside this stage.

API references: [NeoForge attachments](https://docs.neoforged.net/docs/datastorage/attachments/),
[item components](https://docs.neoforged.net/docs/items/datacomponents/), and
[GameTests](https://docs.neoforged.net/docs/misc/gametest/), checked against the resolved
Minecraft 26.1.2 / NeoForge 26.1.2.125 sources.
