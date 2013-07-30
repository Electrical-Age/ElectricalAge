package mods.eln;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import org.lwjgl.Sys;

import mods.eln.client.ClientKeyHandler;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;

import cpw.mods.fml.common.FMLCommonHandler;

import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.Player;

public class PacketHandler implements IPacketHandler {

	// ByteBuffer stream = ByteBuffer.allocate(100000);

	public static final byte stuffInteractAId = 0;
	public static final byte stuffInteractBId = 1;
	public static final byte interactEnableId = 2;
	public static final byte interactDisableId = 3;

	@Override
	public void onPacketData(INetworkManager manager,
			Packet250CustomPayload packet, Player player) {
		DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
				packet.data));

		if (packet.channel.equals(Eln.channelName)) {
			// stream.position(0);
			// stream.put(packet.data);
			// stream.position(0);

			try {
				switch (stream.readByte()) {
				case Eln.packetNodeSerialized24bitPosition:
					packetNodeSerialized24bitPosition(stream, manager, player);
					break;
				case Eln.packetPlayerKey:
					packetPlayerKey(stream, manager, player);
					break;
				case Eln.packetNodeSingleSerialized:
					packetNodeSingleSerialized(stream, manager, player);
					break;
				case Eln.packetPublishForNode:
					packetForNode(stream, manager, player);
					break;
				case Eln.packetForClientNode:
					packetForClientNode(stream, manager, player);
					break;
				case Eln.packetOpenLocalGui:
					packetOpenLocalGui(stream, manager, player);
					break;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	void packetOpenLocalGui(DataInputStream stream, INetworkManager manager,
			Player player) {
		EntityClientPlayerMP clientPlayer = (EntityClientPlayerMP) player;
		try {
			clientPlayer.openGui(Eln.instance, stream.readInt(),
					clientPlayer.worldObj, stream.readInt(), stream.readInt(),
					stream.readInt());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// entityPlayer.openGui( Eln.instance, side.getInt(),
		// coordonate.world(),coordonate.x , coordonate.y, coordonate.z);
	}

	void packetForNode(DataInputStream stream, INetworkManager manager,
			Player player) {
		try {
			Coordonate coordonate = new Coordonate(stream.readInt(),
					stream.readInt(), stream.readInt(), stream.readByte());

			NodeBase node = NodeManager.instance.getNodeFromCoordonate(coordonate);
			if (node != null && node.getBlockId() == stream.readShort()) {
				node.networkUnserialize(stream, player);
			} else {
				System.out.println("packetForNode node found");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void packetForClientNode(DataInputStream stream, INetworkManager manager,
			Player player) {
		try {
			EntityClientPlayerMP clientPlayer = (EntityClientPlayerMP) player;
			int x, y, z, dimention;
			x = stream.readInt();
			y = stream.readInt();
			z = stream.readInt();
			dimention = stream.readByte();

			NodeBlockEntity node;
			if (clientPlayer.dimension == dimention
					&& (node = NodeBlockEntity.getEntity(x, y, z)) != null) {
				if (node.getBlockId() == stream.readShort()) {
					node.serverPacketUnserialize(stream);
					if (0 != stream.available()) {
						System.out.println("0 != stream.available()");
						// while(true);
					}
				} else {
					System.out.println("Wrong node UUID warning");
					int dataSkipLength = stream.readByte();
					for (int idx = 0; idx < dataSkipLength; idx++) {
						stream.readByte();
					}
				}
			} else {
				System.out.println("No node found");
				stream.readShort();
				int dataSkipLength = stream.readByte();
				for (int idx = 0; idx < dataSkipLength; idx++) {
					stream.readByte();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void packetNodeSingleSerialized(DataInputStream stream,
			INetworkManager manager, Player player) {
		try {
			EntityClientPlayerMP clientPlayer = (EntityClientPlayerMP) player;
			int x, y, z, dimention;
			x = stream.readInt();
			y = stream.readInt();
			z = stream.readInt();
			dimention = stream.readByte();

			NodeBlockEntity node;
			if (clientPlayer.dimension == dimention
					&& (node = NodeBlockEntity.getEntity(x, y, z)) != null) {
				if (node.getBlockId() == stream.readShort()) {
					node.networkUnserialize(stream);
					if (0 != stream.available()) {
						System.out.println("0 != stream.available()");
						while (true)
							;
					}
				} else {
					System.out.println("Wrong node UUID warning");
					int dataSkipLength = stream.readByte();
					for (int idx = 0; idx < dataSkipLength; idx++) {
						stream.readByte();
					}
				}
			} else {
				System.out.println("No node found");
				stream.readShort();
				int dataSkipLength = stream.readByte();
				for (int idx = 0; idx < dataSkipLength; idx++) {
					stream.readByte();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void packetPlayerKey(DataInputStream stream, INetworkManager manager,
			Player player) {
		EntityPlayerMP playerMP = (EntityPlayerMP) player;
		byte value;
		try {
			value = stream.readByte();
			if (value == stuffInteractAId || value == stuffInteractBId) {
				{
					ItemStack itemStack = playerMP.getCurrentEquippedItem();
					if (itemStack != null) {
						if (Utils.hasTheInterface(itemStack.getItem(),
								IInteract.class)) {
							IInteract interactItem = ((IInteract) itemStack
									.getItem());
							interactItem.interact(playerMP, itemStack, value);
						}
					}
				}

				for (ItemStack itemStack : playerMP.inventory.armorInventory) {
					if (itemStack != null) {
						if (Utils.hasTheInterface(itemStack.getItem(),
								IInteract.class)) {
							IInteract interactItem = ((IInteract) itemStack
									.getItem());
							interactItem.interact(playerMP, itemStack, value);
						}
					}

				}
			}

			if (value == interactEnableId) {
				PlayerManager.PlayerMetadata metadata = Eln.playerManager
						.get(playerMP);
				metadata.setInteractEnable(true);
			}
			if (value == interactDisableId) {
				PlayerManager.PlayerMetadata metadata = Eln.playerManager
						.get(playerMP);
				metadata.setInteractEnable(false);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	void packetNodeSerialized24bitPosition(DataInputStream stream,
			INetworkManager manager, Player player) {
		System.out.println("packetNodeSerialized24bitPosition OLD");
		while (true)
			;/*
			 * try{ EntityClientPlayerMP clientPlayer = (EntityClientPlayerMP)
			 * player; int x,y,z,dimention; x = stream.readInt(); y =
			 * stream.readInt(); z = stream.readInt(); dimention =
			 * stream.readByte(); Minecraft minecraft =
			 * Minecraft.getMinecraft(); if(clientPlayer.dimension != dimention)
			 * return;
			 * 
			 * while(stream.available() != 0) { int nx,ny,nz; nx = x +
			 * stream.readByte(); ny = y + stream.readByte(); nz = z +
			 * stream.readByte();
			 * 
			 * NodeBlockEntity node; if((node = NodeBlockEntity.getEntity(nx,
			 * ny, nz)) != null) { if(node.getBlockId() == stream.readShort()) {
			 * int streamByteNbrToRead = stream.readByte(); int streamByteLeft =
			 * stream.available(); node.networkUnserialize(stream);
			 * if(streamByteLeft-stream.available() != streamByteNbrToRead) {
			 * System.out.println(
			 * "if(streamByteLeft-stream.available() != streamByteNbrToRead) error"
			 * ); while(true); } } else {
			 * System.out.println("Wrong node UUID warning"); int dataSkipLength
			 * = stream.readByte(); for(int idx = 0;idx< dataSkipLength;idx++) {
			 * stream.readByte(); } } } else {
			 * System.out.println("No node found"); stream.readShort(); int
			 * dataSkipLength = stream.readByte(); for(int idx = 0;idx<
			 * dataSkipLength;idx++) { stream.readByte(); } } } } catch
			 * (IOException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
	}

}