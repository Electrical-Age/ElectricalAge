package mods.eln.sim.mna.component;

public class InterSystem extends Resistor {

    public static class InterSystemDestructor {
        boolean done = false;
    }

    @Override
    public boolean canBeReplacedByInterSystem() {
        return true;
    }
}
