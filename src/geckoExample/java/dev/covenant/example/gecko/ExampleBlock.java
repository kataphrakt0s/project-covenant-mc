package dev.covenant.example.gecko;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Compile-only host for the animated block entity; native block properties still own interaction. */
public final class ExampleBlock extends Block implements EntityBlock {
    private final Supplier<BlockEntityType<ExampleBlockEntity>> entityType;

    public ExampleBlock(Properties properties, Supplier<BlockEntityType<ExampleBlockEntity>> entityType) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return entityType.get().create(pos, state);
    }
}
