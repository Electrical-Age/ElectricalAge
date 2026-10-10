package mods.eln.sim;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.Transformer;
import mods.eln.sim.mna.component.VoltageSource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Baughn 2026-10-09: a generator in motor mode fed through a non-isolating transformer didn't spin up. */
public class TransformerMotorTest {
    SimHarness h = new SimHarness();

    ElectricalLoad load() {
        ElectricalLoad l = new ElectricalLoad();
        h.sim.addElectricalLoad(l);
        return l;
    }

    SubSystem.Th th(boolean viaTransformer, int nTransformers) {
        h = new SimHarness();
        ElectricalLoad emf = load(), p = load();
        h.sim.addElectricalComponent(new VoltageSource("src", emf, null).setU(200));
        h.sim.addElectricalComponent(new Resistor(emf, p).setR(1));
        ElectricalLoad cur = p;
        if (viaTransformer) {
            for (int i = 0; i < nTransformers; i++) {
                ElectricalLoad s = load();
                Transformer t = new Transformer(cur, s);
                t.setRatio(1);
                h.sim.addElectricalComponent(t);
                ElectricalLoad next = load();
                h.sim.addElectricalComponent(new Resistor(s, next).setR(0.01)); // cable between devices
                cur = next;
            }
        }
        ElectricalLoad pos = load();
        h.sim.addElectricalComponent(new Resistor(cur, pos).setR(0.5)); // generator inputToPositiveResistor
        VoltageSource gen = new VoltageSource("gen", pos, null);
        h.sim.addElectricalComponent(gen);
        h.tick();
        SubSystem.Th th = pos.getSubSystem().getTh(pos, gen);
        gen.setU(th.U * 0.999);
        h.ticks(5);
        System.out.println("transformers=" + (viaTransformer ? nTransformers : 0) + " Th.U=" + th.U + " Th.R=" + th.R
            + " gen I=" + gen.getI() + " U=" + gen.getU() + " P=" + gen.getP());
        return th;
    }

    @Test
    public void thevenin() {
        SubSystem.Th d = th(false, 0);
        SubSystem.Th t1 = th(true, 1);
        SubSystem.Th t2 = th(true, 2);
        assertEquals(d.U, t1.U, 1e-6);
        assertEquals(d.U, t2.U, 1e-6);
    }
}
