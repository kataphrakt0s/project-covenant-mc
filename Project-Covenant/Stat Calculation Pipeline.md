# Stat Calculation Pipeline

Use Minecraft's native attribute calculation: base plus flat additions, then summed base-percentage adjustments, then independent total multipliers, then the attribute's range clamp. Native equipment and effect modifiers share this calculation.

Invariant: item modifiers apply while equipped and are not copied into the player profile. Equipped Affix Pages supply transient modifiers through the item attribute event. Stable page/entry IDs prevent accumulation. Minecraft handles equipment change detection and attribute synchronization; there is no parallel stat cache or scan.

Skill properties are their own base values multiplied by effective skill coefficients. Character levels do not grant traditional attribute points. Profession effects may add modest, class-agnostic combat benefits when deliberately authored; active/conditional effects should use appropriate gameplay/effect hooks rather than introduce an unnecessary universal modifier framework.

See [the implementation contract](../docs/RPG_STATS.md) for current examples and boundaries.

Related: [[Statistics]], [[Attributes]], [[Equipment]], [[Affix Pages]], [[Combat]], [[Profession Progression]].
