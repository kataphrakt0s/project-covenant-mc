# Technical Architecture

Build a maintainable standalone NeoForge mod. Verify exact Minecraft 26.1.2 NeoForge/Java requirements against the official MDK before implementation; do not reuse old-version API assumptions.

Boundaries: character progression in player profiles; item-instance RPG data on stacks; effective stats derived from sources; server authority; data-driven content where useful; optional integrations separate from core.

Related: [[Player Profiles]], [[Item Data and Persistence]], [[Stat Calculation Pipeline]], [[Data-Driven Content]], [[Networking and Authority]], [[Mod Compatibility]], [[Testing Strategy]].
