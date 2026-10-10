package mods.eln.sixnode.electricallightsensor;

import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalLightSensorDescriptor extends SixNodeDescriptor {

    private Obj3DPart main;
    public boolean dayLightOnly;
    public float[] pinDistance;

    Obj3D obj;

    public ElectricalLightSensorDescriptor(String name, Obj3D obj, boolean dayLightOnly) {
        super(name, ElectricalLightSensorElement.class, null);
        this.obj = obj;
        this.dayLightOnly = dayLightOnly;

        if (obj != null) {
            main = obj.getPart("main");
            pinDistance = Utils.getSixNodePinDistance(main);
        }

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

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        if (dayLightOnly) {
            Collections.addAll(list, tr("Provides an electrical voltage\nwhich is proportional to\nthe intensity of daylight.").split("\n"));
            list.add(tr("0V at night, %1$V at noon.", Utils.plotValue(Eln.SVU)));
        } else {
            Collections.addAll(list, tr("Provides an electrical voltage\nin the presence of light.").split("\n"));
        }
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)
    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).right();
    }
}
