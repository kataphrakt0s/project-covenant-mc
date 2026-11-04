# Stat Calculation Pipeline

Use Minecraft's native attribute calculation: base plus flat additions, then summed base-percentage adjustments, then independent total multipliers, then the attribute's range clamp. Native equipment and effect modifiers share this calculation.

Invariant: item modifiers apply while equipped and are not copied into the player profile.

Equipped Affix Pages supply transient modifiers through the item attribute event. Stable page/entry IDs prevent accumulation. Minecraft handles equipment change detection and attribute synchronization; there is no parallel stat cache or scan. Skill properties are their own base values multiplied by effective skill coefficients.

See [the implementation contract](../docs/RPG_STATS.md) for examples and boundaries. Additional progression/talent/conditional sources are deferred.

Related: [[Statistics]], [[Attributes]], [[Equipment]], [[Affix Pages]], [[Combat]].
