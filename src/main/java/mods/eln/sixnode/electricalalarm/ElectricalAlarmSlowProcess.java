package mods.eln.sixnode.electricalalarm;

import mods.eln.Eln;
import mods.eln.init.Cable;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import mods.eln.sound.SoundCommand;

public class ElectricalAlarmSlowProcess implements IProcess {

    ElectricalAlarmElement element;

    double timeCounter = 0, soundTimeTimeout = Math.random() * 2;
    static final double refreshPeriode = 0.25;
    int soundUuid = Utils.getUuid();
    boolean oldWarm = false;
    boolean oldMute = true;

    public ElectricalAlarmSlowProcess(ElectricalAlarmElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        timeCounter += time;
        if (timeCounter > refreshPeriode) {
            timeCounter -= refreshPeriode;

            boolean warm = element.inputGate.getU() > Cable.SVU / 2;
            element.setWarm(warm);
            if (warm & !element.mute) {
                if (soundTimeTimeout == 0) {
                    float speed = 1f;
                    Coordinate coord = element.sixNode.coordinate;
                    element.play(new SoundCommand(element.descriptor.soundName).mulVolume(1F, 1.0F).longRange().addUuid(soundUuid));
                    soundTimeTimeout = element.descriptor.soundTime;
                }
            }
            if ((oldWarm && !warm) || (!oldMute && element.mute)) {
                stopSound();
            }

            oldWarm = warm;
            oldMute = element.mute;
        }
        soundTimeTimeout -= time;
        if (soundTimeTimeout < 0) soundTimeTimeout = 0;
    }

    void stopSound() {
        element.stop(soundUuid);
        soundTimeTimeout = 0;
    }
}
