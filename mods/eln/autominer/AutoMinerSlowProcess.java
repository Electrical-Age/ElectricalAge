package mods.eln.autominer;

import java.util.ArrayList;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.ghost.GhostElement;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.item.OreScanner;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.ore.OreBlock;
import mods.eln.sim.IProcess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;

public class AutoMinerSlowProcess implements IProcess,INBTTReady{
	AutoMinerElement miner;
	public AutoMinerSlowProcess(AutoMinerElement autoMiner) {
		this.miner = autoMiner;
	}
	
	int pipeLength = 0;
	
	double energyCounter = 0,energyTarget = 0;
	
	int workY;
	
	static final int pipeGhostUUID = 75;

	enum jobType {none,ore,pipeAdd,pipeRemove};
	jobType job = jobType.none;
	Coordonate jobCoord = new Coordonate();
	
	@Override
	public void process(double time) {
		ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.electricalDrillSlotId));
		OreScanner scanner = (OreScanner) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.OreScannerSlotId));
		MiningPipeDescriptor pipe = (MiningPipeDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.MiningPipeSlotId));
		
		energyCounter += miner.inPowerLoad.getRpPower()*time;
		
		if(job != jobType.none) {
			if(energyCounter >= energyTarget || (job == jobType.ore && drill == null)) {
				setupJob();
			}		
			
			if(energyCounter >= energyTarget) {
				switch(job) {
					case ore:
						Block block = Block.blocksList[jobCoord.world().getBlockId(jobCoord.x, jobCoord.y, jobCoord.z)];
						int meta = jobCoord.world().getBlockMetadata(jobCoord.x, jobCoord.y, jobCoord.z);
						ArrayList<ItemStack> drop = block.getBlockDropped(jobCoord.world(), jobCoord.x, jobCoord.y, jobCoord.z, meta, 0);
						
						for(ItemStack stack : drop) {
							drop(stack);
						}
						
						jobCoord.world().setBlock(jobCoord.x, jobCoord.y, jobCoord.z, 0);
						
						energyCounter -= energyTarget;
						setupJob();
							
					break;
					case pipeAdd:
						Eln.ghostManager.createGhost(jobCoord, miner.node.coordonate, jobCoord.y);
						miner.inventory.decrStackSize(AutoMinerContainer.MiningPipeSlotId, 1);
												
						pipeLength++;
						miner.needPublish();
						
						energyCounter -= energyTarget;
						setupJob();	
					break;	
					
					case pipeRemove:
						Eln.ghostManager.removeGhostAndBlock(jobCoord);
						if(miner.inventory.getStackInSlot(AutoMinerContainer.MiningPipeSlotId) == null) {
							miner.inventory.setInventorySlotContents(AutoMinerContainer.MiningPipeSlotId, Eln.miningPipeDescriptor.newItemStack(1));
						}
						else{
							miner.inventory.decrStackSize(AutoMinerContainer.MiningPipeSlotId, -1);
						}
						 
						pipeLength--;
						miner.needPublish();
						
						energyCounter -= energyTarget;
						setupJob();	
					break;
				default:
					break;
				}
			}
		}
		else {
			setupJob();
		}
		
		switch(job) {
		case none:
			miner.inPowerLoad.setRp(Double.POSITIVE_INFINITY);
			break;
		case ore:
			if(drill == null){
				miner.inPowerLoad.setRp(Double.POSITIVE_INFINITY);
			}
			else {
				miner.inPowerLoad.setRp(drill.getRp(scanner != null ? scanner.OperationEnergy : 0));
			}
			break;
		case pipeAdd:
			miner.inPowerLoad.setRp(miner.descriptor.pipeOperationRp);
			break;
		case pipeRemove:
			miner.inPowerLoad.setRp(miner.descriptor.pipeOperationRp);
			break;		
		}
	}
	
	public void drop(ItemStack stack) {
		Direction dir = miner.front;
		TileEntityChest chestEntity = null;
		
		for(int idx = 0; idx < 4; idx++) {
			if(dir.getBlock(miner.node.coordonate) instanceof BlockChest) {
				chestEntity = (TileEntityChest) dir.getTileEntity(miner.node.coordonate);
				break;
			}					
			dir = dir.left();
		}
		
		//for(ItemStack stack : drop)
	//	{
			if(chestEntity != null) {
				Utils.tryPutStackInInventory(stack, chestEntity);
			}
			if(stack.stackSize != 0) miner.node.dropItem(stack);
		//}
	}
	
	void setupJob() {
		ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.electricalDrillSlotId));
		OreScanner scanner = (OreScanner) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.OreScannerSlotId));
		MiningPipeDescriptor pipe = (MiningPipeDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.MiningPipeSlotId));
		
		int scannerRadius = 0;
		double scannerEnergy = 0;
		if(scanner != null) {
			scannerRadius = scanner.radius;
			scannerEnergy = scanner.OperationEnergy;
		}
		
		World world = miner.node.coordonate.world();
		jobCoord.dimention = miner.node.coordonate.dimention;
		jobCoord.x = miner.node.coordonate.x;
		jobCoord.y = miner.node.coordonate.y - pipeLength;
		jobCoord.z = miner.node.coordonate.z;
		/*for(jobCoord.y = miner.node.coordonate.y - 1; jobCoord.y > 0;jobCoord.y--)
		{
			GhostElement ghost = Eln.ghostManager.getGhost(jobCoord);
			if(ghost == null || ghost.getObservatorCoordonate().equals(miner.node.coordonate) != true)
			{
				jobCoord.y++;
				break;
			}
		}*/
		//JobCoord at last pipe		
		
		boolean jobFind = false;
		if(drill == null) {
			if(jobCoord.y != miner.node.coordonate.y) {
				ItemStack pipeStack = miner.inventory.getStackInSlot(AutoMinerContainer.MiningPipeSlotId);
				if(pipeStack == null || (pipeStack.stackSize != pipeStack.getMaxStackSize() && pipeStack.stackSize != miner.inventory.getInventoryStackLimit())) {
					jobFind = true;
					setJob(jobType.pipeRemove);
				}
			}
		}
		else if(pipe != null) {		
			if(jobCoord.y < miner.node.coordonate.y - 1) {
				for(jobCoord.z = miner.node.coordonate.z - scannerRadius;jobCoord.z <= miner.node.coordonate.z + scannerRadius;jobCoord.z++) {
					for(jobCoord.x = miner.node.coordonate.x - scannerRadius;jobCoord.x <= miner.node.coordonate.x + scannerRadius;jobCoord.x++) {
						if(checkIsOre(jobCoord)) {
							jobFind = true;
							setJob(jobType.ore);
							break;
						}
					}
					if(jobFind == true) break;
				}
			}
				
			if(jobFind == false && jobCoord.y > 2) {
				jobCoord.x = miner.node.coordonate.x;
				jobCoord.y--;
				jobCoord.z = miner.node.coordonate.z;
				
				int blockId = jobCoord.world().getBlockId(jobCoord.x, jobCoord.y, jobCoord.z);
				if(		blockId != 0 
						&& blockId != Block.waterMoving.blockID && blockId != Block.waterStill.blockID
						&& blockId != Block.lavaMoving.blockID && blockId != Block.lavaStill.blockID) {
					if(blockId != Block.obsidian.blockID && blockId != Block.bedrock.blockID) {
						jobFind = true;
						setJob(jobType.ore);
					}
				}
				else{
					jobFind = true;
					setJob(jobType.pipeAdd);
				}
			}
		}	
		if(jobFind == false) setJob(jobType.none);
		
		switch(job) {
		case none: 
			energyTarget = 0;
			break;
		case ore:
			energyTarget = drill.OperationEnergy + scannerEnergy;
			break;
		case pipeAdd:
			energyTarget = miner.descriptor.pipeOperationEnergy;
			break;
		case pipeRemove:
			energyTarget = miner.descriptor.pipeOperationEnergy;
			break;
		default:
			break;
		}
	}
	
	void setJob(jobType job) {
		if(job != this.job) {
			energyCounter = 0;
		}
		this.job = job;
	}
	
	boolean checkIsOre(Coordonate coordonate) {
		int blockId = coordonate.world().getBlockId(coordonate.x, coordonate.y, coordonate.z);
		Block block = Block.blocksList[blockId];
		if(block instanceof BlockOre) return true;
		if(block instanceof OreBlock) return true;
		if(block instanceof BlockRedstoneOre)return true;
		
		return false;
	}

	public void onBreakElement() {
		// TODO Auto-generated method stub
		destroyPipe(-1);
	}
	
	void destroyPipe(int jumpY) {
		dropPipe(jumpY);
		Eln.ghostManager.removeGhostAndBlockWithObserver(miner.node.coordonate);
		pipeLength = 0;
		miner.needPublish();
	}
	
	void dropPipe(int jumpY) {
		World world = miner.node.coordonate.world();
		Coordonate coord = new Coordonate(miner.node.coordonate);
/*
		for(coord.y = miner.node.coordonate.y - 1; coord.y > 0;coord.y--)
		{
			GhostElement ghost = Eln.ghostManager.getGhost(coord);
			if(coord.y != jumpY)
			{
				if(ghost == null || ghost.getObservatorCoordonate().equals(miner.node.coordonate) != true)
				{
					coord.y++;
					break;
				}
			}
			
			Utils.dropItem(Eln.miningPipeDescriptor.newItemStack(1), coord);
		}	
		*/
		for(coord.y = miner.node.coordonate.y - 1; coord.y >= miner.node.coordonate.y - pipeLength;coord.y--) {
			Utils.dropItem(Eln.miningPipeDescriptor.newItemStack(1), coord);
		}
	}
	
	public void ghostDestroyed(int UUID) {
		destroyPipe(UUID);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		pipeLength = nbt.getInteger(str + "AMSP" + "pipeLength");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setInteger(str + "AMSP" + "pipeLength", pipeLength);
	}
}