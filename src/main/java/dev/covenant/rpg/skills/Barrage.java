package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;

/** Server-owned held channel. Input renews only a short lease; every arrow validates and spends here. */
public final class Barrage {
    private static final Identifier SLOW = Identifier.fromNamespaceAndPath("covenant", "barrage_draw");
    private static final AttributeModifier DRAW = new AttributeModifier(SLOW, -.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    static final class Channel {
        final SkillDefinition skill;
        final double interval;
        long heldUntil;
        int slot = -1;
        long sequence;
        Channel(SkillDefinition skill, double rate, long now) { this.skill = skill; interval = 20 / rate; heldUntil = now + 15; }
    }
    private Barrage() {}
    public static boolean active(ServerPlayer player) { return player.getData(RpgData.SKILL_RUNTIME).channel != null; }

    /** Called behind Skills.activate's preflight; firing and reentrant mod hooks retain its execution guard. */
    static net.minecraft.network.chat.Component start(ServerPlayer player, SkillDefinition skill) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        if (runtime.channel != null) { return Skills.message("busy"); }
        runtime.channel = new Channel(skill, CombatSkillData.get(player.level().getServer()).barrage().rate(), ClassResources.now(player));
        player.stopUsingItem(); // Cancel an ordinary draw without releasing its arrow.
        player.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(DRAW);
        runtime.nextActivation = ClassResources.now(player) + 1;
        tick(player);
        return runtime.channel == null ? Skills.message("unavailable") : Skills.message("used", skill.displayName(), skill.cost());
    }
    public static void stop(ServerPlayer player) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        if (runtime.channel == null) { return; }
        runtime.channel = null;
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SLOW);
        player.setData(RpgData.BOW_DRAW_UNTIL, 0L);
    }
    /** Stop/lease packets never activate a skill. Sequence and slot prevent stale releases affecting a new channel. */
    static void input(ServerPlayer player, SkillNetworking.ChannelInput input) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        if (input.start()) {
            if (!input.held() || input.sequence() <= runtime.channelInputSequence || !SkillLoadout.validSlot(input.slot())) { return; }
            runtime.channelInputSequence = input.sequence();
            var id = player.getData(RpgData.SKILL_LOADOUT).bindings().get(input.slot());
            if (!SkillDefinition.BARRAGE.id().equals(id)) { return; }
            var result = SkillLoadouts.activateSlot(player, input.slot());
            if (runtime.channel != null && runtime.channel.slot == -1) {
                runtime.channel.slot = input.slot(); runtime.channel.sequence = input.sequence();
            }
            player.sendOverlayMessage(result);
        } else {
            var channel = runtime.channel;
            if (channel == null || channel.sequence != input.sequence() || channel.slot != input.slot()) { return; }
            if (!input.held()) { stop(player); }
            else { channel.heldUntil = ClassResources.now(player) + 15; }
        }
    }
    static void tick(ServerPlayer player) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME); var channel = runtime.channel;
        if (channel == null) { return; }
        long now = ClassResources.now(player);
        var bow = player.getMainHandItem();
        if (!player.isAlive() || player.isSpectator() || !SkillControls.canAct(player)
                || dev.covenant.rpg.PlayerProfiles.getSelectedClass(player).orElse(null) != PlayerClass.STALKER
                || !player.getData(RpgData.SKILL_LOADOUT).learned().contains(SkillDefinition.BARRAGE.id())
                || (channel.slot >= 0 && !SkillDefinition.BARRAGE.id().equals(player.getData(RpgData.SKILL_LOADOUT).bindings().get(channel.slot)))
                || now >= channel.heldUntil || !(bow.getItem() instanceof BowItem)
                || !(player.getProjectile(bow).getItem() instanceof ArrowItem) || player.isUsingItem()) { stop(player); return; }
        double cost = RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, channel.skill.cost());
        if (!ClassResources.canAfford(player, cost)) { stop(player); return; }
        if (now + 1e-7 < runtime.nextBarrageShot || runtime.executing) { return; }
        runtime.executing = true;
        try {
            var shot = BowSkills.prepare(player);
            if (shot == null || runtime.channel != channel || !player.isAlive() || !SkillControls.canAct(player)
                    || player.getMainHandItem() != shot.bow() || !ClassResources.consume(player, cost)) { stop(player); return; }
            // Fractional deadlines yield 7/7/6 tick intervals at 3/s. Never catch up missed arrows.
            if (now - runtime.nextBarrageShot > 1) { runtime.nextBarrageShot = now; }
            runtime.nextBarrageShot += channel.interval;
            BowSkills.fire(player, channel.skill, shot);
        } finally { runtime.executing = false; }
    }
}
