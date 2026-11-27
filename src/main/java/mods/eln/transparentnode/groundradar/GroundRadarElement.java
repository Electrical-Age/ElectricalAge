package mods.eln.transparentnode.groundradar;

import mods.eln.i18n.I18N;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Ground Radar: a scan, asked for from its screen, reads the ground under it (GroundScan, at the screen's range and
 * detail) as fast as its batteries pay for the cells (tick); the finished picture goes to every player with the screen
 * open, and to each player who opens it later. The batteries stay in it when it is broken (Baughn: they remain in the
 * item): its inventory and the goggles' band go into the dropped item's data and come back when it is placed. The
 * picture is kept in the world's save; not in the item.
 */
public class GroundRadarElement extends TransparentNodeElement {
    public static final byte SCAN_ID = 1, RANGE_ID = 2, DETAIL_ID = 3, ADDRESS_ID = 4, BAND_ID = 5;

    /**
     * Its RS485 registers (Baughn: a bus port; players get creative). READ answers with a REPLY; WRITE sets. 0 state (0
     * idle, 1 scanning, 2 a fault), 1 progress (0..1, -1 idle), 2 range, 3 detail (writable), 4 the picture's layers, 5
     * its cells across, 6..8 the cursor's layer, x and z cell (writable), 9 density at the cursor (g/cm3), 10 its flags
     * (GroundScan: 1 liquid, 2 oil, 4 lava, 8 a structure, 16 unknown, 32 sky), 11 its signal (0..1), 12 the cursor layer's
     * depth under the radar and 13 its cell size (blocks), 14 the depth of the first oil under the centre and 15 of the
     * first void (-1: none), 16 the batteries' charge (0..1), 17 the last picture's age (s, -1: none); write 20 to scan,
     * 21 k to have layer k's densities sent back as data packets (a byte a cell, 0..255 for 0..8.5 g/cm3), 22 k its
     * flags (index = the 64-byte piece, row by row, i along x; a new request replaces one still going out).
     */
    public static final int REG_STATE = 0, REG_PROGRESS = 1, REG_RANGE = 2, REG_DETAIL = 3, REG_LAYERS = 4, REG_CELLS = 5,
        REG_CUR_LAYER = 6, REG_CUR_X = 7, REG_CUR_Z = 8, REG_DENSITY = 9, REG_FLAGS = 10, REG_SIGNAL = 11,
        REG_LAYER_DEPTH = 12, REG_LAYER_CELL = 13, REG_OIL_DEPTH = 14, REG_VOID_DEPTH = 15, REG_CHARGE = 16, REG_AGE = 17,
        REG_SCAN = 20, REG_SEND_DENSITY = 21, REG_SEND_FLAGS = 22;
    /** Data packets it sends a tick at most (the bus carries 64 a tick in all). */
    static final int DATA_PER_TICK = 16;

    public final mods.eln.bus.BusPort port = new mods.eln.bus.BusPort();
    private final java.util.ArrayDeque<mods.eln.bus.Packet> outbox = new java.util.ArrayDeque<>();
    private int curLayer, curX, curZ;
    /** The last picture decoded (for the bus), and the bytes it came from. */
    private GroundScan decoded;
    private byte[] decodedFrom;
    /** Kinds of the packets to the screen. */
    public static final byte PICTURE_ID = 1;

    final GroundRadarDescriptor descriptor;
    final TransparentNodeElementInventory inventory;
    /** The scan under way, or null. */
    GroundScan scanning;
    double scanTime;
    private int ticks;
    /** The scan's settings (the screen's sliders): range 1..3 (24, 48, 72 cells across), detail 1..4 (cells grow 1, 2, 3,
     *  4 times slower with depth). */
    int range = 1, detail = 1;
    /** Energy paid for this scan's next cells (J). */
    private double credit;
    /** The last finished picture (GroundScan.encode), or null, and the game time it was finished at. */
    byte[] picture;
    long pictureAt;
    /** Why the last scan stopped short, or null. */
    String fault;
    private final java.util.Set<java.util.UUID> sentTo = new java.util.HashSet<>();

    public GroundRadarElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (GroundRadarDescriptor) descriptor;
        inventory = new TransparentNodeElementInventory(GroundRadarDescriptor.BATTERY_SLOTS, 1, this) {
            @Override
            public boolean isItemValidForSlot(int i, ItemStack s) {
                return Utils.getItemObject(s) instanceof IItemEnergyBattery;
            }
        };
        slowProcessList.add(this::tick);
        port.listen(true);
    }

    // ---- the bus

    /** Its port is on its back. */
    @Override
    public mods.eln.bus.BusPort busPortOn(Direction side) {
        return front != null && side == front.getInverse() ? port : null;
    }

    public void setAddress(int a) {
        port.setAddress(mods.eln.bus.Packet.deviceAddress(a) ? a : 0);
        needPublish();
    }

    GroundScan picture() {
        if (picture != decodedFrom) {
            decoded = picture == null ? null : GroundScan.decode(picture);
            decodedFrom = picture;
        }
        return decoded;
    }

    /** The packets for it, then up to DATA_PER_TICK of the data it owes. */
    void serveBus() {
        for (mods.eln.bus.Packet k; (k = port.poll()) != null; ) {
            // (answers go to the asker: only a device address, not a broadcast or a colour channel a forged source names)
            if (k.target() != port.address() || port.address() == 0 || !mods.eln.bus.Packet.deviceAddress(k.source())) continue;
            switch (k.type()) {
                case WRITE -> write(k.source(), k.index(), k.value());
                case READ -> {
                    double v = register(k.index());
                    if (!Double.isNaN(v)) port.send(new mods.eln.bus.Packet(k.source(), port.address(), mods.eln.bus.PacketType.REPLY, k.index(), (float) v));
                }
                default -> {
                }
            }
        }
        for (int i = 0; i < DATA_PER_TICK && !outbox.isEmpty(); i++) port.send(outbox.poll());
    }

    void write(int from, int r, float v) {
        int iv = Math.round(v);
        switch (r) {
            case REG_RANGE -> setSettings(iv, detail);
            case REG_DETAIL -> setSettings(range, iv);
            case REG_CUR_LAYER -> curLayer = iv;
            case REG_CUR_X -> curX = iv;
            case REG_CUR_Z -> curZ = iv;
            case REG_SCAN -> startScan();
            case REG_SEND_DENSITY, REG_SEND_FLAGS -> {
                GroundScan p = picture();
                if (p == null || iv < 0 || iv >= p.layers.length) return;
                byte[] src = r == REG_SEND_DENSITY ? p.densityBytes() : p.flags;
                int off = iv * p.n * p.n, len = p.n * p.n;
                outbox.clear();   // (a new request replaces one still going out)
                for (int piece = 0; piece * mods.eln.bus.Packet.MAX_DATA < len; piece++) {
                    int a = piece * mods.eln.bus.Packet.MAX_DATA, b = Math.min(len, a + mods.eln.bus.Packet.MAX_DATA);
                    outbox.add(mods.eln.bus.Packet.data(from, port.address(), piece, java.util.Arrays.copyOfRange(src, off + a, off + b)));
                }
            }
            default -> {
            }
        }
    }

    /** Register r's value, or NaN: no such register (or nothing there). */
    public double register(int r) {
        GroundScan p = picture();
        boolean cur = p != null && curLayer >= 0 && curLayer < p.layers.length && curX >= 0 && curX < p.n && curZ >= 0 && curZ < p.n;
        return switch (r) {
            case REG_STATE -> scanning != null ? 1 : fault != null ? 2 : 0;
            case REG_PROGRESS -> progress();
            case REG_RANGE -> range;
            case REG_DETAIL -> detail;
            case REG_LAYERS -> p == null ? 0 : p.layers.length;
            case REG_CELLS -> p == null ? 0 : p.n;
            case REG_CUR_LAYER -> curLayer;
            case REG_CUR_X -> curX;
            case REG_CUR_Z -> curZ;
            case REG_DENSITY -> cur ? p.densityAt(curLayer, curX, curZ) : -1;
            case REG_FLAGS -> cur ? p.flagsAt(curLayer, curX, curZ) : -1;
            case REG_SIGNAL -> cur ? p.signalAt(curLayer, curX, curZ) : -1;
            case REG_LAYER_DEPTH -> p != null && curLayer >= 0 && curLayer < p.layers.length ? p.layers[curLayer].depth() : -1;
            case REG_LAYER_CELL -> p != null && curLayer >= 0 && curLayer < p.layers.length ? p.layers[curLayer].cell() : -1;
            case REG_OIL_DEPTH -> firstUnderCentre(p, true);
            case REG_VOID_DEPTH -> firstUnderCentre(p, false);
            case REG_CHARGE -> capacity() > 0 ? charge() / capacity() : 0;
            case REG_AGE -> p == null ? -1 : (node.coordonate.world().getGameTime() - pictureAt) / 20.0;
            default -> Double.NaN;
        };
    }

    /** The depth (blocks under the radar) of the first oil cell, or of the first cell under 0.5 g/cm3, in the centre column; -1: none. */
    static double firstUnderCentre(GroundScan p, boolean oil) {
        if (p == null) return -1;
        int c = p.n / 2;
        for (int k = 0; k < p.layers.length; k++) {
            int f = p.flagsAt(k, c, c);
            if ((f & GroundScan.UNKNOWN) != 0) continue;
            if (oil ? (f & GroundScan.OIL) != 0 : (f & (GroundScan.LIQUID | GroundScan.SKY)) == 0 && p.densityAt(k, c, c) < 0.5) return p.layers[k].depth();
        }
        return -1;
    }

    /** The energy in the batteries (J), and what they hold full. */
    double charge() {
        double e = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (Utils.getItemObject(s) instanceof IItemEnergyBattery b) e += b.getEnergy(s);
        }
        return e;
    }

    /** What the batteries can give together over a tick (W): each DRAIN times its discharge rate, or what it holds left. */
    double rate() {
        return rate(0.05);
    }

    /** As rate(), over dt. */
    double rate(double dt) {
        double w = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (Utils.getItemObject(s) instanceof IItemEnergyBattery b) w += Math.min(GroundRadarDescriptor.DRAIN * b.getDischagePower(s), b.getEnergy(s) / dt);
        }
        return w;
    }

    double capacity() {
        double e = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (Utils.getItemObject(s) instanceof IItemEnergyBattery b) e += b.getEnergyMax(s);
        }
        return e;
    }

    /**
     * Draw w watts for dt from the batteries, each giving at most DRAIN times its own discharge rate, shared in proportion to the
     * energy they hold; false (and nothing taken) if together they can't.
     */
    boolean draw(double w, double dt) {
        int n = inventory.getContainerSize();
        double[] can = new double[n];
        double total = 0;
        for (int i = 0; i < n; i++) {
            ItemStack s = inventory.getItem(i);
            if (Utils.getItemObject(s) instanceof IItemEnergyBattery b) total += can[i] = Math.min(b.getEnergy(s), GroundRadarDescriptor.DRAIN * b.getDischagePower(s) * dt);
        }
        double j = w * dt;
        if (total < j || total <= 0) return false;
        for (int i = 0; i < n; i++) {
            if (can[i] <= 0) continue;
            ItemStack s = inventory.getItem(i);
            IItemEnergyBattery b = (IItemEnergyBattery) Utils.getItemObject(s);
            b.setEnergy(s, b.getEnergy(s) - j * can[i] / total);
        }
        return true;
    }

    /**
     * Stasis Delvers: it runs in stopped land too. A radar is prospecting gear, carried out to land nobody holds yet; it
     * reads the ground and moves nothing.
     */
    @Override
    public boolean timeExempt() {
        return true;
    }

    /** Keep a picture made elsewhere (the lab's /elnlab radar) as its last one. */
    public void keepPicture(GroundScan s) {
        picture = s.encode();
        pictureAt = node.coordonate.world().getGameTime();
        sentTo.clear();
        needPublish();
    }

    /** Cells across the auto-miner's views (2-block cells at its mining height: its 48 m reach). */
    public static final int MINER_CELLS = 24;

    /** The auto-miner's slice from above at its drill head: one layer of 2-block cells holding the head's level and the one under it. */
    public static GroundScan minerSlice(ServerLevel level, net.minecraft.core.BlockPos head) {
        GroundRadarWorld world = new GroundRadarWorld(level, head.getX(), head.getZ(), MINER_CELLS + 16);
        GroundScan s = GroundScan.slab(world, head.getX(), head.getY() + 1, head.getZ(), level.getMinY(), MINER_CELLS, new GroundScan.Layer(0, 2));
        while (!s.step(1 << 20)) ;
        return s;
    }

    /** The auto-miner's side views: the cone under top (the miner) to the world's floor, its cuts along x and z only. */
    public static GroundScan minerCross(ServerLevel level, net.minecraft.core.BlockPos top) {
        int view = level.getServer() == null ? 10 : level.getServer().getPlayerList().getViewDistance();
        // (structures within 96 blocks: it is redone every 5 s, and a wide structure lookup each time cost more than the cuts)
        int half = Math.min(GroundScan.cellAt(top.getY() - level.getMinY(), GroundScan.GROWTH) * MINER_CELLS / 2 + 16, Math.min(96, (view + 1) * 16));
        GroundRadarWorld world = new GroundRadarWorld(level, top.getX(), top.getZ(), half);
        GroundScan s = GroundScan.cross(world, top.getX(), top.getY(), top.getZ(), level.getMinY(), MINER_CELLS, GroundScan.GROWTH);
        while (!s.step(1 << 20)) ;
        return s;
    }

    /** A whole scan of the ground under pos at once, at this range and detail (the lab's /elnlab radar). */
    public static GroundScan scanNow(ServerLevel level, net.minecraft.core.BlockPos pos, int range, int detail) {
        GroundScan s = newScan(level, pos, range, detail);
        while (!s.step(1 << 20)) ;
        return s;
    }

    static GroundScan newScan(ServerLevel level, net.minecraft.core.BlockPos p, int range, int detail) {
        int floor = level.getMinY(), n = GroundRadarDescriptor.cellsAcross(range), growth = GroundRadarDescriptor.growth(detail);
        // (structures within the cone's widest half-width, but no farther than players' view: past that nothing is loaded)
        int view = level.getServer() == null ? 10 : level.getServer().getPlayerList().getViewDistance();
        int deepest = Math.min(GroundScan.cellAt(p.getY() - floor, growth) * n / 2 + 16, (view + 1) * 16);
        GroundRadarWorld world = new GroundRadarWorld(level, p.getX(), p.getZ(), deepest);
        return new GroundScan(world, p.getX(), p.getY(), p.getZ(), floor, n, growth);
    }

    /** Set the scan's range and detail (clamped). */
    public void setSettings(int range, int detail) {
        this.range = Math.max(1, Math.min(GroundRadarDescriptor.MAX_RANGE, range));
        this.detail = Math.max(1, Math.min(GroundRadarDescriptor.MAX_DETAIL, detail));
        if (scanning == null) fault = null;   // (another scan: the last complaint is stale)
        needPublish();
    }

    /** What a scan from here at the current settings costs (J). */
    double scanEnergy() {
        var level = node.coordonate.world();
        int depth = node.coordonate.pos().getY() - level.getMinY();
        return GroundRadarDescriptor.energy(depth, range, detail);
    }

    /** Start a scan; false (with fault set) if it can't. */
    public boolean startScan() {
        if (scanning != null) return false;
        if (!(node.coordonate.world() instanceof ServerLevel level)) return false;
        double need = scanEnergy();
        if (charge() < need) {
            fault = capacity() <= 0 ? I18N.tr("The batteries are flat")
                : I18N.tr("This scan needs %1$", mods.eln.api.Meters.joules(need));
            needPublish();
            return false;
        }
        GroundScan s = newScan(level, node.coordonate.pos(), range, detail);
        if (s.cells() == 0) {
            fault = I18N.tr("No ground under it");
            needPublish();
            return false;
        }
        scanning = s;
        scanTime = 0;
        credit = 0;
        fault = null;
        needPublish();
        return true;
    }

    /**
     * A tick of a scan: as many cells as DRAIN times the batteries' discharge rate pays for this tick (J_PER_CELL each;
     * Baughn: energy goes as resolution x range, and the time as how fast the batteries can deliver it), at most SAMPLES_PER_TICK
     * samples of work; done once every cell is read and MIN_S has passed.
     */
    void tick(double dt) {
        serveBus();
        if (scanning == null) {
            sendToNewViewers();
            return;
        }
        scanTime += dt;
        // (the batteries give all they can each tick, into a credit the cells are read against: what a tick pays for
        // beyond whole cells carries over, so slow batteries still get there, slowly; while the work cap holds the reading
        // back, no more is drawn)
        boolean paid = true;
        if (!scanning.done() && credit < 4 * rate(dt) * dt + GroundRadarDescriptor.J_PER_CELL) {
            double w = rate(dt);
            paid = w > 0 && draw(w, dt);
            if (paid) credit += w * dt;
        }
        int cells = (int) Math.floor(credit / GroundRadarDescriptor.J_PER_CELL);
        credit -= scanning.stepCells(cells, GroundRadarDescriptor.SAMPLES_PER_TICK) * GroundRadarDescriptor.J_PER_CELL;
        if (!paid) {
            scanning = null;
            fault = I18N.tr("The batteries ran flat during the scan");
            needPublish();
            return;
        }
        if (scanning.done() && scanTime >= GroundRadarDescriptor.MIN_S) {
            picture = scanning.encode();
            pictureAt = node.coordonate.world().getGameTime();
            scanning = null;
            sentTo.clear();
            needPublish();
        } else if (++ticks % 5 == 0) needPublish();
        sendToNewViewers();
    }

    /** The last finished picture, or null. */
    public GroundScan lastPicture() {
        return picture == null ? null : GroundScan.decode(picture);
    }

    /** The scan's progress, 0..1 (by time and by cells, whichever is behind), or -1 when not scanning. */
    double progress() {
        if (scanning == null) return -1;
        return Math.min(scanning.progress(), Math.min(1, scanTime / GroundRadarDescriptor.MIN_S));
    }

    /** The picture to each player with the screen open who hasn't had it yet (opening the screen again resends it). */
    private void sendToNewViewers() {
        List<ServerPlayer> v = mods.eln.gui.ElnNodeMenu.viewers(node);
        sentTo.retainAll(v.stream().map(Player::getUUID).toList());
        if (picture == null) return;
        for (ServerPlayer p : v) if (sentTo.add(p.getUUID())) sendPicture(p);
    }

    private void sendPicture(ServerPlayer p) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(picture.length + 64);
        DataOutputStream o = new DataOutputStream(bos);
        preparePacketForClient(o);
        try {
            o.writeByte(PICTURE_ID);
            o.writeLong(pictureAt);
            o.writeInt(picture.length);
            o.write(picture);
        } catch (IOException e) {
            return;
        }
        node.sendPacketToClient(bos, p);
    }

    @Override
    public byte networkUnserialize(DataInputStream stream) {
        byte id = super.networkUnserialize(stream);
        try {
            switch (id) {
                case SCAN_ID -> startScan();
                case RANGE_ID -> setSettings(Math.round(stream.readFloat()), detail);
                case DETAIL_ID -> setSettings(range, Math.round(stream.readFloat()));
                case ADDRESS_ID -> setAddress(Math.round(stream.readFloat()));
                case BAND_ID -> band = RadarBand.read(stream.readByte(), stream.readFloat(), stream.readFloat());
                default -> {
                    return id;
                }
            }
        } catch (IOException e) {
            return unserializeNulldId;
        }
        if (id != SCAN_ID) needPublish();
        return unserializeNulldId;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            double cap = capacity();
            stream.writeFloat(cap > 0 ? (float) (charge() / cap) : -1);
            stream.writeFloat((float) progress());
            stream.writeUTF(fault == null ? "" : fault);
            stream.writeByte(range);
            stream.writeByte(detail);
            stream.writeFloat((float) scanEnergy());
            stream.writeFloat((float) rate());
            stream.writeShort(port.address());
            stream.writeBoolean(port.bus() != null);
            stream.writeByte(band.kind());
            stream.writeFloat((float) band.lo());
            stream.writeFloat((float) band.hi());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---- the batteries and the goggles' band ride in the item (Baughn: the band is there when it is next placed)

    /** What the goggles show (RadarBand; Everything until a player picks another). */
    RadarBand band = RadarBand.DEFAULT;

    public RadarBand band() {
        return band;
    }

    static void writeBand(CompoundTag nbt, RadarBand b) {
        nbt.putByte("band", (byte) b.kind());
        if (b.kind() == RadarBand.CUSTOM) {
            nbt.putFloat("bandLo", (float) b.lo());
            nbt.putFloat("bandHi", (float) b.hi());
        }
    }

    static RadarBand readBand(CompoundTag nbt) {
        return RadarBand.read(nbt.getByteOr("band", (byte) RadarBand.EVERYTHING), nbt.getFloatOr("bandLo", 0), nbt.getFloatOr("bandHi", 0));
    }

    @Override
    public CompoundTag getItemStackNBT() {
        CompoundTag nbt = new CompoundTag();
        Utils.writeToNBT(nbt, "inv", inventory);
        writeBand(nbt, band);
        return nbt;
    }

    @Override
    public void readItemStackNBT(CompoundTag nbt) {
        super.readItemStackNBT(nbt);
        if (nbt != null) {
            Utils.readFromNBT(nbt, "inv", inventory);
            band = readBand(nbt);
        }
    }

    /** Set while it is being broken: the inventory is then not spilled (it rides in the dropped item instead). */
    private boolean breaking;

    @Override
    public void onBreakElement() {
        // (Baughn: the batteries remain in the item when it is broken; super spills getInventory() before it makes the
        // drop, whose data getItemStackNBT writes from the inventory itself)
        // (unless the break drops nothing, as in creative: then they spill, as other devices' contents do)
        breaking = Utils.mustDropItem(node.removedByPlayer);
        super.onBreakElement();
    }

    @Override
    public Container getInventory() {
        return breaking ? null : inventory;
    }

    // ---- the picture is kept with the world

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (port.address() != 0) nbt.putInt("addr", port.address());
        nbt.putByte("range", (byte) range);
        nbt.putByte("detail", (byte) detail);
        writeBand(nbt, band);
        if (picture != null) {
            nbt.putByteArray("picture", picture);
            nbt.putLong("pictureAt", pictureAt);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        picture = nbt.getByteArray("picture").orElse(null);
        pictureAt = nbt.getLongOr("pictureAt", 0);
        port.setAddress(nbt.getIntOr("addr", 0));
        range = Math.max(1, Math.min(GroundRadarDescriptor.MAX_RANGE, nbt.getByteOr("range", (byte) 1)));
        detail = Math.max(1, Math.min(GroundRadarDescriptor.MAX_DETAIL, nbt.getByteOr("detail", (byte) 1)));
        band = readBand(nbt);
        if (picture != null && GroundScan.decode(picture) == null) picture = null;   // (an older picture's format)
    }

    // ---- no wires

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        connect();
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    public void inventoryChange(Container inventory) {
        fault = null;   // (batteries changed: the last scan's complaint is stale)
        needPublish();
    }

    @Override
    public Map<String, String> getWaila() {
        mods.eln.compat.jade.ProbeLines info = mods.eln.compat.jade.ProbeLines.of();
        double cap = capacity();
        info.headline(I18N.tr("Batteries"), cap > 0 ? Utils.plotPercent(charge() / cap) : I18N.tr("None"));
        if (scanning != null) info.headline(I18N.tr("Scanning"), Utils.plotPercent(Math.max(0, progress())));
        else if (fault != null) info.headlineFirst(I18N.tr("Fault"), fault);
        else if (picture != null) {
            long ago = (node.coordonate.world().getGameTime() - pictureAt) / 20;
            info.headline(I18N.tr("Last scan"), I18N.tr("%1$ ago", GroundRadarDescriptor.ago(ago)));
        }
        return info;
    }
}
