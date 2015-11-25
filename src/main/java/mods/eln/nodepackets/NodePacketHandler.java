package mods.eln.nodepackets;

import java.util.Map;

import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.nbt.NbtBatteryProcess;
import mods.eln.transparentnode.battery.BatteryElement;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class NodePacketHandler implements IMessageHandler<NodePacket, TransparentNodeReturn>{

	public static NBTTagCompound batteryData = new NBTTagCompound();
	
	@Override
	public TransparentNodeReturn onMessage(NodePacket message, MessageContext ctx) {
		Coordonate c = message.coord;
		NodeBase node = NodeManager.instance.getNodeFromCoordonate(c);
		TransparentNode tnode = (TransparentNode) node;
		TransparentNodeElement tNodeElement = tnode.element;
		Map<String, String> stringMap = tNodeElement.getWaila();
		return new TransparentNodeReturn(stringMap, c);
		
		
	}

}
