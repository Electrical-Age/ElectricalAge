package mods.eln.gametest;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.Dimensions;
import mods.eln.network.ServerPacketHandler;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.transparent.TransparentNode;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client GUI packets reach the element (P4b): the bytes a render's clientSetFloat / clientSendId would send go through
 * ServerPacketHandler like a real packet (node header, element id, payload), for a six-node device (electrical
 * source: output voltage) and a transparent one (electrical furnace: target temperature, auto shutdown); a player
 * too far away is ignored.
 */
final class GuiPacketCase implements TestCase {
    public String id() {
        return "gui_packets";
    }

    public int width() {
        return 6;
    }

    public int ticks() {
        return 2;
    }

    BlockPos sourceAt, furnaceAt;
    SixNode source;
    TransparentNode furnace;

    public void build(TestContext ctx) {
        sourceAt = ctx.at(1, 1, 1);
        source = ctx.placeSix(LabCommands.sixId("Electrical Source"), sourceAt, Direction.YN).sixNode;
        furnaceAt = ctx.at(4, 1, 1);
        furnace = ctx.placeTransparent(LabCommands.transparentId("Electrical Furnace"), furnaceAt).node;
    }

    interface Payload {
        void write(DataOutputStream out) throws IOException;
    }

    static byte[] packet(NodeBase node, boolean six, Direction side, short elementId, Payload p) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        out.writeByte(Eln.packetPublishForNode);
        out.writeInt(node.coordonate.x);
        out.writeInt(node.coordonate.y);
        out.writeInt(node.coordonate.z);
        out.writeInt(Dimensions.id(node.coordonate.world()));
        out.writeUTF(node.getNodeUuid());
        if (six) out.writeByte(side.getInt());
        out.writeShort(elementId);
        p.write(out);
        return bos.toByteArray();
    }

    static Object field(Object o, String name) throws ReflectiveOperationException {
        var f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    public void measure(TestContext ctx) {
        FakePlayer fp = FakePlayerFactory.getMinecraft(ctx.level());
        try {
            var se = source.getElement(Direction.YN);
            short sid = (short) se.sixNodeElementDescriptor.parentItemDamage;
            fp.setPos(sourceAt.getX() + 0.5, sourceAt.getY() + 1, sourceAt.getZ() + 2);
            ServerPacketHandler.handle(packet(source, true, Direction.YN, sid, o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat(50f);
            }), fp);
            var vs = (mods.eln.sim.mna.component.VoltageSource) field(se, "voltageSource");
            ctx.checkValue("source: voltage set by packet", vs.getU(), 50);

            var fe = furnace.element;
            short fid = (short) fe.transparentNodeDescriptor.parentItemDamage;
            ServerPacketHandler.handle(packet(furnace, false, null, fid, o -> {
                o.writeByte(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement.unserializeTemperatureTarget);
                o.writeFloat(400f);
            }), fp);
            var reg = (mods.eln.sim.RegulatorProcess) field(fe, "thermalRegulator");
            ctx.checkValue("furnace: target set by packet", reg.getTarget(), 400);
            boolean autoBefore = (boolean) field(fe, "autoShutDown");
            ServerPacketHandler.handle(packet(furnace, false, null, fid,
                o -> o.writeByte(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement.unserializeAutoShutDownId)), fp);
            ctx.check("furnace: auto shutdown toggled by packet", (boolean) field(fe, "autoShutDown") != autoBefore, "");

            fp.setPos(sourceAt.getX() + 40, sourceAt.getY(), sourceAt.getZ());
            ServerPacketHandler.handle(packet(source, true, Direction.YN, sid, o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat(200f);
            }), fp);
            ctx.checkValue("source: a far player's packet is ignored", vs.getU(), 50);
            // the wrench key (packetPlayerKey): a held key makes a switch open its screen instead of toggling
            fp.setPos(sourceAt.getX() + 0.5, sourceAt.getY() + 1, sourceAt.getZ() + 2);
            var ws = ctx.placeSix(LabCommands.sixId("Wireless Switch"), ctx.at(2, 1, 2), Direction.YN);
            boolean before = (boolean) field(ws, "state");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeByte(Eln.packetPlayerKey);
            out.writeByte(mods.eln.server.PlayerKeys.WRENCH);
            out.writeBoolean(true);
            ServerPacketHandler.handle(bos.toByteArray(), fp);
            ctx.check("wrench key: the player counts as using a wrench", mods.eln.misc.Utils.isPlayerUsingWrench(fp), "");
            boolean consumed = ws.onBlockActivated(fp, Direction.YN, 0.5f, 0.1f, 0.5f);
            ctx.check("wrench key: the wireless switch doesn't toggle", !consumed && (boolean) field(ws, "state") == before, "consumed " + consumed);
            mods.eln.server.PlayerKeys.set(fp, mods.eln.server.PlayerKeys.WRENCH, false);
            // hub: the grid toggle (HubScreen click)
            ctx.floor(ctx.at(3, 0, 2));
            var hubNode = ctx.placeSix(LabCommands.sixId("Hub"), ctx.at(3, 1, 2), Direction.YN).sixNode;
            var hub = hubNode.getElement(Direction.YN);
            boolean[] grid = (boolean[]) field(hub, "connectionGrid");
            boolean g0 = grid[0];
            ServerPacketHandler.handle(packet(hubNode, true, Direction.YN, (short) hub.sixNodeElementDescriptor.parentItemDamage, o -> {
                o.writeByte(mods.eln.sixnode.hub.HubElement.clientConnectionGridToggle);
                o.writeByte(0);
            }), fp);
            ctx.check("hub: link 0 toggled by packet", ((boolean[]) field(hub, "connectionGrid"))[0] != g0, "");
        } catch (Exception e) {
            ctx.check("gui packets", false, e.toString());
        }
    }
}
