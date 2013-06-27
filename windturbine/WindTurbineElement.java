package mods.eln.windturbine;

import mods.eln.Eln;
import mods.eln.ghost.GhostObserver;
import mods.eln.item.DynamoDescriptor;
import mods.eln.item.WindRotorDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalPowerSource;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class WindTurbineElement extends TransparentNodeElement implements GhostObserver{

	NodeElectricalLoad positiveLoad = new NodeElectricalLoad("positiveLoad");
	NodeElectricalLoad negativeLoad = new NodeElectricalLoad("negativeLoad");
	
	ElectricalPowerSource powerSource = new ElectricalPowerSource(positiveLoad, negativeLoad);
	
	WindTurbineSlowProcess slowProcess = new WindTurbineSlowProcess("slowProcess",this);
	
	WindTurbineDescriptor descriptor;
	
	public WindTurbineElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		
		Eln.ghostManager.addObserver(this);
		
		this.descriptor = (WindTurbineDescriptor) descriptor;
		
		electricalLoadList.add(positiveLoad);
		electricalLoadList.add(negativeLoad);
		
		electricalProcessList.add(powerSource);
		
		slowProcessList.add(slowProcess);
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return null;
		if(side == front.left()) return positiveLoad;
		if(side == front.right() && ! grounded) return negativeLoad;
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
		if(lrdu != LRDU.Down) return 0;
		if(side == front.left()) return Node.maskElectricalPower;
		if(side == front.right() && ! grounded) return node.maskElectricalPower;
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
	public void initialize() {
		setPhysicalValue(true);
		
		connect();
	}

	boolean inventoryChangeFlag = false;
	@Override
	public void inventoryChange(IInventory inventory) {
		//setPhysicalValue();
		inventoryChangeFlag = true;
		super.inventoryChange(inventory);
	}
	
	public void setPhysicalValue(boolean  boot) {
		ItemStack rotorStack = getInventory().getStackInSlot(WindTurbineContainer.windRotorSlotId);
		ItemStack dynamoStack = getInventory().getStackInSlot(WindTurbineContainer.dynamoSlotId);
		
		if(! boot)
		{
			Eln.ghostManager.removeGhostAndBlockWithObserver(node.coordonate);
		}
		
		if(rotorStack != null)
		{
			WindRotorDescriptor rotor = (WindRotorDescriptor) WindRotorDescriptor.getDescriptor(rotorStack);
			
			
			if(boot == true || rotor.ghostGroupe.newRotate(front).plot(node.coordonate, node.coordonate, 42))
			{
				
			}
			else
			{
				getInventory().setInventorySlotContents(WindTurbineContainer.windRotorSlotId, null);
				node.dropItem(rotorStack);
			}
			
		}
		if(dynamoStack != null)
		{
			DynamoDescriptor rotor = (DynamoDescriptor) WindRotorDescriptor.getDescriptor(dynamoStack);
			rotor.applyTo(positiveLoad,false);
			rotor.applyTo(negativeLoad,grounded);
		}
		else
		{
			positiveLoad.highImpedance();
			negativeLoad.highImpedance();
		}
		
	}
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(2 , 64, this);
	
	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new WindTurbineContainer(this.node, player, inventory);
	}

	@Override
	public Coordonate getGhostObserverCoordonate() {
		// TODO Auto-generated method stub
		return node.coordonate;
	}

	@Override
	public void ghostDestroyed(int UUID) {
		// TODO Auto-generated method stub
		//System.out.println("destroy : " + UUID);
		Eln.ghostManager.removeGhostAndBlockWithObserver(this.node.coordonate);
		ItemStack rotorStack = getInventory().getStackInSlot(WindTurbineContainer.windRotorSlotId);
		if(rotorStack != null)
		{
			node.dropItem(rotorStack);
			inventory.setInventorySlotContents(WindTurbineContainer.windRotorSlotId, null);
		}
		setPhysicalValue(false);
	}

	@Override
	public boolean ghostBlockActivated(int UUID, EntityPlayer entityPlayer,
			Direction side, float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onBreakElement() {
		Eln.ghostManager.removeObserver(this.node.coordonate);
		Eln.ghostManager.removeGhostAndBlockWithObserver(this.node.coordonate);
		super.onBreakElement();
	}
}
