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
 * item): its inventory goes into the dropped item's data and comes back when it is placed. The picture is kept in the
 * world's save; not in the item.
 */
public class GroundRadarElement extends TransparentNodeElement {
    public static final byte SCAN_ID = 1, RANGE_ID = 2, DETAIL_ID = 3;
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

    /** What the batteries can give together over a tick (W): each its discharge rate, or what it holds left. */
    double rate() {
        return rate(0.05);
    }

    /** As rate(), over dt. */
    double rate(double dt) {
        double w = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (Utils.getItemObject(s) instanceof IItemEnergyBattery b) w += Math.min(b.getDischagePower(s), b.getEnergy(s) / dt);
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
     * Draw w watts for dt from the batteries, each giving at most its own discharge rate, shared in proportion to the
     * energy they hold; false (and nothing taken) if together they can't.
     */
    boolean draw(double w, double dt) {
        int n = inventory.getContainerSize();
        double[] can = new double[n];
        double total = 0;
        for (int i = 0; i < n; i++) {
            ItemStack s = inventory.getItem(i);
            if (Utils.getItemObject(s) instanceof IItemEnergyBattery b) total += can[i] = Math.min(b.getEnergy(s), b.getDischagePower(s) * dt);
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
     * A tick of a scan: as many cells as the batteries' discharge rate pays for this tick (J_PER_CELL each; Baughn: energy
     * goes as resolution x range, and the time as how fast the batteries can deliver it), at most SAMPLES_PER_TICK
     * samples of work; done once every cell is read and MIN_S has passed.
     */
    void tick(double dt) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---- the batteries ride in the item

    @Override
    public CompoundTag getItemStackNBT() {
        CompoundTag nbt = new CompoundTag();
        Utils.writeToNBT(nbt, "inv", inventory);
        return nbt;
    }

    @Override
    public void readItemStackNBT(CompoundTag nbt) {
        super.readItemStackNBT(nbt);
        if (nbt != null) Utils.readFromNBT(nbt, "inv", inventory);
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
        nbt.putByte("range", (byte) range);
        nbt.putByte("detail", (byte) detail);
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
        range = Math.max(1, Math.min(GroundRadarDescriptor.MAX_RANGE, nbt.getByteOr("range", (byte) 1)));
        detail = Math.max(1, Math.min(GroundRadarDescriptor.MAX_DETAIL, nbt.getByteOr("detail", (byte) 1)));
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
        Map<String, String> info = new HashMap<>();
        double cap = capacity();
        info.put(I18N.tr("Batteries"), cap > 0 ? Utils.plotPercent(charge() / cap) : I18N.tr("None"));
        if (scanning != null) info.put(I18N.tr("Scanning"), Utils.plotPercent(Math.max(0, progress())));
        else if (fault != null) info.put(I18N.tr("Fault"), fault);
        else if (picture != null) {
            long ago = (node.coordonate.world().getGameTime() - pictureAt) / 20;
            info.put(I18N.tr("Last scan"), I18N.tr("%1$ ago", GroundRadarDescriptor.ago(ago)));
        }
        return info;
    }
}
