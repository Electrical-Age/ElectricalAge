package mods.eln.sixnode.electricalweathersensor;

import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalWeatherSensorDescriptor extends SixNodeDescriptor {

    private Obj3DPart main;
    public float[] pinDistance;

    Obj3D obj;

    public ElectricalWeatherSensorDescriptor(String name, Obj3D obj) {
        super(name, ElectricalWeatherSensorElement.class, null);
        this.obj = obj;

        if (obj != null) {
            main = obj.getPart("main");

            pinDistance = Utils.getSixNodePinDistance(main);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }
    // 26.x port: draw() removed (client rendering; redone in P3)

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Provides an electrical signal\ndepending the actual weather.").split("\n"));
        list.add(tr("Clear: %1$V", 0));
        list.add(tr("Rain: %1$V", Utils.plotValue(Eln.SVU / 2)));
        list.add(tr("Storm: %1$V", Utils.plotValue(Eln.SVU)));
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
