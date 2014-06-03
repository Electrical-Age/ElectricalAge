package mods.eln.autominer;

import java.util.ArrayList;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.ghost.GhostElement;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.item.OreScanner;
import mods.eln.lampsocket.LightBlockEntity;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.ore.OreBlock;
import mods.eln.sim.IProcess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;

public class AutoMinerSlowProcess implements IProcess,INBTTReady {
	
	AutoMinerElement miner;
	
	public AutoMinerSlowProcess(AutoMinerElement autoMiner) {
		this.miner = autoMiner;
	}
	
	int pipeLength = 0;
	
	double energyCounter = 0, energyTarget = 0;
	
	int workY;
	
	//static final int pipeGhostUUID = 75;

	enum jobType {none,done,full, ore, pipeAdd, pipeRemove};
	jobType job = jobType.none,oldJob = jobType.none;
	Coordonate jobCoord = new Coordonate();
	int blinkCounter = 0;
	boolean isReadyToDrill(){
		ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.electricalDrillSlotId));
		if(drill == null) return false;
		return isStorageReady();
	}
	
	
	boolean isStorageReady(){
		for(int idx = AutoMinerContainer.StorageStartId;idx < AutoMinerContainer.StorageSize + AutoMinerContainer.StorageStartId;idx++){
			if(miner.inventory.getStackInSlot(idx) == null){
				return true;
			}
		}
		return false;
	}
	
	double oreRand = Math.random();
	
	@Override
	public void process(double time) {
		ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.electricalDrillSlotId));
		MiningPipeDescriptor pipe = (MiningPipeDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.MiningPipeSlotId));
		
		if(++blinkCounter >= 9){
			blinkCounter = 0;
			if((miner.inPowerLoad.Uc/miner.descriptor.nominalVoltage-0.5)*3 > Math.random())
				LightBlockEntity.addLight(miner.lightCoordonate, 12, 11);
		}
		
		energyCounter += miner.inPowerLoad.getRpPower() * time;
		
		if(job != jobType.none && job != jobType.full && job != jobType.done) {
			if(energyCounter >= energyTarget || (job == jobType.ore && !isReadyToDrill())) {
				setupJob();
			}		
			
			if(energyCounter >= energyTarget) {
				switch(job) {
				case ore:
					Block block = jobCoord.world().getBlock(jobCoord.x, jobCoord.y, jobCoord.z);
					int meta = jobCoord.world().getBlockMetadata(jobCoord.x, jobCoord.y, jobCoord.z);
					ArrayList<ItemStack> drop = block.getDrops(jobCoord.world(), jobCoord.x, jobCoord.y, jobCoord.z, meta, 0);
					
					for(ItemStack stack : drop) {
						drop(stack);
					}
					
					jobCoord.world().setBlockToAir(jobCoord.x, jobCoord.y, jobCoord.z);
					
					energyCounter -= energyTarget;
					oreRand = Math.random();
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
					else {
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
		default:
			miner.inPowerLoad.setRp(Double.POSITIVE_INFINITY);
			break;
		case ore:
			if(drill == null) {
				miner.inPowerLoad.setRp(Double.POSITIVE_INFINITY);
			}
			else {
			//	double p = drill.nominalPower + (scanner != null ? scanner.OperationEnergy/drill.operationTime : 0);
				double p = drill.nominalPower;
				miner.inPowerLoad.setRp(Math.pow(miner.descriptor.nominalVoltage,2.0)/p);
			}
			break;
		case pipeAdd:
			miner.inPowerLoad.setRp(miner.descriptor.pipeOperationRp);
			break;
		case pipeRemove:
			miner.inPowerLoad.setRp(miner.descriptor.pipeOperationRp);
			break;		
		}
		
		
		if(oldJob != job){
			miner.needPublish();
		}
		oldJob = job;
		//Utils.println(job);
	}
	
	public void drop(ItemStack stack) {
		/*Direction dir = miner.front;
		TileEntityChest chestEntity = null;
		
		for(int idx = 0; idx < 4; idx++) {
			if(dir.getBlock(miner.node.coordonate) instanceof BlockChest) {
				chestEntity = (TileEntityChest) dir.getTileEntity(miner.node.coordonate);
				break;
			}					
			dir = dir.left();
		}*/
		
			/*if(chestEntity != null) {
				Utils.tryPutStackInInventory(stack, chestEntity);
			}
			if(stack.stackSize != 0) miner.node.dropItem(stack);*/
		Utils.tryPutStackInInventory(stack, miner.inventory,AutoMinerContainer.StorageStartId,AutoMinerContainer.StorageSize);
	}
	
	boolean isMinable(Block block){
		return block != Blocks.air 
				&& (block) != Blocks.flowing_water && (block) != Blocks.water
				&& (block) != Blocks.flowing_lava && (block) != Blocks.lava
				&& (block) != Blocks.obsidian && (block) != Blocks.bedrock;
	}
	
	void setupJob() {
		ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.electricalDrillSlotId));
	//	OreScanner scanner = (OreScanner) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.OreScannerSlotId));
		MiningPipeDescriptor pipe = (MiningPipeDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getStackInSlot(AutoMinerContainer.MiningPipeSlotId));
		
		int scannerRadius = 10;
		double scannerEnergy = 0;

		
		World world = miner.node.coordonate.world();
		jobCoord.dimention = miner.node.coordonate.dimention;
		jobCoord.x = miner.node.coordonate.x;
		jobCoord.y = miner.node.coordonate.y - pipeLength;
		jobCoord.z = miner.node.coordonate.z;
		/*for(jobCoord.y = miner.node.coordonate.y - 1; jobCoord.y > 0; jobCoord.y--)
		{
			GhostElement ghost = Eln.ghostManager.getGhost(jobCoord);
			if(ghost == null || ghost.getObservatorCoordonate().equals(miner.node.coordonate) != true) {
				jobCoord.y++;
				break;
			}
		}*/
		//JobCoord at last pipe		
		
		/*int invFreeCnt = 0;
		for(int idx = AutoMinerContainer.StorageStartId;idx < AutoMinerContainer.StorageSize + AutoMinerContainer.StorageStartId;idx++){
			if(miner.inventory.getStackInSlot(idx) == null){
				invFreeCnt++;
			}
		}			
		Utils.println(" " + invFreeCnt);
		*/
		boolean jobFind = false;
		if(drill == null) {
			if(jobCoord.y != miner.node.coordonate.y) {
				ItemStack pipeStack = miner.inventory.getStackInSlot(AutoMinerContainer.MiningPipeSlotId);
				if(pipeStack == null || (pipeStack.stackSize != pipeStack.getMaxStackSize() && pipeStack.stackSize != miner.inventory.getInventoryStackLimit())) {
					jobFind = true;
					setJob(jobType.pipeRemove);
				}else{
					jobFind = true;
					setJob(jobType.full);
				}
			}
		}
		else if(!isStorageReady()){
			setJob(jobType.full);
			jobFind = true;
		}
		else if(pipe != null) {		
			if(jobCoord.y < miner.node.coordonate.y - 2) {
				int depth = (miner.node.coordonate.y - jobCoord.y);
				double miningRay = depth/10 + 0.1;
				miningRay = Math.min(miningRay, 3);
				if(depth < scannerRadius) scannerRadius = depth+1;
				miningRay = Math.min(miningRay, scannerRadius-2);
				for(jobCoord.z = miner.node.coordonate.z - scannerRadius; jobCoord.z <= miner.node.coordonate.z + scannerRadius; jobCoord.z++) {
					for(jobCoord.x = miner.node.coordonate.x - scannerRadius; jobCoord.x <= miner.node.coordonate.x + scannerRadius; jobCoord.x++) {
						double dx = jobCoord.x - miner.node.coordonate.x;
						double dy = 0;
						double dz = jobCoord.z - miner.node.coordonate.z;
						double distance = Math.sqrt(dx*dx+dy*dy+dz*dz)*(0.9 + 0.2*oreRand);
						Block block = jobCoord.world().getBlock(jobCoord.x, jobCoord.y, jobCoord.z);
						if(checkIsOre(jobCoord) || (distance > 0.1 && distance < miningRay && isMinable(block))) {
							jobFind = true;
							setJob(jobType.ore);
							break;
						}
					}
					if(jobFind == true) break;
				}
			}
				
			if(jobFind == false) {
				if(jobCoord.y < 3){
					jobFind = true;
					setJob(jobType.done);					
				}else{
					jobCoord.x = miner.node.coordonate.x;
					jobCoord.y--;
					jobCoord.z = miner.node.coordonate.z;
					
					Block block = jobCoord.world().getBlock(jobCoord.x, jobCoord.y, jobCoord.z);
					if(		block != Blocks.air 
							&& block != Blocks.flowing_water && block != Blocks.water
							&& block != Blocks.flowing_lava && block != Blocks.lava) {
						if(block != Blocks.obsidian && block != Blocks.bedrock) {
							jobFind = true;
							setJob(jobType.ore);
						}
						else{
							jobFind = true;
							setJob(jobType.done);					
						}
					}
					else {
						jobFind = true;
						setJob(jobType.pipeAdd);
					}
				}
			}
		}	
		if(jobFind == false) setJob(jobType.none);
		
		switch(job) {

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
			energyTarget = 0;
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
		Block block = coordonate.world().getBlock(coordonate.x, coordonate.y, coordonate.z);
		if(block instanceof BlockOre) return true;
		if(block instanceof OreBlock) return true;
		if(block instanceof BlockRedstoneOre) return true;
		
		return false;
	}

	public void onBreakElement() {
		destroyPipe(-1);
	}
	
	void destroyPipe(int jumpY) {
		dropPipe(jumpY);
		Eln.ghostManager.removeGhostAndBlockWithObserverAndNotUuid(miner.node.coordonate,miner.descriptor.getGhostGroupUuid());
		pipeLength = 0;
		miner.needPublish();
	}
	
	void dropPipe(int jumpY) {
		World world = miner.node.coordonate.world();
		Coordonate coord = new Coordonate(miner.node.coordonate);
/*
		for(coord.y = miner.node.coordonate.y - 1; coord.y > 0; coord.y--)
		{
			GhostElement ghost = Eln.ghostManager.getGhost(coord);
			if(coord.y != jumpY) {
				if(ghost == null || ghost.getObservatorCoordonate().equals(miner.node.coordonate) != true) {
					coord.y++;
					break;
				}
			}
			Utils.dropItem(Eln.miningPipeDescriptor.newItemStack(1), coord);
		}	
		*/
		for(coord.y = miner.node.coordonate.y - 1; coord.y >= miner.node.coordonate.y - pipeLength; coord.y--) {
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
