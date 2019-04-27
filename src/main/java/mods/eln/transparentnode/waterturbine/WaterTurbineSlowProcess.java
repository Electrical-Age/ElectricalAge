package mods.eln.transparentnode.waterturbine;

import mods.eln.misc.INBTTReady;
import mods.eln.misc.RcRcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

public class WaterTurbineSlowProcess implements IProcess, INBTTReady {

    WaterTurbineElement turbine;

    public WaterTurbineSlowProcess(WaterTurbineElement turbine) {
        this.turbine = turbine;
    }

    double refreshTimeout = 0;
    double refreshPeriode = 0.2;

    RcRcInterpolator filter = new RcRcInterpolator(2, 2);

    @Override
    public void process(double time) {
        WaterTurbineDescriptor d = turbine.descriptor;

        refreshTimeout -= time;
        if (refreshTimeout < 0) {
            refreshTimeout = refreshPeriode;
            double waterFactor = getWaterFactor();
            if (waterFactor < 0) {
                filter.setValue((float) (filter.get() * (1 - 0.5f * time)));
            } else {
                filter.setTarget((float) (waterFactor * d.nominalPower));
                filter.step((float) time);
            }

            turbine.powerSource.setP(filter.get());
        }
    }

    double getWaterFactor() {
        //Block b = turbine.waterCoord.getBlock();
        double time = 0;
        if (turbine.waterCoord.doesBlockExist()) {
            IBlockState state = turbine.waterCoord.getBlockState();
            Block block = state.getBlock();
            int blockMeta = block.getMetaFromState(state);
            //Utils.println("WATER : " + b + "    " + turbine.waterCoord.getMeta());
            if (block != Blocks.FLOWING_WATER && block != Blocks.WATER) return -1;
            if (blockMeta == 0) return 0;
            time = Utils.getWorldTime(turbine.world());
        }

        double timeFactor = 1 + 0.2 * Math.sin((time - 0.20) * Math.PI * 2);
        double weatherFactor = 1 + Utils.getWeatherNoLoad(turbine.coordinate().getDimension()) * 2;
        return timeFactor * weatherFactor;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        filter.readFromNBT(nbt, str + "filter");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt, String str) {

        filter.writeToNBT(nbt, str + "filter");
        return nbt;

    }
}
