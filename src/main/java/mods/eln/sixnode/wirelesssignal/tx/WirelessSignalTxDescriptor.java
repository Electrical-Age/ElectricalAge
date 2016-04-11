package mods.eln.sixnode.wirelesssignal.tx;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class WirelessSignalTxDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	Obj3DPart main;
    
    int range;
    
    public WirelessSignalTxDescriptor(String name, 
                                      Obj3D obj, 
                                      int range) {
		super(name, WirelessSignalTxElement.class, WirelessSignalTxRender.class);
		this.range = range;
		this.obj = obj;
		if (obj != null) main = obj.getPart("main");

		voltageLevelColor = VoltageLevelColor.SignalVoltage;
	}
	
	@Override
	public boolean use2DIcon() {
		return true;
	}
    
	public void draw() {
		if (main != null) main.draw();
	}
    
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return type != ItemRenderType.INVENTORY;
	}
    
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
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
				//	GL11.glTranslatef(1.0f, 0f, 0f);
				GL11.glScalef(2.8f, 2.8f, 2.8f);
			}
			draw();
		}
	}
}
