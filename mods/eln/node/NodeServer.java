package mods.eln.node;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Type;
import cpw.mods.fml.relauncher.Side;

public class NodeServer {

	public NodeServer()
	{
		FMLCommonHandler.instance().bus().register(this);

	}

	public void init()
	{
		//	NodeBlockEntity.nodeAddedList.clear();
	}

	public void stop()
	{
		//	NodeBlockEntity.nodeAddedList.clear();
	}

	public int counter = 0;

	@SubscribeEvent
	public void tick(ServerTickEvent event) {
		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

		if (server != null)
		{

			for (NodeBase node : NodeManager.instance.getNodeList())
			{
				if (node.getNeedPublish())
				{
					node.publishToAllPlayer();
				}
			}

			for (Object obj : server.getConfigurationManager().playerEntityList)
			{
				EntityPlayerMP player = (EntityPlayerMP) obj;

				NodeBase openContainerNode = null;
				INodeContainer container = null;
				if (player.openContainer != null && player.openContainer instanceof INodeContainer)
				{
					container = ((INodeContainer) player.openContainer);
					openContainerNode = container.getNode();
				}

				for (NodeBase node : NodeManager.instance.getNodeList())
				{

					if (node == openContainerNode)
					{
						if ((counter % (1 + container.getRefreshRateDivider())) == 0)
							node.publishToPlayer(player);
					}
				}
			}

			counter++;
		}

	}


}
