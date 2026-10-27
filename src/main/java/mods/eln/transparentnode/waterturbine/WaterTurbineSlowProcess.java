package mods.eln.transparentnode.waterturbine;

import mods.eln.misc.INBTTReady;
import mods.eln.misc.RcRcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;

public class WaterTurbineSlowProcess implements IProcess, INBTTReady {

    WaterTurbineElement turbine;

    public WaterTurbineSlowProcess(WaterTurbineElement turbine) {
        this.turbine = turbine;
    }

    double refreshTimeout = 0;
    double refreshPeriode = 0.2;

    // WP16b: 1.7.10 stepped this filter (taus 2 s, decay 0.5 /s) with one tick (0.05 s) once per refresh, i.e. every
    // 5 ticks (refreshTimeout is reset, not decremented by the period), so its real time constants were 5x longer.
    // Now stepped with the elapsed time, constants retuned to what players had: taus 10 s, decay 0.1 /s.
    RcRcInterpolator filter = new RcRcInterpolator(10, 10);
    double elapsed = 0;

    @Override
    public void process(double time) {
        WaterTurbineDescriptor d = turbine.descriptor;

        elapsed += time;
        refreshTimeout -= time;
        if (refreshTimeout < 0) {
            refreshTimeout = refreshPeriode;
            double dt = elapsed;
            elapsed = 0;
            double waterFactor = getWaterFactor();
            if (waterFactor < 0) {
                filter.setValue((float) (filter.get() * Math.max(0, 1 - 0.1 * dt)));
            } else {
                filter.setTarget((float) (waterFactor * d.nominalPower));
                filter.step((float) dt);
            }

            turbine.powerSource.setP(filter.get());
        }
    }

    double getWaterFactor() {
        //Block b = turbine.waterCoord.getBlock();
        double time = 0;
        if (turbine.waterCoord.getBlockExist()) {
            Block block = turbine.waterCoord.getBlock();
            net.minecraft.world.level.block.state.BlockState waterState = turbine.waterCoord.world().getBlockState(turbine.waterCoord.pos());
            int blockMeta = waterState.hasProperty(net.minecraft.world.level.block.LiquidBlock.LEVEL) ? waterState.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL) : 0;
            //Utils.println("WATER : " + b + "    " + turbine.waterCoord.getMeta());
            if (block != Blocks.WATER) return -1; // 1.12: FLOWING_WATER or WATER
            if (blockMeta == 0) return 0;
            time = Utils.getWorldTime(turbine.world());
        }

        double timeFactor = 1 + 0.2 * Math.sin((time - 0.20) * Math.PI * 2);
        double weatherFactor = 1 + Utils.getWeatherNoLoad(turbine.coordonate()) * 2;
        return timeFactor * weatherFactor;
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        filter.readFromNBT(nbt, str + "filter");
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {

        filter.writeToNBT(nbt, str + "filter");

    }
}
