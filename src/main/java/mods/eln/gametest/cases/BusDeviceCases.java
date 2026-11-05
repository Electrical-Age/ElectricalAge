package mods.eln.gametest.cases;

import mods.eln.bus.BusNet;
import mods.eln.bus.BusPort;
import mods.eln.bus.Packet;
import mods.eln.bus.PacketType;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.sixnode.busdevice.AddressableButtonElement;
import mods.eln.sixnode.busdevice.AddressableLightElement;
import mods.eln.sixnode.busdevice.BusDeviceElement;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The RS485 bus devices (mods.eln.sixnode.busdevice; Baughn 2026-11-05): an Addressable Button and an Addressable Light
 * on a bus cable, with a listening port for a computer. The button's press reaches the listener from the button's
 * address, power sent to the button lights its lamp, power sent to the light sets its brightness and block light, and
 * addresses (17..255 only) survive a save and load.
 */
public final class BusDeviceCases {
    private BusDeviceCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new ButtonAndLight());
        cases.add(new Addresses());
    }

    static final int BUTTON = 2 + (117 << 6), LIGHT = 3 + (117 << 6);

    /** A device on the floor of a voxel block at p, its bus edge toward dir. */
    static BusDeviceElement device(TestContext c, int id, BlockPos p, Direction dir) {
        var d = (BusDeviceElement) CableCases.host(c, id, p, Direction.YN);
        Wp9aCases.orient(d, l -> l, dir);
        return d;
    }

    /** A port standing in for a computer, linked to the bus cable at p (its first bus port). */
    static BusPort computer(TestContext c, BlockPos p) {
        BusPort pc = new BusPort();
        pc.setAddress(200);
        pc.listen(true);
        var v = (VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, c.level()));
        BusNet.link(pc, v.busPorts().get(0));
        return pc;
    }

    /** Button (x 1, address 30) and light (x 3, address 31) on bus cable (x 2), the computer on the cable. */
    static final class ButtonAndLight implements TestCase {
        AddressableButtonElement button;
        AddressableLightElement light;
        BusPort pc;
        String why = "";
        final List<Packet> heard = new ArrayList<>();
        boolean litAfter, onBus;
        int lightValue = -1;

        public String id() {
            return "bus_device_button_and_light";
        }

        public int ticks() {
            return 40;
        }

        public void build(TestContext c) {
            try {
                button = (AddressableButtonElement) device(c, BUTTON, c.at(1, 1, 1), Direction.XP);
                BusModuleCases.across(c, c.at(2, 1, 1), BusModuleCases.BUS);
                light = (AddressableLightElement) device(c, LIGHT, c.at(3, 1, 1), Direction.XN);
                button.setAddress(30);
                light.setAddress(31);
            } catch (RuntimeException e) {
                why = String.valueOf(e.getMessage());
                return;
            }
            c.after(5, () -> {
                pc = computer(c, c.at(2, 1, 1));
                BusNet.refresh();
            });
            c.after(10, button::press);
            c.after(14, () -> {
                for (Packet k; (k = pc.poll()) != null; ) heard.add(k);
                onBus = button.port.bus() != null && button.port.bus() == light.port.bus();
                pc.send(new Packet(30, 200, PacketType.POWER, 0, 1));
                pc.send(new Packet(31, 200, PacketType.POWER, 0, 0.5f));
            });
            c.after(18, () -> {
                litAfter = button.lit();
                lightValue = light.getLightValue();
            });
        }

        public void measure(TestContext c) {
            c.check("bus device: built", why.isEmpty(), why);
            if (!why.isEmpty()) return;
            BusNet.detach(List.of(pc));
            c.check("bus device: button and light on one bus", onBus, "");
            c.check("bus device: the press reached the computer, from the button's address",
                heard.size() == 1 && heard.get(0).type() == PacketType.PRESS && heard.get(0).source() == 30
                    && heard.get(0).target() == Packet.BROADCAST, heard.toString());
            c.check("bus device: power sent to the button lights its lamp", litAfter, "");
            Wp9cCases.within(c, "bus device: power sent to the light sets it [0..1]", light.level(), 0.5, 1e-6);
            c.check("bus device: and its block light", lightValue == 4, "" + lightValue);
            var w = button.getWaila();
            c.check("bus device: Jade shows the address and the lamp", "30".equals(w.get("Address")) && "ON".equals(w.get("Lamp")), w.toString());
        }
    }

    /** Addresses: 1..16 and 256 refused (none), 17 and 255 taken; saved and loaded. */
    static final class Addresses implements TestCase {
        AddressableButtonElement button;
        String why = "";

        public String id() {
            return "bus_device_addresses";
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            try {
                button = (AddressableButtonElement) device(c, BUTTON, c.at(1, 1, 1), Direction.XP);
            } catch (RuntimeException e) {
                why = String.valueOf(e.getMessage());
            }
        }

        public void measure(TestContext c) {
            c.check("bus device addresses: built", why.isEmpty(), why);
            if (!why.isEmpty()) return;
            int[] refused = {1, 16, 256, -1};
            boolean none = true;
            for (int a : refused) {
                button.networkUnserialize(c.stream(o -> {
                    o.writeByte(BusDeviceElement.setAddressId);
                    o.writeInt(a);
                }));
                none &= button.address() == 0;
            }
            c.check("bus device addresses: colour channels, broadcast and past 255 are refused", none, "" + button.address());
            button.networkUnserialize(c.stream(o -> {
                o.writeByte(BusDeviceElement.setAddressId);
                o.writeInt(255);
            }));
            c.check("bus device addresses: the screen sets 255", button.address() == 255, "" + button.address());
            var tag = new net.minecraft.nbt.CompoundTag();
            button.writeToNBT(tag);
            var fresh = (AddressableButtonElement) device(c, BUTTON, c.at(3, 1, 1), Direction.XP);
            fresh.readFromNBT(tag);
            c.check("bus device addresses: saved and loaded", fresh.address() == 255 && fresh.port.address() == 255, "" + fresh.address());
        }
    }
}
