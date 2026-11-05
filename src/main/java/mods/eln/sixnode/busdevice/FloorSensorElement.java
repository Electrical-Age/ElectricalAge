package mods.eln.sixnode.busdevice;

import mods.eln.bus.Packet;
import mods.eln.bus.PacketType;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.robotics.servo.ElevatorCar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

/** The Floor Sensor (FloorSensorDescriptor). */
public class FloorSensorElement extends BusDeviceElement {
    /** A car stopped level with its floor (register 0). */
    boolean level;
    /** Arrivals it sent (tests). */
    public int arrivals;

    public FloorSensorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
    }

    @Override
    protected void handle(Packet p) {
        if (p.type() == PacketType.READ && p.target() == address() && address() != 0)
            send(new Packet(p.source(), address(), PacketType.REPLY, p.index(), p.index() == 0 ? (level ? 1 : 0) : 0));
    }

    @Override
    protected void tick() {
        boolean now = carLevel();
        if (now == level) return;
        level = now;
        if (now) {
            send(new Packet(Packet.BROADCAST, address(), PacketType.ARRIVE, 0, 0));
            arrivals++;
        }
        needPublish();
    }

    /** Is a car stopped with its floor level with this block's floor, within REACH across? */
    boolean carLevel() {
        if (sixNode == null || sixNode.coordonate == null || sixNode.coordonate.world() == null) return false;
        BlockPos b = sixNode.coordonate.pos();
        double r = FloorSensorDescriptor.REACH;
        AABB look = new AABB(b.getX() - r, b.getY() - 0.5, b.getZ() - r, b.getX() + 1 + r, b.getY() + 0.5, b.getZ() + 1 + r);
        for (ElevatorCar c : sixNode.coordonate.world().getEntitiesOfClass(ElevatorCar.class, look))
            if (Math.abs(c.getBoundingBox().maxY - b.getY()) < 0.1 && c.stopped()) return true;
        return false;
    }

    public boolean level() {
        return level;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(level);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = super.getWaila();
        info.put(I18N.tr("Car"), level ? I18N.tr("level with this floor") : I18N.tr("not here"));
        return info;
    }
}
