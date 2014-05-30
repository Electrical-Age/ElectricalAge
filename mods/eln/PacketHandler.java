package mods.eln;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import org.lwjgl.Sys;

import com.jcraft.jogg.Packet;

import mods.eln.client.ClientKeyHandler;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import mods.eln.sound.SoundClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;

import cpw.mods.fml.common.FMLCommonHandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;

//public class PacketHandler implements IPacketHandler {

@Sharable
public class PacketHandler /*extends SimpleChannelInboundHandler<FMLProxyPacket> */{

	public PacketHandler() {
		//FMLCommonHandler.instance().bus().register(this);
		Eln.eventChannel.register(this);
	}

	public static final byte stuffInteractAId = 0;
	public static final byte stuffInteractBId = 1;
	public static final byte openWikiId = 2;
	public static final byte interactId = 3;

	@SubscribeEvent
	public void onServerPacket(ServerCustomPacketEvent event) {

		//Utils.println("onServerPacket");

		FMLProxyPacket packet = event.packet;
		DataInputStream stream = new DataInputStream(new ByteArrayInputStream(packet.payload().array()));
		NetworkManager manager = event.manager;
		EntityPlayer player = ((NetHandlerPlayServer) event.handler).playerEntity;

		packetRx(stream, manager, player);
	}

	@SubscribeEvent
	public void onClientPacket(ClientCustomPacketEvent event) {

		//Utils.println("onClientPacket");
		FMLProxyPacket packet = event.packet;
		DataInputStream stream = new DataInputStream(new ByteArrayInputStream(packet.payload().array()));
		NetworkManager manager = event.manager;
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;

		packetRx(stream, manager, player);
	}

	void packetRx(DataInputStream stream, NetworkManager manager,
			EntityPlayer player) {

		try {
			switch (stream.readByte()) {
			case Eln.packetNodeSerialized24bitPosition:
				packetNodeSerialized24bitPosition(stream);
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
			case Eln.packetPlaySound:
				packetPlaySound(stream, manager, player);
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	void packetPlaySound(DataInputStream stream, NetworkManager manager,
			EntityPlayer player) {
		EntityClientPlayerMP clientPlayer = (EntityClientPlayerMP) player;
		try {
			if (stream.readByte() != clientPlayer.dimension)
				return;

			SoundClient.play(
					clientPlayer.worldObj,
					stream.readInt() / 8.0,
					stream.readInt() / 8.0,
					stream.readInt() / 8.0,
					stream.readUTF(),
					stream.readFloat(),
					stream.readFloat(),
					stream.readFloat(),
					stream.readFloat());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	void packetOpenLocalGui(DataInputStream stream, NetworkManager manager,
			EntityPlayer player) {
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

	void packetForNode(DataInputStream stream, NetworkManager manager,
			EntityPlayer player) {
		try {
			Coordonate coordonate = new Coordonate(stream.readInt(),
					stream.readInt(), stream.readInt(), stream.readByte());

			NodeBase node = NodeManager.instance.getNodeFromCoordonate(coordonate);
			if (node != null && node.getInfo().getUuid().equals(stream.readUTF())) {
				node.networkUnserialize(stream, (EntityPlayerMP) player);
			} else {
				Utils.println("packetForNode node found");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void packetForClientNode(DataInputStream stream, NetworkManager manager,
			EntityPlayer player) {
		EntityClientPlayerMP clientPlayer = (EntityClientPlayerMP) player;
		int x = 0, y= 0, z= 0, dimention= 0;
		try {

			x = stream.readInt();
			y = stream.readInt();
			z = stream.readInt();
			dimention = stream.readByte();

			NodeBlockEntity node;
			if (clientPlayer.dimension == dimention
					&& (node = NodeBlockEntity.getEntity(x, y, z)) != null) {
				if (node.getInfo().getUuid().equals(stream.readUTF())) {
					node.serverPacketUnserialize(stream);
					if (0 != stream.available()) {
						Utils.println("0 != stream.available()");
						// while(true);
					}
				} else {
					Utils.println("Wrong node UUID warning");
					int dataSkipLength = stream.readByte();
					for (int idx = 0; idx < dataSkipLength; idx++) {
						stream.readByte();
					}
				}
			} else {
				Utils.println("No node found at " + x + " " + y + " " + z);
				/*stream.readShort();
				int dataSkipLength = stream.readByte();
				for (int idx = 0; idx < dataSkipLength; idx++) {
					stream.readByte();
				}*/
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void packetNodeSingleSerialized(DataInputStream stream,
			NetworkManager manager, EntityPlayer player) {
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
				if (node.getInfo().getUuid().equals(stream.readUTF())) {
					node.networkUnserialize(stream);
					if (0 != stream.available()) {
						Utils.println("0 != stream.available()");
						while (true)
							;
					}
				} else {
					Utils.println("Wrong node UUID warning");
					int dataSkipLength = stream.readByte();
					for (int idx = 0; idx < dataSkipLength; idx++) {
						stream.readByte();
					}
				}
			} else {
				Utils.println("No node found");
				/*stream.readShort();
				int dataSkipLength = stream.readByte();
				for (int idx = 0; idx < dataSkipLength; idx++) {
					stream.readByte();
				}*/
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void packetPlayerKey(DataInputStream stream, NetworkManager manager,
			EntityPlayer player) {
		EntityPlayerMP playerMP = (EntityPlayerMP) player;
		byte id;
		try {
			id = stream.readByte();
			boolean state = stream.readBoolean();
			if (state) {
				if (id == stuffInteractAId || id == stuffInteractBId) {
					{
						ItemStack itemStack = playerMP.getCurrentEquippedItem();
						if (itemStack != null) {
							if (Utils.hasTheInterface(itemStack.getItem(),
									IInteract.class)) {
								IInteract interactItem = ((IInteract) itemStack
										.getItem());
								interactItem.interact(playerMP, itemStack, id);
							}
						}
					}

					for (ItemStack itemStack : playerMP.inventory.armorInventory) {
						if (itemStack != null) {
							if (Utils.hasTheInterface(itemStack.getItem(),
									IInteract.class)) {
								IInteract interactItem = ((IInteract) itemStack
										.getItem());
								interactItem.interact(playerMP, itemStack, id);
							}
						}

					}
				}
			}
			if (id == interactId) {
				PlayerManager.PlayerMetadata metadata = Eln.playerManager
						.get(playerMP);
				metadata.setInteractEnable(state);

			}
			if (id == openWikiId) {

			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	void packetNodeSerialized24bitPosition(DataInputStream stream) {
		Utils.println("packetNodeSerialized24bitPosition OLD");
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
				* Utils.println(
				* "if(streamByteLeft-stream.available() != streamByteNbrToRead) error"
				* ); while(true); } } else {
				* Utils.println("Wrong node UUID warning"); int dataSkipLength
				* = stream.readByte(); for(int idx = 0;idx< dataSkipLength;idx++) {
				* stream.readByte(); } } } else {
				* Utils.println("No node found"); stream.readShort(); int
				* dataSkipLength = stream.readByte(); for(int idx = 0;idx<
				* dataSkipLength;idx++) { stream.readByte(); } } } } catch
				* (IOException e) { // TODO Auto-generated catch block
				* e.printStackTrace(); }
				*/
	}

}