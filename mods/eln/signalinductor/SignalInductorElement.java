





package mods.eln.signalinductor;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.entity.player.EntityPlayer;


public class SignalInductorElement extends SixNodeElement{

	public SignalInductorElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
    	electricalLoadList.add(postiveLoad);
    	electricalLoadList.add(negativeLoad);
    	electricalComponentList.add(inductor);
    	postiveLoad.setAsMustBeFarFromInterSystem();
    	this.descriptor = (SignalInductorDescriptor) descriptor;
	}


	public SignalInductorDescriptor descriptor;
	public NbtElectricalLoad postiveLoad = new NbtElectricalLoad("postiveLoad");
	public NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");
	public Inductor inductor = new Inductor("inductor",postiveLoad, negativeLoad);


	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		if(front == lrdu) return postiveLoad;
		if(front.inverse() == lrdu) return negativeLoad;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		// TODO Auto-generated method stub4
		if(front == lrdu) return descriptor.cable.getNodeMask();
		if(front.inverse() == lrdu) return descriptor.cable.getNodeMask();

		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return Utils.plotAmpere("I", inductor.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return "";
	}




	@Override
	public void initialize() {
		// TODO Auto-generated method stub
 
		descriptor.applyTo(negativeLoad);
		descriptor.applyTo(postiveLoad);
		descriptor.applyTo(inductor);
	}


	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{	
		return super.onBlockActivatedRotate(entityPlayer);
	}
	

}
