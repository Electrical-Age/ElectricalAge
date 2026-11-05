package mods.eln.robotics.computer.hw;

import mods.eln.bus.Packet;

/**
 * One computer's link between the server thread and the pool (Baughn: computers run off the server thread, their I/O
 * snapshotted once a tick). The server thread calls tick() every game tick the computer is loaded. If the computer is
 * between slices, that is its exchange: input gathered since the last one goes in, output comes out, and it is queued
 * for its next slice of budget instructions. If it is still in a slice, the server doesn't wait: input keeps
 * gathering, and the computer catches up on time at its next exchange.
 * <p>
 * Whoever holds the Machine has won the state's compare-and-set from IDLE to RUNNING: the server thread for an exchange
 * or hold(), the pool for a slice. The screen has a fast path that needs no server thread at all (Baughn: latency; a
 * busy server's ticks would otherwise sit between a keystroke and its answer): screen input (toEditor, pressBreak) may
 * come from any thread and kick() the computer with what is left of its tick's budget, and with screenOut set, the
 * screen's output goes to it as each slice or exchange ends, in order, while the Machine is still held. The bus, disks
 * and power change only at the tick's exchange. Everything else here is for the server thread.
 */
public final class Runner implements Runnable {
    private static final int IDLE = 0, RUNNING = 1, STOPPED = 2;

    public final Machine machine;
    private final java.util.concurrent.atomic.AtomicInteger state = new java.util.concurrent.atomic.AtomicInteger(IDLE);
    private volatile int ran;
    public int budget = 1_000_000;
    /** Instructions run this tick so far (slices and kicks), and what the slice now running may run. */
    private volatile int ranThisTick;
    private volatile int sliceBudget;
    private volatile boolean powered;

    /** Where the screen's output goes as it is made (any thread, the Machine held); null: takeEditor/takeTerminal. */
    public interface ScreenOut {
        /** resets: the machine's reset count (a change empties the screen first). */
        void accept(int resets, byte[] display, byte[] console);
    }

    public volatile ScreenOut screenOut;

    // input gathered for the next exchange; the screen's from any thread
    private final java.util.List<Packet> packetsIn = new java.util.ArrayList<>();
    private final java.io.ByteArrayOutputStream keys = new java.io.ByteArrayOutputStream();
    private final java.util.concurrent.ConcurrentLinkedQueue<byte[]> displayIn = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private byte[] displayLeft;
    private final Floppy[] disks = new Floppy[2];
    private final boolean[] diskChanged = new boolean[2];
    private long hostTicks, machineTicks;
    private int address;
    private boolean onBus;
    private double[] levels;
    private boolean saveWanted;
    private volatile boolean breakWanted;

    // output from the last exchange
    private final java.util.List<Packet> packetsOut = new java.util.ArrayList<>();
    private final java.io.ByteArrayOutputStream terminalOut = new java.io.ByteArrayOutputStream(), editorOut = new java.io.ByteArrayOutputStream();
    private Machine.Saved saved;
    private long lastRan, late;

    public Runner(Machine machine) {
        this.machine = machine;
        machineTicks = hostTicks = machine.ticks();
    }

    // --- input ---

    public void deliver(java.util.Collection<Packet> packets) {
        packetsIn.addAll(packets);
        if (packetsIn.size() > BusDevice.IN_CAP) packetsIn.subList(0, packetsIn.size() - BusDevice.IN_CAP).clear();
    }

    public void type(byte[] b) {
        keys.writeBytes(b);
    }

    /** Screen input (any thread); kick() to have it taken at once. */
    public void toEditor(byte[] b) {
        displayIn.add(b);
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

    /** Press Break (any thread): taken by the next kick, slice or exchange. */
    public void pressBreak() {
        breakWanted = true;
    }

    /** Take a save at the next exchange (see saved()). */
    public void requestSave() {
        saveWanted = true;
    }

    /** Switched off: no kick runs it from now on (tick() learns it at the next tick). */
    public void powerOff() {
        powered = false;
    }

    // --- the tick ---

    /**
     * One game tick with the computer loaded. powered: whether it may run a slice. Returns whether it exchanged
     * (false: it was still running a slice).
     */
    public boolean tick(boolean powered) {
        hostTicks++;
        this.powered = powered;
        if (!state.compareAndSet(IDLE, RUNNING)) {
            late++;
            return false;
        }
        exchange();
        ranThisTick = 0;
        if (powered && machine.runnable()) {
            sliceBudget = budget;
            ComputerPool.get().execute(this);
        } else {
            ran = 0;
            state.set(IDLE);
        }
        return true;
    }

    /**
     * Screen input arrived (the fast path; any thread): if the computer is between slices and has budget left this
     * tick, it takes the input now and runs on what is left, instead of waiting for the next tick. It never runs more
     * than its budget a tick (Argon's review). A computer in a slice takes the input as the slice ends.
     */
    public void kick() {
        if (!state.compareAndSet(IDLE, RUNNING)) return;
        if (!powered) {
            state.set(IDLE);
            return;
        }
        takeScreenInput();
        machine.lines();
        if (ranThisTick < budget && machine.runnable()) {
            sliceBudget = budget - ranThisTick;
            ComputerPool.get().execute(this);
        } else state.set(IDLE);   // (input it couldn't take waits for the next tick)
    }

    /** Run body holding the Machine (server thread): waits out a slice; a stopped runner's Machine is the caller's. */
    public void hold(Runnable body) {
        while (true) {
            if (state.compareAndSet(IDLE, RUNNING)) break;
            if (state.get() == STOPPED) {
                body.run();
                return;
            }
            java.util.concurrent.locks.LockSupport.parkNanos(200_000);
        }
        try {
            body.run();
        } finally {
            release();
        }
    }

    /** Never run again (before the runner is dropped): waits out a slice; kicks and ticks do nothing from then on. */
    public void stop() {
        while (!state.compareAndSet(IDLE, STOPPED)) {
            if (state.get() == STOPPED) return;
            java.util.concurrent.locks.LockSupport.parkNanos(200_000);
        }
    }

    /** Let go of the Machine, and kick again if screen input came while it was held. */
    private void release() {
        state.set(IDLE);
        if (!displayIn.isEmpty() || breakWanted) kick();
    }

    /** Screen input into the machine, as far as its buffer takes it (the Machine held). */
    private void takeScreenInput() {
        if (breakWanted) {
            breakWanted = false;
            machine.pressBreak();
        }
        while (true) {
            byte[] b = displayLeft != null ? displayLeft : displayIn.poll();
            if (b == null) return;
            int n = machine.editor.give(b);
            displayLeft = n < b.length ? java.util.Arrays.copyOfRange(b, n, b.length) : null;
            if (displayLeft != null) return;
        }
    }

    /** The screen's output so far, to screenOut or the exchange's buffers (the Machine held). */
    private void screenOutput() {
        byte[] d = machine.editor.take(), t = machine.terminal.take();
        ScreenOut out = screenOut;
        if (out != null) out.accept(machine.resets(), d, t);
        else {
            editorOut.writeBytes(d);
            terminalOut.writeBytes(t);
        }
    }

    private void exchange() {
        Machine m = machine;
        lastRan = ranThisTick;
        m.advance((int) Math.min(Integer.MAX_VALUE, hostTicks - machineTicks));
        machineTicks = hostTicks;
        m.bus.setState(address, onBus, levels);
        if (!packetsIn.isEmpty()) {
            m.bus.give(packetsIn);
            packetsIn.clear();
        }
        give(m.terminal, keys);
        takeScreenInput();
        for (int d = 0; d < 2; d++)
            if (diskChanged[d]) {
                m.floppy.insert(d, disks[d]);
                diskChanged[d] = false;
            }
        packetsOut.addAll(m.bus.take());
        screenOutput();
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
            takeScreenInput();
            machine.lines();
            ran = machine.run(sliceBudget);
            ranThisTick += ran;
            if (screenOut != null) screenOutput();   // (else the exchange takes it)
        } catch (RuntimeException e) {
            machine.halted = true;   // an emulator bug must not kill the pool thread; the computer stops
            ran = 0;
        } finally {
            release();
        }
    }

    // --- output, after tick() ---

    /** Packets the program sent, in order (cleared). */
    public java.util.List<Packet> takePackets() {
        java.util.List<Packet> l = new java.util.ArrayList<>(packetsOut);
        packetsOut.clear();
        return l;
    }

    /** The console's output (without screenOut). */
    public byte[] takeTerminal() {
        byte[] b = terminalOut.toByteArray();
        terminalOut.reset();
        return b;
    }

    /** The display channel's output (without screenOut). */
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

    /** Instructions run in the last tick (for power draw). */
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

    /** Wait until it is between slices (a slice is a few ms, plus its wait in the pool's queue); tests. */
    public void awaitIdle() {
        while (state.get() == RUNNING) java.util.concurrent.locks.LockSupport.parkNanos(200_000);
    }
}
