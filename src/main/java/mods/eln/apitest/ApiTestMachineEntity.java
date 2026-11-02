package mods.eln.apitest;

import mods.eln.api.ElnApi;
import mods.eln.api.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The fixture's block entity: each server tick, while "enable" is high, it takes a tick's rated energy for its work,
 * writes on the bus whether it got any, and shows LIT while it works. Nothing else: EA hosts, saves and syncs it.
 */
public final class ApiTestMachineEntity extends BlockEntity {
    private double workJoules;
    private int workTicks;

    public ApiTestMachineEntity(BlockPos pos, BlockState state) {
        super(ApiTestMachines.TYPE.get(), pos, state);
    }

    void serverTick() {
        Machine m = ElnApi.machines().of(this);
        double got = m.signal("enable") > 0.5 ? m.takeJoules(m.ratedJoulesPerTick()) : 0;
        workJoules += got;
        if (got > 0) workTicks++;
        m.bus().write(ApiTestMachines.BUSY_CHANNEL, got > 0 ? 1 : 0);
        boolean lit = got > 0;
        if (getBlockState().getValue(BlockStateProperties.LIT) != lit && level != null)
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, lit), 3);
    }

    /** The energy its work has had since it was made, J (not saved). */
    public double workJoules() {
        return workJoules;
    }

    /** Ticks in which it worked. */
    public int workTicks() {
        return workTicks;
    }
}
