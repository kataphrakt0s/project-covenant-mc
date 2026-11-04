package dev.covenant.rpg.classes;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.covenant.rpg.PlayerProfiles;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Self-service, logical-server class selection. No operator permission or target-player argument. */
public final class ClassCommands {
    private static final DynamicCommandExceptionType UNKNOWN_CLASS = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.covenant.class.unknown", id.toString()));
    private static final SimpleCommandExceptionType ALREADY_SELECTED = new SimpleCommandExceptionType(
            Component.translatable("commands.covenant.class.already_selected"));

    private ClassCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("covenant")
                .then(Commands.literal("class")
                        .executes(context -> show(context.getSource()))
                        .then(Commands.literal("select")
                                .then(Commands.argument("class", IdentifierArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                PlayerClass.all().stream().map(PlayerClass::id), builder))
                                        .executes(context -> select(context.getSource(),
                                                IdentifierArgument.getId(context, "class")))))));
    }

    private static int show(CommandSourceStack source) throws CommandSyntaxException {
        var profile = PlayerProfiles.get(source.getPlayerOrException());
        if (profile.selectedClass().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.covenant.class.none"), false);
        } else {
            var id = profile.selectedClass().orElseThrow();
            Component name = PlayerClass.find(id).map(PlayerClass::displayName)
                    .orElseGet(() -> Component.literal(id.toString()));
            source.sendSuccess(() -> Component.translatable("commands.covenant.class.current", name), false);
        }
        for (var definition : PlayerClass.all()) {
            source.sendSuccess(() -> Component.translatable("commands.covenant.class.entry",
                    definition.id().toString(), definition.displayName(), definition.description()), false);
        }
        source.sendSuccess(() -> Component.translatable("commands.covenant.class.usage"), false);
        return 1;
    }

    private static int select(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var definition = PlayerClass.find(id).orElseThrow(() -> UNKNOWN_CLASS.create(id));
        if (PlayerProfiles.get(player).created()) {
            throw ALREADY_SELECTED.create();
        }
        PlayerProfiles.selectClass(player, definition);
        source.sendSuccess(() -> Component.translatable("commands.covenant.class.selected",
                definition.displayName()), false);
        return 1;
    }
}
