package dev.covenant.rpg.skills;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Short native cues at accepted skill transitions; no loops, recurring ticks, or gameplay state. */
final class SkillSounds {
    private SkillSounds() {}

    /** A single quiet acknowledgement that an accepted wind-up started. */
    static void castStart(ServerPlayer player, SkillDefinition skill) {
        SoundEvent sound;
        float pitch = 1.4f;
        if (skill == SkillDefinition.AIMED_SHOT) { sound = SoundEvents.CROSSBOW_LOADING_START.value(); }
        else if (skill == SkillDefinition.FIRE_BOLT) { sound = SoundEvents.FLINTANDSTEEL_USE; }
        else if (skill == SkillDefinition.ICE_BOLT) { sound = SoundEvents.POWDER_SNOW_STEP; }
        else if (skill == SkillDefinition.LIGHTNING_BOLT) { sound = SoundEvents.COPPER_BULB_TURN_ON; }
        else if (skill.effect() == SkillDefinition.Effect.RAGE_STORM) { sound = SoundEvents.BREEZE_INHALE; pitch = 0.9f; }
        else { sound = SoundEvents.AMETHYST_BLOCK_RESONATE; }
        play(player.level(), player.position(), sound, 0.3f, pitch);
    }

    /** One release cue per accepted action, including one cue for an entire arrow volley. */
    static void release(ServerPlayer player, SkillDefinition skill) {
        SoundEvent sound;
        float volume = 0.55f;
        float pitch = 1.0f;
        switch (skill.effect()) {
            case BOW_SHOT -> {
                sound = SoundEvents.ARROW_SHOOT;
                pitch = skill == SkillDefinition.AIMED_SHOT ? 0.85f
                        : skill == SkillDefinition.MULTISHOT ? 1.0f : 1.2f;
            }
            case ARCANE_BOLT -> { sound = SoundEvents.AMETHYST_BLOCK_CHIME; pitch = 1.4f; }
            case ELEMENTAL_BOLT -> {
                sound = skill == SkillDefinition.FIRE_BOLT ? SoundEvents.FIRECHARGE_USE : SoundEvents.SNOWBALL_THROW;
                pitch = skill == SkillDefinition.FIRE_BOLT ? 1.15f : 0.8f;
            }
            case LIGHTNING_BOLT -> { sound = SoundEvents.TRIDENT_THROW.value(); pitch = 1.8f; }
            case CLEAVE, COMBO -> { sound = SoundEvents.PLAYER_ATTACK_SWEEP; volume = 0.65f; }
            case REND -> { sound = SoundEvents.PLAYER_ATTACK_SWEEP; pitch = 1.4f; }
            case TRAP -> { sound = SoundEvents.SNOWBALL_THROW; volume = 0.4f; pitch = 0.8f; }
            case DISENGAGE -> { sound = SoundEvents.BREEZE_JUMP; volume = 0.4f; pitch = 1.3f; }
            case BATTLE_CRY -> { sound = SoundEvents.BREEZE_SHOOT; pitch = 0.65f; }
            case RAGE_STORM -> { sound = SoundEvents.BREEZE_SHOOT; volume = 0.65f; pitch = 0.9f; }
            case DISCHARGE -> { sound = SoundEvents.GENERIC_EXPLODE.value(); volume = 0.6f; pitch = 1.6f; }
            case THREEFOLD -> { sound = SoundEvents.AMETHYST_BLOCK_RESONATE; pitch = 1.1f; }
            case TELEPORT -> { sound = SoundEvents.ENDERMAN_TELEPORT; volume = 0.35f; pitch = 1.3f; }
            case AURA -> {
                volume = 0.4f;
                if (skill == SkillDefinition.ICY_VEINS || skill == SkillDefinition.FREEZING_AURA) {
                    sound = SoundEvents.POWDER_SNOW_BREAK; pitch = 1.4f;
                } else if (skill == SkillDefinition.FLAME_CLOAK || skill == SkillDefinition.IMMOLATION_AURA) {
                    sound = SoundEvents.FIRECHARGE_USE; pitch = 1.3f;
                } else if (skill == SkillDefinition.SHOCK_AURA) {
                    sound = SoundEvents.COPPER_BULB_TURN_ON; pitch = 0.8f;
                } else if (skill == SkillDefinition.DODGE_AURA || skill == SkillDefinition.SWIFTNESS_AURA) {
                    sound = SoundEvents.BREEZE_JUMP; pitch = 1.4f;
                } else if (skill == SkillDefinition.RIPPING_AURA || skill == SkillDefinition.RAGING_AURA) {
                    sound = SoundEvents.PLAYER_ATTACK_STRONG; pitch = 0.8f;
                } else { sound = SoundEvents.ENCHANTMENT_TABLE_USE; pitch = 1.3f; }
            }
            case PASSIVE -> { return; }
            default -> throw new IllegalArgumentException("No release cue for " + skill.id());
        }
        play(player.level(), player.position(), sound, volume, pitch);
    }

    /** Collision feedback; this does not claim that the collided entity took damage. */
    static void impact(ServerLevel level, Vec3 position, SkillDefinition skill) {
        SoundEvent sound;
        float volume = 0.35f;
        float pitch = 1.4f;
        if (skill == SkillDefinition.FIRE_BOLT) { sound = SoundEvents.FIRE_EXTINGUISH; }
        else if (skill == SkillDefinition.ICE_BOLT) { sound = SoundEvents.GLASS_BREAK; }
        else if (skill == SkillDefinition.LIGHTNING_BOLT) {
            sound = SoundEvents.LIGHTNING_BOLT_IMPACT; volume = 0.3f; pitch = 1.8f;
        } else { sound = SoundEvents.AMETHYST_BLOCK_HIT; }
        play(level, position, sound, volume, pitch);
    }

    /** Aggregate positive-damage confirmation: callers emit once per strike, not once per victim. */
    static void meleeHit(ServerPlayer player) {
        play(player.level(), player.position(), SoundEvents.PLAYER_ATTACK_STRONG, 0.4f, 1.15f);
    }

    /** Native positional playback includes the caster and uses the Players volume slider and normal attenuation. */
    static void play(ServerLevel level, Vec3 position, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
