package mods.eln.sim;

import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sixnode.resistor.ResistorDescriptor;
import mods.eln.sixnode.resistor.ResistorElement;

/**
 * Created by svein on 07/08/15.
 */
public class ResistorProcess implements IProcess {

    ResistorElement element;
    ResistorDescriptor descriptor;
    Resistor r;
    ThermalLoad thermal;

    private double lastR = -1;

    public ResistorProcess(ResistorElement element, Resistor r, ThermalLoad thermal, ResistorDescriptor descriptor) {
        this.element = element;
        this.descriptor = descriptor;
        this.r = r;
        this.thermal = thermal;
    }

    @Override
    public void process(double time) {
        double newR = Math.max(
            MnaConst.noImpedance,
            element.nominalRs * (1 + descriptor.tempCoef * thermal.Tc));
        if (element.control != null) {
            newR *= (element.control.getNormalized() + 0.01) / 1.01;
        }
        if (newR > lastR * 1.01 || newR < lastR * 0.99) {
            r.setR(newR);
            lastR = newR;
            element.needPublish();
        }

//        /*
//        * https://en.wikipedia.org/wiki/Thermistor
//        *
//        * R = exp[(x - y/2)^(1/3) - (x + y/2)^(1/3)]
//        * y = 1/c*(a - 1/T)
//        * x = sqrt((b/3c)^3 + (y/2)^2)
//        */
//
//        double T = thermal.Tc;
//        double y = 1.0 / descriptor.shC * (descriptor.shA - 1.0/T);
//        double x = Math.sqrt(Math.pow(descriptor.shB / 3.0 / descriptor.shC, 3) + Math.pow(y / 2.0, 2));
//        double R = Math.exp(Math.pow(x - y/2, 1.0/3.0) - Math.pow(x + y/2, 1.0/3.0));
//
//        r.setR(R);
    }
}
