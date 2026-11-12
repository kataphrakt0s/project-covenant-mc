package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.equipment.*;
import dev.covenant.rpg.progression.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import static dev.covenant.rpg.equipment.WorkbenchOperations.*;

/** Observe real owner packets and unchanged transaction/storage boundaries, not inferred client state. */
final class PhaseFiveGameTests {
    private static void check(boolean ok, String message) { if (!ok) { throw new IllegalStateException(message); } }
    private static long sounds(List<Packet<?>> packets, net.minecraft.sounds.SoundEvent sound) {
        return packets.stream().filter(p -> p instanceof ClientboundSoundPacket s && s.getSound().value().location().equals(sound.location())).count();
    }
    private static long levels(List<Packet<?>> packets) {
        return packets.stream().filter(p -> p instanceof ClientboundSystemChatPacket s && s.overlay()
                && s.content().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().startsWith("progression.covenant.earned_")).count();
    }
    static void progression(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var packets = new ArrayList<Packet<?>>(); var observerPackets = new ArrayList<Packet<?>>();
            var p = f.player(null, packets); var observer = f.player(null, observerPackets); packets.clear(); observerPackets.clear();
            check(!Progression.awardCharacter(p, h.getLevel(), 1000) && levels(packets) == 0, "No class means no character award or cue");
            PlayerProfiles.selectClass(p, PlayerClass.WEAVER); Progression.refreshView(p);
            check(levels(packets) == 0, "Class initialization and initial sync are silent");
            var d = ProgressionData.get(h.getLevel().getServer());
            Progression.awardCharacter(p, h.getLevel(), d.required(ProgressionData.CHARACTER, 1) - 1);
            check(levels(packets) == 0 && sounds(packets, SoundEvents.PLAYER_LEVELUP) == 0, "Ordinary XP remains quiet");
            Progression.awardCharacter(p, h.getLevel(), 1);
            check(levels(packets) == 1 && sounds(packets, SoundEvents.PLAYER_LEVELUP) == 1, "One accepted level gets one owner overlay and sound");
            packets.clear();
            Progression.awardCharacter(p, h.getLevel(), d.required(ProgressionData.CHARACTER, 2) + d.required(ProgressionData.CHARACTER, 3));
            check(PlayerProfiles.get(p).level() == 4 && levels(packets) == 1 && sounds(packets, SoundEvents.PLAYER_LEVELUP) == 1, "Multi-level award aggregates into one cue");
            var notice = packets.stream().filter(v -> v instanceof ClientboundSystemChatPacket s && s.overlay()).map(v -> (ClientboundSystemChatPacket)v).findFirst().orElseThrow();
            var args = ((net.minecraft.network.chat.contents.TranslatableContents)notice.content().getContents()).getArgs();
            check(args[2].equals(2), "Cue reports the two newly earned skill points");
            packets.clear(); Progression.refreshView(p); Progression.refreshView(p); Progression.awardCharacter(p, h.getLevel(), 0);
            check(levels(packets) == 0 && sounds(packets, SoundEvents.PLAYER_LEVELUP) == 0, "Repeated login/reload/dimension view path and zero awards cannot replay cues");
            check(Progression.firstWither(p) && !Progression.firstWither(p) && levels(packets) == 1, "Wither milestone cues only the first committed level");
            packets.clear(); PlayerProfiles.update(p, old -> Progression.atLevel(old, 60)); Progression.refreshView(p);
            check(!Progression.awardCharacter(p, h.getLevel(), 1000) && levels(packets) == 0, "Admin snapshots and capped awards stay quiet");
            check(levels(observerPackets) == 0 && sounds(observerPackets, SoundEvents.PLAYER_LEVELUP) == 0, "Another player never receives the owner's progression cue");
            var mining = ProfessionData.id("mining");
            Progression.awardProfession(observer, h.getLevel(), mining, d.required(mining, 0) + d.required(mining, 1));
            check(!PlayerProfiles.get(observer).created() && observer.getData(RpgData.PROFESSIONS).tracks().get(mining).level() == 2
                    && levels(observerPackets) == 1 && sounds(observerPackets, SoundEvents.PLAYER_LEVELUP) == 1, "Classless profession levels aggregate independently");
            check(observer.getData(RpgData.PROFESSIONS).tracks().get(ProgressionData.BUILDING).level() == 1
                    && observerPackets.stream().filter(v -> v instanceof ClientboundSystemChatPacket s && s.overlay())
                            .map(v -> ((ClientboundSystemChatPacket)v).content()).anyMatch(content -> content.getSiblings().stream()
                                    .anyMatch(sibling -> sibling.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                                            && t.getKey().equals("progression.covenant.earned_profession"))),
                    "One source award includes Building's level gain in the same owner cue");
            h.succeed();
        }
    }

    static void workbench(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var packets = new ArrayList<Packet<?>>(); var otherPackets = new ArrayList<Packet<?>>();
            var p = f.player(null, packets); var other = f.player(null, otherPackets);
            var pos = h.absolutePos(new BlockPos(3, 100, 2)); h.getLevel().setBlock(pos, Workbench.BLOCK.get().defaultBlockState(), 3);
            var s = (WorkbenchBlockEntity)h.getLevel().getBlockEntity(pos);
            var state = p.getData(RpgData.PROFESSIONS); p.setData(RpgData.PROFESSIONS, state.with(CRAFTING, new ProfessionState.Track(20, 0)));
            ProfessionAllocations.apply(p, CRAFTING, ProfessionData.snapshot(p.level().getServer()).revision(), Map.of(), Map.of(BASIC, 1, SALVAGING, 1));
            s.setItem(0, new ItemStack(Items.IRON_SWORD));
            var menu = new WorkbenchMenu(18, p.getInventory(), s); p.containerMenu = menu;
            var otherMenu = new WorkbenchMenu(19, other.getInventory(), s); other.containerMenu = otherMenu;
            p.getRandom().setSeed(45); menu.broadcastChanges();
            check(p.getRandom().nextLong() == net.minecraft.util.RandomSource.create(45).nextLong(), "Preview consumes no random roll");
            check(!menu.view.enabled() && menu.view.details().cost().contains("3") && menu.view.details().potential().contains("20 → 15")
                    && menu.view.details().result().contains("random") && !s.getItem(0).has(Workbench.QUALITY.get()), "Missing ingredients preserve useful structured costs/range without initializing gear");
            int stale = menu.view.token(); p.getInventory().setItem(1, new ItemStack(Items.LAPIS_LAZULI, 16)); p.getInventory().setChanged();
            check(!menu.select(p, new WorkbenchMenu.Selection(menu.containerId, stale, 1, 1)), "Changed inventory rejects stale direct selection");
            int current = menu.view.token();
            check(!menu.select(p, new WorkbenchMenu.Selection(menu.containerId, current, 1, 1000))
                    && !menu.select(p, new WorkbenchMenu.Selection(menu.containerId + 1, current, 1, 0)), "Forged choice and wrong menu rejected");
            check(menu.select(p, new WorkbenchMenu.Selection(menu.containerId, current, 1, 5)) && menu.view.family().equals("Movement Speed"), "Direct choice selects the intended server family");
            check(!menu.select(p, new WorkbenchMenu.Selection(menu.containerId, current, 1, 0)), "Replayed choice cannot change selection");
            var buf = new FriendlyByteBuf(Unpooled.buffer());
            try { WorkbenchMenu.View.STREAM_CODEC.encode(buf, menu.view); check(WorkbenchMenu.View.STREAM_CODEC.decode(buf).equals(menu.view), "Structured view with complete choices round trips"); }
            finally { buf.release(); }
            packets.clear(); otherPackets.clear(); int action = 1000 + menu.view.token();
            check(menu.clickMenuButton(p, action) && menu.view.receipt().equals("Add complete") && sounds(packets, SoundEvents.UI_STONECUTTER_TAKE_RESULT) == 1, "Accepted commit has one receipt and sound");
            var saved = s.getItem(0).copy();
            check(!menu.clickMenuButton(p, action) && ItemStack.matches(saved, s.getItem(0)) && sounds(packets, SoundEvents.UI_STONECUTTER_TAKE_RESULT) == 1, "Stale action cannot mutate or replay success");
            otherMenu.broadcastChanges(); check(otherMenu.view.receipt().isEmpty() && sounds(otherPackets, SoundEvents.UI_STONECUTTER_TAKE_RESULT) == 0, "Shared viewers do not inherit success receipts or sounds");
            check(menu.select(p, new WorkbenchMenu.Selection(menu.containerId, menu.view.token(), 0, Operation.REMOVE.ordinal())), "Direct Remove selection");
            check(menu.view.details().result().contains("no material or Potential refund"), "Destructive operation states its consequence");
            s.getItem(0).set(Workbench.QUALITY.get(), new EquipmentQuality(ItemizationData.id("common"), 0)); s.setChanged(); menu.broadcastChanges();
            packets.clear(); check(!menu.view.enabled() && menu.view.details().potential().contains("requires 5"), "Exhausted Potential shows requirement without a fictitious negative result");
            check(!menu.clickMenuButton(p, 1000 + menu.view.token()) && sounds(packets, SoundEvents.UI_STONECUTTER_TAKE_RESULT) == 0, "Failed action has no success cue");
            menu.clickMenuButton(p, 5); s.setItem(1, new ItemStack(Items.IRON_SWORD)); s.setItem(2, new ItemStack(Items.IRON_AXE)); menu.broadcastChanges();
            check(menu.view.preview().contains("AND enchantments") && menu.view.details().result().contains("Refund"), "Salvage explains destruction and probabilities");
            packets.clear(); check(menu.clickMenuButton(p, 1000 + menu.view.token()) && s.getItem(1).isEmpty() && s.getItem(2).isEmpty()
                    && menu.view.receipt().equals("Salvaged 2 item(s)") && sounds(packets, SoundEvents.UI_STONECUTTER_TAKE_RESULT) == 1, "One accepted-operation cue per multi-item salvage batch");
            check(AffixText.value(ItemizationData.id("dodge"), RpgModifier.Operation.ADD, .025).equals("+2.5%")
                    && AffixText.value(ItemizationData.id("resource_cost"), RpgModifier.Operation.ADD, -.05).equals("-0.05"), "Display distinguishes additive multiplier deltas from percentage attributes");
            check(Workbench.BLOCK.get().defaultBlockState().getCollisionShape(h.getLevel(), pos).bounds().getSize() == 1, "Full-block collision remains unchanged");
            h.getLevel().removeBlock(pos, false); h.succeed();
        }
    }
}
