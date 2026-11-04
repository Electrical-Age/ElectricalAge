package mods.eln.api.docs;

import mods.eln.api.ElnApi;
import mods.eln.api.Machine;
import mods.eln.api.MachineView;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** docs/api.md's mill, as another mod's block entity. */
public class MillBlockEntity extends BlockEntity {
    private final Mill.Work work = new Mill.Work();
    private int items, input = 16;

    public MillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // doc:begin tick
    /** The block's server ticker. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, MillBlockEntity be) {
        Machine m = ElnApi.machines().of(be);         // the same handle for as long as the block entity lasts
        if (be.work.tick(m, be.input > 0)) {
            be.input--;
            be.items++;
            be.setChanged();
        }
    }
    // doc:end tick

    // doc:begin client
    /** Anywhere, server or client: the machine's view at a block, or null if there is none. */
    public static MachineView viewAt(Level level, BlockPos pos) {
        return level.getCapability(ElnApi.MACHINE, pos, null);
    }
    // doc:end client
}
