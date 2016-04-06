package mods.eln.sixnode.wirelesssignal.rx;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class WirelessSignalRxDescriptor extends SixNodeDescriptor {
    
	private Obj3D obj;
	Obj3DPart main, led;

	public WirelessSignalRxDescriptor(String name, Obj3D obj) {
		super(name, WirelessSignalRxElement.class, WirelessSignalRxRender.class);

		this.obj = obj;
		if (obj != null) {
			main = obj.getPart("main");
			led = obj.getPart("led");
		}

		voltageLevelColor = VoltageLevelColor.SignalVoltage;
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return type != ItemRenderType.INVENTORY;
	}
    
	@Override
	public boolean use2DIcon() {
		return true;
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
		} else {
			if (type == ItemRenderType.ENTITY) {
				GL11.glScalef(2.8f, 2.8f, 2.8f);
			}
			draw(false);
		}
	}
	
	public void draw(boolean connection) {
		if (main != null) main.draw();
		
		if (led != null) {
			UtilsClient.ledOnOffColor(connection);
			UtilsClient.drawLight(led);
		}
	}
}
