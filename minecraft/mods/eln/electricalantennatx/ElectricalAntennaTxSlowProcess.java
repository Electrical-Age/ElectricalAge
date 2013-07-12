package mods.eln.electricalantennatx;

import java.util.List;

import org.lwjgl.Sys;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import mods.eln.Eln;
import mods.eln.electricalantennarx.ElectricalAntennaRxElement;
import mods.eln.misc.Coordonate;
import mods.eln.node.Node;
import mods.eln.node.NodeManager;
import mods.eln.node.TransparentNode;
import mods.eln.sim.IProcess;

public class ElectricalAntennaTxSlowProcess implements IProcess{
	ElectricalAntennaTxElement element;
	
	public ElectricalAntennaTxSlowProcess(ElectricalAntennaTxElement element) {
		this.element = element;
	}
	
	double timeCounter = 0;
	final double periode = 0.2;
	
	@SuppressWarnings("null")
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		//if(element.rxCoord == null)
		World world = element.node.coordonate.world();

		if(timeCounter <= 0.0)
		{
			timeCounter = periode;
			int rangeMax = element.descriptor.rangeMax;
			Coordonate coord = new Coordonate(element.node.coordonate);
			
			int distance = 0;
			TransparentNode node = null;
			boolean find = false;
			//int a = 0,b = 0;
			do{
				coord.move(element.front);
				distance++;
				int blockId;
				if(element.placeBoot || element.rxCoord == null || coord.world().blockExists(coord.x, coord.y, coord.z))
				{
				//	a++;
					if((blockId = coord.getBlockId()) != 0 && blockId != Block.fire.blockID)
					{
						if(blockId == Eln.transparentNodeBlock.blockID 
							&& (node = (TransparentNode) NodeManager.instance.getNodeFromCoordonate(coord)) != null 
							&& (node.element instanceof ElectricalAntennaRxElement))
						{
							ElectricalAntennaRxElement rx = (ElectricalAntennaRxElement) node.element;
							if(rx.front == element.front.getInverse())
							{
								find = true;
							}
						}
	
						break;
					}
				}
				else
				{
				//	b++;
					Node unknowNode = NodeManager.instance.getNodeFromCoordonate(coord);
					if(node != null)
					{
						if(		unknowNode instanceof TransparentNode
								&& (((TransparentNode)unknowNode).element instanceof ElectricalAntennaRxElement))
						{
							node = (TransparentNode) unknowNode;
							ElectricalAntennaRxElement rx = (ElectricalAntennaRxElement) node.element;
							if(rx.front == element.front.getInverse())
							{
								find = true;
								
							}
								
						}
						break;	
					}
			
				}
			}while(distance < rangeMax);
			if(find == false)
			{
				element.txDisconnect();
				Coordonate coordCpy = new Coordonate(coord);
				coordCpy.move(element.front.getInverse());
				if(element.powerIn.getRpPower() > 50)
				{
					if(coordCpy.world().blockExists(coordCpy.x, coordCpy.y, coordCpy.z))
					{
						if(coordCpy.getBlockId() == 0)
						{
							coordCpy.world().setBlock(coordCpy.x, coordCpy.y, coordCpy.z, Block.fire.blockID);
						}
					}
				}
			}
			else
			{
				element.powerEfficency =  1 - (element.descriptor.electricalPowerRatioLostOffset  + element.descriptor.electricalPowerRatioLostPerBlock * distance);

				if(world.getWorldInfo().isRaining()) element.powerEfficency *= 0.707;
				if(world.getWorldInfo().isThundering()) element.powerEfficency *= 0.707;
				
				
				element.rxCoord = node.coordonate;
				element.rxElement = (ElectricalAntennaRxElement) node.element;
				


			}
			List list = world.getEntitiesWithinAABBExcludingEntity((Entity)null, Coordonate.getAxisAlignedBB(element.node.coordonate,coord));
			
			for(Object o : list)
			{
				Entity e = (Entity) o;
				e.setFire((int) (Math.pow(element.powerIn.getRpPower()/100.0,2)+ 0.5));
			}			

		}
		
		
		if(element.powerIn.getRpPower() > element.descriptor.electricalMaximalPower)
		{
			element.node.physicalSelfDestruction(2.0f);
		}
		if(element.powerIn.Uc > element.descriptor.electricalMaximalVoltage)
		{
			element.node.physicalSelfDestruction(2.0f);
		}
		
		element.placeBoot = false;
		timeCounter -= time;
	}
	

}
