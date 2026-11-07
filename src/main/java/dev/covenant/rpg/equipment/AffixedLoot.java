package dev.covenant.rpg.equipment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.covenant.rpg.RpgData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Only fresh loot-table output is initialized. Equipment carried/dropped by players is never rerolled. */
public final class AffixedLoot extends LootModifier {
    public static final MapCodec<AffixedLoot> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).apply(i, AffixedLoot::new));
    public AffixedLoot(LootItemCondition[] conditions, int priority) { super(conditions, priority); }
    @Override public MapCodec<AffixedLoot> codec() { return CODEC; }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var table = context.getQueriedLootTableId();
        boolean chest = table.getNamespace().equals("minecraft") && table.getPath().startsWith("chests/");
        boolean entity = context.hasParameter(LootContextParams.DAMAGE_SOURCE)
                && context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof net.minecraft.world.entity.LivingEntity;
        if (!chest && !entity) { return loot; }
        var data = ItemizationData.get(context.getLevel().getServer()); var random = context.getRandom();
        var result = new ObjectArrayList<ItemStack>();
        for (var stack : loot) {
            // A pack may opt in stackable equipment; split before affixing so every instance is unique.
            if (data.category(stack).isPresent() && !stack.has(RpgData.AFFIX_PAGE.get()) && !stack.has(Workbench.QUALITY.get())) {
                for (int count = 0; count < stack.getCount(); count++) {
                    var single = stack.copyWithCount(1);
                    if (random.nextDouble() < data.settings().affixChance()) { initialize(context, single, data); }
                    result.add(single);
                }
            } else { result.add(stack); }
        }
        int bonus = chest ? data.settings().chestBonus() :
                context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Enemy
                && context.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER) != null
                && random.nextDouble() < data.settings().mobBonusChance() ? 1 : 0;
        for (int n = 0; n < bonus; n++) {
            var item = data.bonusPool().get(random.nextInt(data.bonusPool().size()));
            var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(item));
            if (random.nextDouble() < data.settings().affixChance()) { initialize(context, stack, data); }
            result.add(stack);
        }
        return result;
    }
    private static void initialize(LootContext context, ItemStack stack, ItemizationData.Definitions data) {
        initialize(context.getLevel(), stack, data, context.getRandom());
    }
    /** Trusted one-time initialization, also used by deterministic lifecycle tests. */
    public static boolean initialize(net.minecraft.server.level.ServerLevel level, ItemStack stack,
            ItemizationData.Definitions data, RandomSource random) {
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("Loot requires server thread"); }
        var category = data.category(stack).orElse(null);
        if (category == null || stack.getCount() != 1 || stack.has(RpgData.AFFIX_PAGE.get()) || stack.has(Workbench.QUALITY.get())) { return false; }
        var families = data.families().stream().filter(f -> f.supports(stack, category)).toList();
        if (families.isEmpty()) { return false; }
        double choice = random.nextDouble() * data.rarities().stream().mapToDouble(ItemizationData.Rarity::weight).sum();
        var rarity = data.rarities().getLast();
        for (var r : data.rarities()) { choice -= r.weight(); if (choice < 0) { rarity = r; break; } }
        var finalRarity = rarity;
        families = families.stream().filter(f -> f.prefix() ? finalRarity.prefixes() > 0 : finalRarity.suffixes() > 0).toList();
        if (families.isEmpty()) { return false; }
        var family = families.get(random.nextInt(families.size())); int tier = 1 + random.nextInt(rarity.tier());
        AffixPages.attach(level, stack, category, List.of(), List.of(roll(family, rarity, tier, random)));
        stack.set(Workbench.QUALITY.get(), new EquipmentQuality(rarity.id(), rarity.potential()));
        return true;
    }
    public static Affix roll(ItemizationData.Family family, ItemizationData.Rarity rarity, int tier, RandomSource random) {
        var range = family.ranges().get(tier - 1); double best = 0;
        for (int n = 0; n < rarity.samples(); n++) { best = Math.max(best, range.min() + random.nextDouble() * (range.max() - range.min())); }
        double amount = best * rarity.magnitude() * (family.reduction() ? -1 : 1);
        return new Affix(family.id(), rarity.id(), tier, List.of(new RpgModifier(family.stat(), family.operation(), amount)));
    }
}
