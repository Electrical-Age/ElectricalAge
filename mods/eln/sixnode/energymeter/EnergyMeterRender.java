package mods.eln.sixnode.energymeter;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.energymeter.EnergyMeterElement.Mod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class EnergyMeterRender extends SixNodeElementRender {

	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
	EnergyMeterDescriptor descriptor;

	public EnergyMeterRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (EnergyMeterDescriptor) descriptor;
		
		for(int idx = 0;idx < energyRc.length;idx++){
			energyRc[idx] = new RcInterpolator(0.2f);
		}

	}
	
	RcInterpolator[] energyRc = new RcInterpolator[7];
	
	@Override
	public void draw() {
		super.draw();
		
		//front.glRotateOnX();	
		descriptor.draw(energyStack/100.0);			
	}
	
	@Override
	public void refresh(float deltaT) {
		double errorComp = error * 1 * deltaT;
		energyStack += power*deltaT + errorComp;
		error -= errorComp;
		
		double stack = energyStack;
		for(int idx = 0;idx < energyRc.length;idx++){
			
			energyRc[idx].setTarget((float) ((stack) % 10));
			energyRc[idx].step(deltaT);
			stack /= 10.0;
		}
		
		
		timerCouter += deltaT;
		serverPowerIdTimer += deltaT;
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return cableRender;
	}

	double timerCouter,energyStack;
	boolean switchState;
	String password;
	Mod mod;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);

		try {
			
			switchState = stream.readBoolean();
			password = stream.readUTF();
			mod = Mod.valueOf(stream.readUTF());
			timerCouter = stream.readDouble();
			//energyStack = stream.readDouble();
			ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(Utils.unserialiseItemStack(stream), ElectricalCableDescriptor.class);
			
			if(desc == null)
				cableRender = null;
			else
				cableRender = desc.render;
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	CableRenderDescriptor cableRender;

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new EnergyMeterGui(player, inventory, this);
	}
	
	double power;
	double error;
	double serverPowerIdTimer = EnergyMeterElement.SlowProcess.publishTimeoutReset*34;
	@Override
	public void serverPacketUnserialize(DataInputStream stream) throws IOException {
		// TODO Auto-generated method stub
		super.serverPacketUnserialize(stream);
		
		switch (stream.readByte()) {
		case EnergyMeterElement.serverPowerId:
			if(serverPowerIdTimer > EnergyMeterElement.SlowProcess.publishTimeoutReset*3){
				energyStack = stream.readDouble();
				error = 0;
			}else{
				error = stream.readDouble() - energyStack;
			}
			power = stream.readDouble();
			serverPowerIdTimer = 0;
			break;

		default:
			break;
		}
	}



}
