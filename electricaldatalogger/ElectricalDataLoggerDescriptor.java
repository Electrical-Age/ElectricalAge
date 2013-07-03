package mods.eln.electricaldatalogger;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.misc.Direction;
import mods.eln.misc.IFunction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ThermalLoadInitializer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import com.google.common.base.Function;


public class ElectricalDataLoggerDescriptor extends SixNodeDescriptor{

	public ElectricalDataLoggerDescriptor(		
					String name,
					boolean onFloor,
					String objName,
					float cr,float cg,float cb
					) {
		super(name, ElectricalDataLoggerElement.class, ElectricalDataLoggerRender.class);
		this.cb = cb;
		this.cr = cr;
		this.cg = cg;
		this.onFloor = onFloor;
		obj = Eln.obj.getObj(objName);
		if(obj != null)
		{
			main = obj.getPart("main");
			if(main != null)
			{
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
			}
		}
	}
	Obj3D obj;
	Obj3DPart main;
	float sx,sy,sz;
	float tx,ty,tz;
	float rx,ry,rz,ra;
	float mx,my;
	
	float cr, cg, cb;
	
	public boolean onFloor;
	void draw(DataLogs log,LRDU front)
	{
		front.glRotateOnX();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		if(main != null) main.drawList();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		if(log != null)
		{
			GL11.glColor4f(1f, 0f, 0f, 1f);
			GL11.glDisable(GL11.GL_LIGHTING);
	       // GL11.glPushMatrix();	
        		GL11.glTranslatef(tx,ty,tz); 	
        		GL11.glRotatef(ra,rx,ry,rz);  
	        	GL11.glScalef(sx, sy, sz);
	        	log.draw(mx,my);

				
				/*
	        	GL11.glTranslatef(1f-0.22f,-0.5f+0.16f,-0.5f+0.1f); 	        	
	        	GL11.glRotatef(90,0f,0f,1f);	
	        	GL11.glScalef(0.7f/1.5f, 0.7f/1.5f, 1f);
		        log.draw(0.8f*1.5f,0.8f*1.5f);*/
	        //GL11.glPopMatrix();
	        GL11.glEnable(GL11.GL_LIGHTING);
		}
	}
	
	
	
	
	@Override
	public boolean hasVolume() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean canBePlacedOnSide(Direction side) {
		if(onFloor && side != Direction.YN) return false;
		return super.canBePlacedOnSide(side);
	}
}


/*
 * 	        	GL11.glScalef(1f, -1f, 1f);
	        	GL11.glTranslatef(0.1f,-0.5f,0.5f); 	
	        	GL11.glRotatef(90,0f,1f,0f);  */
