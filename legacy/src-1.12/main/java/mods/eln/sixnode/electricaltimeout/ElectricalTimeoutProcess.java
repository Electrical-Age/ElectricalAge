package mods.eln.sixnode.electricaltimeout;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.IProcess;
import mods.eln.sound.SoundCommand;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalTimeoutProcess implements IProcess, INBTTReady {

    ElectricalTimeoutElement element;
    boolean inputState = false;

    int tickCounter = 0;

    public ElectricalTimeoutProcess(ElectricalTimeoutElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        boolean oldInputState = inputState;

        if (inputState) {
            if (element.inputGate.stateLow()) inputState = false;
        } else {
            if (element.inputGate.stateHigh()) inputState = true;
        }

        if (inputState) {
            element.timeOutCounter = element.timeOutValue;
        }

        if (element.timeOutCounter != 0.0) {
            element.outputGateProcess.state(true);
            if (!inputState) element.timeOutCounter -= time;
            if (element.timeOutCounter < 0.0) element.timeOutCounter = 0.0;

            if (!inputState && ++tickCounter % 200 == 0)
                element.play(new SoundCommand(element.descriptor.tickSound).mulVolume(element.descriptor.tickVolume, 1f).verySmallRange());
        } else {
            element.outputGateProcess.state(false);
        }

        if (inputState != oldInputState) {
            element.needPublish();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        inputState = nbt.getBoolean(str + "SProcinputState");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setBoolean(str + "SProcinputState", inputState);
    }
}
