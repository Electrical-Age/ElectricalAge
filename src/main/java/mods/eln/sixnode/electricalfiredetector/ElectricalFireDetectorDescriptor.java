package mods.eln.sixnode.electricalfiredetector;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;

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

public class ElectricalFireDetectorDescriptor extends SixNodeDescriptor {

    private Obj3DPart detector;
    private Obj3DPart led;
    public boolean batteryPowered;
    double maxRange;
    public float[] pinDistance;
    final double updateInterval = 0.5;
    static final double PowerComsumption = 20000.0 / (3600 * 40);

    public ElectricalFireDetectorDescriptor(String name, Obj3D obj, double maxRange, boolean batteryPowered) {
        super(name, ElectricalFireDetectorElement.class, null);
        this.batteryPowered = batteryPowered;
        this.maxRange = maxRange;
        if (obj != null) {
            detector = obj.getPart("Detector");
            led = obj.getPart("Led");

            pinDistance = Utils.getSixNodePinDistance(detector);
        }

        if (batteryPowered) {
            voltageLevelColor = VoltageLevelColor.Neutral;
        } else {
            voltageLevelColor = VoltageLevelColor.SignalVoltage;
        }
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }
    void draw(boolean firePresent) {
        if (detector != null) detector.draw();
        if (led != null) {
            if (firePresent) {
                UtilsClient.drawLight(led);
            } else {
                GL11.glColor3f(0.5f, 0.5f, 0.5f);
                led.draw();
                GL11.glColor3f(1, 1, 1);
            }
        }
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        if (batteryPowered) {
            Collections.addAll(list, tr("Battery powered buzzer \nactivated in presence of fire.").split("\n"));
        } else {
            Collections.addAll(list, tr("Output voltage increases\nif a fire has been detected.").split("\n"));
        }
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
