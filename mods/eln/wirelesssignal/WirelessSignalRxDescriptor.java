package mods.eln.wirelesssignal;

import org.lwjgl.opengl.GL11;

import net.minecraft.item.ItemStack;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;

public class WirelessSignalRxDescriptor extends SixNodeDescriptor{

	public boolean repeater;
	public int range;
	private Obj3D obj;
	Obj3DPart main,led;

	public WirelessSignalRxDescriptor(
			String name,
			Obj3D obj,
			boolean repeater,int range
			) {
		super(name, WirelessSignalRxElement.class, WirelessSignalRxRender.class);
		this.repeater = repeater;
		this.range = range;
		
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
			led = obj.getPart("led");
		}
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if(type == ItemRenderType.INVENTORY) {
			GL11.glScalef(2.8f, 2.8f, 2.8f);
			GL11.glTranslatef(-0.1f, 0.0f, 0f);
		}
		draw(false);
	}
	
	public void draw(boolean connection)
	{
		if(main != null) main.draw();
		
		if(led != null){
			Utils.ledOnOffColor(connection);
			Utils.drawLight(led);
		}
	}

}
