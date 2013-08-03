package mods.eln.solarpannel;

import org.lwjgl.opengl.GL11;

import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.IFunction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;

public class SolarPannelDescriptor extends TransparentNodeDescriptor{

	boolean basicModel;
	private Obj3D obj;
	private Obj3DPart main;
	private Obj3DPart panneau;
	private Obj3DPart foot;
	public SolarPannelDescriptor(
			String name,
			Obj3D obj,CableRenderDescriptor cableRender,
			GhostGroup ghostGroup, int solarOffsetX,int solarOffsetY,int solarOffsetZ,
			//FunctionTable solarIfSBase,
			double electricalUmax,double electricalPmax,
			double electricalDropFactor,
			double alphaMin,double alphaMax
			
			) {
		super(name, SolarPannelElement.class,SolarPannelRender.class);
		this.ghostGroup = ghostGroup;

		electricalRs = 	electricalUmax*electricalUmax*electricalDropFactor
						/electricalPmax/2.0;
		this.electricalPmax = electricalPmax;
		this.solarOffsetX = solarOffsetX;
		this.solarOffsetY = solarOffsetY;
		this.solarOffsetZ = solarOffsetZ;
		this.alphaMax = alphaMax;
		this.alphaMin = alphaMin;
		basicModel = true;
		this.electricalUmax = electricalUmax;
		
		this.obj = obj;
		if(obj != null){
			main = obj.getPart("main");
			panneau = obj.getPart("panneau");
			foot = obj.getPart("foot");
		}

		this.cableRender = cableRender;
		
		canRotate = alphaMax != alphaMin;
	}
	
	
	CableRenderDescriptor cableRender;
	double electricalUmax;
	double electricalPmax;
	public SolarPannelDescriptor(
			String name,
			GhostGroup ghostGroup, int solarOffsetX,int solarOffsetY,int solarOffsetZ,
			FunctionTable diodeIfUBase,
			FunctionTable solarIfSBase,
			double electricalUmax,double electricalImax,
			double electricalDropFactor,
			double alphaMin,double alphaMax
			
			) {
		super(name, SolarPannelElement.class,SolarPannelRender.class);
		this.ghostGroup = ghostGroup;
		this.diodeIfU = diodeIfUBase.duplicate(electricalUmax,electricalImax);
		electricalRs = 	electricalUmax*electricalImax*electricalDropFactor
						/electricalImax/electricalImax/2.0;
	//	this.efficiency = efficiency;
		this.solarIfS = solarIfSBase.duplicate(1.0,electricalImax);
		this.solarOffsetX = solarOffsetX;
		this.solarOffsetY = solarOffsetY;
		this.solarOffsetZ = solarOffsetZ;
		this.alphaMax = alphaMax;
		this.alphaMin = alphaMin;
		basicModel = false;
		canRotate = alphaMax != alphaMin;
	}
	int solarOffsetX, solarOffsetY, solarOffsetZ;
	double alphaMin, alphaMax;
	//double efficiency;
	double electricalRs;
	IFunction diodeIfU;
	FunctionTable solarIfS;

	boolean canRotate;
	
	public void applyTo(ElectricalLoad load,boolean grounded)
	{
		load.setRs(electricalRs);
		load.setMinimalC(Eln.simulator);
		load.grounded(grounded);
	}
	
	public void applyTo(DiodeProcess diode)
	{
		diode.IfU = diodeIfU;
	}
	
	public double alphaTrunk(double alpha)
	{
		if(alpha > alphaMax) return alphaMax;
		if(alpha < alphaMin) return alphaMin;
		return alpha;
	}
	
	
	void draw(float alpha,Direction front)
	{
		if(foot != null) foot.draw();
		if(panneau != null){
			GL11.glPushMatrix();
			panneau.draw(alpha,0f,0f,1f);
			GL11.glPopMatrix();
		}
		front.glRotateXnRef();
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
		// TODO Auto-generated method stub
		draw((float) alphaMin,Direction.XN);
	}
}
