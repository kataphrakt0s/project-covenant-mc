package dev.covenant.rpg.equipment;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.covenant.rpg.RpgData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only, persistent test equipment edits. No crafting costs, profession gates, or XP. */
public final class AffixCommands {
    private AffixCommands() {}

    public static void register(RegisterCommandsEvent event) {
        var set = Commands.argument("family", IdentifierArgument.id()).suggests(AffixCommands::families)
                .then(Commands.argument("rarity", IdentifierArgument.id()).suggests(AffixCommands::rarities)
                        .then(Commands.argument("tier", IntegerArgumentType.integer(1))
                                .then(Commands.literal("random").executes(c -> set(c, "random")))
                                .then(Commands.literal("min").executes(c -> set(c, "min")))
                                .then(Commands.literal("max").executes(c -> set(c, "max")))));
        event.getDispatcher().register(Commands.literal("covenant")
                .then(Commands.literal("affix")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("set").then(set))
                        .then(Commands.literal("rarity")
                                .then(Commands.argument("rarity", IdentifierArgument.id()).suggests(AffixCommands::rarities)
                                        .executes(c -> rarity(c.getSource(), IdentifierArgument.getId(c, "rarity")))))
                        .then(Commands.literal("potential")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(c -> potential(c.getSource(), IntegerArgumentType.getInteger(c, "amount"))))
                                .then(Commands.literal("full").executes(c -> potential(c.getSource(), null))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("family", IdentifierArgument.id()).suggests(AffixCommands::heldFamilies)
                                        .executes(c -> remove(c.getSource(), IdentifierArgument.getId(c, "family")))))
                        .then(Commands.literal("clear").executes(c -> clear(c.getSource(), false)))
                        .then(Commands.literal("reset").executes(c -> clear(c.getSource(), true)))
                        .then(Commands.literal("component")
                                .then(Commands.argument("family", IdentifierArgument.id()).suggests(AffixCommands::families)
                                        .then(Commands.argument("rarity", IdentifierArgument.id())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                                                        data(c.getSource()).rarities().stream()
                                                                .filter(r -> !r.id().equals(ItemizationData.id("common")))
                                                                .map(ItemizationData.Rarity::id), b))
                                                .executes(c -> component(c, 1))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                        .executes(c -> component(c, IntegerArgumentType.getInteger(c, "count")))))))));
    }

    private static CompletableFuture<Suggestions> families(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(data(context.getSource()).families().stream().map(ItemizationData.Family::id), builder);
    }

    private static CompletableFuture<Suggestions> rarities(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(data(context.getSource()).rarities().stream().map(ItemizationData.Rarity::id), builder);
    }

    private static CompletableFuture<Suggestions> heldFamilies(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        var player = context.getSource().getPlayer();
        var page = player == null ? null : AffixPages.get(player.getMainHandItem()).orElse(null);
        return SharedSuggestionProvider.suggestResource(page == null ? List.<Identifier>of().stream()
                : page.affixes().stream().map(Affix::id), builder);
    }

    private static ItemizationData.Definitions data(CommandSourceStack source) {
        return ItemizationData.get(source.getServer());
    }

    private static ItemStack held(ServerPlayer player) throws CommandSyntaxException {
        var stack = player.getMainHandItem();
        if (stack.isEmpty()) { throw error("empty_hand"); }
        if (stack.getCount() != 1) { throw error("stacked"); }
        return stack;
    }

    private static EquipmentCategory category(ItemizationData.Definitions data, ItemStack stack) throws CommandSyntaxException {
        var selected = data.category(stack).orElseThrow(() -> error("ineligible"));
        var page = AffixPages.get(stack).orElse(null);
        return page == null ? selected : page.category();
    }

    private static ItemizationData.Rarity rarity(ItemizationData.Definitions data, Identifier id) throws CommandSyntaxException {
        var rarity = data.rarity(id);
        if (rarity == null) { throw error("unknown_rarity", id); }
        return rarity;
    }

    private static ItemizationData.Family family(ItemizationData.Definitions data, Identifier id) throws CommandSyntaxException {
        var family = data.family(id);
        if (family == null) { throw error("unknown_family", id); }
        return family;
    }

    /** Saved roll values remain snapshots across reloads; validate structure, not old roll ranges. */
    private static void validate(ItemizationData.Definitions data, ItemStack stack, EquipmentCategory category,
            ItemizationData.Rarity equipment, int potential, List<Affix> affixes) throws CommandSyntaxException {
        if (potential > equipment.potential()) { throw error("potential_limit", equipment.potential()); }
        int prefixes = 0;
        int suffixes = 0;
        var seen = new HashSet<Identifier>();
        for (var affix : affixes) {
            var family = family(data, affix.id());
            var rarity = rarity(data, affix.rarity());
            if (!family.supports(stack, category)) { throw error("incompatible", family.id()); }
            if (!seen.add(family.id())) { throw error("duplicate", family.id()); }
            if (rarity.order() > equipment.order()) { throw error("rarity_limit", equipment.id()); }
            if (affix.tier() > equipment.tier() || affix.tier() > family.ranges().size()) {
                throw error("tier_limit", equipment.tier());
            }
            if (family.prefix()) { prefixes++; } else { suffixes++; }
        }
        if (prefixes > equipment.prefixes() || suffixes > equipment.suffixes() || affixes.size() > AffixPage.MAX_AFFIXES) {
            throw error("capacity", equipment.prefixes(), equipment.suffixes());
        }
    }

    private static int set(CommandContext<CommandSourceStack> context, String mode) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrException();
        var stack = held(player);
        var data = data(source);
        var category = category(data, stack);
        var family = family(data, IdentifierArgument.getId(context, "family"));
        var rarity = rarity(data, IdentifierArgument.getId(context, "rarity"));
        int tier = IntegerArgumentType.getInteger(context, "tier");
        var page = AffixPages.get(stack).orElse(null);
        var quality = stack.get(Workbench.QUALITY.get());
        var equipment = quality == null ? rarity : rarity(data, quality.rarity());
        int potential = quality == null ? equipment.potential() : quality.potential();
        var affixes = new ArrayList<>(page == null ? List.<Affix>of() : page.affixes());
        int index = -1;
        for (int i = 0; i < affixes.size(); i++) { if (affixes.get(i).id().equals(family.id())) { index = i; break; } }
        // Validate before randomness, initialization, or changes to the held item.
        var candidate = new Affix(family.id(), rarity.id(), tier, List.of());
        if (index < 0) { index = affixes.size(); affixes.add(candidate); } else { affixes.set(index, candidate); }
        validate(data, stack, category, equipment, potential, affixes);
        Affix rolled;
        if (mode.equals("random")) { rolled = AffixedLoot.roll(family, rarity, tier, player.getRandom()); }
        else {
            var range = family.ranges().get(tier - 1);
            double magnitude = (mode.equals("max") ? range.max() : range.min()) * rarity.magnitude();
            rolled = new Affix(family.id(), rarity.id(), tier, List.of(new RpgModifier(family.stat(), family.operation(),
                    magnitude * (family.reduction() ? -1 : 1))));
        }
        affixes.set(index, rolled);
        write(player, stack, category, affixes, new EquipmentQuality(equipment.id(), potential));
        return success(source, "set", family.id(), rarity.id(), tier, mode);
    }

    private static int rarity(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var stack = held(player);
        var data = data(source);
        var category = category(data, stack);
        var rarity = rarity(data, id);
        var quality = stack.get(Workbench.QUALITY.get());
        int potential = quality == null ? rarity.potential() : quality.potential();
        var affixes = AffixPages.get(stack).map(AffixPage::affixes).orElse(List.of());
        validate(data, stack, category, rarity, potential, affixes);
        write(player, stack, category, affixes, new EquipmentQuality(id, potential));
        return success(source, "rarity_set", id, potential);
    }

    private static int potential(CommandSourceStack source, Integer amount) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var stack = held(player);
        var quality = stack.get(Workbench.QUALITY.get());
        if (quality == null) { throw error("uninitialized"); }
        int maximum = rarity(data(source), quality.rarity()).potential();
        int value = amount == null ? maximum : amount;
        if (value > maximum) { throw error("potential_limit", maximum); }
        stack.set(Workbench.QUALITY.get(), new EquipmentQuality(quality.rarity(), value));
        changed(player);
        return success(source, "potential_set", value);
    }

    private static int remove(CommandSourceStack source, Identifier family) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var stack = held(player);
        var page = AffixPages.get(stack).orElse(null);
        if (page == null || page.affixes().stream().noneMatch(a -> a.id().equals(family))) { throw error("missing_affix", family); }
        // Removal also permits cleaning up definitions removed by a datapack reload.
        AffixPages.update(player.level(), stack, page.modifiers(), page.affixes().stream()
                .filter(a -> !a.id().equals(family)).toList(), page.progression());
        changed(player);
        return success(source, "removed", family);
    }

    private static int clear(CommandSourceStack source, boolean reset) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var stack = held(player);
        var page = AffixPages.get(stack).orElse(null);
        if (reset) {
            boolean changed = AffixPages.remove(player.level(), stack);
            changed |= stack.remove(Workbench.QUALITY.get()) != null;
            if (!changed) { success(source, "unchanged"); return 0; }
        } else {
            if (page == null || (page.affixes().isEmpty() && page.modifiers().isEmpty())) { success(source, "unchanged"); return 0; }
            AffixPages.update(player.level(), stack, List.of(), List.of(), page.progression());
        }
        changed(player);
        return success(source, reset ? "reset" : "cleared");
    }

    private static int component(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrException();
        var data = data(source);
        var family = family(data, IdentifierArgument.getId(context, "family"));
        var rarity = rarity(data, IdentifierArgument.getId(context, "rarity"));
        if (rarity.id().equals(ItemizationData.id("common"))) { throw error("common_component"); }
        // Native insertion merges exact component identities and drops overflow by the player.
        player.getInventory().placeItemBackInInventory(Workbench.material(new AffixMaterial(family.id(), rarity.id()), count));
        changed(player);
        return success(source, "component_given", count, family.id(), rarity.id());
    }

    /** Prepare the complete page on a copy before committing its components to the original stack. */
    private static void write(ServerPlayer player, ItemStack stack, EquipmentCategory category, List<Affix> affixes, EquipmentQuality quality) {
        var result = stack.copy();
        var page = AffixPages.get(stack).orElse(null);
        if (page == null) { AffixPages.attach(player.level(), result, category, List.of(), affixes); }
        else { AffixPages.update(player.level(), result, page.modifiers(), affixes, page.progression()); }
        stack.set(RpgData.AFFIX_PAGE.get(), result.get(RpgData.AFFIX_PAGE.get()));
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(Workbench.QUALITY.get(), quality);
        changed(player);
    }

    private static void changed(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) { player.containerMenu.broadcastChanges(); }
    }

    private static int success(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> message(key, args), false);
        return 1;
    }

    private static CommandSyntaxException error(String key, Object... args) {
        return new SimpleCommandExceptionType(message(key, args)).create();
    }

    private static Component message(String key, Object... args) {
        var textArgs = java.util.Arrays.stream(args).map(value -> value instanceof Identifier id ? id.toString() : value).toArray();
        return Component.translatable("commands.covenant.affix." + key, textArgs);
    }
}
