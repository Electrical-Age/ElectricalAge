package mods.eln.autominer;

import java.util.List;

import org.lwjgl.opengl.GL11;
 
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElectricalLoadWatchdog;
import mods.eln.sim.ElectricalLoad;
import mods.eln.wiki.Data;

public class AutoMinerDescriptor extends TransparentNodeDescriptor {

	Coordonate[] powerCoord;
	Coordonate lightCoord, miningCoord;
	public Obj3D obj;
	public Obj3DPart core,gui,lamp,head,pipe;
	int deltaX,deltaY,deltaZ;
	public AutoMinerDescriptor(
			String name,
			Obj3D obj,
			Coordonate[] powerCoord,Coordonate lightCoord,Coordonate miningCoord,
			int deltaX,int deltaY,int deltaZ,
			double nominalVoltage, double maximalVoltage,
			double nominalPower, double nominalDropFactor,
			double pipeOperationTime, double pipeOperationEnergy
			) {
		super(name, AutoMinerElement.class, AutoMinerRender.class);
		this.nominalVoltage = nominalVoltage;
		this.maximalVoltage = maximalVoltage;
		this.pipeOperationTime = pipeOperationTime;
		this.pipeOperationEnergy = pipeOperationEnergy;
		pipeOperationPower = pipeOperationEnergy / pipeOperationTime;
		pipeOperationRp = nominalVoltage * nominalVoltage / pipeOperationPower;
		
		Rs = nominalVoltage * nominalVoltage / nominalPower * nominalDropFactor;
		
		this.powerCoord = powerCoord;
		this.lightCoord = lightCoord;
		this.miningCoord = miningCoord;
		this.obj = obj;
		
		this.deltaX = deltaX;
		this.deltaY = deltaY;
		this.deltaZ = deltaZ;
		
		core = obj.getPart("AutominerCore");
		gui = obj.getPart("AutominerGUI");
		lamp = obj.getPart("LampUser");
		head = obj.getPart("MinerHead");
		pipe = obj.getPart("MinerPipe");
	}

	double nominalVoltage, maximalVoltage;
	double pipeOperationTime, pipeOperationEnergy, pipeOperationPower;
	
	double pipeOperationRp;
	double Rs;
	
	
	
	public void applyTo(ElectricalLoad load) {
		load.setRs(Rs);
		load.setMinimalC(Eln.simulator);
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addMachine(newItemStack());
	}
	
	public void applyTo(TransparentNodeElectricalLoadWatchdog watch) {
		watch.negativeLimit = -maximalVoltage * 0.1;
		watch.positiveLimit = maximalVoltage;
	}
	
	@Override
	public boolean mustHaveFloor() {
		return false;
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		
	}
	
	void draw(){
		GL11.glRotatef(-90, 0, 1, 0);
		GL11.glTranslatef(0, -1.5f, 0);
		GL11.glScalef(0.5f, 0.5f, 0.5f);
		
		Utils.disableCulling();
		core.draw();
		gui.draw();
		lamp.draw();
		Utils.enableCulling();
	}
	
	
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// TODO Auto-generated method stub
		super.renderItem(type, item, data);
		draw();
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		return true;
	}
	
	public Coordonate[] getPowerCoordonate(World w) {
		Coordonate[] temp = new Coordonate[powerCoord.length];
		for(int idx = 0;idx < temp.length;idx++){
			temp[idx] = new Coordonate(powerCoord[idx]);
			temp[idx].setDimention(w.provider.dimensionId);
		}
		return temp;
	}
	
	public int getSpawnDeltaX() {
		// TODO Auto-generated method stub
		return deltaX;
	}
	public int getSpawnDeltaY() {
		// TODO Auto-generated method stub
		return deltaY;
	}
	public int getSpawnDeltaZ() {
		// TODO Auto-generated method stub
		return deltaZ;
	}
}
