package mods.eln.sixnode.modbusrtu;


import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModbusRtuDescriptor extends SixNodeDescriptor {

    Obj3D obj;
    Obj3DPart main, door, led_power, led_activity, led_error, display;
    float alphaOff;

    public ModbusRtuDescriptor(String name, Obj3D obj) {
        super(name, ModbusRtuElement.class, null);

        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            door = obj.getPart("door");
            led_power = obj.getPart("led-power");
            led_activity = obj.getPart("led-activity");
            led_error = obj.getPart("led-error");
            display = obj.getPart("display");
            if (door != null) {
                alphaOff = door.getFloat("alphaOff");
            }
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)
    // 26.x port: draw() removed (client rendering; redone in P3)
}
