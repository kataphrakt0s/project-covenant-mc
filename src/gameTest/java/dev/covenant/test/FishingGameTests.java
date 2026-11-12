package dev.covenant.test;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import static dev.covenant.test.ProfessionGameTests.check;

/** Native reel-in path tests exercise the completion Mixin after the cancellable fishing event. */
final class FishingGameTests {
    private static ProfessionState.Track fishing(ServerPlayer p) { return p.getData(RpgData.PROFESSIONS).tracks().get(ProgressionData.FISHING); }
    private static FishingHook hook(ServerPlayer p) {
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FISHING_ROD));
        var hook = new FishingHook(p, p.level(), 0, 0); hook.setPos(p.getX(), p.getY(), p.getZ() + 1);
        p.level().addFreshEntity(hook);
        try { var field = FishingHook.class.getDeclaredField("nibble"); field.setAccessible(true); field.setInt(hook, 20); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        return hook;
    }
    static void nativeCatch(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null); var hook = hook(p);
            check(hook.retrieve(p.getMainHandItem()) > 0, "Native accepted reel-in retains its rod cost");
            check(fishing(p).experience() == 50 && p.getData(RpgData.PROFESSIONS).tracks().get(ProgressionData.BUILDING).experience() == 32
                    && p.getData(RpgData.PROFESSIONS).tracks().get(ProgressionData.BUILDING).experienceFraction() == 50, "Native catch pays 50 Fishing XP and exactly 32.5 Building XP");
            hook.retrieve(p.getMainHandItem());
            check(fishing(p).experience() == 50, "A discarded hook cannot award a second time");
            var rejected = hook(p);
            Consumer<ItemFishedEvent> cancel = event -> { if (event.getHookEntity() == rejected) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(cancel);
            try { rejected.retrieve(p.getMainHandItem()); } finally { NeoForge.EVENT_BUS.unregister(cancel); rejected.discard(); }
            check(fishing(p).experience() == 50, "Cancelled ItemFishedEvent grants no profession XP");
            var empty = hook(p);
            ProfessionFishing.caught(p, empty, List.of(ItemStack.EMPTY)); empty.discard();
            check(fishing(p).experience() == 50, "Empty catch/hooked-entity payloads grant nothing");
            h.succeed();
        }
    }
    static void rewards(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null);
            for (var item : List.of(Items.COD, Items.STICK, Items.ENCHANTED_BOOK)) {
                var hook = hook(p);
                ProfessionFishing.caught(p, hook, List.of(new ItemStack(item, 3), new ItemStack(Items.SALMON, 2)));
                ProfessionFishing.caught(p, hook, List.of(new ItemStack(item))); hook.discard();
            }
            check(fishing(p).level() == 1 && fishing(p).experience() == 50, "Fish, junk and treasure each pay once per catch regardless of stack size or item count");
            p.setGameMode(GameType.CREATIVE); var creativeHook = hook(p);
            ProfessionFishing.caught(p, creativeHook, List.of(new ItemStack(Items.COD))); creativeHook.discard(); p.setGameMode(GameType.SURVIVAL);
            check(fishing(p).experience() == 50, "Creative catches do not advance professions");
            p.setData(RpgData.PROFESSIONS, p.getData(RpgData.PROFESSIONS).with(ProgressionData.FISHING, new ProfessionState.Track(19, 1990)));
            p.setData(RpgData.PROFESSIONS, p.getData(RpgData.PROFESSIONS).with(ProgressionData.BUILDING, new ProfessionState.Track(0, 0)));
            var capped = hook(p); var building = p.getData(RpgData.PROFESSIONS).tracks().get(ProgressionData.BUILDING);
            ProfessionFishing.caught(p, capped, List.of(new ItemStack(Items.COD))); capped.discard();
            var after = p.getData(RpgData.PROFESSIONS).tracks().get(ProgressionData.BUILDING);
            check(fishing(p).level() == 20 && after.experience() - building.experience() == 6 && after.experienceFraction() == 50, "Only ten accepted Fishing XP contribute 6.5 Building XP; source overflow is discarded");
            var full = p.getData(RpgData.PROFESSIONS); var another = hook(p);
            ProfessionFishing.caught(p, another, List.of(new ItemStack(Items.COD))); another.discard();
            check(p.getData(RpgData.PROFESSIONS).equals(full), "Capped Fishing cannot continue feeding Building");
            h.succeed();
        }
    }
}
