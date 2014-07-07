package mods.eln.sixnode.modbusrtu;

import java.util.List;

import javax.rmi.CORBA.Util;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.client.FrameTime;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;

public class ModbusRtuDescriptor extends SixNodeDescriptor {

	public ModbusRtuDescriptor(
			String name,
			Obj3D obj
			) {
		super(name, ModbusRtuElement.class,ModbusRtuRender.class);

		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
			door = obj.getPart("door");
			led_power = obj.getPart("led-power");
			led_activity = obj.getPart("led-activity");
			led_error = obj.getPart("led-error");
			display = obj.getPart("display");
			if(door != null){
				alphaOff = door.getFloat("alphaOff");
			}
		}
	}
	
	Obj3D obj;
	Obj3DPart main,door,led_power,led_activity,led_error,display;
	float alphaOff;
	
	@Override
	public void setParent(Item item, int damage) {
		
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);

	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		
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
		
		GL11.glTranslatef(-0.3f, -0.1f, 0f);
		draw(0.7f, false, false);
	}
	
	void draw(float open, boolean activityLed, boolean errorLed)
	{
		if (main != null) main.draw();
		if (door != null) door.draw((1f-open)*alphaOff, 0f, 0f, 1f);
		
		if (led_power != null)
		{
			GL11.glColor3f(0f, 0.8f, 0f);
			UtilsClient.drawLight(led_power);
		}
		
		if (led_activity != null)
		{
			if (activityLed) {
				GL11.glColor3f(0.8f, 0.8f, 0f);
				UtilsClient.drawLight(led_activity);
			}
			else {
				GL11.glColor3f(0.4f, 0.4f, 0.1f);
				led_activity.draw();
			}
		}
		
		if (led_error != null)
		{
			if (errorLed)
			{
				GL11.glColor3f(0.8f, 0f, 0f);
				UtilsClient.drawLight(led_error);
			}
			else {
				GL11.glColor3f(0.4f, 0.2f, 0.2f);
				led_error.draw();
			}
		}
		
		if (display != null )
		{
			if (open > 0.9)
			{
				GL11.glColor3f(1f, 1f, 1f);
				UtilsClient.drawLight(display);
			}
			else
			{
				GL11.glColor3f(0.5f, 0.5f, 0.5f);
				display.draw();
			}
		}
	}
}
