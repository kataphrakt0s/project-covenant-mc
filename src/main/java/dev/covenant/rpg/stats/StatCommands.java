package dev.covenant.rpg.stats;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import dev.covenant.Covenant;
import java.util.Comparator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only, self-targeted stat inspection and unsaved base adjustments on the logical server. */
public final class StatCommands {
    private static final Identifier OVERRIDE = Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "debug/base_override");
    private static final DynamicCommandExceptionType UNKNOWN_STAT = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.covenant.stats.unknown", id.toString()));
    private static final Dynamic3CommandExceptionType INVALID_VALUE = new Dynamic3CommandExceptionType(
            (id, min, max) -> Component.translatable("commands.covenant.stats.invalid_value", id, min, max));

    private StatCommands() {}

    public static void register(RegisterCommandsEvent event) {
        // Brigadier merges this branch into the existing root without touching class commands.
        event.getDispatcher().register(Commands.literal("covenant")
                .then(Commands.literal("stats")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> show(context.getSource()))
                        .then(Commands.literal("get")
                                .then(statArgument().executes(context -> inspect(context.getSource(),
                                        IdentifierArgument.getId(context, "stat")))))
                        .then(Commands.literal("set")
                                .then(statArgument().then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(context -> set(context.getSource(),
                                                IdentifierArgument.getId(context, "stat"),
                                                DoubleArgumentType.getDouble(context, "value"))))))
                        .then(Commands.literal("reset").executes(context -> reset(context.getSource())))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Identifier> statArgument() {
        return Commands.argument("stat", IdentifierArgument.id())
                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                        RpgStats.SUPPORTED.stream().map(attribute -> BuiltInRegistries.ATTRIBUTE.getKey(attribute.value())), builder));
    }

    private static AttributeInstance resolve(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var attribute = RpgStats.SUPPORTED.stream()
                .filter(candidate -> BuiltInRegistries.ATTRIBUTE.getKey(candidate.value()).equals(id))
                .findFirst().orElseThrow(() -> UNKNOWN_STAT.create(id));
        var instance = player.getAttribute(attribute);
        if (instance == null) { throw UNKNOWN_STAT.create(id); }
        return instance;
    }

    private static int show(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.translatable("commands.covenant.stats.header"), false);
        for (var attribute : RpgStats.SUPPORTED) {
            var id = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());
            source.sendSuccess(() -> Component.translatable("commands.covenant.stats.entry",
                    id.toString(), player.getAttributeValue(attribute)), false);
        }
        return RpgStats.SUPPORTED.size();
    }

    private static int inspect(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        var instance = resolve(source, id);
        source.sendSuccess(() -> Component.translatable("commands.covenant.stats.base",
                id.toString(), instance.getBaseValue()), false);
        var override = instance.getModifier(OVERRIDE);
        if (override != null) {
            source.sendSuccess(() -> Component.translatable("commands.covenant.stats.override",
                    instance.getBaseValue() + override.amount()), false);
        }
        var modifiers = instance.getModifiers().stream()
                .sorted(Comparator.comparing(AttributeModifier::operation).thenComparing(modifier -> modifier.id().toString()))
                .toList();
        if (modifiers.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.covenant.stats.no_modifiers"), false);
        }
        for (var modifier : modifiers) {
            source.sendSuccess(() -> Component.translatable("commands.covenant.stats.modifier",
                    modifier.id().toString(), modifier.operation().getSerializedName(), modifier.amount()), false);
        }
        source.sendSuccess(() -> Component.translatable("commands.covenant.stats.effective", instance.getValue()), false);
        return 1;
    }

    private static int set(CommandSourceStack source, Identifier id, double value) throws CommandSyntaxException {
        var instance = resolve(source, id);
        var range = (RangedAttribute) instance.getAttribute().value();
        double adjustment = value - instance.getBaseValue();
        if (!Double.isFinite(value) || !Double.isFinite(adjustment)
                || value < range.getMinValue() || value > range.getMaxValue()) {
            throw INVALID_VALUE.create(id.toString(), range.getMinValue(), range.getMaxValue());
        }
        // A flat offset substitutes the testing base before all native percentage modifiers.
        // Never setBaseValue: native bases and permanent modifiers are written to player saves.
        instance.removeModifier(OVERRIDE);
        instance.addTransientModifier(new AttributeModifier(OVERRIDE, adjustment, AttributeModifier.Operation.ADD_VALUE));
        source.sendSuccess(() -> Component.translatable("commands.covenant.stats.set",
                id.toString(), value, instance.getValue()), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        int cleared = 0;
        for (var attribute : RpgStats.SUPPORTED) {
            var instance = player.getAttribute(attribute);
            if (instance != null && instance.removeModifier(OVERRIDE)) { cleared++; }
        }
        int count = cleared;
        source.sendSuccess(() -> Component.translatable("commands.covenant.stats.reset", count), false);
        return cleared;
    }
}
