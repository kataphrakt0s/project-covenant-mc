package dev.covenant.rpg.skills;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.classes.PlayerClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Self-targeted inspection/activation; debug edits and class switching require operator permission. */
public final class SkillCommands {
    private SkillCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("covenant")
                .then(Commands.literal("resource").executes(c -> inspect(c.getSource()))
                        .then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(c -> set(c.getSource(), DoubleArgumentType.getDouble(c, "amount")))))
                        .then(Commands.literal("infinite").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(c -> infinite(c.getSource(), null))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(c -> infinite(c.getSource(), BoolArgumentType.getBool(c, "enabled"))))))
                .then(Commands.literal("skill").executes(c -> list(c.getSource()))
                        .then(Commands.literal("ignorecooldowns").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(c -> ignoreCooldowns(c.getSource(), null))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(c -> ignoreCooldowns(c.getSource(), BoolArgumentType.getBool(c, "enabled")))))
                        .then(Commands.literal("use").then(Commands.argument("skill", IdentifierArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(SkillDefinition.ALL.stream().map(SkillDefinition::id), b))
                                .executes(c -> {
                                    var skill = SkillDefinition.find(IdentifierArgument.getId(c, "skill"))
                                            .orElseThrow(() -> error("Unknown skill"));
                                    var result = Skills.activate(c.getSource().getPlayerOrException(), skill);
                                    c.getSource().sendSuccess(() -> result, false);
                                    return 1;
                                }))))
                .then(Commands.literal("class").then(Commands.literal("switch")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("class", IdentifierArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(PlayerClass.all().stream().map(PlayerClass::id), b))
                                .executes(c -> {
                                    var type = PlayerClass.find(IdentifierArgument.getId(c, "class")).orElseThrow(() -> error("Unknown class"));
                                    PlayerProfiles.switchClass(c.getSource().getPlayerOrException(), type);
                                    return inspect(c.getSource());
                                })))));
    }

    private static int inspect(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var selected = PlayerProfiles.getSelectedClass(player);
        for (var type : PlayerClass.all()) {
            source.sendSuccess(() -> Component.translatable("commands.covenant.resource.entry", type.displayName(),
                    ClassResources.name(type), ClassResources.current(player, type), ClassResources.maximum(player, type),
                    ClassResources.regeneration(player, type), selected.orElse(null) == type ? "active" : "inactive"), false);
        }
        source.sendSuccess(() -> Component.translatable("commands.covenant.resource.combat", ClassResources.inCombat(player)), false);
        source.sendSuccess(() -> infiniteFeedback(ClassResources.infinite(player)), false);
        return 1;
    }

    private static int set(CommandSourceStack source, double value) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var type = PlayerProfiles.getSelectedClass(player).orElseThrow(() -> error("Select a known class first"));
        if (!Double.isFinite(value)) { throw error("Amount must be finite and nonnegative"); }
        ClassResources.set(player, type, value);
        return inspect(source);
    }

    private static int infinite(CommandSourceStack source, Boolean enabled) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        boolean next = enabled == null ? !ClassResources.infinite(player) : enabled;
        ClassResources.setInfinite(player, next);
        source.sendSuccess(() -> infiniteFeedback(next), false);
        return 1;
    }

    private static Component infiniteFeedback(boolean enabled) {
        return Component.translatable("commands.covenant.resource.infinite." + (enabled ? "enabled" : "disabled"));
    }

    private static int ignoreCooldowns(CommandSourceStack source, Boolean enabled) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        boolean next = enabled == null ? !Skills.ignoresCooldowns(player) : enabled;
        Skills.setIgnoreCooldowns(player, next);
        source.sendSuccess(() -> cooldownFeedback(next), false);
        return 1;
    }

    private static Component cooldownFeedback(boolean enabled) {
        return Component.translatable("commands.covenant.skill.ignorecooldowns." + (enabled ? "enabled" : "disabled"));
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        source.sendSuccess(() -> cooldownFeedback(Skills.ignoresCooldowns(player)), false);
        for (var definition : SkillDefinition.ALL) {
            var skill = HunterPetData.resolve(player, WeaverSkillData.resolve(player, ReaverSkillData.resolve(player, definition)));
            source.sendSuccess(() -> Component.translatable("commands.covenant.skill.entry", skill.id().toString(),
                    skill.displayName(), skill.description(),
                    dev.covenant.rpg.stats.RpgStats.scaleSkillValue(player, dev.covenant.rpg.stats.RpgStats.RESOURCE_COST, skill.cost()),
                    dev.covenant.rpg.stats.RpgStats.scaleSkillValue(player, dev.covenant.rpg.stats.RpgStats.COOLDOWN, skill.cooldownTicks()) / 20,
                    dev.covenant.rpg.stats.RpgStats.scaleSkillValue(player, dev.covenant.rpg.stats.RpgStats.CAST_TIME, skill.castTicks()) / 20,
                    Skills.cooldownRemaining(player, skill) / 20.0), false);
        }
        return 1;
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}
