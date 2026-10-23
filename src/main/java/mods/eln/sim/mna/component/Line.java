package mods.eln.sim.mna.component;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessFlush;
import mods.eln.sim.mna.state.State;

import java.util.Iterator;
import java.util.LinkedList;

public class Line extends Resistor implements ISubSystemProcessFlush, IAbstractor {

    public LinkedList<Resistor> resistors = new LinkedList<Resistor>(); //from a to b
    public LinkedList<State> states = new LinkedList<State>(); //from a to b

    boolean ofInterSystem;

    boolean canAdd(Component c) {
        return (c instanceof Resistor);
    }

    void add(Resistor c) {
        ofInterSystem |= c.canBeReplacedByInterSystem();
        resistors.add(c);
    }

    @Override
    public boolean canBeReplacedByInterSystem() {
        // a line ending at ground (null pin) has nothing on the other side to couple to
        return ofInterSystem && aPin != null && bPin != null;
    }

    public void recalculateR() {
        double R = 0;
        for (Resistor r : resistors) {
            R += r.getR();
        }

        setR(R);
    }

    void restoreResistorIntoCircuit() {
        //aPin.add(resistors.getFirst());
        //bPin.add(resistors.getLast());
        this.breakConnection();
    }

    void removeResistorFromCircuit() {
        //aPin.remove(resistors.getFirst());
        //bPin.remove(resistors.getLast());
    }

	/*void removeCompFromState(Resistor r, State s) {
		State sNext = (r.aPin == s ? r.bPin : r.aPin);
		if (sNext != null) sNext.remove(r);
	}	
	void addCompFromState(Resistor r, State s) {
		State sNext = (r.aPin == s ? r.bPin : r.aPin);
		if (sNext != null) sNext.add(r);
	}*/

    public static void newLine(RootSystem root, LinkedList<Resistor> resistors, LinkedList<State> states) {
        if (resistors.isEmpty()) {
        } else if (resistors.size() == 1) {
            //root.addComponent(resistors.getFirst());
        } else {
            Resistor first = resistors.getFirst();
            Resistor last = resistors.getLast();
            State stateBefore = first.aPin == states.getFirst() ? first.bPin : first.aPin;
            State stateAfter = last.aPin == states.getLast() ? last.bPin : last.aPin;
            //stateBefore.remove(first);
            //stateAfter.remove(last);
            // ground at both ends: no state outside would reach the Line, so no SubSystem would take it; leave the
            // resistors and states as they are (solved as a small SubSystem of their own)
            if (stateBefore == null && stateAfter == null) return;

            Line l = new Line();
            l.resistors = resistors;
            l.states = states;
            l.recalculateR();
            root.addComponents.removeAll(resistors);
            root.addStates.removeAll(states);
            root.addComponents.add(l);
            l.connectTo(stateBefore, stateAfter);
            l.removeResistorFromCircuit();

            root.addProcess(l);

            for (Resistor r : resistors) {
                r.abstractedBy = l;
                l.ofInterSystem |= r.canBeReplacedByInterSystem();
            }

            for (State s : states) {
                s.abstractedBy = l;
            }
        }
    }

    @Override
    public void returnToRootSystem(RootSystem root) {
        for (Resistor r : resistors) {
            r.abstractedBy = null;
        }

        for (State s : states) {
            s.abstractedBy = null;
        }

        restoreResistorIntoCircuit();

        root.addStates.addAll(states);
        root.addComponents.addAll(resistors);

        root.removeProcess(this);
    }

    @Override
    public void simProcessFlush() {
        // either end may be ground (null): a chain of cable loads that ends in a resistor to ground
        double ua = aPin == null ? 0 : aPin.state, ub = bPin == null ? 0 : bPin.state;
        double i = (ua - ub) * getRInv();
        double u = ua;
        Iterator<Resistor> ir = resistors.iterator();
        Iterator<State> is = states.iterator();

        while (is.hasNext()) {
            State s = is.next();
            Resistor r = ir.next();
            u -= r.getR() * i;
            s.state = u;
            //u -= r.getR() * i;
        }
    }

    @Override
    public void addedTo(SubSystem s) {
        s.addProcess(this);
        super.addedTo(s);
    }

    @Override
    public void quitSubSystem() {
    }

    @Override
    public void dirty(Component component) {
        recalculateR();
        if (isAbstracted())
            abstractedBy.dirty(this);
    }

    @Override
    public SubSystem getAbstractorSubSystem() {
        return getSubSystem();
    }
}
