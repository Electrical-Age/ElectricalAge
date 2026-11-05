package mods.eln.sixnode.busdevice;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The Floor Sensor (Baughn 2026-11-05: an RS485 bus device for the elevator): the proximity sensor's model, on a
 * landing beside the shaft. Register 0 is 1 while an elevator car's floor is level with the floor of its block (within
 * REACH blocks across) and stopped there; when that starts it sends `arrive` (index 0) to everyone, from its address,
 * so a program can find or check the floors.
 */
public class FloorSensorDescriptor extends SixNodeDescriptor {
    /** How far across (blocks, from the sensor's block to the car's middle) it sees a car. */
    public static final double REACH = 4;
    final Obj3D.Obj3DPart detector, halo;

    public FloorSensorDescriptor(String name, Obj3D obj) {
        super(name, FloorSensorElement.class, null);
        detector = obj == null ? null : obj.getPart("Detector");
        halo = obj == null ? null : obj.getPart("HaloMask");
        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    void draw(boolean level) {
        if (detector != null) detector.draw();
        if (level && halo != null) {
            GL11.glColor3f(0.3f, 1f, 0.3f);
            UtilsClient.drawLight(halo);
            GL11.glColor3f(1f, 1f, 1f);
        }
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Sees an elevator car stop level with the floor of its block, up to %1$ blocks away.", (int) REACH));
        list.add("  " + tr("On the RS485 bus: register 0 is 1 while it's there; sends arrive when it stops. Set its address in its screen."));
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        GL11.glScalef(2f, 2f, 2f);
        draw(false);
    }

    @Override
    public void drawPlaced(mods.eln.misc.Direction side, mods.eln.misc.LRDU front) {
        draw(false);
    }
}
