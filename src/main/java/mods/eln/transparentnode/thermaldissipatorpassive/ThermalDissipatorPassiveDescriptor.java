package mods.eln.transparentnode.thermaldissipatorpassive;

import java.util.List;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ThermalLoad;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import static mods.eln.i18n.I18N.tr;

public class ThermalDissipatorPassiveDescriptor extends TransparentNodeDescriptor {
	public double thermalRs,thermalRp,thermalC;
	private Obj3D obj;
	Obj3DPart main;
	public ThermalDissipatorPassiveDescriptor(
			String name,
			Obj3D obj,
			double warmLimit,double coolLimit,
			double nominalP,double nominalT,
			double nominalTao,double nominalConnectionDrop
			) {
		super(name, ThermalDissipatorPassiveElement.class, ThermalDissipatorPassiveRender.class);
		thermalC = nominalP * nominalTao / nominalT;
		thermalRp = nominalT / nominalP;
		thermalRs = nominalConnectionDrop / nominalP;
		this.coolLimit = coolLimit;
		this.warmLimit = warmLimit;
		this.nominalP = nominalP;
		this.nominalT = nominalT;
		Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);
		this.obj = obj;
		if(obj != null) main = obj.getPart("main");
	}
	double warmLimit,coolLimit;
	double nominalP, nominalT;
	public void applyTo(ThermalLoad load)
	{
		load.set(thermalRs, thermalRp, thermalC);
	}

	public void setParent(net.minecraft.item.Item item, int damage)
	{
		super.setParent(item, damage);
		Data.addThermal(newItemStack());
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add(tr("Used to cool down turbines."));
		list.add(tr("Max. temperature: %1$°C", Utils.plotValue(warmLimit)));
		list.add(tr("Nominal usage:"));
		list.add("  " + tr("Temperature: %1$°C", Utils.plotValue(nominalT)));
		list.add("  " + tr("Cooling power: %1$W", Utils.plotValue(nominalP)));
	}
	
	
	public void draw()
	{
		if(main != null) main.draw();
	}
	
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		
		return true;
	}
	@Override
	public boolean use2DIcon() {
		return false;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		
		return true;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		
		draw();
	}
}
