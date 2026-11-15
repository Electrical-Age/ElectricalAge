package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.voxel.CablePlacer;
import mods.eln.node.voxel.CableRouter;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelNodeEntity;
import mods.eln.sixnode.hub.HubSlots.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Hub's cable connector as a client sees it (todo #484: a client saw no connector on a hub with cable kits in its
 * slots, and the cables stopping at the edge of its block). A Hub on its face and one off centre get a kit through
 * their screen, then a cable is routed to them a few ticks later, as a player does it. The client is the last packet
 * the server sent each hub's block (NodeBase.publishWatch), read into a fresh block entity: its render must have the
 * kit's cable on that side, draw the connector there (on the face: the stub from the block's edge to the hub; off
 * centre: the pin), and look the same with its static parts in the chunk.
 */
public final class HubRenderCases {
    private HubRenderCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new ConnectorCase());
    }

    static final class ConnectorCase implements TestCase {
        final SixNodeElement[] hubs = new SixNodeElement[2];
        final BlockPos[] at = new BlockPos[2];
        final Map<NodeBase, List<byte[]>> sent = new IdentityHashMap<>();
        final String[] laid = {"", ""};

        public String id() {
            return "hub_kit_connector";
        }

        public int width() {
            return 12;
        }

        public void build(TestContext c) {
            mods.eln.client.ClientRenders.registerAll();
            for (int x = 0; x < 12; x++) for (int z = 0; z < 3; z++) c.floor(c.at(x, 0, z));
            Player player = VoxelDeviceCases.player(c);
            for (int k = 0; k < 2; k++) {
                at[k] = c.at(2 + 5 * k, 1, 0);
                VoxelNode v = VoxelDeviceCases.block(c, at[k]);
                if (k == 0) {
                    // on its face, as a player places it in the middle of a floor
                    if (!v.createSubBlock(mods.eln.Eln.sixNodeItem.getDescriptor(Wp9cCases.HUB).newItemStack(), Direction.YN, player))
                        throw new IllegalStateException("hub not placed on its face");
                    hubs[k] = v.getElement(Direction.YN.getInt());
                } else hubs[k] = VoxelDeviceCases.put(c, v, "Hub", Direction.YN, 0, 0, 0, Direction.ZN);
            }
            NodeBase.publishWatch = (node, bytes) -> {
                for (var h : hubs) if (h != null && h.sixNode == node) synchronized (sent) {
                    sent.computeIfAbsent(node, n -> new java.util.ArrayList<>()).add(bytes);
                }
            };
            // a kit in the slot the screen shows on top, the player facing south: the south side
            c.after(4, () -> {
                float yaw = player.getYRot();
                try {
                    for (var h : hubs) HubSlotCases.click(player, HubSlotCases.SOUTH, h.sixNode, h, Place.TOP, KitDeviceCases.kit(KitDeviceCases.LV, null, 4));
                } finally {
                    player.setYRot(yaw);
                }
            });
            // then a cable routed from the floor two blocks south to the hub's south side
            c.after(8, () -> {
                for (int k = 0; k < 2; k++) {
                    BlockPos from = at[k].south(2);
                    var a = new CableRouter.End(from.getX() * 16L + 8, from.getY() * 16L, from.getZ() * 16L + 8, 0, -1);
                    var b = CablePlacer.deviceEnd((VoxelNode) hubs[k].sixNode, at[k], hubs[k].slot, new double[]{0.5, 0, 0.95}, false, KitDeviceCases.LV);
                    String err = b == null ? "no end at the hub" : CablePlacer.routeAndLayForTests(c.level(), KitDeviceCases.LV, a, b);
                    laid[k] = err == null ? "laid" : err;
                }
            });
        }

        public void measure(TestContext c) {
            NodeBase.publishWatch = null;
            for (int k = 0; k < 2; k++) {
                String what = k == 0 ? "hub on its face" : "hub off centre";
                SixNodeElement h = hubs[k];
                LRDU south = h.side.getLRDUGoingTo(Direction.ZP);
                List<byte[]> packets;
                synchronized (sent) {
                    packets = sent.getOrDefault(h.sixNode, List.of());
                }
                c.check(what + ": a cable was routed to its kitted side", laid[k].equals("laid") && h.getConnectionMask(south) != 0, laid[k]);
                VoxelNodeEntity be = new VoxelNodeEntity(at[k], c.level().getBlockState(at[k]));
                be.setLevel(c.level());
                // every packet in turn into one block entity, drawn after each, as a client that watched it all along
                String read = packets.isEmpty() ? "nothing was sent" : null;
                for (byte[] bytes : packets) {
                    read = read(bytes, be);
                    if (read != null) break;
                    var each = be.elementRenderList[h.slot];
                    if (each != null) {
                        GlFrame.beginRenderFrame();
                        GlFrame.open();
                        try {
                            each.draw();
                        } finally {
                            GlFrame.close();
                        }
                    }
                }
                c.line("INFO " + what + ": " + packets.size() + " packets sent");
                SixNodeElementRender r = read != null ? null : be.elementRenderList[h.slot];
                c.check(what + ": the client got its block and made the render", r != null, read != null ? read : r == null ? "no render in slot " + h.slot : "slot " + h.slot);
                if (r == null) continue;
                c.check(what + ": the client render has the kit's cable on that side", r.getCableRender(south) != null, "side " + south);
                boolean shown = k == 0 ? r.connectedSide.get(south) : (r.pinMask & (1 << south.toInt())) != 0;
                c.check(what + ": the client was told the side is connected", shown,
                    "connected " + r.connectedSide.mask + ", pins " + r.pinMask + ", side " + south + " (server " + h.stubMask().mask + ")");
                // the connector: on the face, a stub in the cable's texture to the block's edge; off centre, an untextured pin
                GlFrame.beginRenderFrame();
                GlFrame f = GlFrame.open();
                try {
                    r.draw();
                } finally {
                    GlFrame.close();
                }
                float reach = 0;
                StringBuilder seen = new StringBuilder();
                for (var e : f.batches().entrySet()) {
                    var t = e.getKey().texture();
                    boolean connector = k == 0 ? t != null && t.getPath().equals("sprites/cable.png") : t == null;
                    seen.append(t).append(' ').append(e.getValue().vertices()).append("; ");
                    if (!connector) continue;
                    float[] d = e.getValue().data;
                    for (int i = 0; i < e.getValue().floats; i += GlFrame.STRIDE) reach = Math.max(reach, Math.max(Math.abs(d[i + 1]), Math.abs(d[i + 2])));
                }
                // (the element frame: across the face in y and z; the face's edge at 0.5, the hub's body to 2 px)
                float need = k == 0 ? 0.49f : 0.2f;
                c.check(what + ": it draws the connector out to " + (k == 0 ? "the block's edge" : "past its body"), reach >= need,
                    String.format("reaches %.3f, needs %.2f; %s", reach, need, seen));
                mods.eln.gametest.BakeOnceCheck.check(c, what + " with a kit and a cable", be, at[k], r::draw);
            }
        }

        /** The packet into a client block entity, as the client reads it; null if read exactly, else what went wrong. */
        static String read(byte[] bytes, mods.eln.node.NodeBlockEntity be) {
            try {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
                in.readByte();
                in.readInt();
                in.readInt();
                in.readInt();
                in.readInt();
                in.readUTF();
                be.serverPublishUnserialize(in);
                return in.available() == 0 ? null : in.available() + " bytes left of " + bytes.length;
            } catch (Exception e) {
                return e.toString();
            }
        }
    }
}
