package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** One unsaved cast and its start-time resource/timer snapshot. Completion still revalidates eligibility. */
record PendingCast(SkillDefinition skill, PlayerClass playerClass, long endsAt, double cost, long cooldown) {
    private static final Identifier AIMED_SLOW = Identifier.fromNamespaceAndPath("covenant", "aimed_shot_slow");
    private static final AttributeModifier AIMED_MOVEMENT = new AttributeModifier(
            AIMED_SLOW, -0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    void begin(ServerPlayer player, long ticks) {
        SkillSounds.castStart(player, skill);
        if (skill == SkillDefinition.AIMED_SHOT) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(AIMED_MOVEMENT);
            SkillVisuals.bowDraw(player, ticks);
        } else { player.swing(InteractionHand.MAIN_HAND, true); }
    }

    /** Idempotent cleanup shared by completion and every cancellation reason. */
    void finish(ServerPlayer player) {
        if (skill == SkillDefinition.AIMED_SHOT) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(AIMED_SLOW);
            player.setData(RpgData.BOW_DRAW_UNTIL, 0L);
        }
    }
}
