package mods.eln.apitest;

import mods.eln.api.ElnApi;
import mods.eln.api.Machine;
import mods.eln.api.MachineSpec;
import mods.eln.api.TripCause;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * The fixture's block entity: each server tick, while "enable" is high, it takes a tick's rated energy for its work,
 * writes on the bus whether it got any, and shows LIT while it works. Nothing else: EA hosts, saves and syncs it.
 */
public final class ApiTestMachineEntity extends BlockEntity {
    private double workJoules;
    private int workTicks;
    private MachineSpec spec = ApiTestMachines.SPEC;
    private final List<TripCause> burnCauses = new ArrayList<>();

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

    /** Hosts it with spec instead of SPEC (BURNING_SPEC, say); call before it is hosted, as it is placed. Not saved. */
    public void useSpec(MachineSpec spec) {
        this.spec = spec;
    }

    /** The spec it is hosted with (the registered function's answer). */
    public MachineSpec spec() {
        return spec;
    }

    /** BURNING_SPEC's burn handler. */
    void burnt(TripCause cause) {
        burnCauses.add(cause);
    }

    /** The causes its burn handler was called with, in order. */
    public List<TripCause> burnCauses() {
        return List.copyOf(burnCauses);
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
