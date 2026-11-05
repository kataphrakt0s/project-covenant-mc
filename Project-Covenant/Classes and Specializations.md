# Classes and Specializations

The initial playable identities are **Stalker**, **Reaver**, and **Weaver**. Classes do not restrict equipment.

- **Stalker**: bow-focused ranged combat, precision, ranged damage, and maintaining distance. Intended for bows and other suitable ranged weapons.
- **Reaver**: dedicated melee combat, close-quarters damage, and direct combat. Intended for swords, axes, and other melee weapons.
- **Weaver**: elemental spellcasting and simple offensive magic. Intended for magical equipment where supported; elemental skills and abilities are future work.

Implemented foundation: stable IDs `covenant:stalker`, `covenant:reaver`, and `covenant:weaver`, localized descriptions, and one-time server-authoritative selection via `/covenant class select <id>`. `/covenant class` lists choices and the current selection. The existing player profile owns and persists the choice; classes add no bonuses, resources, skills, or equipment restrictions yet. See `docs/RPG_DATA.md` for APIs and persistence contracts.

Future scope: recognizable class abilities and resources, meaningful within-class customization, specializations, and talents. No specialization structure, talent economy, or cross-class customization is implemented.

Planned Reaver build direction: support both bleed-focused and non-bleed builds.
Overpower rewards attacking a non-bleeding enemy with a huge melee hit. This is
design intent recorded in [[Skill Trees Brainstorm]], not implemented behavior.

Related: [[Abilities]], [[Class Resources]], [[Character Progression]].
