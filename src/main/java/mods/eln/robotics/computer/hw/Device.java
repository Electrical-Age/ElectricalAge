package mods.eln.robotics.computer.hw;

/** One device page of the computer: 32-bit registers at word offsets, and whether it wants the external interrupt. */
public interface Device {
    int read(int offset);

    void write(int offset, int value);

    default boolean interrupt() {
        return false;
    }
}
