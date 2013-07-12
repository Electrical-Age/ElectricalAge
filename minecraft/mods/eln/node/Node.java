package mods.eln.node;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUCubeMask;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public abstract class Node implements INBTTReady , INode {
	//NodeBlockEntity entity;
	
	public static final double networkSerializeUFactor = 10.0;
	public static final double networkSerializeIFactor = 100.0;
	public static final double networkSerializeTFactor = 10.0;
	
	
	public short[] neighborBlock = new short[6];
	
	public static int teststatic;
	
	public Coordonate coordonate;
	
	public ArrayList<NodeConnection> nodeConnectionList = new ArrayList<NodeConnection>(4);	


	private boolean initialized = false;
	
	private boolean isAdded = false;
	
	private boolean needPublish = false;
	
	//public static boolean canBePlacedOn(ItemStack itemStack,Direction side)

	public int getBlockMetadata()
	{
		return 0;
	}
	public void networkUnserialize(DataInputStream stream,Player player)
	{
		
	}
	
	public void notifyNeighbor()
	{
	//	coordonate.world().setBlockMetadataWithNotify(coordonate.x, coordonate.y, coordonate.z,0,0x02);
		coordonate.world().notifyBlockChange(coordonate.x, coordonate.y, coordonate.z, getBlockId());
	}
	
	
	public IInventory getInventory()
	{
		return null;
	}
	


	
	public LRDUCubeMask lrduCubeMask = new LRDUCubeMask();
	
	public void neighborBlockRead()
	{
		int[] vector = new int[3];
		World world = coordonate.world();
		for(Direction direction : Direction.values())
		{
			vector[0] = coordonate.x;
			vector[1] = coordonate.y;
			vector[2] = coordonate.z;
			
			direction.applyTo(vector, 1);
					
			neighborBlock[direction.getInt()] = (short)world.getBlockId(vector[0], vector[1], vector[2]);
		}
	}
	
	private int lastLight = 0;
	public void setLightValue(int light)
	{
		if(light>15)light = 15;
		if(light<0)light = 0;
		if(lastLight != light)
		{
			lastLight = light;
			coordonate.world().updateLightByType(EnumSkyBlock.Block,coordonate.x, coordonate.y, coordonate.z);
			setNeedPublish(true);
		}
		
	}
	
	public int getLightValue()
	{
		return lastLight;
	}
	
	@Override
	public boolean hasGui(Direction side) {
		// TODO Auto-generated method stub
		return false;
	}
	/*
	public IInventory getInventory(Direction side)
	{
		return null;
	}*/
	
	public void onNeighborBlockChange()
	{
		//super.onNeighborBlockChange();
		neighborBlockRead();
		reconnect();
	}
	
	public boolean isBlockWrappable(Direction direction)
	{
		return isBlockWrappable(neighborBlock[direction.getInt()]); 
	}	
	public static boolean isBlockWrappable(int blockId)
	{
		if(blockId == 0) return true;
		if(blockId == Eln.sixNodeBlock.blockID) return true;
		if(blockId == Eln.ghostBlock.blockID) return true;
		if(blockId == Block.torchWood.blockID) return true;
		if(blockId == Block.torchRedstoneIdle.blockID) return true;
		if(blockId == Block.torchRedstoneActive.blockID) return true;
		if(blockId == Block.redstoneWire.blockID) return true;
			
		return false;
	}
	
	
	public Node()
	{
		coordonate = new Coordonate();
		//if(getInventaireSize() != 0)
		{
		/*	this.inventaire = new ItemStack[this.getInventaireSize()];
			for(int idx = 0;idx<inventaire.length;idx++) inventaire[idx] = null;*/
		}
	}

/*	
	public float physicalSelfDestructionExplosionStrength()
	{
		return 0f;
	}*/
	boolean destructed = false;
	public void physicalSelfDestruction(float explosionStrength)
	{
		if(destructed == true) return;
		destructed = true;
		disconnect();
		coordonate.world().setBlock(coordonate.x, coordonate.y, coordonate.z, 0);
		NodeManager.instance.removeNode(this);
		if(explosionStrength != 0)
		{
			coordonate.world().createExplosion((Entity)null, coordonate.x,coordonate.y,coordonate.z, explosionStrength, true);
		}
	}
	
	public void onBlockPlacedBy(Coordonate coordonate, Direction front, EntityLivingBase entityLiving, ItemStack itemStack)
	{
		//this.entity = entity;
		this.coordonate = coordonate;
		neighborBlockRead();
		NodeManager.instance.addNode(this);
		
		initializeFromThat(front, entityLiving, itemStack);

		System.out.println("Node::constructor( meta = " + itemStack.getItemDamage() + ")");
	}
	
	
	public Node getNeighbor(Direction direction)
	{
		int[] position = new int[3];
		position[0] = coordonate.x;
		position[1] = coordonate.y;
		position[2] = coordonate.z;
		direction.applyTo(position, 1);
 		Coordonate nodeCoordonate = new Coordonate(position[0], position[1], position[2],coordonate.dimention); 
		return NodeManager.instance.getNodeFromCoordonate(nodeCoordonate);	
	}
	
	
	public void onBreakBlock()
	{
		dropInventory(getInventory());
		disconnect();
		NodeManager.instance.removeNode(this);
		System.out.println("Node::onBreakBlock()");
	}
	
	public  boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,float vx,float vy,float vz)
	{
    	if(!entityPlayer.worldObj.isRemote)
    	{
    		if(entityPlayer.getCurrentEquippedItem() != null)
			{
		    	if(Eln.multiMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
		    	{ 
		    		entityPlayer.addChatMessage(multiMeterString(side));	
		    		return true;
		    	}
		    	if(Eln.thermoMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
		    	{ 
		    		entityPlayer.addChatMessage(thermoMeterString(side));	
		    		return true;
		    	}
		    	if(Eln.allMeterElement.checkSameItemStack(entityPlayer.getCurrentEquippedItem()))
		    	{
		    		entityPlayer.addChatMessage(multiMeterString(side) + thermoMeterString(side));	
		    		return true;
		    	}    			
    			
	    	}
    	}
    	if(hasGui(side))
    	{
			entityPlayer.openGui( Eln.instance, side.getInt(), coordonate.world(),coordonate.x , coordonate.y, coordonate.z);
			return true;
    	}
    		
    	return false;
	}

	public void reconnect()
	{
		disconnect();
		connect();
	}
	
	
	
	public static void tryConnectTwoNode(Node nodeA,Direction directionA,LRDU lrduA,Node nodeB,Direction directionB,LRDU lrduB)
	{
		if(compareConnectionMask(nodeA.getSideConnectionMask(directionA,lrduA) , nodeB.getSideConnectionMask(directionB, lrduB)))
		{
			ElectricalConnection eCon = null;
			ThermalConnection tCon = null;
			
			nodeA.lrduCubeMask.set(directionA,lrduA,true);
			nodeB.lrduCubeMask.set(directionB,lrduB,true);
			
			nodeA.newConnectionAt(directionA,lrduA);
			nodeB.newConnectionAt(directionB,lrduB);
			
			ElectricalLoad eLoad;
			if((eLoad = nodeA.getElectricalLoad(directionA,lrduA)) != null)
			{
				
				ElectricalLoad otherELoad = nodeB.getElectricalLoad(directionB,lrduB);
				if(otherELoad != null) 
				{
					eCon = new ElectricalConnection(eLoad,otherELoad);
					
					Eln.simulator.addElectricalConnection(eCon);
				}
			}
			ThermalLoad tLoad;
			if((tLoad = nodeA.getThermalLoad(directionA,lrduA)) != null)
			{
				
				ThermalLoad otherTLoad = nodeB.getThermalLoad(directionB,lrduB);
				if(otherTLoad != null)
				{					
					tCon = new ThermalConnection(tLoad,otherTLoad);
					
					Eln.simulator.addThermalConnection(tCon);
				}
				
			}
			NodeConnection nodeConnection = new NodeConnection(nodeA,directionA,lrduA,nodeB,directionB,lrduB,eCon,tCon);

			nodeA.nodeConnectionList.add(nodeConnection);
			nodeB.nodeConnectionList.add(nodeConnection);
					
			nodeA.setNeedPublish(true);
			nodeB.setNeedPublish(true);
		}
	}
		
	public void checkCanStay(boolean onCreate)
	{
		
	}
	
	
	public void connectJob()
	{
		//EXTERNAL OTHERS SIXNODE
		{
			int[] emptyBlockCoord = new int[3];
			int[] otherBlockCoord = new int[3];
			for(Direction direction : Direction.values())
			{
				if(isBlockWrappable(direction))
				{
					emptyBlockCoord[0] = coordonate.x;emptyBlockCoord[1] = coordonate.y;emptyBlockCoord[2] = coordonate.z;
					direction.applyTo(emptyBlockCoord, 1);
					for(LRDU lrdu : LRDU.values())
					{
						Direction elementSide = direction.applyLRDU(lrdu);
						otherBlockCoord[0] = emptyBlockCoord[0];otherBlockCoord[1] = emptyBlockCoord[1];otherBlockCoord[2] = emptyBlockCoord[2];
						elementSide.applyTo(otherBlockCoord, 1);
						Node otherNode = NodeManager.instance.getNodeFromCoordonate(new Coordonate(otherBlockCoord[0],otherBlockCoord[1],otherBlockCoord[2],coordonate.dimention));
						if(otherNode == null) continue;
						Direction otherDirection = elementSide.getInverse();
						LRDU otherLRDU = otherDirection.getLRDUGoingTo(direction).inverse();
						if(this instanceof SixNode || otherNode instanceof SixNode)
						{
							tryConnectTwoNode(this,direction,lrdu,otherNode,otherDirection,otherLRDU);
						}
					}
				}
			}
		}
    	
    	{
    		for(Direction dir : Direction.values()) 
			{
    			Node otherNode = getNeighbor(dir);
				if(otherNode != null  && otherNode.isAdded)
				{
		    		for(LRDU lrdu : LRDU.values()) 
					{
		    			tryConnectTwoNode(this,dir,lrdu,otherNode,dir.getInverse(),lrdu.inverseIfLR());	    			
					}
				}
    			
			}
		}
		
	
		
	}
	public void disconnectJob()
	{


		
		for(NodeConnection c : nodeConnectionList)
		{
		
			if(c.N1 != this)
			{
				c.N1.nodeConnectionList.remove(c);
				c.N1.setNeedPublish(true);
				c.N1.lrduCubeMask.set(c.dir1,c.lrdu1,false);
			}
			else
			{
				c.N2.nodeConnectionList.remove(c);
				c.N2.setNeedPublish(true);
				c.N2.lrduCubeMask.set(c.dir2,c.lrdu2,false);
			}
			c.destroy();
		}

		lrduCubeMask.clear();
		
    	nodeConnectionList.clear();		
	}
	
	public static boolean compareConnectionMask(int mask1,int mask2)
	{
		if(((mask1 & 0xFFFF) & (mask2 & 0xFFFF)) == 0) return false;
		if(((mask1 & maskColorCareData) & (mask2 & maskColorCareData)) == 0) return true;
		if((mask1 & maskColorData) == (mask2 & maskColorData)) return true;
		return false;
	}
	
	public void externalDisconnect(Direction side,LRDU lrdu){}
	public void newConnectionAt(Direction side,LRDU lrdu){}
	public void connectInit()
	{
    	lrduCubeMask.clear();		
    	nodeConnectionList.clear();	
	}
	
	public void connect()
	{

    	if(isAdded)
		{
    		disconnect();
		}
    	


    	connectInit();   	
    	connectJob();

    	

		isAdded = true;   
		
		setNeedPublish(true);
		
	}

	public void disconnect()
	{
		if(! isAdded)
		{
			System.out.println("Node destroy error already destroy");
			return;			
		}
		
		disconnectJob();
    	

		isAdded = false;
	}
	
	public boolean nodeAutoSave()
	{
		return true;
	}

    public void readFromNBT(NBTTagCompound nbt,String str)
    {
    	
        coordonate.readFromNBT(nbt, str + "c");		
        
        IInventory inv = getInventory();
        if(inv != null)
        {
        	Utils.readFromNBT(nbt, str + "inv", inv);
        }
        
        lastLight = nbt.getByte("lastLight");
        
      /*  {
            NBTTagList var2 = nbt.getTagList("Items");
            

            for (int var3 = 0; var3 < var2.tagCount(); ++var3)
            {
                NBTTagCompound var4 = (NBTTagCompound)var2.tagAt(var3);
                int var5 = var4.getByte("Slot") & 255;

                if (var5 >= 0 && var5 < this.inventaire.length)
                {
                    this.inventaire[var5] = ItemStack.loadItemStackFromNBT(var4);
                }
            }
        }*/
        
		for(int idx = 0;idx<6;idx++)
		{
			neighborBlock[idx] =  nbt.getShort(str + "NBID"+idx);
		}

		initialized = true;
    }

    
    

    public void writeToNBT(NBTTagCompound nbt,String str)
    {
        
        coordonate.writeToNBT(nbt, str + "c");
        IInventory inv = getInventory();
        if(inv != null)
        {
        	Utils.writeToNBT(nbt, str + "inv", inv);
        }
        nbt.setByte("lastLight",(byte) lastLight);
        
        int idx;
        /*
        for(idx = 0;idx<getInventaireSize();idx++)
        {
        	getInventaire()[idx].writeToNBT(par1NBTTagCompound);
        }*/
      /*  {
            NBTTagList var2 = new NBTTagList();

            for (int var3 = 0; var3 < this.getInventaire().length; ++var3)
            {
                if (this.getInventaire()[var3] != null)
                {
                    NBTTagCompound var4 = new NBTTagCompound();
                    var4.setByte("Slot", (byte)var3);
                    this.getInventaire()[var3].writeToNBT(var4);
                    var2.appendTag(var4);
                }
            }

            nbt.setTag("Items", var2);
        }*/
        
        
        for(idx = 0;idx<6;idx++)
        {
			nbt.setShort(str + "NBID"+idx,neighborBlock[idx]);
        }
        

    }
	
    public String multiMeterString(Direction side)
    {
    	return "";
    }
    
    
    
    public void setNeedPublish(boolean needPublish)
    {
    	this.needPublish = needPublish;
    }
    public boolean getNeedPublish()
    {
    	return needPublish;
    }
    
    private boolean isINodeProcess(IProcess process)
    {
    	for(Class c : process.getClass().getInterfaces())
    	{
    		if(c == INBTTReady.class) return true;
    	}
    	return false;
    }
    
    

    boolean needNotify = false;
    boolean oldSendedRedstone = false;
    @Override
    public void networkSerialize(DataOutputStream stream) {
    	// TODO Auto-generated method stub
    	
    	try {
    		boolean redstone = canConnectRedstone();
			stream.writeByte(lastLight | (redstone ? 0x10 : 0x00));
			if(redstone != oldSendedRedstone)
				needNotify = true;
			oldSendedRedstone = redstone;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void preparePacketForClient(DataOutputStream stream)
    {
    	try {
    		stream.writeByte(Eln.packetForClientNode);
    		 		
			stream.writeInt(coordonate.x);
	    	stream.writeInt(coordonate.y);
	    	stream.writeInt(coordonate.z);
	    	
	    	stream.writeByte(coordonate.dimention);
	    	
	    	stream.writeShort(getBlockId());
	    		    	
	    	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	    	
    }
	
    public void sendPacketToClient(ByteArrayOutputStream bos,Player player)
    {
    	Utils.sendPacketToClient(bos,player);
    } 
    
    public void sendPacketToAllClient(ByteArrayOutputStream bos)
    {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();       

	    for (Object obj :  server.getConfigurationManager().playerEntityList)
	    {
	    	EntityPlayerMP player = (EntityPlayerMP) obj;
	        WorldServer worldServer = (WorldServer) MinecraftServer.getServer().worldServerForDimension(player.dimension);
	    	PlayerManager playerManager = worldServer.getPlayerManager(); 
	    	if(player.dimension != this.coordonate.dimention) continue;
	    	if(! playerManager.isPlayerWatchingChunk(player, coordonate.x/16, coordonate.z/16)) continue;
	    	
		    Packet250CustomPayload packet = new Packet250CustomPayload();
	        packet.channel = Eln.channelName;
	        packet.data = bos.toByteArray();
	        packet.length = bos.size();
	        	    	
	    	PacketDispatcher.sendPacketToPlayer(packet,(Player) player);	
	    }   	
    }
    
    
    
    public Packet getPacketNodeSingleSerialized()
    {	
    	
  
    	ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);   	

        try {
        
	        stream.writeByte(Eln.packetNodeSingleSerialized);
	        
	        stream.writeInt(coordonate.x);
	        stream.writeInt(coordonate.y);
	        stream.writeInt(coordonate.z);
	        stream.writeByte(coordonate.dimention);
	        
	       
	        stream.writeShort(getBlockId());
		
	        
	        networkSerialize(stream);

	        Packet250CustomPayload packet = new Packet250CustomPayload();
	        packet.channel = Eln.channelName;
	        packet.data = bos.toByteArray();
	        packet.length = bos.size();  
	    	return packet;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
        return null;
    }
    

    public void publishToAllPlayer()
    {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();       

	    for (Object obj :  server.getConfigurationManager().playerEntityList)
	    {
	    	EntityPlayerMP player = (EntityPlayerMP) obj;
	        WorldServer worldServer = (WorldServer) MinecraftServer.getServer().worldServerForDimension(player.dimension);
	    	PlayerManager playerManager = worldServer.getPlayerManager(); 
	    	if(player.dimension != this.coordonate.dimention) continue;
	    	if(! playerManager.isPlayerWatchingChunk(player, coordonate.x/16, coordonate.z/16)) continue;
	    	
	    	PacketDispatcher.sendPacketToPlayer(getPacketNodeSingleSerialized(),(Player)player);
	    }
	    if(needNotify)
	    {
	    	needNotify = false;
	    	notifyNeighbor();
	    }
	    needPublish = false;
    }
    public void publishToPlayer(Player player)
    {
    	PacketDispatcher.sendPacketToPlayer(getPacketNodeSingleSerialized(),(Player)player);
    }
    
    
    public NodeBlockEntity getEntity()
    {
    	return (NodeBlockEntity) coordonate.world().getBlockTileEntity(coordonate.x, coordonate.y, coordonate.z);
    }
    
    public  void dropItem(ItemStack itemStack)
    {
    	if(itemStack == null) return;
        if (coordonate.world().getGameRules().getGameRuleBooleanValue("doTileDrops"))
        {
            float var6 = 0.7F;
            double var7 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
            double var9 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
            double var11 = (double)(coordonate.world().rand.nextFloat() * var6) + (double)(1.0F - var6) * 0.5D;
            EntityItem var13 = new EntityItem(coordonate.world(), (double)coordonate.x + var7, (double)coordonate.y + var9, (double)coordonate.z + var11, itemStack);
            var13.delayBeforeCanPickup = 10;
            coordonate.world().spawnEntityInWorld(var13);
        }
    }
    
    public void dropInventory(IInventory inventory) {
		if(inventory == null) return;
		for(int idx = 0;idx < inventory.getSizeInventory();idx++)
		{
			dropItem(inventory.getStackInSlot(idx));
		}
	}
	int isProvidingWeakPower(Direction side) {
		// TODO Auto-generated method stub
		return 0;
	}
	public boolean canConnectRedstone()
	{
		return false;
	}
	
	
	

}
