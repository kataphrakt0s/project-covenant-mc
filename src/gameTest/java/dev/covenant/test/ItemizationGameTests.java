package dev.covenant.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.equipment.*;
import dev.covenant.rpg.progression.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.AABB;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import static dev.covenant.rpg.equipment.WorkbenchOperations.*;

/** Dedicated-server acceptance tests of the actual station, native slots, loot hook and saved components. */
final class ItemizationGameTests {
    private static void check(boolean value, String message) { if (!value) { throw new IllegalStateException(message); } }
    private static ItemizationData.Definitions data(GameTestHelper h) { return ItemizationData.get(h.getLevel().getServer()); }
    private static WorkbenchBlockEntity station(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(3, 100, 2)); h.getLevel().setBlock(pos, Workbench.BLOCK.get().defaultBlockState(), 3);
        return (WorkbenchBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static void learn(ServerPlayer p, int level, Map<Identifier, Integer> ranks) {
        var state = p.getData(RpgData.PROFESSIONS); p.setData(RpgData.PROFESSIONS, state.with(CRAFTING, new ProfessionState.Track(level, 0)));
        ProfessionAllocations.apply(p, CRAFTING, ProfessionData.snapshot(p.level().getServer()).revision(), Map.of(), ranks);
        check(track(p).ranks().equals(ranks), "Crafting nodes are actually purchasable with earned points");
    }
    private static WorkbenchMenu menu(ServerPlayer p, WorkbenchBlockEntity s, int id) {
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(p.connection.getConnection());
        var menu = new WorkbenchMenu(id, p.getInventory(), s); p.containerMenu = menu; menu.broadcastChanges(); return menu;
    }
    private static ItemStack gear(GameTestHelper h, Item item, int family, int rarity, int tier) {
        var stack = new ItemStack(item); var d = data(h); var r = d.rarities().get(rarity);
        AffixPages.attach(h.getLevel(), stack, d.category(stack).orElseThrow(), List.of(), List.of(AffixedLoot.roll(d.families().get(family), r, tier, RandomSource.create(4))));
        stack.set(Workbench.QUALITY.get(), new EquipmentQuality(r.id(), r.potential())); return stack;
    }
    static void rules(GameTestHelper h) {
        var d = data(h); check(d.rarities().size() == 5 && d.families().size() == 10 && d.bonusPool().size() == 12, "Shipped content matches the agreed minimum");
        for (var family : d.families()) { for (var rarity : d.rarities()) { for (int tier = 1; tier <= rarity.tier(); tier++) {
            var affix = AffixedLoot.roll(family, rarity, tier, RandomSource.create(123)); var range = family.ranges().get(tier - 1);
            double magnitude = Math.abs(affix.modifiers().getFirst().amount());
            check(magnitude >= range.min() * rarity.magnitude() && magnitude <= range.max() * rarity.magnitude(), "Roll remains within configured range and rarity boost");
            check((affix.modifiers().getFirst().amount() < 0) == family.reduction(), "Reduction signs remain beneficial");
        } } }
        var quality = new EquipmentQuality(ItemizationData.id("legendary"), 37);
        check(EquipmentQuality.CODEC.parse(JsonOps.INSTANCE, EquipmentQuality.CODEC.encodeStart(JsonOps.INSTANCE, quality).getOrThrow()).getOrThrow().equals(quality), "Quality codec retains instance rarity/Potential");
        var material = Workbench.material(new AffixMaterial(ItemizationData.id("health"), ItemizationData.id("rare")), 3);
        check(material.getMaxStackSize() == 64 && !ItemStack.isSameItemSameComponents(material,
                Workbench.material(new AffixMaterial(ItemizationData.id("health"), ItemizationData.id("epic")), 3)), "Components stack by family AND rarity");
        try (var reader = h.getLevel().getServer().getResourceManager().getResourceOrThrow(ItemizationData.FILE).openAsReader()) {
            var json = JsonParser.parseReader(reader).getAsJsonObject(); json.getAsJsonArray("families").get(0).getAsJsonObject().getAsJsonArray("ranges").get(0).getAsJsonObject().addProperty("min", -1);
            boolean rejected = false; try { ItemizationData.parse(json); } catch (RuntimeException expected) { rejected = true; }
            check(rejected, "Invalid range rejected before replacing live definitions");
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        check(d.refund(new ItemStack(Items.IRON_HOE)).count() == 1 && d.refund(new ItemStack(Items.DIAMOND_BOOTS)).count() == 1
                && d.refund(new ItemStack(Items.NETHERITE_SWORD)) == null && d.refund(new ItemStack(Items.BOW)) == null, "Generic refunds cover only iron and diamond");
        h.succeed();
    }
    static void common(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null); var s = station(h); var raw = new ItemStack(Items.IRON_SWORD);
            raw.setDamageValue(8); raw.set(DataComponents.CUSTOM_NAME, Component.literal("Kept name")); s.setItem(0, raw);
            check(!craft(p, s, Operation.ADD, 0, 0, 0), "Unpurchased Basic Affix Crafting rejects mutation");
            learn(p, 1, Map.of(BASIC, 1));
            check(!craft(p, s, Operation.ADD, 0, 0, 0) && !s.getItem(0).has(Workbench.QUALITY.get()), "Missing lapis never initializes crafted equipment");
            p.getInventory().setItem(0, new ItemStack(Items.LAPIS_LAZULI, 16));
            check(craft(p, s, Operation.ADD, 0, 0, 0), "Classless Common Add succeeds");
            var identity = AffixPages.get(s.getItem(0)).orElseThrow().identity();
            check(p.getInventory().getItem(0).getCount() == 13 && s.getItem(0).get(Workbench.QUALITY.get()).potential() == 15, "First Add spends three lapis and five of initialized twenty Potential");
            var state = s.getItem(0).copy(); long xp = track(p).experience();
            check(!craft(p, s, Operation.ADD, 0, 0, 0) && !craft(p, s, Operation.ADD, 1, 0, 0), "Duplicate family and exhausted prefix capacity reject Add");
            check(ItemStack.matches(state, s.getItem(0)) && track(p).experience() == xp && p.getInventory().getItem(0).getCount() == 13, "Rejected operations spend nothing");
            check(!craft(p, s, Operation.UPGRADE, 0, 0, 0), "Common equipment cannot upgrade beyond Tier 1");
            check(craft(p, s, Operation.REROLL, 0, 0, 0) && p.getInventory().getItem(0).getCount() == 12, "Common Reroll spends one lapis");
            check(craft(p, s, Operation.REMOVE, 0, 0, 0), "Remove needs Potential but no ingredient");
            check(AffixPages.get(s.getItem(0)).orElseThrow().affixes().isEmpty() && s.getItem(0).get(Workbench.QUALITY.get()).potential() == 5, "Removing last affix keeps rarity and spent Potential");
            check(craft(p, s, Operation.ADD, 5, 0, 0) && s.getItem(0).get(Workbench.QUALITY.get()).potential() == 0, "Re-adding cannot refill Potential");
            check(!craft(p, s, Operation.REMOVE, 0, 0, 0) && track(p).experience() == 80, "Exhausted Potential rejects even Remove; exactly four successes award XP");
            check(AffixPages.get(s.getItem(0)).orElseThrow().identity().equals(identity) && s.getItem(0).getDamageValue() == 8
                    && s.getItem(0).getHoverName().getString().equals("Kept name") && !p.getData(RpgData.PROFILE).created(), "Page identity, unrelated components and classless state remain intact");
            h.getLevel().removeBlock(s.getBlockPos(), false); h.succeed();
        }
    }
    static void upgrades(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null); var s = station(h); learn(p, 20, Map.of(BASIC, 1));
            var material = new AffixMaterial(ItemizationData.id("health"), ItemizationData.id("legendary"));
            s.setItem(0, new ItemStack(Items.DIAMOND_CHESTPLATE));
            p.getInventory().setItem(0, Workbench.material(new AffixMaterial(material.family(), ItemizationData.id("rare")), 40));
            check(!craft(p, s, Operation.ADD, 0, 4, 0) && !s.getItem(0).has(Workbench.QUALITY.get()), "Wrong-rarity components cannot pay for initialization");
            p.getInventory().setItem(0, Workbench.material(material, 40));
            check(craft(p, s, Operation.ADD, 0, 4, 0), "A starting Legendary affix initializes Legendary equipment");
            var pageId = AffixPages.get(s.getItem(0)).orElseThrow().identity();
            for (int tier = 2; tier <= 5; tier++) { check(craft(p, s, Operation.UPGRADE, 0, 0, 0)
                    && AffixPages.get(s.getItem(0)).orElseThrow().affixes().getFirst().tier() == tier, "Upgrade raises exactly one numerical tier"); }
            check(s.getItem(0).get(Workbench.QUALITY.get()).potential() == 35 && p.getInventory().getItem(0).getCount() == 15, "Every Legendary Add/Upgrade costs five components and Potential");
            check(!craft(p, s, Operation.UPGRADE, 0, 0, 0), "Tier ceiling rejects sixth tier");
            check(craft(p, s, Operation.REROLL, 0, 0, 0) && p.getInventory().getItem(0).getCount() == 12, "Legendary Reroll costs three matching components");
            var affix = AffixPages.get(s.getItem(0)).orElseThrow().affixes().getFirst();
            p.setItemSlot(EquipmentSlot.CHEST, s.getItem(0).copy()); p.doTick();
            check(p.getMaxHealth() > 21.7 && p.getMaxHealth() <= 23.45, "Rolled equipment uses existing native equipped-stat bridge");
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); p.doTick();
            check(craft(p, s, Operation.REMOVE, 0, 0, 0) && s.getItem(0).get(Workbench.QUALITY.get()).rarity().equals(material.rarity()), "Removing Legendary starting affix preserves Legendary rarity");
            p.getInventory().setItem(1, new ItemStack(Items.LAPIS_LAZULI, 16));
            check(craft(p, s, Operation.ADD, 0, 0, 0) && craft(p, s, Operation.UPGRADE, 0, 0, 0), "Common affix on Legendary equipment can upgrade using lapis");
            check(AffixPages.get(s.getItem(0)).orElseThrow().identity().equals(pageId), "All operations preserve page identity");
            var q = s.getItem(0).get(Workbench.QUALITY.get()); s.getItem(0).set(Workbench.QUALITY.get(), new EquipmentQuality(ItemizationData.id("common"), q.potential())); s.setChanged();
            check(!craft(p, s, Operation.ADD, 5, 4, 0), "Initialized Common equipment rejects a Legendary addition");
            learn(p, 1, Map.of(BASIC, 1));
            s.setItem(0, gear(h, Items.IRON_SWORD, 0, 4, 5));
            check(!craft(p, s, Operation.REROLL, 0, 0, 0), "Found high-tier gear does not bypass crafter tier access");
            check(affix.tier() == 5, "Reroll preserved tier"); h.getLevel().removeBlock(s.getBlockPos(), false); h.succeed();
        }
    }
    static void salvageBatch(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null); var s = station(h); s.setItem(1, gear(h, Items.IRON_SWORD, 0, 4, 1));
            check(salvage(p, s) == 0 && !s.getItem(1).isEmpty(), "Unpurchased Salvaging consumes nothing");
            learn(p, 15, Map.of(BASIC, 1, SALVAGING, 1, RECOVERY, 3, FOCUSED, 3));
            check(Math.abs(genericChance(p, data(h)) - 1) < .0001 && Math.abs(componentRate(p, data(h)) - 3.6) < .0001, "Purchased specializations apply agreed rates");
            for (int slot = 11; slot < 23; slot++) { s.setItem(slot, new ItemStack(Items.COBBLESTONE, 64)); }
            s.setItem(22, ItemStack.EMPTY);
            check(salvage(p, s) == 0 && !s.getItem(1).isEmpty() && track(p).experience() == 0, "One free output slot cannot fit maximum generic plus component rewards");
            s.setItem(21, ItemStack.EMPTY); s.setItem(2, gear(h, Items.IRON_AXE, 5, 2, 1));
            check(salvage(p, s) == 1 && s.getItem(1).isEmpty() && !s.getItem(2).isEmpty(), "Batch stops before second item's maximum rewards overflow");
            var components = outputCopy(s).stream().filter(i -> i.is(Workbench.MATERIAL.get())).findFirst().orElseThrow();
            check(components.getCount() >= 3 && components.getCount() <= 4 && components.get(Workbench.MATERIAL_ID.get()).rarity().equals(ItemizationData.id("legendary")), "Focused Recovery returns matching affix components independently");
            s.setItem(11, ItemStack.EMPTY);
            check(!s.getItem(2).isEmpty(), "Collecting outputs does not resume queued salvage");
            check(salvage(p, s) == 1 && track(p).experience() == 80, "Explicit second action continues batch and pays only consumed rarity rewards");
            for (int slot = 11; slot < 23; slot++) { s.setItem(slot, ItemStack.EMPTY); }
            s.setItem(1, new ItemStack(Items.IRON_SWORD)); check(salvage(p, s) == 1 && track(p).experience() == 80, "Unaffixed salvage returns materials with zero Crafting XP");
            s.setItem(1, gear(h, Items.BOW, 5, 1, 1)); var results = maximumOutputs(p, s.getItem(1), data(h));
            check(results.size() == 1 && results.getFirst().is(Workbench.MATERIAL.get()), "Unmapped bows still yield components, without generic refunds");
            check(salvage(p, s) == 1 && track(p).experience() == 100, "Affixed unmapped gear pays its normal salvage XP");
            s.setItem(1, gear(h, Items.BOW, 5, 0, 1));
            check(maximumOutputs(p, s.getItem(1), data(h)).isEmpty() && salvage(p, s) == 1 && track(p).experience() == 110,
                    "Zero-output Common salvage still consumes affixed gear and pays XP once");
            var empty = gear(h, Items.IRON_SWORD, 0, 4, 1); var page = AffixPages.get(empty).orElseThrow();
            AffixPages.update(h.getLevel(), empty, page.modifiers(), List.of(), page.progression()); s.setItem(1, empty);
            check(salvage(p, s) == 1 && track(p).experience() == 110, "Initialized rarity without remaining affixes grants no salvage XP");
            h.getLevel().removeBlock(s.getBlockPos(), false); h.succeed();
        }
    }
    static void menus(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var a = f.player(null); var b = f.player(null); var s = station(h);
            for (var p : List.of(a, b)) { learn(p, 1, Map.of(BASIC, 1)); p.getInventory().setItem(0, new ItemStack(Items.LAPIS_LAZULI, 32)); }
            s.setItem(0, new ItemStack(Items.IRON_SWORD)); var ma = menu(a, s, 8); var mb = menu(b, s, 9);
            int old = 1000 + ma.view.token(), stale = 1000 + mb.view.token();
            a.connection.handleContainerButtonClick(new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(ma.containerId, old));
            check(s.getItem(0).get(Workbench.QUALITY.get()).potential() == 15, "Native connection dispatch accepts the current explicit Craft action");
            var quality = s.getItem(0).get(Workbench.QUALITY.get());
            check(!ma.clickMenuButton(a, old) && !mb.clickMenuButton(b, stale), "Duplicate action and another viewer's stale shared-item preview reject");
            check(s.getItem(0).get(Workbench.QUALITY.get()).equals(quality) && b.getInventory().getItem(0).getCount() == 32, "Stale action cannot spend either player's ingredients or Potential");
            check(!ma.slots.get(11).mayPlace(new ItemStack(Items.DIRT)), "Salvage result slots reject deposits");
            check(!ma.slots.get(1).isActive() && ma.quickMoveStack(a, 1).isEmpty(), "Hidden-tab input slots cannot be manipulated");
            a.closeContainer(); check(!s.getItem(0).isEmpty(), "Closing leaves shared craft equipment in the block entity");
            b.setPos(s.getBlockPos().getX() + 100, s.getBlockPos().getY(), s.getBlockPos().getZ());
            check(!mb.clickMenuButton(b, 1000 + mb.view.token()), "Out-of-range menu buttons cannot mutate shared storage");
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try { WorkbenchMenu.View.STREAM_CODEC.encode(buffer, ma.view); check(WorkbenchMenu.View.STREAM_CODEC.decode(buffer).equals(ma.view), "Server preview round trips its bounded payload"); }
            finally { buffer.release(); }
            h.getLevel().removeBlock(s.getBlockPos(), false); h.succeed();
        }
    }
    static void storage(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null); var s = station(h); var gear = gear(h, Items.IRON_SWORD, 0, 2, 3);
            s.setItem(0, gear); s.setItem(1, new ItemStack(Items.DIAMOND_HELMET));
            s.setItem(11, Workbench.material(new AffixMaterial(ItemizationData.id("health"), ItemizationData.id("rare")), 6));
            var saved = s.saveWithFullMetadata(h.getLevel().registryAccess());
            var loaded = (WorkbenchBlockEntity) BlockEntity.loadStatic(s.getBlockPos(), s.getBlockState(), saved, h.getLevel().registryAccess());
            check(loaded != null && ItemStack.matches(loaded.getItem(0), gear) && loaded.getItem(1).is(Items.DIAMOND_HELMET)
                    && loaded.getItem(11).getCount() == 6, "Native block-entity save/load retains craft, queued input, and results with full item components");
            h.getLevel().setBlockEntity(loaded);
            var collected = loaded.removeItem(0, 1); p.getInventory().setItem(0, collected); p.containerMenu.broadcastChanges();
            var itemData = ItemStack.CODEC.encodeStart(h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), collected).getOrThrow();
            var restored = ItemStack.CODEC.parse(h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), itemData).getOrThrow();
            check(ItemStack.matches(collected, restored), "Collected gear retains rarity, Potential and rolled affixes through native item persistence");
            h.getLevel().destroyBlock(loaded.getBlockPos(), true);
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(loaded.getBlockPos()).inflate(2));
            check(drops.stream().filter(e -> e.getItem().is(Workbench.MATERIAL.get())).mapToInt(e -> e.getItem().getCount()).sum() == 6
                    && drops.stream().filter(e -> e.getItem().is(Items.DIAMOND_HELMET)).mapToInt(e -> e.getItem().getCount()).sum() == 1,
                    "Breaking station drops stored contents exactly once");
            drops.forEach(Entity::discard); h.succeed();
        }
    }
    static void loot(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null); var mob = f.mob(EntityType.ZOMBIE);
            long seed = 0; while (RandomSource.create(seed).nextDouble() >= .25) { seed++; }
            var modifier = new AffixedLoot(new LootItemCondition[0], 1000);
            for (boolean credited : List.of(false, true)) {
                var builder = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, mob.position())
                        .withParameter(LootContextParams.THIS_ENTITY, mob).withParameter(LootContextParams.DAMAGE_SOURCE, mob.damageSources().fall());
                if (credited) { builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, p); }
                var context = new LootContext.Builder(builder.create(LootContextParamSets.ENTITY)).withOptionalRandomSource(RandomSource.create(seed))
                        .withQueriedLootTableId(Identifier.withDefaultNamespace("entities/zombie")).create(Optional.empty());
                var output = modifier.apply(new ObjectArrayList<>(), context);
                check(output.size() == (credited ? 1 : 0), "Only native player credit enables the mob bonus, even with environmental damage and no class");
                if (credited) { var quality = output.getFirst().get(Workbench.QUALITY.get());
                    check(quality != null && quality.potential() == data(h).rarity(quality.rarity()).potential(), "Bonus gear receives full initial loot Potential");
                    var before = output.getFirst().copy(); check(!AffixedLoot.initialize(h.getLevel(), output.getFirst(), data(h), RandomSource.create(8))
                            && ItemStack.matches(before, output.getFirst()), "Existing initialized loot cannot be reinitialized"); }
            }
            var params = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, mob.position()).create(LootContextParamSets.CHEST);
            var context = new LootContext.Builder(params).withOptionalRandomSource(RandomSource.create(17))
                    .withQueriedLootTableId(Identifier.withDefaultNamespace("chests/simple_dungeon")).create(Optional.empty());
            var source = new ObjectArrayList<ItemStack>(); source.add(new ItemStack(Items.DIAMOND_AXE));
            var loot = net.neoforged.neoforge.common.CommonHooks.modifyLoot(Identifier.withDefaultNamespace("chests/simple_dungeon"), source, context);
            check(loot.size() == 2 && loot.stream().allMatch(s -> s.has(Workbench.QUALITY.get())), "Registered global modifier affixes ordinary chest gear and adds exactly one bonus");
            var pos = h.absolutePos(new BlockPos(3, 100, 4)); h.getLevel().setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
            var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(pos);
            chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon")), 17);
            chest.getItem(0); var contents = new ArrayList<ItemStack>(); for (int n = 0; n < chest.getContainerSize(); n++) { contents.add(chest.getItem(n).copy()); }
            check(contents.stream().anyMatch(s -> s.has(Workbench.QUALITY.get())), "Native first chest unpack generates affixed bonus equipment");
            chest.getItem(0); for (int n = 0; n < contents.size(); n++) { check(ItemStack.matches(contents.get(n), chest.getItem(n)), "Chest reopening cannot generate fresh loot or refill Potential"); }
            h.getLevel().removeBlock(pos, false); h.succeed();
        }
    }
}
