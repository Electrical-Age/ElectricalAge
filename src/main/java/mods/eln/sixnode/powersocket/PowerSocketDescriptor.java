package mods.eln.sixnode.powersocket;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class PowerSocketDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	private Obj3DPart base;
	private Obj3DPart socket;
	private int subID;

    public int range;

    public PowerSocketDescriptor(int subID, String name, Obj3D obj, int range) {
		super(name, PowerSocketElement.class, PowerSocketRender.class);
		this.subID = subID;
		this.range = range;
		this.obj = obj;
		if (obj != null) {
			base = obj.getPart("SocketBase");
			switch (subID) {
				case 1:
					socket = obj.getPart("Socket50V");
					break;
				case 2:
					socket = obj.getPart("Socket200V");
					break;
				default:
					socket = null;
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
	
	public void draw() {
		//GL11.glRotatef(90.f,1.f,0.f,0.f);
		if(base != null)
			base.draw();
		if(socket != null)
			socket.draw();
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
			GL11.glScalef(3.5f, 3.5f, 3.5f);
			GL11.glRotatef(90.f,1.f,0.f,0.f);
		}
		draw();
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);		
		list.add("Provides a supply to any");
		list.add("device plugged on it.");
	}
}
