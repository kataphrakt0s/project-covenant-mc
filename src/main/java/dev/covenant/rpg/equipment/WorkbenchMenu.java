package dev.covenant.rpg.equipment;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.ProfessionState;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Native synchronized slots and intent buttons; the server supplies every cost/availability preview. */
public final class WorkbenchMenu extends AbstractContainerMenu {
    private final Container contents;
    private final Inventory inventory;
    private int tab, operation, family, rarity, target, token;
    private long observedStation = -1, observedData = -1;
    private int observedInventory = -1;
    private ProfessionState observedProfession;
    private boolean dirty = true;
    private String receipt = "";
    public View view;
    public WorkbenchMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(23)); }
    public WorkbenchMenu(int id, Inventory inventory, Container contents) {
        super(Workbench.MENU.get(), id); this.inventory = inventory; this.contents = contents;
        view = new View(id, 0, 0, "Add", "Health", "Common", "No affix", "Waiting for server…", false,
                Details.EMPTY, List.of(), List.of(), List.of(), "");
        addSlot(stationSlot(0, 20, 48));
        for (int n = 0; n < 10; n++) { addSlot(stationSlot(n + 1, 8 + n % 5 * 18, 36 + n / 5 * 18)); }
        for (int n = 0; n < 12; n++) { addSlot(stationSlot(n + 11, 112 + n % 6 * 18, 36 + n / 6 * 18)); }
        for (int row = 0; row < 3; row++) { for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 151 + row * 18)); } }
        for (int col = 0; col < 9; col++) { addSlot(new Slot(inventory, col, 8 + col * 18, 209)); }
    }
    private Slot stationSlot(int index, int x, int y) {
        return new Slot(contents, index, x, y) {
            @Override public boolean isActive() { return (inventory.player.level().isClientSide() ? view.tab : tab) == (index == 0 ? 0 : 1); }
            @Override public boolean mayPickup(Player player) { return isActive(); }
            @Override public int getMaxStackSize() { return index < 11 ? 1 : super.getMaxStackSize(); }
            @Override public boolean mayPlace(ItemStack stack) {
                if (index >= 11 || !isActive()) { return false; }
                return inventory.player.level().isClientSide() || contents.canPlaceItem(index, stack.copyWithCount(1));
            }
        };
    }
    @Override public boolean stillValid(Player player) { return contents.stillValid(player); }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (slot >= 0 && slot < slots.size() && !slots.get(slot).isActive()) { return; }
        super.clicked(slot, button, input, player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) { return ItemStack.EMPTY; }
        var slot = slots.get(index); if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) { return ItemStack.EMPTY; }
        var stack = slot.getItem(); var previous = stack.copy();
        if (index < 23) { if (!moveItemStackTo(stack, 23, slots.size(), true)) { return ItemStack.EMPTY; } }
        else { int activeTab = inventory.player.level().isClientSide() ? view.tab : tab;
            if (!moveItemStackTo(stack, activeTab == 0 ? 0 : 1, activeTab == 0 ? 1 : 11, false)) { return ItemStack.EMPTY; } }
        if (stack.isEmpty()) { slot.setByPlayer(ItemStack.EMPTY); } else { slot.setChanged(); }
        slot.onTake(player, stack); return previous;
    }
    private boolean changed(ServerPlayer player, WorkbenchBlockEntity station) {
        return dirty || observedStation != station.revision() || observedInventory != inventory.getTimesChanged()
                || observedData != ItemizationData.snapshot(player.level().getServer()).revision()
                || observedProfession != player.getData(RpgData.PROFESSIONS);
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (inventory.player instanceof ServerPlayer player && contents instanceof WorkbenchBlockEntity station && changed(player, station)) { refresh(player, station); }
    }
    private void refresh(ServerPlayer player, WorkbenchBlockEntity station) {
        try (var zone = Profiler.get().zone("covenant:workbench_preview")) {
            var snapshot = ItemizationData.snapshot(player.level().getServer()); var data = snapshot.definitions();
            family = Math.floorMod(family, data.families().size()); rarity = Math.floorMod(rarity, data.rarities().size());
            var page = AffixPages.get(station.getItem(0)).orElse(null); int affixes = page == null ? 0 : page.affixes().size();
            target = affixes == 0 ? 0 : Math.floorMod(target, affixes);
            String targetName = affixes == 0 ? "No affix" : Workbench.name(page.affixes().get(target).id()) + " T" + page.affixes().get(target).tier();
            String preview; boolean enabled; Details details;
            if (tab == 0) {
                var plan = WorkbenchOperations.preview(player, station, WorkbenchOperations.Operation.values()[operation], family, rarity, target);
                var quality = station.getItem(0).get(Workbench.QUALITY.get());
                var lines = new ArrayList<String>();
                lines.add(quality == null ? "Uninitialized equipment" : Workbench.name(quality.rarity()) + " · " + quality.potential() + " Potential remaining");
                if (plan.family() != null) {
                    lines.add("Cost: " + plan.potential() + " Potential" + (plan.cost() == 0 ? "" : " + " + plan.cost() + " " + plan.ingredient().getHoverName().getString()));
                    lines.add(Workbench.name(plan.family().id()) + " · " + Workbench.name(plan.affixRarity().id()) + " · Tier " + plan.tier());
                    var equipmentRarity = data.rarity(plan.quality().rarity());
                    lines.add("Capacity: " + equipmentRarity.prefixes() + " prefixes / " + equipmentRarity.suffixes() + " suffixes");
                    lines.add("Successful operation: " + plan.affixRarity().operationXp() + " Crafting XP");
                }
                lines.add(plan.valid() ? "Ready. Ingredients come from your inventory." : plan.error());
                String cost = plan.family() == null ? "Unavailable" : plan.cost() == 0 ? "No ingredients"
                        : plan.cost() + " " + plan.ingredient().getHoverName().getString() + " (inventory)";
                String potential = plan.quality() == null ? "Unavailable" : plan.quality().potential() < plan.potential()
                        ? plan.quality().potential() + " remaining · requires " + plan.potential() : plan.quality().potential() + " → "
                        + (plan.quality().potential() - plan.potential()) + " (−" + plan.potential() + ")"
                        + (quality == null ? " · initializes " + Workbench.name(plan.quality().rarity()) : "");
                String result = plan.family() == null ? "Select eligible gear and an affix." : operation == WorkbenchOperations.Operation.REMOVE.ordinal()
                        ? "Removes " + targetName + "; no material or Potential refund."
                        : "T" + plan.tier() + " " + Workbench.name(plan.family().id()) + ": " + AffixText.range(plan) + " (random)";
                details = new Details(cost, potential, result, plan.valid() ? "Ready · ingredients from your inventory" : plan.error());
                lines.add("Potential: " + potential); lines.add("Possible result: " + result);
                preview = String.join("\n", lines); enabled = plan.valid();
            } else {
                var lines = new ArrayList<String>(); String access = WorkbenchOperations.salvageAccess(player, station);
                lines.add("Consumes queued equipment AND enchantments. No Potential refund.");
                double generic = WorkbenchOperations.genericChance(player, data), rate = WorkbenchOperations.componentRate(player, data);
                lines.add(String.format(Locale.ROOT, "Generic refund: %.0f%% chance. Components: %d guaranteed + %.0f%% for one extra per eligible affix.",
                        generic * 100, (int) rate, (rate - (int) rate) * 100));
                enabled = false; boolean first = true;
                for (int slot = 1; slot < 11; slot++) {
                    var input = station.getItem(slot); if (input.isEmpty()) { continue; }
                    var maximum = WorkbenchOperations.maximumOutputs(player, input, data);
                    boolean valid = input.getCount() == 1 && data.category(input).isPresent();
                    boolean fits = valid && WorkbenchOperations.insert(WorkbenchOperations.outputCopy(station), maximum);
                    if (first) { enabled = fits; first = false; }
                    var outputs = maximum.stream().map(s -> "up to " + s.getCount() + " " + s.getHoverName().getString()).toList();
                    lines.add(slot + ". " + input.getHoverName().getString() + ": " + (outputs.isEmpty() ? "no material outputs" : String.join(", ", outputs)));
                    if (!fits) { lines.add("Cannot process input " + slot + ": " + (valid ? "maximum results need more space." : "equipment is no longer eligible.")); }
                }
                String reason = first ? "Queue equipment above." : enabled ? "Ready · batch stops before blocked input" : "First queued item is invalid or results need more space.";
                if (first) { lines.add("Queue equipment above. Salvaging consumes each item and its enchantments."); }
                if (!access.isEmpty()) { lines.add(access); enabled = false; }
                lines.add("Maximum amounts are possibilities, not guaranteed rolls. The batch checks space again before each input; partial batches may stop earlier.");
                details = new Details("Consumes queued gear AND enchantments", "No Potential refund",
                        String.format(Locale.ROOT, "Refund %.0f%% · %d components + %.0f%% for one extra / affix", generic * 100, (int) rate, (rate - (int) rate) * 100),
                        access.isEmpty() ? reason : access);
                preview = String.join("\n", lines);
            }
            token = token >= 1_000_000_000 ? 1 : token + 1;
            view = new View(containerId, token, tab, Workbench.name(ItemizationData.id(WorkbenchOperations.Operation.values()[operation].name().toLowerCase(Locale.ROOT))),
                    Workbench.name(data.families().get(family).id()), Workbench.name(data.rarities().get(rarity).id()), targetName, preview, enabled,
                    details, data.families().stream().map(f -> choiceName(f.id())).toList(),
                    data.rarities().stream().map(r -> choiceName(r.id())).toList(),
                    page == null ? List.of() : page.affixes().stream().map(a -> choiceName(a.id()) + " · T" + a.tier() + " · " + Workbench.name(a.rarity())).toList(), receipt);
            observedStation = station.revision(); observedInventory = inventory.getTimesChanged(); observedData = snapshot.revision();
            observedProfession = player.getData(RpgData.PROFESSIONS); dirty = false;
            PacketDistributor.sendToPlayer(player, view);
        }
    }
    private static String choiceName(net.minecraft.resources.Identifier id) {
        return Workbench.name(id) + (id.getNamespace().equals("covenant") ? "" : " (" + id + ")");
    }
    /** Selection buttons only stage intent. Token-bearing action buttons reject stale views and replays. */
    @Override public boolean clickMenuButton(Player actor, int button) {
        try (var zone = Profiler.get().zone("covenant:workbench_action")) {
            if (!(actor instanceof ServerPlayer player) || player.containerMenu != this || !(contents instanceof WorkbenchBlockEntity station)
                    || !player.isAlive() || player.isSpectator() || !stillValid(player) || station.isRemoved()) { return false; }
            if (button >= 1000) {
                if (button != 1000 + token || changed(player, station) || !view.enabled) {
                    receipt = "Preview changed · review before confirming"; refresh(player, station); return false;
                }
                int accepted = tab == 0 ? (WorkbenchOperations.craft(player, station, WorkbenchOperations.Operation.values()[operation], family, rarity, target) ? 1 : 0)
                        : WorkbenchOperations.salvage(player, station);
                receipt = accepted == 0 ? "No changes made · review the current preview" : tab == 0 ? view.operation + " complete" : "Salvaged " + accepted + " item(s)";
                if (accepted > 0) { player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.UI_STONECUTTER_TAKE_RESULT), net.minecraft.sounds.SoundSource.PLAYERS,
                        player.getX(), player.getY(), player.getZ(), .55f, 1.1f, 0L)); }
            } else {
                receipt = "";
                switch (button) {
                    case 0 -> operation = (operation + 1) % 4;
                    case 1 -> family++;
                    case 2 -> rarity++;
                    case 3 -> target++;
                    case 4 -> tab = 0;
                    case 5 -> tab = 1;
                    default -> { return false; }
                }
            }
            dirty = true; refresh(player, station); broadcastChanges(); return true;
        }
    }
    /** Token-bound direct choice. Reloaded catalogues and shared-storage edits cannot select a different index silently. */
    public boolean select(ServerPlayer player, Selection choice) {
        if (choice.containerId != containerId || player.containerMenu != this || !(contents instanceof WorkbenchBlockEntity station)
                || !player.isAlive() || player.isSpectator() || !stillValid(player) || station.isRemoved()) { return false; }
        if (choice.token != token || changed(player, station)) { receipt = "Choices changed · select again"; refresh(player, station); return false; }
        int size = switch (choice.kind) { case 0 -> 4; case 1 -> view.families.size(); case 2 -> view.rarities.size(); case 3 -> view.targets.size(); default -> 0; };
        if (choice.index < 0 || choice.index >= size || tab != 0) { return false; }
        switch (choice.kind) { case 0 -> operation = choice.index; case 1 -> family = choice.index; case 2 -> rarity = choice.index; case 3 -> target = choice.index; }
        receipt = ""; dirty = true; refresh(player, station); return true;
    }
    public static void registerNetwork(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(dev.covenant.CovenantNetwork.version("workbench/4"));
        registrar.playToClient(View.TYPE, View.STREAM_CODEC, (view, context) -> {
            if (context.player().containerMenu instanceof WorkbenchMenu menu && menu.containerId == view.containerId) { menu.view = view; }
        });
        registrar.playToServer(Selection.TYPE, Selection.STREAM_CODEC, (choice, context) -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof WorkbenchMenu menu) { menu.select(player, choice); }
        });
    }
    public record Details(String cost, String potential, String result, String reason) {
        static final Details EMPTY = new Details("", "", "", "Waiting for server…");
        public Details { cost = View.bounded(cost, 1024); potential = View.bounded(potential, 1024); result = View.bounded(result, 1024); reason = View.bounded(reason, 1024); }
        void write(FriendlyByteBuf b) { b.writeUtf(cost, 1024); b.writeUtf(potential, 1024); b.writeUtf(result, 1024); b.writeUtf(reason, 1024); }
        static Details read(FriendlyByteBuf b) { return new Details(b.readUtf(1024), b.readUtf(1024), b.readUtf(1024), b.readUtf(1024)); }
    }
    public record Selection(int containerId, int token, int kind, int index) implements CustomPacketPayload {
        public static final Type<Selection> TYPE = new Type<>(ItemizationData.id("workbench_selection"));
        public static final StreamCodec<FriendlyByteBuf, Selection> STREAM_CODEC = StreamCodec.of((b, s) -> {
            b.writeVarInt(s.containerId); b.writeVarInt(s.token); b.writeVarInt(s.kind); b.writeVarInt(s.index);
        }, b -> new Selection(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
        @Override public Type<Selection> type() { return TYPE; }
    }
    /** Presentation only. No client-submitted price, rolled value, item, profession rank, or output is trusted. */
    public record View(int containerId, int token, int tab, String operation, String family, String rarity,
            String target, String preview, boolean enabled, Details details, List<String> families, List<String> rarities,
            List<String> targets, String receipt) implements CustomPacketPayload {
        public View {
            operation = bounded(operation, 256); family = bounded(family, 256); rarity = bounded(rarity, 256);
            target = bounded(target, 256); preview = bounded(preview, 16384);
            families = choices(families); rarities = choices(rarities); targets = choices(targets); receipt = bounded(receipt, 256);
        }
        private static List<String> choices(List<String> values) {
            if (values.size() > 64) { throw new IllegalArgumentException("Too many Workbench choices"); }
            return values.stream().map(s -> bounded(s, 512)).toList();
        }
        private static void writeChoices(FriendlyByteBuf b, List<String> choices) { b.writeVarInt(choices.size()); choices.forEach(s -> b.writeUtf(s, 512)); }
        private static List<String> readChoices(FriendlyByteBuf b) {
            int count = b.readVarInt(); if (count < 0 || count > 64) { throw new IllegalArgumentException("Invalid Workbench choices"); }
            var result = new ArrayList<String>(count); for (int n = 0; n < count; n++) { result.add(b.readUtf(512)); } return result;
        }
        private static String bounded(String text, int limit) { return text.length() > limit ? text.substring(0, limit - 1) + "…" : text; }
        public static final Type<View> TYPE = new Type<>(ItemizationData.id("workbench_view"));
        public static final StreamCodec<FriendlyByteBuf, View> STREAM_CODEC = StreamCodec.of((buf, v) -> {
            buf.writeVarInt(v.containerId); buf.writeVarInt(v.token); buf.writeVarInt(v.tab);
            buf.writeUtf(v.operation, 256); buf.writeUtf(v.family, 256); buf.writeUtf(v.rarity, 256); buf.writeUtf(v.target, 256);
            buf.writeUtf(v.preview, 16384); buf.writeBoolean(v.enabled);
            v.details.write(buf); writeChoices(buf, v.families); writeChoices(buf, v.rarities); writeChoices(buf, v.targets); buf.writeUtf(v.receipt, 256);
        }, buf -> new View(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(256), buf.readUtf(256),
                buf.readUtf(256), buf.readUtf(256), buf.readUtf(16384), buf.readBoolean(), Details.read(buf), readChoices(buf), readChoices(buf), readChoices(buf), buf.readUtf(256)));
        @Override public Type<View> type() { return TYPE; }
    }
}
