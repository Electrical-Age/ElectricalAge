package mods.eln.robotics.computer;

import mods.eln.api.MachineState;
import mods.eln.api.element.ElementMachine;
import mods.eln.api.element.Machines;
import mods.eln.bus.BusChannel;
import mods.eln.bus.BusPort;
import mods.eln.bus.Packet;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.robotics.computer.cpu.Rom;
import mods.eln.robotics.computer.hw.Floppy;
import mods.eln.robotics.computer.hw.Machine;
import mods.eln.robotics.computer.hw.Runner;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The robotics computer (ComputerDescriptor). Slots 0..7 hold its chips (slot k's chip at k MB), 8 and 9 its floppies
 * (drives A and B). Changing a chip rebuilds the machine and cold-boots it (Baughn: RAM is volatile; a chip pulled
 * from a running computer resets it). Each tick its Runner exchanges with it: bus packets in and out, the colour
 * channels' levels, keystrokes, terminal output to the players with its screen open. Saving takes its RAM
 * copy-on-write and writes it beside the world (ComputerStore); the NBT keeps only its id.
 */
public class ComputerElement extends TransparentNodeElement {
    public static final int CHIP_SLOTS = 8, DRIVE_A = 8, DRIVE_B = 9;
    /** Its state, for the lamp, the screen and Jade. */
    public static final byte OFF = 0, RUNNING = 1, SLEEPING = 2, HALTED = 3, NO_CHIPS = 4;
    /** Screen packets. */
    public static final byte TYPE_ID = 1, RESET_ID = 2, SET_ADDRESS_ID = 3, BREAK_ID = 4, WATCH_ID = 5;
    /** Server-to-screen packets: terminal bytes, or the whole terminal again. */
    public static final byte TERMINAL_ID = 1, TERMINAL_ALL_ID = 2;
    /** Terminal output kept for a screen that opens later, and the most sent to screens a tick. */
    static final int SCROLLBACK = 8192, MAX_SEND = 4096;

    final ElementMachine machine;
    public final BusPort port;
    final TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(10, 16, this);
    UUID id = UUID.randomUUID();
    Runner runner;
    /** The chips the machine was built from (item ids by slot), to see a change. */
    String builtFrom = "";
    boolean loadPending = true;
    byte status = OFF;
    final byte[] scrollback = new byte[SCROLLBACK];
    int scrollStart, scrollLen;
    final java.util.Set<UUID> watchers = new java.util.HashSet<>();
    final UUID[] mountedDisk = new UUID[2];
    final Floppy[] floppies = new Floppy[2];
    final double[] levels = new double[BusChannel.COUNT];

    public ComputerElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        machine = Machines.forElement(this, ComputerDescriptor.spec());
        port = machine.busPort();
        port.listen(true);
        slowProcessList.add(this::step);
    }

    MinecraftServer server() {
        var l = node.coordonate.world();
        return l == null ? null : l.getServer();
    }

    // ---- chips and disks

    static String chipKey(ItemStack s) {
        return s.isEmpty() || !(s.getItem() instanceof ChipItem c) ? "-" : c.kind.name();
    }

    String chipsNow() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < CHIP_SLOTS; i++) b.append(chipKey(inventory.getItem(i))).append(',');
        return b.toString();
    }

    /** The machine from the chips now in the slots, cold-booted; none if no slot holds a chip. */
    void rebuild() {
        String now = chipsNow();
        if (runner != null) {
            runner.awaitIdle();
            unmountAll();
        }
        runner = null;
        builtFrom = now;
        Machine m = new Machine();
        boolean any = false;
        for (int s = 0; s < CHIP_SLOTS; s++) {
            ItemStack st = inventory.getItem(s);
            if (!(st.getItem() instanceof ChipItem c)) continue;
            switch (c.kind) {
                case RAM -> m.mem.setRam(s, Machine.RAM_CHIP);
                case JANET -> {
                    Rom r = Roms.janet();
                    if (r != null) m.mem.setRom(s, r);
                }
            }
            any = true;
        }
        if (!any) return;
        m.reset(Machine.RESET_POWER);
        runner = new Runner(m);
        clearScrollback();
        syncDisks();
    }

    void clearScrollback() {
        scrollStart = scrollLen = 0;
        for (ServerPlayer p : viewers()) sendTerminal(p, TERMINAL_ALL_ID, new byte[0]);
    }

    /** Put the floppies in the drive slots into the drives (and take out what left). */
    void syncDisks() {
        MinecraftServer s = server();
        for (int d = 0; d < 2; d++) {
            ItemStack st = inventory.getItem(DRIVE_A + d);
            UUID want = null;
            if (st.getItem() instanceof FloppyItem && s != null) {
                want = FloppyItem.disk(st);
                if (want == null || (!want.equals(mountedDisk[d]) && !ComputerStore.mount(want))) {
                    // a blank disk, or a clone of one in another drive: it gets a disk of its own
                    UUID fresh = UUID.randomUUID();
                    if (want != null) ComputerStore.copyFloppy(s, want, fresh);
                    FloppyItem.setDisk(st, fresh);
                    want = fresh;
                    ComputerStore.mount(want);
                }
            }
            if (java.util.Objects.equals(want, mountedDisk[d])) {
                if (floppies[d] != null) floppies[d].writeProtected = FloppyItem.writeProtected(st);
                continue;
            }
            eject(d);
            if (want != null) {
                floppies[d] = new Floppy(want, ComputerStore.loadFloppy(s, want), FloppyItem.writeProtected(st));
                mountedDisk[d] = want;
                if (runner != null) runner.insert(d, floppies[d]);
            }
        }
    }

    void eject(int d) {
        if (mountedDisk[d] == null) return;
        if (runner != null) {
            runner.awaitIdle();
            runner.insert(d, null);
        }
        saveDisk(d);
        ComputerStore.unmount(mountedDisk[d]);
        mountedDisk[d] = null;
        floppies[d] = null;
    }

    void saveDisk(int d) {
        MinecraftServer s = server();
        if (floppies[d] != null && floppies[d].dirty() && s != null) ComputerStore.saveFloppy(s, floppies[d].id, floppies[d].snapshot());
    }

    void unmountAll() {
        for (int d = 0; d < 2; d++) eject(d);
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public void inventoryChange(Container inv) {
        super.inventoryChange(inv);
        if (!chipsNow().equals(builtFrom)) rebuild();
        else syncDisks();
        needPublish();
    }

    // ---- the tick

    void step(double dt) {
        if (loadPending) {
            loadPending = false;
            resume();
        }
        if (runner == null) {
            setStatus(chipsNow().replace("-,", "").isEmpty() ? NO_CHIPS : OFF);
            return;
        }
        boolean powered = machine.state() == MachineState.RUNNING;
        List<Packet> in = new ArrayList<>();
        for (Packet k; (k = port.poll()) != null; ) in.add(k);
        if (!in.isEmpty()) runner.deliver(in);
        for (int c = 0; c < levels.length; c++) levels[c] = port.read(c);
        runner.busState(port.address(), port.bus() != null, levels);
        if (runner.tick(powered)) {
            for (Packet p : runner.takePackets()) port.send(p);
            byte[] out = runner.takeTerminal();
            if (out.length > 0) terminalOut(out);
            if (powered) machine.takeJoules(machine.ratedJoulesPerTick() * Math.min(1.0, runner.lastRan() / (double) runner.budget));
        }
        watch();
        Machine m = runner.machine;
        setStatus(!powered ? OFF : m.halted() ? HALTED : runner.running() || runner.lastRan() > 0 ? RUNNING : SLEEPING);
    }

    void setStatus(byte s) {
        if (s != status) {
            status = s;
            needPublish();
        }
    }

    /** After a load: the chips' machine, resumed from its saved RAM if the chips are the same (else cold-booted). */
    void resume() {
        rebuild();
        MinecraftServer s = server();
        if (runner == null || s == null) return;
        Machine.Saved saved = ComputerStore.loadMachine(s, id);
        if (saved != null) runner.machine.load(saved);
        runner = new Runner(runner.machine);
        for (int d = 0; d < 2; d++) if (floppies[d] != null) runner.insert(d, floppies[d]);
    }

    // ---- the terminal and its viewers

    List<ServerPlayer> viewers() {
        List<ServerPlayer> l = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return l;
        for (ServerPlayer p : s.getPlayerList().getPlayers())
            if (p.containerMenu instanceof ElnNodeMenu m && m.getNode() == node) l.add(p);
        return l;
    }

    /** New viewers get the scrollback; viewers who left are forgotten. */
    void watch() {
        List<ServerPlayer> v = viewers();
        java.util.Set<UUID> now = new java.util.HashSet<>();
        for (ServerPlayer p : v) {
            now.add(p.getUUID());
            if (!watchers.contains(p.getUUID())) sendTerminal(p, TERMINAL_ALL_ID, scrollbackBytes());
        }
        watchers.clear();
        watchers.addAll(now);
    }

    void terminalOut(byte[] out) {
        for (byte b : out) {
            scrollback[(scrollStart + scrollLen) % SCROLLBACK] = b;
            if (scrollLen < SCROLLBACK) scrollLen++;
            else scrollStart = (scrollStart + 1) % SCROLLBACK;
        }
        byte[] send = out.length > MAX_SEND ? java.util.Arrays.copyOfRange(out, out.length - MAX_SEND, out.length) : out;
        for (ServerPlayer p : viewers()) if (watchers.contains(p.getUUID())) sendTerminal(p, TERMINAL_ID, send);
    }

    /** The terminal output kept for screens (tests read it too). */
    public byte[] scrollbackBytes() {
        byte[] b = new byte[scrollLen];
        for (int i = 0; i < scrollLen; i++) b[i] = scrollback[(scrollStart + i) % SCROLLBACK];
        return b;
    }

    void sendTerminal(ServerPlayer p, byte kind, byte[] bytes) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length + 64);
        DataOutputStream o = new DataOutputStream(bos);
        preparePacketForClient(o);
        try {
            o.writeByte(kind);
            o.writeInt(bytes.length);
            o.write(bytes);
        } catch (IOException e) {
            return;
        }
        node.sendPacketToClient(bos, p);
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void initialize() {
        connect();
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public byte networkUnserialize(DataInputStream stream, ServerPlayer player) {
        byte id = super.networkUnserialize(stream, player);
        try {
            switch (id) {
                case TYPE_ID -> {
                    String s = stream.readUTF();
                    if (runner != null && s.length() <= 1024) runner.type(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                case RESET_ID -> {
                    if (runner != null) {
                        runner.awaitIdle();
                        runner.machine.reset(Machine.RESET_POWER);
                        clearScrollback();
                    }
                }
                case BREAK_ID -> {
                    if (runner != null) runner.pressBreak();
                }
                case SET_ADDRESS_ID -> {
                    port.setAddress(Math.round(stream.readFloat()));
                    needPublish();
                }
                default -> {
                    return id;
                }
            }
        } catch (IOException e) {
            return unserializeNulldId;
        }
        return unserializeNulldId;
    }

    // ---- faces

    @Override
    public mods.eln.bus.BusPort busPortOn(Direction side) {
        return machine.busPortOn(side);
    }

    @Override
    public List<mods.eln.node.FacePins.FacePin> facePins(Direction side) {
        return machine.facePins(side);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return machine.electricalLoad(side, lrdu);
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return machine.connectionMask(side, lrdu);
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(machine.volts(), machine.amps());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return "";
    }

    // ---- sync and saving

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(status);
            stream.writeByte(port.address());
            stream.writeBoolean(port.bus() != null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putString("cid", id.toString());
        if (port.address() != 0) nbt.putInt("addr", port.address());
        MinecraftServer s = server();
        if (runner != null && s != null && !loadPending) {
            runner.awaitIdle();
            ComputerStore.saveMachine(s, id, runner.machine.save());
            for (int d = 0; d < 2; d++) saveDisk(d);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        try {
            id = UUID.fromString(nbt.getStringOr("cid", ""));
        } catch (IllegalArgumentException e) {
            id = UUID.randomUUID();
        }
        port.setAddress(nbt.getIntOr("addr", 0));
        loadPending = true;
    }

    @Override
    public void unload() {
        super.unload();
        if (runner != null) runner.awaitIdle();
        for (int d = 0; d < 2; d++) {
            saveDisk(d);
            if (mountedDisk[d] != null) ComputerStore.unmount(mountedDisk[d]);
        }
    }

    @Override
    public void onBreakElement() {
        if (runner != null) runner.awaitIdle();
        unmountAll();
        MinecraftServer s = server();
        if (s != null) ComputerStore.deleteMachine(s, id);
        runner = null;
        super.onBreakElement();
    }

    public byte status() {
        return status;
    }

    public String stateText() {
        return switch (status) {
            case RUNNING -> I18N.tr("Running");
            case SLEEPING -> I18N.tr("Waiting");
            case HALTED -> I18N.tr("Halted");
            case NO_CHIPS -> I18N.tr("No chips");
            default -> runner == null ? I18N.tr("No chips") : I18N.tr("No power");
        };
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new LinkedHashMap<>(machine.wailaLines());
        info.put(I18N.tr("State"), stateText());
        info.put(I18N.tr("Address"), port.address() == 0 ? I18N.tr("none (set it in its screen)") : String.valueOf(port.address()));
        return info;
    }

    public static boolean chipMayGo(ItemStack s) {
        return s.getItem() instanceof ChipItem;
    }

    public static boolean floppyMayGo(ItemStack s) {
        return s.getItem() instanceof FloppyItem;
    }
}
