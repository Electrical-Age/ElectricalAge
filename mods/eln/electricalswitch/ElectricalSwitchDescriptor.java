package mods.eln.electricalswitch;

import java.util.List;

import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ThermalLoadInitializer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import com.google.common.base.Function;


public class ElectricalSwitchDescriptor extends SixNodeDescriptor{

	public ElectricalSwitchDescriptor(
			String name, CableRenderDescriptor cableRender,Obj3D obj,
			double nominalVoltage,double nominalPower,double nominalDropFactor,
			double maximalVoltage,double maximalPower,
			ThermalLoadInitializer thermal,
			boolean signalSwitch
			) {
		super(name, ElectricalSwitchElement.class,ElectricalSwitchRender.class);
		this.nominalVoltage = nominalVoltage;
		this.nominalPower = nominalPower;
		this.maximalPower = maximalPower;
		this.maximalVoltage = maximalVoltage;
		this.nominalDropFactor = nominalDropFactor;
		this.cableRender = cableRender;
		electricalRs = nominalVoltage*nominalVoltage / nominalPower * nominalDropFactor / 3;
		this.obj = obj;
		if(obj != null){
			if(main == null) main = obj.getPart("case");
			if(main == null) main = obj.getPart("main");
			if(lever == null) lever = obj.getPart("lever");
			if(lever == null) lever = obj.getPart("button");
			led = obj.getPart("led");
			halo = obj.getPart("halo");	

			
			if(obj.getString("type").equals("lever")){
				objType = ObjType.Lever;
				if(lever != null){
					speed = lever.getFloat("speed");
					alphaOff = lever.getFloat("alphaOff");
					alphaOn = lever.getFloat("alphaOn");
				}
			}
			else if(obj.getString("type").equals("button")){
				objType = ObjType.Button;
				if(lever != null){
					speed = lever.getFloat("speed");
					leverTx = lever.getFloat("tx");
				}
			}

		}
		this.thermal = thermal;
		double I = maximalPower / nominalVoltage;
		thermal.setMaximalPower(I*I*electricalRs*3);
		this.signalSwitch = signalSwitch;
	}
	
	public float speed = 1f;
	float alphaOn,alphaOff;
	float leverTx;
	enum ObjType {Lever,Button};
	ObjType objType;
	
	boolean signalSwitch;

	double electricalRs;
	ThermalLoadInitializer thermal;
	
	Obj3D obj;
	Obj3DPart main,lever,led,halo;

	CableRenderDescriptor cableRender;
	
	double nominalVoltage, nominalPower,nominalDropFactor;
	double maximalVoltage, maximalPower;	
	public void applyTo(ElectricalLoad load)
	{
		load.setRs(electricalRs);
		load.setMinimalC(Eln.simulator);
	}
	public void applyTo(ElectricalResistor resistor,boolean state)
	{
		if(state)
		{
			resistor.setR(electricalRs);
		}
		else
		{
			resistor.highImpedance();
		}
	}
	


	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return true;
	}	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// TODO Auto-generated method stub
		
		if(type == ItemRenderType.INVENTORY) GL11.glScalef(1.8f, 1.8f, 1.8f);
		draw(0f,0f);
	}	
	public void draw(float on,float distance) {
		switch (objType) {
		case Button:
			
			
			if(main != null)main.draw();
			
			GL11.glTranslatef(leverTx*on, 0f, 0f);
			if(lever != null) lever.draw();
			
			if(on < 0.5f){
				GL11.glColor3f(234f/255f, 80/255f, 0f);
				Utils.disableLight();
				if(led != null) led.draw();
				Utils.enableBlend();
				
				if(halo != null){
	

					Utils.drawHaloNoLightSetup(halo,distance);
				}
			
				Utils.disableBlend();
				Utils.enableLight();
			}
			else
			{
				if(led != null) led.draw();
			}

			break;
		case Lever:
			if(main != null)main.draw();


			if(lever != null) {
				float switchDelta;			
				lever.draw(on*(alphaOn - alphaOff) + alphaOff, 0, 1, 0);
			}		
			break;
		default:
			break;
		}		
	}
	public int getNodeMask()
	{
		if(signalSwitch)
			return NodeBase.maskElectricalGate;
		else 
			return NodeBase.maskElectricalPower;
	}
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Can manualy cut off a line");
	}
}
