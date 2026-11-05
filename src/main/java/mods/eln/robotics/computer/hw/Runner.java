package mods.eln.robotics.computer.hw;

import mods.eln.bus.Packet;

/**
 * One computer's link between the server thread and the pool (Baughn: computers run off the server thread, their I/O
 * snapshotted once a tick). The server thread calls tick() every game tick the computer is loaded. If the computer is
 * between slices, that is its exchange: input gathered since the last one goes in, output comes out, and it is queued
 * for its next slice of budget instructions. If it is still in a slice, the server doesn't wait: input keeps
 * gathering, and the computer catches up on time at its next exchange. Nothing else touches the Machine while it
 * runs; the atomic state is the handoff.
 * <p>
 * Everything here except run() is for the server thread only.
 */
public final class Runner implements Runnable {
    private static final int IDLE = 0, RUNNING = 1;

    public final Machine machine;
    private final java.util.concurrent.atomic.AtomicInteger state = new java.util.concurrent.atomic.AtomicInteger(IDLE);
    private volatile int ran;
    public int budget = 1_000_000;

    // input gathered for the next exchange
    private final java.util.List<Packet> packetsIn = new java.util.ArrayList<>();
    private final java.io.ByteArrayOutputStream keys = new java.io.ByteArrayOutputStream(), editorIn = new java.io.ByteArrayOutputStream();
    private final Floppy[] disks = new Floppy[2];
    private final boolean[] diskChanged = new boolean[2];
    private long hostTicks, machineTicks;
    private int address;
    private boolean onBus;
    private double[] levels;
    private boolean saveWanted;

    // output from the last exchange
    private final java.util.List<Packet> packetsOut = new java.util.ArrayList<>();
    private final java.io.ByteArrayOutputStream terminalOut = new java.io.ByteArrayOutputStream(), editorOut = new java.io.ByteArrayOutputStream();
    private Machine.Saved saved;
    private long lastRan, late;

    public Runner(Machine machine) {
        this.machine = machine;
        machineTicks = hostTicks = machine.ticks();
    }

    // --- input, any time ---

    public void deliver(java.util.Collection<Packet> packets) {
        packetsIn.addAll(packets);
        if (packetsIn.size() > BusDevice.IN_CAP) packetsIn.subList(0, packetsIn.size() - BusDevice.IN_CAP).clear();
    }

    public void type(byte[] b) {
        keys.writeBytes(b);
    }

    public void toEditor(byte[] b) {
        editorIn.writeBytes(b);
    }

    public void insert(int drive, Floppy f) {
        disks[drive] = f;
        diskChanged[drive] = true;
    }

    public void busState(int address, boolean onBus, double[] levels) {
        this.address = address;
        this.onBus = onBus;
        this.levels = levels;
    }

    /** Take a save at the next exchange (see saved()). */
    public void requestSave() {
        saveWanted = true;
    }

    // --- the tick ---

    /**
     * One game tick with the computer loaded. powered: whether it may run a slice. Returns whether it exchanged
     * (false: it was still running its last slice).
     */
    public boolean tick(boolean powered) {
        hostTicks++;
        if (state.get() == RUNNING) {
            late++;
            return false;
        }
        exchange();
        if (powered && machine.runnable()) {
            state.set(RUNNING);
            ComputerPool.get().execute(this);
        } else ran = 0;
        return true;
    }

    private void exchange() {
        Machine m = machine;
        lastRan = ran;
        m.advance((int) Math.min(Integer.MAX_VALUE, hostTicks - machineTicks));
        machineTicks = hostTicks;
        m.bus.setState(address, onBus, levels);
        if (!packetsIn.isEmpty()) {
            m.bus.give(packetsIn);
            packetsIn.clear();
        }
        give(m.terminal, keys);
        give(m.editor, editorIn);
        for (int d = 0; d < 2; d++)
            if (diskChanged[d]) {
                m.floppy.insert(d, disks[d]);
                diskChanged[d] = false;
            }
        packetsOut.addAll(m.bus.take());
        terminalOut.writeBytes(m.terminal.take());
        editorOut.writeBytes(m.editor.take());
        if (saveWanted) {
            saved = m.save();
            saveWanted = false;
        }
        m.lines();
    }

    private static void give(Console c, java.io.ByteArrayOutputStream pending) {
        if (pending.size() == 0) return;
        byte[] b = pending.toByteArray();
        int n = c.give(b);
        pending.reset();
        pending.write(b, n, b.length - n);
    }

    @Override
    public void run() {
        try {
            ran = machine.run(budget);
        } catch (RuntimeException e) {
            machine.halted = true;   // an emulator bug must not kill the pool thread; the computer stops
            ran = 0;
        } finally {
            state.set(IDLE);
        }
    }

    // --- output, after tick() ---

    /** Packets the program sent, in order (cleared). */
    public java.util.List<Packet> takePackets() {
        java.util.List<Packet> l = new java.util.ArrayList<>(packetsOut);
        packetsOut.clear();
        return l;
    }

    public byte[] takeTerminal() {
        byte[] b = terminalOut.toByteArray();
        terminalOut.reset();
        return b;
    }

    public byte[] takeEditor() {
        byte[] b = editorOut.toByteArray();
        editorOut.reset();
        return b;
    }

    /** The save requestSave() asked for, once taken (cleared). */
    public Machine.Saved takeSaved() {
        Machine.Saved s = saved;
        saved = null;
        return s;
    }

    /** Instructions the last finished slice ran (for power draw). */
    public long lastRan() {
        return lastRan;
    }

    public boolean running() {
        return state.get() == RUNNING;
    }

    /** Ticks it was still in a slice at its exchange. */
    public long late() {
        return late;
    }

    /** Wait until it is between slices (a slice is a few ms, plus its wait in the pool's queue), for a final save. */
    public void awaitIdle() {
        while (state.get() == RUNNING) java.util.concurrent.locks.LockSupport.parkNanos(200_000);
    }
}
