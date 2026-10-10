package mods.eln.sixnode.energymeter;


import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class EnergyMeterDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    public Obj3DPart base, powerDisk, energySignWheel, timeUnitWheel, energyUnitWheel;
    public Obj3DPart[] energyNumberWheel, timeNumberWheel;
    public float[] pinDistance;

    float alphaOff, alphaOn, speed;

    public EnergyMeterDescriptor(String name, Obj3D obj, int energyWheelCount, int timeWheelCount) {
        super(name, EnergyMeterElement.class, null);
        this.obj = obj;
        if (obj != null) {
            base = obj.getPart("Base");
            powerDisk = obj.getPart("PowerDisk");
            energySignWheel = obj.getPart("EnergySignWheel");
            timeUnitWheel = obj.getPart("TimeUnitWheel");
            energyUnitWheel = obj.getPart("EnergyUnitWheel");

            energyNumberWheel = new Obj3DPart[energyWheelCount];
            for (int idx = 0; idx < energyNumberWheel.length; idx++) {
                energyNumberWheel[idx] = obj.getPart("EnergyNumberWheel" + idx);
            }
            timeNumberWheel = new Obj3DPart[timeWheelCount];
            for (int idx = 0; idx < timeNumberWheel.length; idx++) {
                timeNumberWheel[idx] = obj.getPart("TimeNumberWheel" + idx);
            }
        }

        pinDistance = Utils.getSixNodePinDistance(base);

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)
    // 26.x port: draw() removed (client rendering; redone in P3)
}
