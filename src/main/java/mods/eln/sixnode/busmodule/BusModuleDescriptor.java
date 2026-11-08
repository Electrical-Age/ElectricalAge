package mods.eln.sixnode.busmodule;

import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The Bus I/O Module (mods.eln.bus): a box on a face whose front edge takes the field bus and whose other three edges
 * hold three signal terminals each (Baughn: 9 terminals). Drawn with the 1.12 Modbus RTU's model, without its door:
 * the cupboard, its display and three lamps (power: on a bus; activity: a terminal above 0; error: a bus past 64
 * channels, or a name just refused).
 */
public class BusModuleDescriptor extends SixNodeDescriptor {
    Obj3DPart main, display, ledPower, ledActivity, ledError;

    public BusModuleDescriptor(String name, Obj3D obj) {
        super(name, BusModuleElement.class, null);
        if (obj != null) {
            main = obj.getPart("main");
            display = obj.getPart("display");
            ledPower = obj.getPart("led-power");
            ledActivity = obj.getPart("led-activity");
            ledError = obj.getPart("led-error");
        }
        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    /** The model with its lamps lit as given. */
    void draw(boolean power, boolean activity, boolean error) {
        if (main != null) main.draw();
        if (display != null) {
            GL11.glColor3f(0.5f, 0.5f, 0.5f);
            display.draw();
        }
        lamp(ledPower, power, 0f, 0.8f, 0f);
        lamp(ledActivity, activity, 0.8f, 0.8f, 0f);
        lamp(ledError, error, 0.8f, 0f, 0f);
        GL11.glColor3f(1f, 1f, 1f);
    }

    private static void lamp(Obj3DPart p, boolean on, float r, float g, float b) {
        mods.eln.anim.Led.draw(p, on, r, g, b, r * 0.4f + 0.1f, g * 0.4f + 0.1f, b * 0.4f + 0.1f);
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        GL11.glTranslatef(-0.3f, -0.1f, 0f);
        draw(true, false, false);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Joins signal wires to a field bus.\nIts front edge takes the bus cable,\nthe other three edges hold 9 terminals.").split("\n"));
        list.add(tr("Each terminal puts its wire on a channel, or drives its wire from one."));
    }
}
