package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.NodeMenus;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sixnode.hub.HubSlots;
import mods.eln.sixnode.hub.HubSlots.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The hub's screen against the world (AdventurAgent: on a floor, cables put in the left and right slots joined north
 * and south for a player facing south, and the top and bottom ones west and east). The screen shows the hub as the
 * player sees it (HubSlots), and these go the player's way: a fake player faces a way, the hub's menu is opened, and
 * a cable is clicked into the slot shown at a place. The side of the world that then has the pin is the one seen there.
 */
public final class HubSlotCases {
    private HubSlotCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new SeenCase());
    }

    /** The world sides the hub connects toward now, e.g. "ZP XP". */
    static String sides(SixNodeElement hub) {
        StringBuilder b = new StringBuilder();
        for (Direction d : Direction.values()) {
            var l = hub.side.getLRDUGoingTo(d);
            if (l != null && hub.getConnectionMask(l) != 0) b.append(b.length() == 0 ? "" : " ").append(d);
        }
        return b.toString();
    }

    /** As a player facing along `yaw` who opens the hub's screen and clicks `stack` into the slot shown at `place`. */
    static void click(Player player, float yaw, mods.eln.node.NodeBase node, SixNodeElement hub, Place place, ItemStack stack) {
        player.setYRot(yaw);
        var menu = NodeMenus.menu(1, player, node, hub.slot);
        if (menu == null) throw new IllegalStateException("no hub menu");
        int slot = HubSlots.slot(hub.side, Direction.fromEnumFacing(player.getDirection()), place);
        menu.setCarried(stack);
        menu.clicked(slot, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        menu.setCarried(ItemStack.EMPTY);
    }

    static final float SOUTH = 0, WEST = 90, NORTH = 180, EAST = -90;

    static final class SeenCase implements TestCase {
        SixNodeElement floor, wall, ceiling;
        String floorTop = "", floorPins = "", floorSecond = "", floorAll = "", wallTop = "", wallLeft = "", ceilingTop = "", ceilingLeft = "";

        public String id() {
            return "hub_slots_as_seen";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 10; x++) for (int z = 0; z < 10; z++) c.floor(c.at(x, 0, z));
            Player player = VoxelDeviceCases.player(c);
            float yaw = player.getYRot();
            try {
                // a hub off centre on the floor of a voxel block, the player facing south: the top slot is the south side
                BlockPos ph = c.at(2, 1, 2);
                var vh = VoxelDeviceCases.block(c, ph);
                floor = VoxelDeviceCases.put(c, vh, "Hub", Direction.YN, 0, 0, 2, Direction.ZN);
                click(player, SOUTH, vh, floor, Place.TOP, Wp9aCases.lvCable());
                floorTop = sides(floor);
                StringBuilder pins = new StringBuilder();
                for (int[] t : vh.pinTips(floor.slot)) pins.append(floor.side.applyLRDU(mods.eln.misc.LRDU.fromInt(t[0]))).append(' ');
                floorPins = pins.toString().trim();
                // a cable from that pin south to a 50 V source: the hub is live (measure)
                VoxelDeviceCases.cableOut(c, vh, ph, floor, Direction.ZP, VoxelDeviceCases.LV, VoxelDeviceCases.FLOOR);
                VoxelDeviceCases.source(c, Wp9aCases.SOURCE, c.at(2, 1, 3), 50);
                // a kit in the left slot: east; then facing west, the top slot is the west side and the right one north
                click(player, SOUTH, vh, floor, Place.LEFT, KitDeviceCases.kit(KitDeviceCases.LV, null, 4));
                floorSecond = sides(floor);
                click(player, WEST, vh, floor, Place.TOP, KitDeviceCases.kit(KitDeviceCases.LV, null, 4));
                click(player, WEST, vh, floor, Place.RIGHT, Wp9aCases.lvCable());
                floorAll = sides(floor);

                // on the west wall of a room (face XN; the player looks west at it, whatever way they are said to face):
                // top is up, left is south
                BlockPos pw = c.at(6, 2, 2);
                c.floor(pw.relative(net.minecraft.core.Direction.WEST));
                wall = c.placeSix(Wp9cCases.HUB, pw, Direction.XN);
                click(player, EAST, wall.sixNode, wall, Place.TOP, Wp9aCases.lvCable());
                wallTop = sides(wall);
                click(player, NORTH, wall.sixNode, wall, Place.LEFT, KitDeviceCases.kit(KitDeviceCases.LV, null, 4));
                wallLeft = sides(wall);

                // on a ceiling, the player facing south and looking up: the top of the view is behind them (north),
                // east is still on the left
                BlockPos pc = c.at(8, 2, 4);
                c.floor(pc.above());
                ceiling = c.placeSix(Wp9cCases.HUB, pc, Direction.YP);
                click(player, SOUTH, ceiling.sixNode, ceiling, Place.TOP, Wp9aCases.lvCable());
                ceilingTop = sides(ceiling);
                click(player, SOUTH, ceiling.sixNode, ceiling, Place.LEFT, KitDeviceCases.kit(KitDeviceCases.LV, null, 4));
                ceilingLeft = sides(ceiling);
            } finally {
                player.setYRot(yaw);
            }
        }

        public void measure(TestContext c) {
            c.check("hub screen: on a floor, facing south, a cable in the top slot connects the south side (the far one)",
                floorTop.equals("ZP") && floorPins.equals("ZP"), "sides " + floorTop + ", pins " + floorPins);
            double u = VoxelDeviceCases.u(floor, Direction.ZP);
            c.check("hub screen: ... and the cable laid from there to a 50 V source is joined", VoxelDeviceCases.linked(floor, Direction.ZP)
                && Math.abs(u - 50) < 1, "linked " + VoxelDeviceCases.linked(floor, Direction.ZP) + ", " + u + " V");
            c.check("hub screen: ... a kit in the left slot the east side", floorSecond.equals("XP ZP"), floorSecond);
            c.check("hub screen: facing west, the top slot is the west side and the right one north", floorAll.equals("XN XP ZN ZP"), floorAll);
            c.check("hub screen: on a wall the top slot is up", wallTop.equals("YP"), wallTop);
            c.check("hub screen: ... and the left one the player's left looking at the wall (west wall: south)", wallLeft.equals("YP ZP"), wallLeft);
            c.check("hub screen: on a ceiling, facing south, the top slot is north (behind, as seen looking up)", ceilingTop.equals("ZN"), ceilingTop);
            c.check("hub screen: ... and the left one east", ceilingLeft.equals("XP ZN"), ceilingLeft);
        }
    }
}
