package mods.eln.item.electricalinterface;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.Player;
import mods.eln.misc.Utils;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.sim.IProcess;

public class ItemEnergyInventoryProcess implements IProcess {

	class Element
	{
		public Element(ItemStack stack, IItemEnergyBattery i) {
			this.stack = stack;
			this.i = i;
			p = i.getPriority(stack);
		}
		public ItemStack stack;
		public IItemEnergyBattery i;
		public int p;
	}
	
	@Override
	public void process(double time) {
       MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
	        
       ArrayList<Element> list = new ArrayList<Element>();
	   for (Object obj :  server.getConfigurationManager().playerEntityList)
	   {
		   	EntityPlayerMP player = (EntityPlayerMP) obj;	
		   	list.clear();
		   	for(ItemStack stack : player.inventory.mainInventory){
		   		Object o = Utils.getItemObject(stack);
		   		if(o instanceof IItemEnergyBattery){
		   			list.add(new Element(stack, (IItemEnergyBattery) o));
		   		}
		   	}
		   boolean rememberDst = false;
		   double rememberDstEToDstMax = 0;
		   while(true){
			   Element src = getMax(list);
			   if(src == null) break;
			   double eFromSrc = Math.min(src.i.getEnergy(src.stack),src.i.getDischagePower(src.stack) * time);
			   double eStart = eFromSrc;
			   
			   boolean done = false;
			   while(eFromSrc != 0){
				   Element dst = getMin(list);
				   if(dst.p == src.p){
					   done = true;
					   break;
				   }
				   
				   double eToDstMax;
				   if(rememberDst){
					   eToDstMax = rememberDstEToDstMax;
					   rememberDst = false;
				   }
				   else{
					   eToDstMax = Math.min(dst.i.getEnergyMax(dst.stack)-dst.i.getEnergy(dst.stack), dst.i.getChargePower(dst.stack)*time);
				   }

				   double eToDst = Math.min(eFromSrc,eToDstMax);
				   eFromSrc -= eToDst;
				   dst.i.setEnergy(dst.stack, dst.i.getEnergy(dst.stack) + eToDst);
				   
				   if(eToDstMax == eToDst){
					   list.remove(dst);
				   }
				   else{
					   rememberDst = true;
					   rememberDstEToDstMax = eToDst;
				   }
			   }
			   
			   
			   src.i.setEnergy(src.stack, src.i.getEnergy(src.stack) - (eStart - eFromSrc));
			   
			   if(done) break;
			   
			   list.remove(src);
			   
			   if(list.size() < 2) break;
		   
		   }
	   	}
	}
	
	Element getElement(ArrayList<Element> list,int priority)
	{

		for(Element e : list){
			if(priority == e.p){
				return e;
			}
		}
		return null;
	}
	
	Element getMin(ArrayList<Element> list)
	{
		Element find = null;
		for(Element e : list){
			if(find == null || find.p > e.p){
				find = e;
			}
		}
		return find;
	}
	
	
	Element getMax(ArrayList<Element> list)
	{
		Element find = null;
		for(Element e : list){
			if(find == null || find.p < e.p){
				find = e;
			}
		}
		return find;
	}
}


