package mods.eln.sixnode.electricalrelay;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalRelayDescriptor extends SixNodeDescriptor {

    private Obj3DPart relay1;
    private Obj3DPart relay0;
    private Obj3DPart main;
    private Obj3DPart backplate;
    private Obj3D obj;

    ElectricalCableDescriptor cable;

    float r0rOff, r0rOn, r1rOff, r1rOn;
    public float speed;

    public ElectricalRelayDescriptor(String name, Obj3D obj, ElectricalCableDescriptor cable) {
        super(name, ElectricalRelayElement.class, null);
        this.cable = cable;
        this.obj = obj;

        if (obj != null) {
            main = obj.getPart("main");
            relay0 = obj.getPart("relay0");
            relay1 = obj.getPart("relay1");
            backplate = obj.getPart("backplate");

            if (relay0 != null) {
                r0rOff = relay0.getFloat("rOff");
                r0rOn = relay0.getFloat("rOn");
                speed = relay0.getFloat("speed");
            }
            if (relay1 != null) {
                r1rOff = relay1.getFloat("rOff");
                r1rOn = relay1.getFloat("rOn");
            }
        }

        voltageLevelColor = mods.eln.sixnode.electricalcable.ElectricalCableDescriptor.voltageLevelColorOf(cable);
    }

    void applyTo(ElectricalLoad load) {
        cable.applyTo(load);
    }

    void applyTo(Resistor load) {
        cable.applyTo(load);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("A relay is an electrical\ncontact that conducts electric\ncurrent or not, depending\nthe actual input signal voltage.").split("\n"));
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    void draw(float factor) {
        //UtilsClient.disableBlend();
        UtilsClient.disableCulling();
        GL11.glScalef(0.5f, 0.5f, 0.5f);
        if (main != null) main.draw();
        if (relay0 != null) relay0.draw(factor * (r0rOn - r0rOff) + r0rOff, 0f, 0f, 1f);
        if (relay1 != null) relay1.draw(factor * (r1rOn - r1rOff) + r1rOff, 0f, 0f, 1f);
        GL11.glPushMatrix();
        voltageLevelColor.setGLColor();
        if (backplate != null) backplate.draw();
        GL11.glPopMatrix();
        UtilsClient.enableCulling();
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).left();
    }
}
