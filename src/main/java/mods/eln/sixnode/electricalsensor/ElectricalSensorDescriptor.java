package mods.eln.sixnode.electricalsensor;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalSensorDescriptor extends SixNodeDescriptor {

    public boolean voltageOnly;
    Obj3DPart main;

    public ElectricalSensorDescriptor(
        String name, String modelName,
        boolean voltageOnly) {
        super(name, ElectricalSensorElement.class, null);
        this.voltageOnly = voltageOnly;
        main = Eln.obj.getPart(modelName, "main");

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }
    void draw() {
        if (main != null) main.draw();
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }
    /*
	
	// 26.x port: handleRenderType() removed (client rendering; redone in P3)
	
	// 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)
	
	// 26.x port: renderItem() removed (client rendering; redone in P3)
*/

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        if (voltageOnly) {
            list.add(tr("Measures voltage on cables."));
            list.add(tr("Has a signal output."));
        } else {
            list.add(tr("Measures electrical values on cables."));
            list.add(tr("Can measure Voltage/Power/Current"));
            list.add(tr("Has a signal output."));
        }
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
