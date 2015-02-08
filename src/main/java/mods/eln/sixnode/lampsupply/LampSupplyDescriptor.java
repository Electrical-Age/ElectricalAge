package mods.eln.sixnode.lampsupply;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class LampSupplyDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	Obj3DPart main;
	private Obj3DPart rot1;
	private Obj3DPart rot2;
	private float rot1AlphaClose;
	private float rot2AlphaClose;
	private Obj3DPart led;

    public int range;

    public LampSupplyDescriptor(String name, Obj3D obj, int range) {
		super(name, LampSupplyElement.class, LampSupplyRender.class);
		this.range = range;
		this.obj = obj;
		if (obj != null) {
			main = obj.getPart("main");
			rot1 = obj.getPart("rot1");
			rot2 = obj.getPart("rot2");
			led = obj.getPart("led");
			if (rot1 != null) {
				rot1AlphaClose = rot1.getFloat("alphaClose");
			}
			if (rot2 != null) {
				rot2AlphaClose = rot2.getFloat("alphaClose");
			}
		}
	}

	@Override
	public boolean use2DIcon() {
		return false;
	}

	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addLight(newItemStack(1));
	}
	
	public void draw(float openFactor) {
		if (main != null) main.draw();
		UtilsClient.drawLight(led);
		UtilsClient.disableCulling();
		if (rot1 != null) rot1.draw((1f - openFactor) * rot1AlphaClose, 0f, 0f, 1f);
		if (rot2 != null) rot2.draw((1f - openFactor) * rot2AlphaClose, 0f, 0f, 1f);
		UtilsClient.enableCulling();
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}

	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if (type == ItemRenderType.INVENTORY) {
			GL11.glScalef(1.5f, 1.5f, 1.5f);
			GL11.glTranslatef(-0.2f, 0.0f, 0f);
		}
		draw(1f);
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);		
		list.add("Provides a supply to any");
		list.add("lamp on the same channel.");
	}
}
