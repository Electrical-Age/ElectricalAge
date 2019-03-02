package mods.eln.transparentnode.heatfurnace;

import mods.eln.Eln;
import mods.eln.init.Cable;
import mods.eln.sim.FurnaceProcess;
import mods.eln.sim.RegulatorFurnaceProcess;

public class HeatFurnaceThermalProcess extends RegulatorFurnaceProcess {

    HeatFurnaceElement element;

    public HeatFurnaceThermalProcess(String name, FurnaceProcess furnace, HeatFurnaceElement element) {
        super(name, furnace);
        this.element = element;
    }

    @Override
    public void process(double time) {
        //	if (!element.getControlExternal())
        super.process(time);
        //	else
        if (element.getControlExternal()) {
            double ratio = element.electricalCmdLoad.getU() / Cable.SVU;

            if (ratio < 0.1) {
                element.setTakeFuel(false);
                setCmd(0.1);
            } else {
                element.setTakeFuel(true);
                setCmd(ratio);
            }
        }
    }
}
