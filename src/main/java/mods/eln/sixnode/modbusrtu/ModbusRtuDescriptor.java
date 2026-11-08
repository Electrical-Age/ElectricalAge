package mods.eln.sixnode.modbusrtu;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;


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

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        GL11.glTranslatef(-0.3f, -0.1f, 0f);
        draw(0.7f, false, false);

    }
    void draw(float open, boolean activityLed, boolean errorLed) {
        if (main != null) main.draw();
        if (door != null) door.draw((1f - open) * alphaOff, 0f, 0f, 1f);

        mods.eln.anim.Led.draw(led_power, true, 0f, 0.8f, 0f, 0f, 0f, 0f);
        mods.eln.anim.Led.draw(led_activity, activityLed, 0.8f, 0.8f, 0f, 0.4f, 0.4f, 0.1f);
        mods.eln.anim.Led.draw(led_error, errorLed, 0.8f, 0f, 0f, 0.4f, 0.2f, 0.2f);

        mods.eln.anim.Led.draw(display, open > 0.9, 1f, 1f, 1f, 0.5f, 0.5f, 0.5f);   // (the display lights with the door open)
    }

    /** Its door is drawn open in the model: not part of the box (Baughn 2026-10-29). */
    @Override
    protected java.util.Set<Obj3DPart> footprintIgnores() {
        return door == null ? java.util.Set.of() : java.util.Set.of(door);
    }
}
