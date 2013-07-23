package mods.eln.wirelesssignal;

import org.lwjgl.opengl.GL11;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;

public class WirelessSignalTxDescriptor extends SixNodeDescriptor{

	private Obj3D obj;
	Obj3DPart main;

	public WirelessSignalTxDescriptor(
			String name,
			Obj3D obj,
			int range
			) {
		super(name, WirelessSignalTxElement.class, WirelessSignalTxRender.class);
		this.range = range;
		this.obj = obj;
		if(obj != null) main = obj.getPart("main");
	}
	int range;
	
	
	public void draw()
	{
		if(main != null) main.draw();
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
		draw();
	}
	
}
