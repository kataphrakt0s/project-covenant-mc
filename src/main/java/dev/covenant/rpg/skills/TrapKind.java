package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Four named prototypes; durations are ticks, damage is health points, colors are RGB. */
public enum TrapKind {
    BEAR("bear_trap", 60, 4, 0, Items.IRON_TRAPDOOR, 0xC4C4C4),
    FROST("frost_trap", 160, 0, 3.5, Items.BLUE_ICE, 0x80CAFF),
    FLAME("flame_trap", 120, 3, 3, Items.MAGMA_BLOCK, 0xFF762B),
    BLAST("blast_trap", 0, 10, 3.5, Items.TNT, 0xF04444);

    public final Identifier skillId;
    public final ResourceKey<DamageType> damageType;
    public final int duration;
    public final float damage;
    /** Effect radius in blocks, measured from target feet; zero for single-target Bear. */
    public final double radius;
    public final Item visual;
    public final int color;
    private static final TrapKind[] VALUES = values();

    static TrapKind byOrdinal(int ordinal) { return VALUES[ordinal]; }

    TrapKind(String name, int duration, float damage, double radius, Item visual, int color) {
        this.skillId = Identifier.fromNamespaceAndPath(Covenant.MOD_ID, name);
        this.damageType = ResourceKey.create(Registries.DAMAGE_TYPE, skillId);
        this.duration = duration;
        this.damage = damage;
        this.radius = radius;
        this.visual = visual;
        this.color = color;
    }

    public static TrapKind forSkill(SkillDefinition skill) {
        for (var kind : values()) { if (kind.skillId.equals(skill.id())) { return kind; } }
        throw new IllegalArgumentException("Not a trap skill: " + skill.id());
    }
}
