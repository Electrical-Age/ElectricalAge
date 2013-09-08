package mods.eln.electricalfurnace;

import java.util.List;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.IFunction;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

public class ElectricalFurnaceDescriptor extends TransparentNodeDescriptor{

	public ElectricalFurnaceDescriptor(
			String name, 
			IFunction PfT,
			IFunction thermalPlostfT,
			double thermalC
			) {
		super(name, ElectricalFurnaceElement.class, ElectricalFurnaceRender.class);
		// TODO Auto-generated constructor stub
		this.PfT = PfT;
		this.thermalPlostfT= thermalPlostfT; 
		this.thermalC = thermalC;
	}
	public IFunction PfT,thermalPlostfT;
	public double thermalC;
	//public double thermalRp;
	//ThermalLoadInitializer thermal;
	
	
	
	
	
	public void applyTo(ThermalLoad load)
	{
		load.set(Double.POSITIVE_INFINITY,thermalPlostfT.getValue(0),thermalC);
	}
	
	public void refreshTo(ThermalLoad load,double conductionFactor)
	{
		double Rp = (load.Tc / thermalPlostfT.getValue(load.Tc)) / conductionFactor ;
		if(Rp < 0.1) Rp = 0.1;
		load.setRp(Rp);
	}
	
	@Override
	public void setParent(Item item, int damage) {
		// TODO Auto-generated method stub
		super.setParent(item, damage);
		Data.addMachine(newItemStack());
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
		 Eln.obj.draw("ElectricFurnace", "furnace");	
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("A minecraft furnace equivalent");
		list.add("But need electricity to work");
	}
}
