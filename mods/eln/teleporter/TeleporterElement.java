package mods.eln.teleporter;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;

public class TeleporterElement extends TransparentNodeElement implements ITeleporter{

	TeleporterDescriptor descriptor;
	NodeElectricalLoad powerLoad = new NodeElectricalLoad("powerLoad");
	TeleporterSlowProcess slowProcess = new TeleporterSlowProcess();
	
	
	static public ArrayList<ITeleporter> teleporterList = new ArrayList<ITeleporter>();
	
	public TeleporterElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		this.descriptor = (TeleporterDescriptor)descriptor;
		
		
		electricalLoadList.add(powerLoad);
		slowProcessList.add(slowProcess);
		
		
		teleporterList.add(this);
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String thermoMeterString(Direction side) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public void initialize() {
		descriptor.cable.applyTo(powerLoad, false);
		
		
		for(Coordonate c : descriptor.getPowerCoordonate(node.coordonate.world())){
			TeleporterPowerNode n = new TeleporterPowerNode();
			n.setElement(this);
			c.applyTransformation(front,node.coordonate);
			n.onBlockPlacedBy(c, Direction.XN, null, null);
			
			powerNodeList.add(n);
		}
		
		
		connect();
	}
	

	@Override
	public void onBreakElement() {
		// TODO Auto-generated method stub
		super.onBreakElement();
		teleporterList.remove(this);
		for(TeleporterPowerNode n : powerNodeList){
			n.onBreakBlock();
		}
		powerNodeList.clear();
		
	}
	ArrayList<TeleporterPowerNode> powerNodeList = new ArrayList<TeleporterPowerNode>();

	
	
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	String name = "Unnamed",targetName = "Unnamed";
	double energyHit = 0;
	double energyTarget = 0;
	double powerCharge = 2000;
	
	
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		
		nbt.setString(str + "name", name);
		nbt.setString(str + "targetName", targetName);
		nbt.setDouble(str + "powerCharge", powerCharge);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
		
		name = nbt.getString(str + "name");
		targetName = nbt.getString(str + "targetName");
		powerCharge = nbt.getDouble(str + "powerCharge");
	}
	
	public static final byte StateIdle = 0;
	public static final byte StateCharge = 1;
	public static final byte StateTeleport = 2;
	public static final byte StateStart = 3;
	public static final byte StateEnd = 4;
	
	byte state = StateIdle;
	float timeCounter;
	
	
	void setState(byte state){
		if(state != this.state){
		
			switch (this.state) {

			case StateCharge:
				powerLoad.setRp(100000000000.0);
				break;
			default:
				break;
			}	
						
			this.state = state;
			
			switch (this.state) {
			case StateStart:
			case StateEnd:
			case StateTeleport:
				timeCounter = 0;
				break;
			case StateCharge:
				powerLoad.setRp(Math.pow(descriptor.cable.electricalNominalVoltage,2) / powerCharge);
				break;
			default:
				break;
			}

			
			
			System.out.println("Teleporter state=" + state);
			needPublish();
			
			
		}
	}
	
	
	public static final byte eventNoTargetFind = 1;
	public static final byte eventMultipleoTargetFind = 2;
	public static final byte eventTargetFind = 3;
	public static final byte eventSameTarget = 4;
	public static final byte eventNotSameDimensionTarget = 5;
	
	class TeleporterSlowProcess implements IProcess{

		int dx,dy,dz;
		
		
		@Override
		public void process(double time) {
			
			
			switch(state)
			{
			case StateIdle:	
				if(startFlag){
					energyHit = 0;
					if(targetName.equals(name)){
						sendIdToAllClient(eventSameTarget);		
						break;
					}
					int count = getTargetCount();
					if(count == 0){
						sendIdToAllClient(eventNoTargetFind);	
						break;
					}
					if(count > 1){
						sendIdToAllClient(eventMultipleoTargetFind);
						break;
					}
					sendIdToAllClient(eventTargetFind);
					
					setState(StateStart);
				
				}
				break;
			case StateStart:
				timeCounter += time;
				if(timeCounter > 2){
					setState(StateCharge);
				}
				break;
			case StateEnd:
				timeCounter += time;
				if(timeCounter > 2){
					setState(StateIdle);
				}
				break;
			case StateCharge:
				{
					if(targetName.equals(name)){
						sendIdToAllClient(eventSameTarget);		
						setState(StateEnd);
						break;
					}
					int count = getTargetCount();
					if(count == 0){
						sendIdToAllClient(eventNoTargetFind);	
						setState(StateEnd);
						break;
					}
					if(count > 1){
						sendIdToAllClient(eventMultipleoTargetFind);
						setState(StateEnd);
						break;
					}
					
					ITeleporter target = getTarget();
					Coordonate c = getTeleportCoordonate();
					double distance = getTeleportCoordonate().trueDistanceTo(c);
					AxisAlignedBB bb = descriptor.getBB(node.coordonate,front);
					int playerCount = c.world().getEntitiesWithinAABB(EntityPlayer.class, bb).size();
					int itemCount = c.world().getEntitiesWithinAABB(EntityItem.class,bb).size();
					int petCount = c.world().getEntitiesWithinAABB(EntityLivingBase.class,bb).size() - playerCount;
					
					energyTarget = 	10000 + 
									20000 *playerCount +
									5000 *petCount +
									1000 *itemCount;
					
					energyTarget *= 1.0 + distance/250.0;
					
					
					energyHit += powerLoad.getRpPower()*time;
					if(energyHit >= energyTarget){
						dx = target.getTeleportCoordonate().x - c.x;
						dy = target.getTeleportCoordonate().y - c.y;
						dz = target.getTeleportCoordonate().z - c.z;
						setState(StateTeleport);
					}
				}
				break;
			
				
			case StateTeleport:
				{
					timeCounter += time;
					if(timeCounter > 2){
						
						AxisAlignedBB bb = descriptor.getBB(node.coordonate,front);
						List list = node.coordonate.world().getEntitiesWithinAABB(Entity.class, bb);
						for(Object o : list){
							Entity e = (Entity)o;
							if(e instanceof EntityPlayerMP)
								((EntityPlayerMP)e).setPositionAndUpdate(e.posX + dx, e.posY + dy, e.posZ + dz);
							else
								e.setPosition(e.posX + dx, e.posY + dy, e.posZ + dz);
						}
						setState(StateEnd);
					}	
				}
				break;

			}
				
			
			
			startFlag = false;
		}
		

	}
	
	int getTargetCount()
	{
		int count = 0;
		for(ITeleporter t : teleporterList){
			if(t.getName().equals(targetName) && node.coordonate.dimention == t.getTeleportCoordonate().dimention){
				count++;
			}
		}
		return count;			
	}
	
	ITeleporter getTarget()
	{
		ITeleporter target = null;
		for(ITeleporter t : teleporterList){
			if(t.getName().equals(targetName)  && node.coordonate.dimention == t.getTeleportCoordonate().dimention){
				if(target != null) return null;
				target = t;
			}
		}
		return target;
	}
	
	boolean startFlag = false;
	
	public static final byte setNameId = 1;
	public static final byte startId = 2;
	public static final byte setTargetNameId = 3;
	public static final byte setChargePowerId = 4;
	
	@Override
	public byte networkUnserialize(DataInputStream stream) {
		
		switch(super.networkUnserialize(stream)){
		case setNameId:
			try {
				name = stream.readUTF();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			needPublish();
			break;
		case setTargetNameId:
			try {
				targetName = stream.readUTF();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			needPublish();
			break;
		case startId:
			startFlag = true;
			break;
		case setChargePowerId:
			try {
				powerCharge = stream.readFloat();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			needPublish();
			break;
		}
		return unserializeNulldId;
	}
	
	
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeUTF(name);
			stream.writeUTF(targetName);
			stream.writeFloat((float) powerCharge);
			stream.writeByte(state);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	
	
	


	@Override
	public Coordonate getTeleportCoordonate() {
		// TODO Auto-generated method stub
		return descriptor.getTeleportCoordonate(front,node.coordonate);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}
}
