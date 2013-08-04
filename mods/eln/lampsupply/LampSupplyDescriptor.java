package mods.eln.lampsupply;

import org.lwjgl.opengl.GL11;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.SixNodeDescriptor;

public class LampSupplyDescriptor extends SixNodeDescriptor{

	private Obj3D obj;
	Obj3DPart main;
	private Obj3DPart rot1;
	private Obj3DPart rot2;
	private float rot1AlphaClose;
	private float rot2AlphaClose;
	private Obj3DPart led;

	public LampSupplyDescriptor(
			String name,
			Obj3D obj,
			int range
			) {
		super(name, LampSupplyElement.class, LampSupplyRender.class);
		this.range = range;
		this.obj = obj;
		if(obj != null) {
			main = obj.getPart("main");
			rot1 = obj.getPart("rot1");
			rot2 = obj.getPart("rot2");
			led = obj.getPart("led");
			if(rot1 != null){
				rot1AlphaClose = rot1.getFloat("alphaClose");
			}
			if(rot2 != null){
				rot2AlphaClose = rot2.getFloat("alphaClose");
			}
		}
	}
	public int range;
	
	
	public void draw(float openFactor)
	{
		if(main != null) main.draw();
		Utils.drawLight(led);
		Utils.disableCulling();
		if(rot1 != null) rot1.draw((1f-openFactor)*rot1AlphaClose, 0f,0f,1f);
		if(rot2 != null) rot2.draw((1f-openFactor)*rot2AlphaClose, 0f,0f,1f);
		Utils.enableCulling();
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
			GL11.glScalef(1.5f, 1.5f, 1.5f);
			GL11.glTranslatef(-0.2f, 0.0f, 0f);
		}
		draw(1f);
	}
	
}
