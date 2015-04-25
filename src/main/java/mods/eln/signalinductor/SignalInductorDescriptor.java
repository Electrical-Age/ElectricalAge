package mods.eln.signalinductor;

import java.util.List;

import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SignalInductorDescriptor extends SixNodeDescriptor {

    ElectricalCableDescriptor cable;
    String descriptor;
    public double henri;

	public SignalInductorDescriptor(String name, double henri, ElectricalCableDescriptor cable) {
		super(name, SignalInductorElement.class, SignalInductorRender.class);
		this.henri = henri;
		this.cable = cable;
	}

	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addEnergy(newItemStack());
	}

	public void applyTo(ElectricalLoad load) {
		cable.applyTo(load);
	}

	public void applyTo(Inductor inductor) {
		inductor.setL(henri);
	}

	@Override
	public boolean use2DIcon() {
		return false;
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
	}
}
