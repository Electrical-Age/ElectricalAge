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
import mods.eln.misc.UtilsClient;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.wiki.Data;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

import com.google.common.base.Function;

public class ElectricalSwitchDescriptor extends SixNodeDescriptor {

	public ElectricalSwitchDescriptor(
			String name, CableRenderDescriptor cableRender, Obj3D obj,
			double nominalVoltage, double nominalPower, double nominalDropFactor,
			double maximalVoltage, double maximalPower,
			ThermalLoadInitializer thermal,
			boolean signalSwitch) {
		super(name, ElectricalSwitchElement.class, ElectricalSwitchRender.class);
		this.nominalVoltage = nominalVoltage;
		this.nominalPower = nominalPower;
		this.maximalPower = maximalPower;
		this.maximalVoltage = maximalVoltage;
		this.nominalDropFactor = nominalDropFactor;
		this.cableRender = cableRender;
		electricalRs = nominalVoltage * nominalVoltage / nominalPower * nominalDropFactor / 3;
		this.obj = obj;
		if(obj != null) {
			if(main == null) main = obj.getPart("case");
			if(main == null) main = obj.getPart("main");
			if(lever == null) lever = obj.getPart("lever");
			if(lever == null) lever = obj.getPart("button");
			led = obj.getPart("led");
			halo = obj.getPart("halo");	

			if(obj.getString("type").equals("lever")) {
				objType = ObjType.Lever;
				if(lever != null) {
					speed = lever.getFloat("speed");
					alphaOff = lever.getFloat("alphaOff");
					alphaOn = lever.getFloat("alphaOn");
				}
			}
			else if(obj.getString("type").equals("button")) {
				objType = ObjType.Button;
				if(lever != null) {
					speed = lever.getFloat("speed");
					leverTx = lever.getFloat("tx");
				}
			}
		}
		this.thermal = thermal;
		double I = maximalPower / nominalVoltage;
		thermal.setMaximalPower(I * I * electricalRs);
		this.signalSwitch = signalSwitch;
		
		pinDistance = Utils.getSixNodePinDistance(main);
	}
	
	public float speed = 1f;
	float alphaOn, alphaOff;
	float leverTx;
	enum ObjType {Lever, Button};
	ObjType objType;
	
	boolean signalSwitch;

	double electricalRs;
	ThermalLoadInitializer thermal;
	
	Obj3D obj;
	Obj3DPart main, lever, led, halo;

	CableRenderDescriptor cableRender;
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}
	
	double nominalVoltage, nominalPower, nominalDropFactor;
	double maximalVoltage, maximalPower;
	public float[] pinDistance;
	
	public void applyTo(ElectricalLoad load) {
		load.setRs(electricalRs);
	}
	
	public void applyTo(Resistor resistor, boolean state) {
		if(state) {
			resistor.setR(electricalRs);
		}
		else {
			resistor.highImpedance();
		}
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
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if(type == ItemRenderType.INVENTORY) GL11.glScalef(1.8f, 1.8f, 1.8f);
		draw(0f, 0f, null);
	}
	
	public void draw(float on, float distance, TileEntity e) {
		switch (objType) {
		case Button:
			if(main != null)main.draw();
			
			GL11.glTranslatef(leverTx * on, 0f, 0f);
			if(lever != null) lever.draw();
			
			if(on < 0.5f) {
				GL11.glColor3f(234f / 255f, 80 / 255f, 0f);
				UtilsClient.disableLight();
				if(led != null) led.draw();
				UtilsClient.enableBlend();
				
				if(halo != null) {
					if(e == null)
						UtilsClient.drawLight(halo);
					else
						UtilsClient.drawHaloNoLightSetup(halo, 234f / 255f, 80 / 255f, 0f, e, false);
				}
			
				UtilsClient.disableBlend();
				UtilsClient.enableLight();
			}
			else {
				if(led != null) led.draw();
			}

			break;
		case Lever:
			if(main != null)main.draw();


			if(lever != null) {
				float switchDelta;			
				lever.draw(on * (alphaOn - alphaOff) + alphaOff, 0, 1, 0);
			}		
			break;
		default:
			break;
		}		
	}
	
	public int getNodeMask() {
		if(signalSwitch)
			return NodeBase.maskElectricalGate;
		else 
			return NodeBase.maskElectricalPower;
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Can manually cut off a power line.");
	}
}
