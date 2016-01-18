package mods.eln.sixnode.electricalrelay;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalRelayDescriptor extends SixNodeDescriptor {

	private Obj3DPart relay1;
	private Obj3DPart relay0;
	private Obj3DPart main;
	private Obj3D obj;

    ElectricalCableDescriptor cable;

    float r0rOff, r0rOn, r1rOff, r1rOn;
    public float speed;

	public ElectricalRelayDescriptor(String name, Obj3D obj, ElectricalCableDescriptor cable) {
        super(name, ElectricalRelayElement.class, ElectricalRelayRender.class);
        this.cable = cable;
        this.obj = obj;

        if (obj != null) {
            main = obj.getPart("main");
            relay0 = obj.getPart("relay0");
            relay1 = obj.getPart("relay1");

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
	}

	void applyTo(ElectricalLoad load) {
		cable.applyTo(load);
	}

	@Override
	public boolean use2DIcon() {
		return true;
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
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		Collections.addAll(list, tr("A relay is an electrical\ncontact that conducts electric\ncurrent or not, depending\nthe actual input signal voltage.").split("\n"));
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return type != ItemRenderType.INVENTORY;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}

	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return type != ItemRenderType.INVENTORY;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if (type == ItemRenderType.INVENTORY) {
			super.renderItem(type, item, data);
		}
		draw(0f);
	}
	
	void draw(float factor) {
		//UtilsClient.disableBlend();
		UtilsClient.disableCulling();
		GL11.glScalef(0.5f, 0.5f, 0.5f);
		if (main != null) main.draw();
		if (relay0 != null) relay0.draw(factor * (r0rOn - r0rOff) + r0rOff, 0f, 0f, 1f);
		if (relay1 != null) relay1.draw(factor * (r1rOn - r1rOff) + r1rOff, 0f, 0f, 1f);
		UtilsClient.enableCulling();
	}
}
