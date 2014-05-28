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
	}
	// ByteBuffer stream = ByteBuffer.allocate(100000);

	public static final byte stuffInteractAId = 0;
	public static final byte stuffInteractBId = 1;
	public static final byte interactEnableId = 2;
	public static final byte interactDisableId = 3;

	/*@Override
	public void onPacketData(INetworkManager manager,
			Packet250CustomPayload packet, Player player) {*/

	//protected void channelRead0(ChannelHandlerContext ctx, FMLProxyPacket packet) throws Exception {
		/*if (packet.channel().equals(Eln.channelName)) {
			ByteBuf payload = packet.payload();
			if (payload.readableBytes() == 4) {
				int number = payload.readInt();
				System.out.println("number = " + number);
			}*/
	
	//1.7.2
	@SubscribeEvent
	public void onServerPacket(ServerCustomPacketEvent event) {
		/*NetHandlerPlayServer a = null;
		event.handler.
		EntityPlayerMP player = ((NetHandlerPlayServer)event.handler).field_147369_b;
		ByteBufInputStream bbis = new ByteBufInputStream(event.packet.payload());		
		*/
		
	}
	
	@SubscribeEvent
	public void onClientPacket(ClientCustomPacketEvent event) {
		/*NetHandlerPlayServer a = null;
		event.handler.
		EntityPlayerMP player = ((NetHandlerPlayServer)event.handler).field_147369_b;
		ByteBufInputStream bbis = new ByteBufInputStream(event.packet.payload());		
		*/
	}
		
	

	void packetRx(DataInputStream stream, NetworkManager manager,
			EntityPlayer player) {

		/*if (packet.channel().equals(Eln.channelName)) */{
		/*	ByteBuf payload = packet.payload();
			DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
					payload.array()));

			// stream.position(0);
			// stream.put(packet.data);
			// stream.position(0);
			ctx.channel().
			NetworkManager manager = packet.getOrigin();
			EntityPlayer player = manager.getNetHandler().;*/
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
			if (node != null && node.getBlockId() == stream.readShort()) {
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
				if (node.getBlockId() == stream.readShort()) {
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
				stream.readShort();
				/*int dataSkipLength = stream.readByte();
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