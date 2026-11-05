package dev.covenant.rpg.equipment;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.covenant.rpg.stats.RpgStats;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only test pages on the main-hand stack, using the normal persistent component and APIs. */
public final class AffixCommands {
    private static final String CATEGORY_TAG_PREFIX = "affix_compatible/";
    private static final SimpleCommandExceptionType EMPTY_HAND = error("empty_hand");
    private static final SimpleCommandExceptionType STACKED = error("stacked");
    private static final SimpleCommandExceptionType ALREADY_PAGED = error("already_paged");
    private static final SimpleCommandExceptionType INVALID_VALUE = error("invalid_value");
    private static final DynamicCommandExceptionType UNKNOWN_CATEGORY = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.covenant.affix.unknown_category", id.toString()));
    private static final DynamicCommandExceptionType INCOMPATIBLE = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.covenant.affix.incompatible", id.toString()));
    private static final DynamicCommandExceptionType UNKNOWN_STAT = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.covenant.affix.unknown_stat", id.toString()));

    private AffixCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("covenant")
                .then(Commands.literal("affix")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("inspect").executes(context -> inspect(context.getSource())))
                        .then(Commands.literal("apply")
                                .then(Commands.argument("category", IdentifierArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(categories(), builder))
                                        .then(Commands.argument("stat", IdentifierArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        RpgStats.SUPPORTED.stream().map(attribute ->
                                                                BuiltInRegistries.ATTRIBUTE.getKey(attribute.value())), builder))
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                        .executes(context -> apply(context.getSource(),
                                                                IdentifierArgument.getId(context, "category"),
                                                                IdentifierArgument.getId(context, "stat"),
                                                                DoubleArgumentType.getDouble(context, "value")))))))
                        .then(Commands.literal("remove").executes(context -> remove(context.getSource())))
                        .then(Commands.literal("clear").executes(context -> remove(context.getSource())))));
    }

    // Loaded compatibility tags are the category catalog, including datapack-provided categories.
    private static Stream<Identifier> categories() {
        return BuiltInRegistries.ITEM.getTags().map(tag -> tag.key().location())
                .filter(id -> id.getPath().startsWith(CATEGORY_TAG_PREFIX))
                .filter(id -> id.getPath().length() > CATEGORY_TAG_PREFIX.length())
                .map(id -> id.withPath(id.getPath().substring(CATEGORY_TAG_PREFIX.length())));
    }

    private static ItemStack held(ServerPlayer player) throws CommandSyntaxException {
        var stack = player.getMainHandItem();
        if (stack.isEmpty()) { throw EMPTY_HAND.create(); }
        return stack;
    }

    private static int inspect(CommandSourceStack source) throws CommandSyntaxException {
        var stack = held(source.getPlayerOrException());
        var page = AffixPages.get(stack);
        if (page.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.covenant.affix.no_page"), false);
            return 0;
        }
        var data = page.orElseThrow();
        source.sendSuccess(() -> Component.translatable("commands.covenant.affix.page",
                data.category().id().toString(), data.identity().toString()), false);
        showModifiers(source, "base", data.modifiers());
        for (int i = 0; i < data.affixes().size(); i++) {
            var affix = data.affixes().get(i);
            showModifiers(source, "affix/" + i + " (" + affix.id() + ")", affix.modifiers());
        }
        if (data.modifiers().isEmpty() && data.affixes().stream().allMatch(affix -> affix.modifiers().isEmpty())) {
            source.sendSuccess(() -> Component.translatable("commands.covenant.affix.no_modifiers"), false);
        }
        source.sendSuccess(() -> Component.translatable("commands.covenant.affix.progression",
                data.progression().upgrades(), data.progression().experience()), false);
        return 1;
    }

    private static void showModifiers(CommandSourceStack source, String group, List<RpgModifier> modifiers) {
        for (int i = 0; i < modifiers.size(); i++) {
            var modifier = modifiers.get(i);
            String entry = group + "/" + i;
            source.sendSuccess(() -> Component.translatable("commands.covenant.affix.modifier", entry,
                    modifier.stat().toString(), modifier.operation().getSerializedName(), modifier.amount()), false);
        }
    }

    private static int apply(CommandSourceStack source, Identifier categoryId, Identifier statId, double value)
            throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var stack = held(player);
        if (categories().noneMatch(categoryId::equals)) { throw UNKNOWN_CATEGORY.create(categoryId); }
        var category = new EquipmentCategory(categoryId);
        if (!category.supports(stack)) { throw INCOMPATIBLE.create(categoryId); }
        if (RpgStats.SUPPORTED.stream().noneMatch(attribute -> BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()).equals(statId))) {
            throw UNKNOWN_STAT.create(statId);
        }
        if (!Double.isFinite(value)) { throw INVALID_VALUE.create(); }
        if (stack.getCount() != 1) { throw STACKED.create(); }
        if (AffixPages.get(stack).isPresent()) { throw ALREADY_PAGED.create(); }
        AffixPages.attach(player.level(), stack, category,
                List.of(new RpgModifier(statId, RpgModifier.Operation.ADD, value)), List.of());
        changed(player);
        source.sendSuccess(() -> Component.translatable("commands.covenant.affix.applied",
                categoryId.toString(), statId.toString(), value), false);
        return 1;
    }

    private static int remove(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var stack = held(player);
        if (!AffixPages.remove(player.level(), stack)) {
            source.sendSuccess(() -> Component.translatable("commands.covenant.affix.no_page"), false);
            return 0;
        }
        changed(player);
        source.sendSuccess(() -> Component.translatable("commands.covenant.affix.removed"), false);
        return 1;
    }

    private static void changed(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) { player.containerMenu.broadcastChanges(); }
    }

    private static SimpleCommandExceptionType error(String key) {
        return new SimpleCommandExceptionType(Component.translatable("commands.covenant.affix." + key));
    }
}
