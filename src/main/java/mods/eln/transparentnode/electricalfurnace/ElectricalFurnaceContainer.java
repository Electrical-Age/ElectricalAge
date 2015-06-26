package mods.eln.transparentnode.electricalfurnace;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkin;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.RegulatorSlot;
import mods.eln.misc.BasicContainer;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import mods.eln.sim.RegulatorType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ElectricalFurnaceContainer extends BasicContainer implements INodeContainer {

    NodeBase node = null;

    public ElectricalFurnaceContainer(NodeBase node, EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{
                new SlotWithSkin(inventory, ElectricalFurnaceElement.outSlotId, 84, 58, SlotSkin.big),
                new SlotWithSkin(inventory, ElectricalFurnaceElement.inSlotId, 7, 58, SlotSkin.medium),

                new GenericItemUsingDamageSlot(inventory, 2, 70, 6 + 20 + 6, 1, HeatingCorpElement.class, SlotSkin.medium, new String[]{"Heating Corp slot"}),
                new GenericItemUsingDamageSlot(inventory, 3, 80 + 18, -2000, 1, ThermalIsolatorElement.class, SlotSkin.medium, new String[]{"Thermal Isolator slot"}),
                new RegulatorSlot(inventory, 4, 70 + 18, 6 + 20 + 6, 1, new RegulatorType[]{RegulatorType.onOff, RegulatorType.analog}, SlotSkin.medium)

                //new SlotFilter(inventory, 2, 62 + 0, 17 + 18, 1, new ItemStackFilter[]{new ItemStackFilter(Eln.heatingCorpItem)}),
                //new SlotFilter(inventory, 3, 62 + 18, 17 + 18, 1, new ItemStackFilter[]{new ItemStackFilter(Eln.thermalIsolatorItem)}),
                //new SlotFilter(inventory, 4, 62 + 36, 17 + 18, 1, new ItemStackFilter[]{new ItemStackFilter(Eln.regulatorItem)})
        });
        this.node = node;
    }

    @Override
    public NodeBase getNode() {
        return node;
    }

    @Override
    public int getRefreshRateDivider() {
        return 1;
    }
}
/*				new SlotFilter(inventory, 0, 62 + 0, 17, new ItemStackFilter[]{new ItemStackFilter(Block.wood, 0, 0)}),
                new SlotFilter(inventory, 1, 62 + 18, 17, new ItemStackFilter[]{new ItemStackFilter(Item.coal, 0, 0)})
*/
