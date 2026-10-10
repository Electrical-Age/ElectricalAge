package mods.eln.sixnode.thermalsensor;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ThermalSensorDescriptor extends SixNodeDescriptor {

    public boolean temperatureOnly;

    Obj3D obj;
    Obj3DPart main;
    Obj3DPart adapter;

    public ThermalSensorDescriptor(String name,
                                   Obj3D obj,
                                   boolean temperatureOnly) {
        super(name, ThermalSensorElement.class, null);
        this.temperatureOnly = temperatureOnly;
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            adapter = obj.getPart("adapter");
        }
        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        if (temperatureOnly) {
            list.add(tr("Measures temperature of cables."));
            list.add(tr("Has a signal output."));
        } else {
            list.add(tr("Measures thermal values on cables."));
            list.add(tr("Can measure:"));
            list.add(tr("  Temperature/Power conducted"));
            list.add(tr("Has a signal output."));
        }
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addThermal(newItemStack());
        Data.addSignal(newItemStack());
    }
    // 26.x port: draw() removed (client rendering; redone in P3)

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
