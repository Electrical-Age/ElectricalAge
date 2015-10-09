package mods.eln.node.transparent;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import mods.eln.Eln;
import mods.eln.ghost.GhostObserver;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sound.IPlayer;
import mods.eln.sound.SoundCommand;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidHandler;

public abstract class TransparentNodeElement implements  GhostObserver,IPlayer{

	public ArrayList<IProcess> slowProcessList  = new ArrayList<IProcess>(4);

	public ArrayList<IProcess> electricalProcessList = new ArrayList<IProcess>(4);
	public ArrayList<Component> electricalComponentList = new ArrayList<Component>(4);
	public ArrayList<State> electricalLoadList = new ArrayList<State>(4);
	
	public ArrayList<IProcess> thermalFastProcessList = new ArrayList<IProcess>(4);
	public ArrayList<ThermalConnection> thermalConnectionList = new ArrayList<ThermalConnection>(4);
	public ArrayList<NbtThermalLoad> thermalLoadList = new ArrayList<NbtThermalLoad>(4);
	
	
	public static final byte unserializeGroundedId = -127;
	public static final byte unserializeNulldId = -128;
	TransparentNodeDescriptor transparentNodeDescriptor;

	protected void serialiseItemStack(DataOutputStream stream,ItemStack stack) throws IOException
	{
		Utils.serialiseItemStack(stream,stack);
	}

	public void connectJob()
	{
		Eln.simulator.addAllSlowProcess(slowProcessList);
		
		Eln.simulator.addAllElectricalComponent(electricalComponentList);
		for(State load : electricalLoadList)Eln.simulator.addElectricalLoad(load);
		Eln.simulator.addAllElectricalProcess(electricalProcessList);
		
		Eln.simulator.addAllThermalConnection(thermalConnectionList);
		for(NbtThermalLoad load : thermalLoadList)Eln.simulator.addThermalLoad(load);
		Eln.simulator.addAllThermalFastProcess(thermalFastProcessList);
	}
	public void disconnectJob()
	{
		Eln.simulator.removeAllSlowProcess(slowProcessList);
		
		Eln.simulator.removeAllElectricalComponent(electricalComponentList);
		for(State load : electricalLoadList)Eln.simulator.removeElectricalLoad(load);
		Eln.simulator.removeAllElectricalProcess(electricalProcessList);
		
		Eln.simulator.removeAllThermalConnection(thermalConnectionList);
		for(NbtThermalLoad load : thermalLoadList)Eln.simulator.removeThermalLoad(load);
		Eln.simulator.removeAllThermalFastProcess(thermalFastProcessList);
	}

	public TransparentNode node;
	public Direction front;
	public boolean grounded = true;
	
	
	public void onGroundedChangedByClient()
	{
		needPublish();
	}

	public byte networkUnserialize(DataInputStream stream,EntityPlayerMP player) 
	{
		return networkUnserialize(stream);
	}
	public byte networkUnserialize(DataInputStream stream) 
	{
		byte readed;
		try {
			switch(readed = stream.readByte())
			{
			case unserializeGroundedId:
				grounded = stream.readByte() != 0 ? true : false;
				onGroundedChangedByClient();
				return unserializeNulldId;
				default:
					return readed;
			}
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return unserializeNulldId;
	}
	
	public int getLightValue() 
	{
		return 0;
	}
	
	public boolean hasGui()
	{
		return false;
	}
	public IInventory getInventory()
	{
		return null;
	}
	
    public void preparePacketForClient(DataOutputStream stream)
    {
    	node.preparePacketForClient(stream); 	
    }
	
	public void sendIdToAllClient(byte id){
		ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);   	
        
		preparePacketForClient(packet);
		
		try {
			packet.writeByte(id);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	
		sendPacketToAllClient(bos);
	}
	
	
	public void sendStringToAllClient(byte id,String str){
		ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);   	
        
		preparePacketForClient(packet);
		
		try {
			packet.writeByte(id);
			packet.writeUTF(str);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	
		sendPacketToAllClient(bos);
	}
	
	
	private void sendPacketToAllClient(ByteArrayOutputStream bos) {
		node.sendPacketToAllClient(bos);
	}

	public Container newContainer(Direction side,EntityPlayer player)
	{
		return null;
	}

	public TransparentNodeElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor)
	{
		this.node = transparentNode;
		this.transparentNodeDescriptor = descriptor;
		if(descriptor.hasGhostGroup())Eln.ghostManager.addObserver(this);
	}

	public IFluidHandler getFluidHandler() {
		return null;
	}


	public void onNeighborBlockChange() 
	{
		checkCanStay(false);
	}
	
	
	public void checkCanStay(boolean onCreate) {
		Block block;
		boolean needDestroy = false;
		if(transparentNodeDescriptor.mustHaveFloor())
		{
			if(! node.isBlockOpaque(Direction.YN)) needDestroy = true;
		}
		if(transparentNodeDescriptor.mustHaveCeiling())
		{
			if(! node.isBlockOpaque(Direction.YP)) needDestroy = true;
		}
		if(transparentNodeDescriptor.mustHaveWallFrontInverse())
		{
			if(! node.isBlockOpaque(front.getInverse())) needDestroy = true;
		}
		if(transparentNodeDescriptor.mustHaveWall())
		{
			boolean wall = false;

			if(node.isBlockOpaque(Direction.XN)) wall = true;
			if(node.isBlockOpaque(Direction.XP)) wall = true;
			if(node.isBlockOpaque(Direction.ZN)) wall = true;
			if(node.isBlockOpaque(Direction.ZP)) wall = true;
			
			if(! wall) needDestroy = true;
		}
		
		if(needDestroy)
		{
			selfDestroy();
		}
	}
	public void selfDestroy()
	{
		node.physicalSelfDestruction(0f);
	}
	/*
	public static boolean canBePlacedOnSide(Direction side,int type)
	{
		return true;
	}
	*/
	
	public void stop(int uuid){
		ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
		DataOutputStream stream = new DataOutputStream(bos);

		try {
			stream.writeByte(Eln.packetDestroyUuid);
			stream.writeInt(uuid);

			sendPacketToAllClient(bos);
		} catch (IOException e) {
			
			e.printStackTrace();

		}
	}
	public void onBreakElement()
	{
		if (useUuid()) stop(uuid);
		
		if(transparentNodeDescriptor.hasGhostGroup()){
			Eln.ghostManager.removeObserver(node.coordonate);
			Eln.ghostManager.removeGhostAndBlockWithObserver(node.coordonate);
			//transparentNodeDescriptor.getGhostGroup(front).erase(node.coordonate);
		}
		node.dropInventory(getInventory());
		node.dropElement(node.removedByPlayer);

	}
	public ItemStack getDropItemStack()
	{
		ItemStack itemStack =  new ItemStack(Eln.transparentNodeBlock, 1, node.elementId);
		itemStack.setTagCompound(getItemStackNBT());
		return itemStack;
	}	
	
	public NBTTagCompound getItemStackNBT()
	{
		return null;
	}


	
	
	
    public abstract ElectricalLoad getElectricalLoad(Direction side,LRDU lrdu);
	public abstract ThermalLoad getThermalLoad(Direction side,LRDU lrdu);
	
	public abstract int getConnectionMask(Direction side,LRDU lrdu);
	
	
	
	public abstract String multiMeterString(Direction side);
	public abstract String thermoMeterString(Direction side);
	

	public void networkSerialize(DataOutputStream stream)
	{
		try {
			stream.writeByte(front.getInt() + (grounded ? 8 : 0));
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	
    public void initializeFromThat(Direction front, EntityLivingBase entityLiving,NBTTagCompound itemStackNbt)
    {
    	this.front = front;
    	readItemStackNBT(itemStackNbt);
    	initialize();
    }
    public abstract void initialize();
    
    public void readItemStackNBT(NBTTagCompound nbt)
    {
    	
    }
    
    
  //  public abstract void destroyFrom(SixNode sixNode);

	public abstract boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz);

	
		
	public void readFromNBT(NBTTagCompound nbt)
	{      

        int idx;
        
        IInventory inv = getInventory();
        if(inv != null)
        {
        	Utils.readFromNBT(nbt, "inv", inv);
        }
        
        idx = 0;
        
		for(State electricalLoad : electricalLoadList) 
		{
			if(electricalLoad instanceof INBTTReady) ((INBTTReady)electricalLoad).readFromNBT(nbt,"" );
		}


		for(NbtThermalLoad thermalLoad : thermalLoadList) 
		{
			thermalLoad.readFromNBT(nbt,"");
		}
		
		
		for(Component c : electricalComponentList)	
			if(c instanceof INBTTReady)
				((INBTTReady) c).readFromNBT(nbt, "");
		
		
		
		for(IProcess process : slowProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).readFromNBT(nbt,"");
		}
		for(IProcess process : electricalProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).readFromNBT(nbt,"");
		}
		for(IProcess process : thermalFastProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).readFromNBT(nbt,"");
		}
			
		
		byte b = nbt.getByte("others");
		front = Direction.fromInt(b & 0x7);		
		grounded = (b & 8) != 0;
	}
	
	    
	    

    public void writeToNBT(NBTTagCompound nbt)
    {
        int idx = 0;
        
        IInventory inv = getInventory();
        if(inv != null)
        {
        	Utils.writeToNBT(nbt,"inv", inv);
        }
        
		for(State electricalLoad : electricalLoadList) 
		{
			if(electricalLoad instanceof INBTTReady) ((INBTTReady)electricalLoad).writeToNBT(nbt,"" );
		}

		for(NbtThermalLoad thermalLoad : thermalLoadList) 
		{
			thermalLoad.writeToNBT(nbt,"");
		}
		
		for(Component c : electricalComponentList)	
			if(c instanceof INBTTReady)
				((INBTTReady) c).writeToNBT(nbt, "");
		
		
		for(IProcess process : slowProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).writeToNBT(nbt,"");
		}
		for(IProcess process : electricalProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).writeToNBT(nbt,"");
		}
		for(IProcess process : thermalFastProcessList) 
		{
			if(process instanceof INBTTReady) ((INBTTReady)process).writeToNBT(nbt,"");
		}


        nbt.setByte("others",(byte) (front.getInt() + (grounded ? 8 : 0))) ;
    }
    
    public void reconnect()
    {
    	node.reconnect();
    }
    
    public void needPublish()
    {
    	node.setNeedPublish(true);
    }
    
    
    public void connect()
    {
    	node.connect();
    }
    public void disconnect()
    {
    	node.disconnect();
    }
    
    
    public void inventoryChange(IInventory inventory)
    {
    	
    }
    
	public float getLightOpacity() {
		
		return 0f;
	}
    
	public Coordonate getGhostObserverCoordonate()
	{
		return node.coordonate;
		
	}
	public void ghostDestroyed(int UUID)
	{
		if(UUID == transparentNodeDescriptor.getGhostGroupUuid()){
			selfDestroy();
		}
	}
	public boolean ghostBlockActivated(int UUID,EntityPlayer entityPlayer, Direction side,float vx, float vy, float vz)
	{
		if(UUID == transparentNodeDescriptor.getGhostGroupUuid()){
			return node.onBlockActivated(entityPlayer, side, vx, vy, vz);
		}
		return false;
	}

  

	
	public World world() {
		
		return node.coordonate.world();
	}
	public Coordonate coordonate(){
		return node.coordonate;
	}
	
	
	private int uuid = 0;
	public int getUuid(){
		if(uuid == 0){
			uuid = Utils.getUuid();
		}
		return uuid;
	}
	public boolean useUuid(){
		return uuid != 0;
	}
	public void play(SoundCommand s){
		s.addUuid(getUuid());
		s.set(node.coordonate);
		s.play();
	}

	public void unload() {

	}

/*	protected boolean hasSidedInventory(){
		return false;
	}
	public int tileEntityMetaTag() {
		return hasSidedInventory() ? 0x4 : 0;
	}
	*/
}
