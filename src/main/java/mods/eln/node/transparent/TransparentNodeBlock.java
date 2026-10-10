package mods.eln.node.transparent;

import mods.eln.node.NodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The block of one transparent-node device (1.12: one shared TransparentNodeBlock; 26.x: one block per device, all of
 * this class, for per-device models, drops and names). The device's node is a TransparentNode; ghosts make up the
 * rest of a multiblock.
 */
public class TransparentNodeBlock extends NodeBlock {
    public final TransparentNodeDescriptor descriptor;

    public TransparentNodeBlock(Properties properties, TransparentNodeDescriptor descriptor) {
        super(properties);
        this.descriptor = descriptor;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransparentNodeEntity(pos, state);
    }
}
