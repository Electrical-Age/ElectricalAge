package mods.eln.sim.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.CompoundTag;

public class NbtResistor extends Resistor implements INBTTReady {

    String name;

    public NbtResistor(String name, State aPin, State bPin) {
        super(aPin, bPin);
        this.name = name;
    }

    // 1.7.10 did `name += str` on every read/write (the name grew by the prefix on each save) and wrote the key
    // without the name (two NbtResistors under one prefix would collide). Key is now prefix + name + "R", like
    // the other INBTTReady sim parts; a missing/zero value (worlds saved before) falls back to high impedance
    // (the three users re-set R every tick anyway).
    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        double r = nbt.getDoubleOr(str + name + "R", 0.0);
        if (Double.isNaN(r) || r <= 0) highImpedance();
        else setR(r);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putDouble(str + name + "R", getR());
    }
}
