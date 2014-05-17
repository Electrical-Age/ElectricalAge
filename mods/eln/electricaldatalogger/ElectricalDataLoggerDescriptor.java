package mods.eln.electricaldatalogger;

import java.util.List;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.misc.Direction;
import mods.eln.misc.IFunction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import com.google.common.base.Function;

public class ElectricalDataLoggerDescriptor extends SixNodeDescriptor {

	public ElectricalDataLoggerDescriptor(String name, boolean onFloor, String objName, float cr, float cg, float cb) {
		super(name, ElectricalDataLoggerElement.class, ElectricalDataLoggerRender.class);
		this.cb = cb;
		this.cr = cr;
		this.cg = cg;
		this.onFloor = onFloor;
		obj = Eln.obj.getObj(objName);
		if(obj != null) {
			main = obj.getPart("main");
			if(main != null) {
				sx = main.getFloat("sx");
				sy = main.getFloat("sy");
				sz = main.getFloat("sz");
				tx = main.getFloat("tx");
				ty = main.getFloat("ty");
				tz = main.getFloat("tz");
				rx = main.getFloat("rx");
				ry = main.getFloat("ry");
				rz = main.getFloat("rz");
				ra = main.getFloat("ra");
				mx = main.getFloat("mx");
				my = main.getFloat("my");
				
				led = obj.getPart("led");
			}
		}
		
		if(onFloor) {
			setPlaceDirection(Direction.YN);
		}
	}
	Obj3D obj;
	Obj3DPart main, led;
	float sx, sy, sz;
	float tx, ty, tz;
	float rx, ry, rz, ra;
	float mx, my;
	
	float cr, cg, cb;
	
	public boolean onFloor;
	void draw(DataLogs log, LRDU front) {
		front.glRotateOnX();
		//GL11.glDisable(GL11.GL_TEXTURE_2D);
		if(main != null) main.draw();
		//GL11.glEnable(GL11.GL_TEXTURE_2D);
		if(log != null) {
			UtilsClient.disableLight();
	       // GL11.glPushMatrix();	
			UtilsClient.ledOnOffColor(true);
        	if(led != null) led.draw();
        	
        	UtilsClient.glDefaultColor();
			
    		GL11.glTranslatef(tx, ty, tz); 	
    		GL11.glRotatef(ra, rx, ry, rz);  
        	GL11.glScalef(sx, sy, sz);
			GL11.glColor4f(1f, 0.5f, 0.0f, 1f);
        	log.draw(mx, my, "\u00a76");
        	
        	UtilsClient.glDefaultColor();

        	UtilsClient.enableLight();
		}
	}
	
	@Override	
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addSignal(newItemStack());
	}
	
	@Override
	public boolean hasVolume() {
		return true;
	}

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if(type == ItemRenderType.INVENTORY) {
			GL11.glRotatef(-90, 0f, 1f, 0f);
			GL11.glTranslatef(-0.4f, 0f, 0f);
		}
		if(main != null) main.draw();
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		
		list.add("This block measure a signal");
		list.add("and plot a graph with");
		list.add("Can store 256 samples.");
	}
}
/*
 * 	        	GL11.glScalef(1f, -1f, 1f);
	        	GL11.glTranslatef(0.1f,-0.5f,0.5f); 	
	        	GL11.glRotatef(90,0f,1f,0f);  */
