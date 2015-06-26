package mods.eln.sound;

import java.util.ArrayList;

public class SoundTrack {

    String track;
    double trackLength;
    float volume = 1, pitch = 1;
    float rangeNominal, rangeMax, blockFactor;
    ArrayList<Integer> uuid = new ArrayList<Integer>();

    enum Range {Small, Mid, Far}

    public SoundTrack() {
    }

    public SoundTrack(String track) {
        this.track = track;
        mediumRange();
    }

    public SoundTrack(String track, double trackLength) {
        this.track = track;
        this.trackLength = trackLength;
        mediumRange();
    }

    public SoundTrack copy() {
        SoundTrack c = new SoundTrack();
        c.track = track;
        c.trackLength = trackLength;
        c.volume = volume;
        c.pitch = pitch;
        c.rangeNominal = rangeNominal;
        c.rangeMax = rangeMax;
        c.blockFactor = blockFactor;
        c.uuid = (ArrayList<Integer>) uuid.clone();
        return c;
    }

    void applyRange(Range range) {
        switch (range) {
            case Small:
                smallRange();
                break;
            case Far:
                longRange();
                break;
            case Mid:
            default:
                mediumRange();
                break;
        }
    }

    public SoundTrack mediumRange() {
        rangeNominal = 4;
        rangeMax = 16;
        blockFactor = 1;
        return this;
    }

    public SoundTrack smallRange() {
        rangeNominal = 2;
        rangeMax = 8;
        blockFactor = 3;
        return this;
    }

    public SoundTrack longRange() {
        rangeNominal = 8;
        rangeMax = 48;
        blockFactor = 0.5f;
        return this;
    }

    public SoundTrack setVolume(float volume, float pitch) {
        this.volume = volume;
        this.pitch = pitch;
        return this;
    }

    public SoundTrack addUuid(int uuid) {
        this.uuid.add(uuid);
        return this;
    }
}
