package mods.eln.sixnode.electricalwindsensor;

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

public class ElectricalWindSensorDescriptor extends SixNodeDescriptor {

    private Obj3DPart baseWall, baseGround, anemometer;

    public float[] pinDistance;

    Obj3D obj;

    public double windMax;

    public ElectricalWindSensorDescriptor(
        String name,
        Obj3D obj,
        double windMax) {
        super(name, ElectricalWindSensorElement.class, null);
        this.windMax = windMax;
        this.obj = obj;

        if (obj != null) {
            baseWall = obj.getPart("base_wall");
            baseGround = obj.getPart("base_ground");
            anemometer = obj.getPart("anemometer");

            pinDistance = Utils.getSixNodePinDistance(baseWall);
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
        Collections.addAll(list, tr("Provides an electrical signal\ndependant on wind speed.").split("\n"));
        list.add(tr("Maximum wind speed is %1$m/s", Utils.plotValue(windMax)));
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)
    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public boolean canBePlacedOnSide(Player player, Direction side) {
        if (side.isY()) {
            Utils.sendMessage(player, tr("You can't place this block on the floor or the ceiling"));
            return false;
        }
        return super.canBePlacedOnSide(player, side);
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).right();
    }
}
