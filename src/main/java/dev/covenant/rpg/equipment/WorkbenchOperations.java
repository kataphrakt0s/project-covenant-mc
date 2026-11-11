package dev.covenant.rpg.equipment;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.*;
import dev.covenant.rpg.skills.ClassResources;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

/** Stateless server transactions. Previews never roll, spend costs, initialize rarity, or award XP. */
public final class WorkbenchOperations {
    public static final net.minecraft.resources.Identifier CRAFTING = ItemizationData.id("crafting");
    public static final net.minecraft.resources.Identifier BASIC = ItemizationData.id("crafting/basic_affix_crafting");
    public static final net.minecraft.resources.Identifier SALVAGING = ItemizationData.id("crafting/salvaging");
    public static final net.minecraft.resources.Identifier RECOVERY = ItemizationData.id("crafting/recovery");
    public static final net.minecraft.resources.Identifier FOCUSED = ItemizationData.id("crafting/focused_recovery");
    public enum Operation { ADD, UPGRADE, REROLL, REMOVE }
    public record Plan(String error, ItemStack ingredient, int cost, int potential, int tier,
            ItemizationData.Family family, ItemizationData.Rarity affixRarity, EquipmentCategory category, EquipmentQuality quality) {
        public boolean valid() { return error.isEmpty(); }
    }
    private WorkbenchOperations() {}
    public static ProfessionState.Track track(ServerPlayer player) { return player.getData(RpgData.PROFESSIONS).tracks().get(CRAFTING); }
    private static int rank(ServerPlayer player, net.minecraft.resources.Identifier id) {
        var node = ProfessionData.snapshot(player.level().getServer()).catalogue().abilities().get(id);
        return node == null || !node.implemented() ? 0 : Math.min(track(player).rank(id), node.ranks().size());
    }
    public static int unlockedTier(ServerPlayer player, ItemizationData.Definitions data) {
        int tier = 0; for (int level : data.settings().tierLevels()) { if (track(player).level() < level) { break; } tier++; } return tier;
    }
    private static boolean available(ServerPlayer player, WorkbenchBlockEntity station) {
        return player.isAlive() && !player.isSpectator() && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer)
                && !station.isRemoved() && station.getLevel() == player.level() && station.stillValid(player);
    }
    private static Plan fail(String error) { return new Plan(error, ItemStack.EMPTY, 0, 0, 0, null, null, null, null); }
    public static Plan preview(ServerPlayer player, WorkbenchBlockEntity station, Operation operation, int familyIndex, int rarityIndex, int targetIndex) {
        ClassResources.requireServer(player);
        if (!available(player, station)) { return fail("Workbench unavailable."); }
        if (rank(player, BASIC) == 0) { return fail("Purchase Basic Affix Crafting at Crafting Level 1."); }
        var data = ItemizationData.get(player.level().getServer()); var stack = station.getItem(0);
        var page = AffixPages.get(stack).orElse(null); var quality = stack.get(Workbench.QUALITY.get());
        var category = page == null ? data.category(stack).orElse(null) : page.category();
        if (stack.getCount() != 1 || category == null || data.category(stack).isEmpty()) { return fail("Insert one eligible equipment item."); }
        if (page != null && quality == null) { return fail("This debug Affix Page has no crafting rarity."); }
        ItemizationData.Family family; ItemizationData.Rarity rarity; int tier;
        if (operation == Operation.ADD) {
            if (familyIndex < 0 || familyIndex >= data.families().size() || rarityIndex < 0 || rarityIndex >= data.rarities().size()) { return fail("Invalid affix selection."); }
            family = data.families().get(familyIndex); rarity = data.rarities().get(rarityIndex); tier = 1;
            if (!family.supports(stack, category)) { return fail("This affix cannot be added to this equipment."); }
            if (page != null && page.affixes().stream().anyMatch(a -> a.id().equals(family.id()))) { return fail("This item already has that affix family."); }
        } else {
            if (page == null || targetIndex < 0 || targetIndex >= page.affixes().size()) { return fail("Select an existing affix."); }
            var target = page.affixes().get(targetIndex); family = data.family(target.id()); rarity = data.rarity(target.rarity());
            tier = target.tier() + (operation == Operation.UPGRADE ? 1 : 0);
            if (family == null || rarity == null) { return fail("This affix definition is unavailable."); }
        }
        var equipmentRarity = quality == null ? rarity : data.rarity(quality.rarity());
        if (equipmentRarity == null) { return fail("This equipment rarity is unavailable."); }
        if (operation == Operation.ADD && rarity.order() > equipmentRarity.order()) { return fail("Affix rarity exceeds the equipment's rarity."); }
        if (operation != Operation.REMOVE && (tier > equipmentRarity.tier() || tier > unlockedTier(player, data))) { return fail("Numerical tier exceeds equipment or Crafting level access."); }
        if (operation == Operation.ADD) {
            long occupied = page == null ? 0 : page.affixes().stream().filter(a -> data.family(a.id()) == null
                    || data.family(a.id()).prefix() == family.prefix()).count();
            if (occupied >= (family.prefix() ? equipmentRarity.prefixes() : equipmentRarity.suffixes())) { return fail("No free " + (family.prefix() ? "prefix" : "suffix") + " slots."); }
        }
        int potential = quality == null ? equipmentRarity.potential() : quality.potential();
        int potentialCost = data.settings().potentialCost();
        boolean common = rarity.id().equals(ItemizationData.id("common"));
        int cost = operation == Operation.REMOVE ? 0 : common ?
                (operation == Operation.REROLL ? data.settings().commonReroll() : data.settings().commonAdd()) :
                (operation == Operation.REROLL ? rarity.rerollCost() : rarity.addCost());
        var ingredient = operation == Operation.REMOVE ? ItemStack.EMPTY : common ? new ItemStack(Items.LAPIS_LAZULI)
                : Workbench.material(new AffixMaterial(family.id(), rarity.id()), 1);
        String error = potential < potentialCost ? "Not enough Crafting Potential (requires " + potentialCost + ")."
                : count(player, ingredient) < cost ? "Missing ingredients: " + cost + " " + ingredient.getHoverName().getString() + "." : "";
        return new Plan(error, ingredient, cost, potentialCost, tier, family, rarity, category,
                new EquipmentQuality(equipmentRarity.id(), potential));
    }
    public static boolean craft(ServerPlayer player, WorkbenchBlockEntity station, Operation operation, int family, int rarity, int target) {
        var plan = preview(player, station, operation, family, rarity, target); if (!plan.valid()) { return false; }
        var stack = station.getItem(0); var page = AffixPages.get(stack).orElse(null);
        var affixes = new ArrayList<Affix>(page == null ? List.of() : page.affixes());
        if (operation == Operation.REMOVE) { affixes.remove(target); }
        else {
            var roll = AffixedLoot.roll(plan.family, plan.affixRarity, plan.tier, player.getRandom());
            if (operation == Operation.ADD) { affixes.add(roll); } else { affixes.set(target, roll); }
        }
        // Complete changes on a copy first. Native component validation cannot leave partially spent costs.
        var result = stack.copy();
        if (page == null) { AffixPages.attach(player.level(), result, plan.category, List.of(), affixes); }
        else { AffixPages.update(player.level(), result, page.modifiers(), affixes, page.progression()); }
        result.set(Workbench.QUALITY.get(), new EquipmentQuality(plan.quality.rarity(), plan.quality.potential() - plan.potential));
        spend(player, plan.ingredient, plan.cost); station.setItem(0, result);
        Progression.awardProfession(player, player.level(), CRAFTING, plan.affixRarity.operationXp()); return true;
    }
    private static int count(ServerPlayer player, ItemStack ingredient) {
        int count = 0; for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) { var item = player.getInventory().getItem(slot);
            if (matches(item, ingredient)) { count += item.getCount(); } } return count;
    }
    private static boolean matches(ItemStack stack, ItemStack ingredient) {
        if (ingredient.isEmpty() || !stack.is(ingredient.getItem())) { return false; }
        var identity = ingredient.get(Workbench.MATERIAL_ID.get());
        return identity == null ? !stack.has(Workbench.MATERIAL_ID.get()) : identity.equals(stack.get(Workbench.MATERIAL_ID.get()));
    }
    private static void spend(ServerPlayer player, ItemStack ingredient, int amount) {
        for (int slot = 0; slot < player.getInventory().getContainerSize() && amount > 0; slot++) { var stack = player.getInventory().getItem(slot);
            if (matches(stack, ingredient)) { int take = Math.min(amount, stack.getCount()); stack.shrink(take); amount -= take; } }
        player.getInventory().setChanged();
    }
    public static double genericChance(ServerPlayer player, ItemizationData.Definitions data) {
        int rank = rank(player, RECOVERY); return rank == 0 ? data.settings().genericChance() : data.settings().recovery().get(Math.min(rank, data.settings().recovery().size()) - 1);
    }
    public static double componentRate(ServerPlayer player, ItemizationData.Definitions data) {
        int rank = rank(player, FOCUSED); return data.settings().componentRate() + (rank == 0 ? 0 : data.settings().focusedRecovery().get(Math.min(rank, data.settings().focusedRecovery().size()) - 1));
    }
    /** Possible outputs, with maximum quantities for capacity checks; never consumes random numbers. */
    public static List<ItemStack> maximumOutputs(ServerPlayer player, ItemStack input, ItemizationData.Definitions data) {
        var result = new ArrayList<ItemStack>(); var refund = data.refund(input);
        if (refund != null && genericChance(player, data) > 0) { result.add(new ItemStack(BuiltInRegistries.ITEM.getValue(refund.output()), refund.count())); }
        int count = (int) Math.ceil(componentRate(player, data)); var page = AffixPages.get(input).orElse(null);
        if (page != null && count > 0) { for (var affix : page.affixes()) {
            if (!affix.rarity().equals(ItemizationData.id("common")) && data.family(affix.id()) != null && data.rarity(affix.rarity()) != null) {
                result.add(Workbench.material(new AffixMaterial(affix.id(), affix.rarity()), count));
            }
        } }
        return result;
    }
    /** Inserts into a COPY for maximum checks, or the caller's transactional result copy for commit. */
    public static boolean insert(List<ItemStack> outputs, List<ItemStack> rewards) {
        for (var reward : rewards) { var remaining = reward.copy();
            for (var stack : outputs) { if (ItemStack.isSameItemSameComponents(stack, remaining)) {
                int transfer = Math.min(remaining.getCount(), stack.getMaxStackSize() - stack.getCount());
                stack.grow(transfer); remaining.shrink(transfer); if (remaining.isEmpty()) { break; }
            } }
            for (int slot = 0; slot < outputs.size() && !remaining.isEmpty(); slot++) { if (outputs.get(slot).isEmpty()) {
                int transfer = Math.min(remaining.getCount(), remaining.getMaxStackSize()); outputs.set(slot, remaining.copyWithCount(transfer)); remaining.shrink(transfer);
            } }
            if (!remaining.isEmpty()) { return false; }
        }
        return true;
    }
    public static String salvageAccess(ServerPlayer player, WorkbenchBlockEntity station) {
        return !available(player, station) ? "Workbench unavailable." : rank(player, SALVAGING) == 0 ? "Purchase Salvaging at Crafting Level 5." : "";
    }
    public static int salvage(ServerPlayer player, WorkbenchBlockEntity station) {
        ClassResources.requireServer(player); if (!salvageAccess(player, station).isEmpty()) { return 0; }
        var data = ItemizationData.get(player.level().getServer()); int processed = 0;
        for (int slot = 1; slot < 11; slot++) {
            var input = station.getItem(slot); if (input.isEmpty()) { continue; }
            if (input.getCount() != 1 || data.category(input).isEmpty()) { break; }
            var outputs = outputCopy(station); var maximum = maximumOutputs(player, input, data);
            if (!insert(outputs, maximum)) { break; } // Stop BEFORE rolling, even if actual outputs might fit.
            outputs = outputCopy(station); var actual = new ArrayList<ItemStack>(); var refund = data.refund(input);
            if (refund != null && player.getRandom().nextDouble() < genericChance(player, data)) {
                actual.add(new ItemStack(BuiltInRegistries.ITEM.getValue(refund.output()), refund.count()));
            }
            var page = AffixPages.get(input).orElse(null); double rate = componentRate(player, data);
            if (page != null) { for (var affix : page.affixes()) {
                if (!affix.rarity().equals(ItemizationData.id("common")) && data.family(affix.id()) != null && data.rarity(affix.rarity()) != null) {
                    int count = (int) rate + (player.getRandom().nextDouble() < rate - (int) rate ? 1 : 0);
                    if (count > 0) { actual.add(Workbench.material(new AffixMaterial(affix.id(), affix.rarity()), count)); }
                }
            } }
            if (!insert(outputs, actual)) { throw new IllegalStateException("Maximum salvage capacity underestimated"); }
            var quality = input.get(Workbench.QUALITY.get()); var rarity = quality == null ? null : data.rarity(quality.rarity());
            long xp = page != null && !page.affixes().isEmpty() && rarity != null ? rarity.salvageXp() : 0;
            station.setItem(slot, ItemStack.EMPTY);
            for (int output = 0; output < outputs.size(); output++) { station.setItem(output + 11, outputs.get(output)); }
            Progression.awardProfession(player, player.level(), CRAFTING, xp); processed++;
        }
        return processed;
    }
    public static List<ItemStack> outputCopy(WorkbenchBlockEntity station) {
        var outputs = new ArrayList<ItemStack>(); for (int slot = 11; slot < 23; slot++) { outputs.add(station.getItem(slot).copy()); } return outputs;
    }
}
