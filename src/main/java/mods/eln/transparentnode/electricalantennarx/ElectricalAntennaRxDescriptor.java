package mods.eln.transparentnode.electricalantennarx;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNode.FrontType;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalAntennaRxDescriptor extends TransparentNodeDescriptor {

    Obj3D obj;
    Obj3DPart main;

    double electricalNominalVoltage, electricalNominalPower;
    double electricalMaximalVoltage, electricalMaximalPower;
    double electricalNominalInputR;
    ElectricalCableDescriptor cable;

    public ElectricalAntennaRxDescriptor(String name, Obj3D obj,
                                         double electricalNominalVoltage, double electricalNominalPower,
                                         double electricalMaximalVoltage, double electricalMaximalPower,
                                         ElectricalCableDescriptor cable) {
        super(name, ElectricalAntennaRxElement.class, null);
        this.electricalNominalVoltage = electricalNominalVoltage;
        this.electricalNominalPower = electricalNominalPower;
        this.electricalMaximalVoltage = electricalMaximalVoltage;
        this.electricalMaximalPower = electricalMaximalPower;
        this.cable = cable;

        this.obj = obj;
        if (obj != null) main = obj.getPart("main");

        setDefaultIcon("electricalantennarx");
        voltageLevelColor = VoltageLevelColor.fromVoltage(electricalNominalVoltage);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    @Override
    public FrontType getFrontType() {
        return FrontType.BlockSideInv;
    }

    @Override
    public boolean mustHaveWallFrontInverse() {
        return true;
    }

    @Override
    public boolean mustHaveFloor() {
        return false;
    }
// 26.x port: draw() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Wireless energy receiver."));
        list.add(tr("Nominal usage:"));
        list.add("  " + tr("Voltage: %1$V", Utils.plotValue(electricalNominalVoltage)));
        list.add("  " + tr("Power: %1$W", Utils.plotValue(electricalNominalPower)));
    }
}
