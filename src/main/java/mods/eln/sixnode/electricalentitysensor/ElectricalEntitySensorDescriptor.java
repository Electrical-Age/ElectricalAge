package mods.eln.sixnode.electricalentitysensor;

import mods.eln.item.EntitySensorFilterDescriptor;
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

public class ElectricalEntitySensorDescriptor extends SixNodeDescriptor {

    boolean useEntitySpeed = true;
    double speedFactor = 1 / 0.10;
    private Obj3DPart detector, haloMask;
    double maxRange;
    public float[] pinDistance;
    Obj3D obj;

    public ElectricalEntitySensorDescriptor(String name, Obj3D obj, double maxRange) {
        super(name, ElectricalEntitySensorElement.class, null);
        this.obj = obj;
        this.maxRange = maxRange;
        if (obj != null) {
            detector = obj.getPart("Detector");
            haloMask = obj.getPart("HaloMask");

            pinDistance = Utils.getSixNodePinDistance(detector);
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
        Collections.addAll(list, tr("Output voltage increases\nif entities are moving around.").split("\n"));
        list.add(tr("Range: %1$ blocks", (int) maxRange));
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
