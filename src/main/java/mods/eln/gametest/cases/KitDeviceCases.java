package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gridnode.GridLink;
import mods.eln.gridnode.electricalpole.ElectricalPoleElement;
import mods.eln.node.voxel.CableItems;
import mods.eln.node.voxel.CableKitItem;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.CableSpec;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sixnode.electricalbreaker.ElectricalBreakerElement;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import mods.eln.transparentnode.transformer.TransformerElement;
import net.minecraft.core.BlockPos;
import mods.eln.misc.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Cable kits in the devices that took 1.12 cable items (Baughn 2026-11-07: kits replace the items): a kit's spec sets a
 * breaker's contacts and trip current, a lamp supply's range counts a kit's blocks and takes a block's piece by right
 * click, and a converter counts a kit's blocks as turns (and the one-time rewind leaves kits alone). Utility poles are
 * strung with overhead cable, taking its length, and refuse kits (Baughn 2026-11-09). The 1.12 items still work (the
 * older cases use them).
 */
public final class KitDeviceCases {
    private KitDeviceCases() {
    }

    static final int HV = 3, LV = 1, VHV = 4;

    static ItemStack kit(int tier, CableSpec spec, int blocks) {
        return CableKitItem.kit(tier, spec, blocks * 16);
    }

    /** A 6 mm² 400 V kit (36 A). */
    static CableSpec thick() {
        return new CableSpec(6, CableSpec.preset(HV).colour());
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new TestCase() {
            public String id() {
                return "kit_cable_items";
            }

            public int ticks() {
                return 1;
            }

            public void build(TestContext ctx) {
            }

            public void measure(TestContext ctx) {
                ItemStack lv = kit(LV, null, 4), hv = kit(HV, thick(), 10);
                var pd = CableItems.descriptor(lv);
                ctx.check("kits: a preset kit is its tier's preset cable (48 V, 2.5 mm²)", pd != null && pd == CableSpec.preset(LV).descriptor(LV)
                    && pd.electricalNominalVoltage == Eln.LVU, pd == null ? "none" : pd.electricalNominalVoltage + " V");
                var d = CableItems.descriptor(hv);
                ctx.check("kits: a 6 mm² 400 V kit carries 36 A, with thermal constants",
                    d != null && Math.abs(d.electricalNominalPower / d.electricalNominalVoltage - 36) < 0.01 && d.thermalC > 0 && d.thermalRp > 0,
                    d == null ? "none" : d.electricalNominalPower / d.electricalNominalVoltage + " A, C " + d.thermalC);
                ctx.check("kits: a 1.12 item is still its cable", CableItems.descriptor(Wp9aCases.lvCable()) == ElnDeviceRegistry.lowVoltageCableDescriptor, "");
                ctx.check("kits: thermal and bus kits aren't electrical", !CableItems.isElectrical(CableKitItem.kit(5, 32))
                    && CableItems.isThermal(CableKitItem.kit(5, 32)) && !CableItems.isElectrical(CableKitItem.kit(CablePart.BUS, 32)), "");
                ItemStack piece = CableItems.take(hv, 48);
                ctx.check("kits: taking 3 blocks off 10 leaves 7, the piece keeps the spec",
                    CableItems.blocks(piece) == 3 && CableItems.blocks(hv) == 7 && thick().equals(CableKitItem.specOf(piece)),
                    CableItems.blocks(piece) + " / " + CableItems.blocks(hv) + ", " + CableKitItem.specOf(piece));
                ItemStack all = CableItems.take(lv, 64);
                ctx.check("kits: taking all of a kit empties it", lv.isEmpty() && CableItems.blocks(all) == 4, "");
            }
        });

        cases.add(new TestCase() {
            ElectricalBreakerElement b;

            public String id() {
                return "kit_breaker";
            }

            public int width() {
                return 3;
            }

            public int ticks() {
                return 2;
            }

            public void build(TestContext ctx) {
                b = (ElectricalBreakerElement) ctx.placeSix(Wp9aCases.BREAKER, ctx.at(1, 1, 1));
                Batch1Cases.orient(b, f -> f, Direction.XN);
                b.getInventory().setItem(0, kit(HV, thick(), 1));
                b.getInventory().setChanged();
                b.networkUnserialize(ctx.stream(out -> out.writeByte(ElectricalBreakerElement.toogleSwitchId)));
            }

            public void measure(TestContext ctx) {
                var d = thick().descriptor(HV);
                ctx.check("kits: a breaker on a 6 mm² kit has that cable's contacts", b.getSwitchState() && Math.abs(b.switchResistor.getR() - d.electricalRs) < 1e-9,
                    b.getSwitchState() + ", " + b.switchResistor.getR() + " vs " + d.electricalRs);
                ctx.check("kits: unset, it trips at 1.05 x 36 A", Math.abs(b.cableTripAmps() - 1.05 * 36) < 0.05, "" + b.cableTripAmps());
            }
        });

        cases.add(new TestCase() {
            ElectricalBreakerElement b;
            String clicked = "", shifted = "";

            public String id() {
                return "kit_cable_slot_piece";
            }

            public int width() {
                return 3;
            }

            public int ticks() {
                return 2;
            }

            public void build(TestContext ctx) {
                b = (ElectricalBreakerElement) ctx.placeSix(Wp9aCases.BREAKER, ctx.at(1, 1, 1));
                var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level());
                var menu = mods.eln.node.NodeMenus.menu(1, fp, b.sixNode, b.side);
                if (menu == null) throw new IllegalStateException("no breaker menu");
                // a cursor click with a 10-block kit: one block goes in
                menu.setCarried(kit(HV, thick(), 10));
                menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, fp);
                clicked = CableItems.blocks(menu.getSlot(0).getItem()) + "/" + CableItems.blocks(menu.getCarried());
                menu.setCarried(ItemStack.EMPTY);
                b.getInventory().setItem(0, ItemStack.EMPTY);
                // a shift-click of a kit in the main inventory: the same
                fp.getInventory().setItem(9, kit(HV, null, 6));
                menu.quickMoveStack(fp, menu.deviceSlots);
                shifted = CableItems.blocks(menu.getSlot(0).getItem()) + "/" + CableItems.blocks(fp.getInventory().getItem(9));
                fp.getInventory().setItem(9, ItemStack.EMPTY);
            }

            public void measure(TestContext ctx) {
                ctx.check("kits: a cable slot takes one block of a clicked kit (1 in, 9 kept)", clicked.equals("1/9"), clicked);
                ctx.check("kits: ... and of a shift-clicked one (1 in, 5 kept)", shifted.equals("1/5"), shifted);
            }
        });

        cases.add(new TestCase() {
            LampSupplyElement s;
            ItemStack hand;
            int afterOne, afterTwo, base;

            public String id() {
                return "kit_lamp_supply";
            }

            public int width() {
                return 3;
            }

            public int ticks() {
                return 2;
            }

            public void build(TestContext ctx) {
                s = (LampSupplyElement) ctx.placeSix(Ids.six("Lamp Supply"), ctx.at(1, 1, 1));
                base = s.getRange();
                hand = kit(LV, null, 5);
                Wp9aCases.use(ctx, s, hand);
                afterOne = s.getRange();
                Wp9aCases.use(ctx, s, hand);
                afterTwo = s.getRange();
            }

            public void measure(TestContext ctx) {
                ctx.check("kits: a right click puts a block of a kit in a lamp supply (range + 1), a second another",
                    afterOne == base + 1 && afterTwo == base + 2 && CableItems.blocks(hand) == 3, base + " -> " + afterOne + " -> " + afterTwo
                        + ", hand " + CableItems.blocks(hand) + " blocks");
                s.getInventory().setItem(0, kit(LV, null, 10));
                ctx.check("kits: a 10-block kit adds 10 blocks of range", s.getRange() == base + 10, "" + s.getRange());
            }
        });

        cases.add(new TestCase() {
            TransformerElement tr;

            public String id() {
                return "kit_converter_turns";
            }

            public int width() {
                return 3;
            }

            public int ticks() {
                return 2;
            }

            public void build(TestContext ctx) {
                tr = (TransformerElement) ctx.placeTransparent(Wp10aCases.TRANSFORMER, ctx.at(1, 1, 1));
                tr.getInventory().setItem(0, kit(LV, null, 5));
                tr.getInventory().setItem(1, kit(2, null, 24));
                tr.getInventory().setChanged();
            }

            public void measure(TestContext ctx) {
                ctx.check("kits: a converter counts a kit's blocks as turns (5 and 24)", tr.primaryTurns() == 5 && tr.secondaryTurns() == 24,
                    tr.primaryTurns() + " / " + tr.secondaryTurns());
                tr.getInventory().setItem(0, kit(LV, null, 1));
                tr.getInventory().setItem(1, kit(2, null, 4));   // 1:4, the 1.12 50 -> 200 V ratio the rewind looks for
                tr.rewindForRealVoltages();
                ctx.check("kits: the one-time rewind leaves kits alone", tr.primaryTurns() == 1 && tr.secondaryTurns() == 4,
                    tr.primaryTurns() + " / " + tr.secondaryTurns());
            }
        });

        cases.add(new TestCase() {
            ElectricalPoleElement a, b, c;
            ItemStack hand;
            String said = "";

            public String id() {
                return "overhead_cable_poles";
            }

            public int height() {
                return 5;
            }

            public int ticks() {
                return 2;
            }

            public void build(TestContext ctx) {
                BlockPos pa = ctx.at(1, 1, 1), pb = ctx.at(6, 1, 1);
                a = (ElectricalPoleElement) ctx.placeTransparent(Wp11Cases.UTILITY_POLE, pa);
                b = (ElectricalPoleElement) ctx.placeTransparent(Wp11Cases.UTILITY_POLE, pb);
                Player p = Wp9aCases.player(ctx);
                hand = mods.eln.gridnode.OverheadCableItem.make(thick().mm2(), 10);
                p.setItemInHand(InteractionHand.MAIN_HAND, hand);
                try {
                    // as a player's click: the block's use
                    for (BlockPos q : new BlockPos[]{pa, pb}) {
                        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(q), net.minecraft.core.Direction.EAST, q, false);
                        ctx.world().getBlockState(q).useItemOn(hand, ctx.world(), p, InteractionHand.MAIN_HAND, hit);
                    }
                    // a third pole: an 11 kV cable kit from it to a is refused (Baughn 2026-11-09: poles take overhead cable only)
                    c = (ElectricalPoleElement) ctx.placeTransparent(Wp11Cases.UTILITY_POLE, ctx.at(3, 1, 1));
                    kitHand = kit(VHV, null, 10);
                    p.setItemInHand(InteractionHand.MAIN_HAND, kitHand);
                    c.onBlockActivated(p, Direction.XP, 0.5F, 0.5F, 0.5F);
                    a.onBlockActivated(p, Direction.XN, 0.5F, 0.5F, 0.5F);
                    said = a.gridLinkList.size() + "," + c.gridLinkList.size();
                } finally {
                    p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }

            ItemStack kitHand;

            public void measure(TestContext ctx) {
                ctx.check("poles: a cable kit is refused, and keeps its length", said.equals("1,0") && CableItems.blocks(kitHand) == 10,
                    said + ", kit " + CableItems.blocks(kitHand));
                ctx.check("poles: an overhead line is 11 kV, set by the poles", a.lineTier() == VHV && b.lineTier() == VHV, a.lineTier() + "," + b.lineTier());
                GridLink l = a.gridLinkList.size() == 1 ? a.gridLinkList.iterator().next() : null;
                ctx.check("poles: overhead cable strings two poles 5 blocks apart and loses 5 blocks",
                    l != null && mods.eln.gridnode.OverheadCableItem.blocks(hand) == 5,
                    a.gridLinkList.size() + " links, hand " + mods.eln.gridnode.OverheadCableItem.blocks(hand));
                if (l == null) return;
                ctx.check("poles: the line keeps a 5-block piece of the cable, its Rs the 6 mm² cable's",
                    mods.eln.gridnode.OverheadCableItem.blocks(l.cableStack()) == 5 && mods.eln.gridnode.OverheadCableItem.mm2(l.cableStack()) == thick().mm2()
                        && Math.abs(l.rs() - 5 * thick().descriptor(VHV).electricalRs) < 1e-9, l.cableStack() + ", rs " + l.rs());
                // two pieces of the same cross-section combine, up to 500 blocks
                ItemStack x = mods.eln.gridnode.OverheadCableItem.make(thick().mm2(), 450), y = mods.eln.gridnode.OverheadCableItem.make(thick().mm2(), 80);
                x.getItem().overrideOtherStackedOnMe(x, y, null, net.minecraft.world.inventory.ClickAction.PRIMARY, null, null);
                ctx.check("overhead cable: pieces combine up to 500 blocks", mods.eln.gridnode.OverheadCableItem.blocks(x) == 500
                    && mods.eln.gridnode.OverheadCableItem.blocks(y) == 30, mods.eln.gridnode.OverheadCableItem.blocks(x) + " + " + mods.eln.gridnode.OverheadCableItem.blocks(y));
            }
        });
    }
}
