package dev.covenant.rpg.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Shared native storage: craft slot 0, salvage inputs 1-10, withdrawal-only results 11-22. */
public final class WorkbenchBlockEntity extends BaseContainerBlockEntity {
    public static final int INPUT_START = 1, OUTPUT_START = 11, SIZE = 23;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private long revision;
    public WorkbenchBlockEntity(BlockPos pos, BlockState state) { super(Workbench.ENTITY.get(), pos, state); }
    public long revision() { return revision; }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return Component.translatable("block.covenant.workbench"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new WorkbenchMenu(id, inventory, this); }
    @Override public void setChanged() { revision++; super.setChanged(); }
    @Override public void clearContent() { super.clearContent(); setChanged(); }
    @Override public ItemStack removeItemNoUpdate(int slot) { var item = super.removeItemNoUpdate(slot); if (!item.isEmpty()) { setChanged(); } return item; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < OUTPUT_START && stack.getCount() == 1 && level != null && !level.isClientSide()
                && ItemizationData.get(level.getServer()).category(stack).isPresent();
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); items = NonNullList.withSize(SIZE, ItemStack.EMPTY); ContainerHelper.loadAllItems(input, items); revision++;
    }
    @Override protected void saveAdditional(ValueOutput output) { super.saveAdditional(output); ContainerHelper.saveAllItems(output, items); }
    // BlockEntity.preRemoveSideEffects already drops Container contents exactly once in 26.1.2.
}
