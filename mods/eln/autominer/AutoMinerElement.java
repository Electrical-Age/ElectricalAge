package mods.eln.autominer;

import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.ghost.GhostObserver;
import mods.eln.heatfurnace.HeatFurnaceContainer;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElectricalLoadWatchdog;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.VoltageWatchdogProcessForInventoryItemDamageSingleLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

public class AutoMinerElement extends TransparentNodeElement implements GhostObserver {
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(3, 64, this);
	
	NodeElectricalLoad inPowerLoad = new NodeElectricalLoad("inPowerLoad");
	AutoMinerSlowProcess slowProcess = new AutoMinerSlowProcess(this);
	TransparentNodeElectricalLoadWatchdog electricalLoadWatchdog = new TransparentNodeElectricalLoadWatchdog(this, inPowerLoad, 2);
	
	VoltageWatchdogProcessForInventoryItemDamageSingleLoad electricalDrillWatchDog = new VoltageWatchdogProcessForInventoryItemDamageSingleLoad(inventory, AutoMinerContainer.electricalDrillSlotId, inPowerLoad);
	VoltageWatchdogProcessForInventoryItemDamageSingleLoad electricalScannerWatchDog = new VoltageWatchdogProcessForInventoryItemDamageSingleLoad(inventory, AutoMinerContainer.OreScannerSlotId, inPowerLoad);
	
	AutoMinerDescriptor descriptor;
	
	public AutoMinerElement(TransparentNode transparentNode,
			TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		this.descriptor = (AutoMinerDescriptor) descriptor;
		this.slowProcessList.add(electricalLoadWatchdog);
		electricalLoadList.add(inPowerLoad);
		slowProcessList.add(slowProcess);
		slowProcessList.add(electricalDrillWatchDog);
		slowProcessList.add(electricalScannerWatchDog);
		Eln.ghostManager.addObserver(this);
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		return inPowerLoad;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		return NodeBase.maskElectricalPower;
	}

	@Override
	public String multiMeterString(Direction side) {
		return Utils.plotUIP(inPowerLoad.Uc, inPowerLoad.getCurrent());
	}

	@Override
	public String thermoMeterString(Direction side) {
		return "";
	}

	@Override
	public void initialize() {
		descriptor.applyTo(inPowerLoad);
		descriptor.applyTo(electricalLoadWatchdog);
		connect();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		return false;
	}

	@Override
	public boolean hasGui() {
		return true;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new AutoMinerContainer(node,player, inventory);
	}
	
	@Override
	public IInventory getInventory() {
		return inventory;
	}	
	
	@Override
	public void onBreakElement() {
		slowProcess.onBreakElement();
		super.onBreakElement();
	}

	@Override
	public Coordonate getGhostObserverCoordonate() {
		return node.coordonate;
	}

	@Override
	public void ghostDestroyed(int UUID) {
		slowProcess.ghostDestroyed(UUID);
	}

	@Override
	public boolean ghostBlockActivated(int UUID, EntityPlayer entityPlayer,
			Direction side, float vx, float vy, float vz) {
		return false;
	}
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeShort(slowProcess.pipeLength);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
