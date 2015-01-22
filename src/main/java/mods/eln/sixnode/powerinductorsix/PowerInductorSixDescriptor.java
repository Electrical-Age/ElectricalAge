package mods.eln.sixnode.powerinductorsix;

import java.util.List;

import mods.eln.Eln;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.series.ISerie;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class PowerInductorSixDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	Obj3DPart InductorBaseExtention, InductorCables, InductorCore, Base;

	public PowerInductorSixDescriptor(
			String name,
			Obj3D obj,
			ISerie serie

	) {
		super(name, PowerInductorSixElement.class, PowerInductorSixRender.class);
		this.serie = serie;
		this.obj = obj;
		if (obj != null) {
			InductorBaseExtention = obj.getPart("InductorBaseExtention");
			InductorCables = obj.getPart("InductorCables");
			InductorCore = obj.getPart("InductorCore");
			Base = obj.getPart("Base");
		}

	}

	ISerie serie;

	public double getlValue(int cableCount) {
		if (cableCount == 0) return 0;
		return serie.getValue(cableCount - 1);
	}

	public double getlValue(IInventory inventory) {
		ItemStack core = inventory.getStackInSlot(PowerInductorSixContainer.cableId);
		if (core == null)
			return getlValue(0);
		else
			return getlValue(core.stackSize);
	}
	@Override
	public boolean use2DIcon() {
		return false;
	}
	public double getRsValue(IInventory inventory) {
		ItemStack core = inventory.getStackInSlot(PowerInductorSixContainer.coreId);

		if (core == null) return MnaConst.highImpedance;
		FerromagneticCoreDescriptor coreDescriptor = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core);

		double coreFactor = coreDescriptor.cableMultiplicator;

		return Eln.instance.lowVoltageCableDescriptor.electricalRs * coreFactor;
	}

	public void setParent(net.minecraft.item.Item item, int damage)
	{
		super.setParent(item, damage);
		Data.addEnergy(newItemStack());
	}

	void draw() {
		//UtilsClient.disableCulling();
	//	UtilsClient.disableTexture();
		if (null != Base) Base.draw();
		if (null != InductorBaseExtention) InductorBaseExtention.draw();
		if (null != InductorCables) InductorCables.draw();
		if (null != InductorCore) InductorCore.draw();
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
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw();
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {

		super.addInformation(itemStack, entityPlayer, list, par4);

	}

}
