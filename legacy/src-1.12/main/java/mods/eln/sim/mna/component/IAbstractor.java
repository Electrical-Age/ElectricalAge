package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;

public interface IAbstractor {

    void dirty(Component component);

    SubSystem getAbstractorSubSystem();
}
