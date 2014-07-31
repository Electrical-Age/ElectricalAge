package mods.eln.simplenode.energyconverter;


import ic2.api.energy.tile.IEnergySource;

import java.io.DataInputStream;
import java.io.IOException;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import mods.eln.Other;
import mods.eln.misc.Direction;
import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


@Optional.InterfaceList({
        @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = Other.modIdIc2),
        @Optional.Interface(iface = "li.cil.oc.api.network.Environment", modid = Other.modIdOc)/*,
        @Optional.Interface(iface = "li.cil.oc.api.network.SidedEnvironment", modid = Other.modIdOc)*/}
)
public class EnergyConverterElnToOtherEntity extends SimpleNodeEntity implements IEnergySource, Environment/*,SidedEnvironment*/ {

	
	
	public EnergyConverterElnToOtherEntity() {
		if(Other.ocLoaded) getOc().constructor();
	}
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {

		return super.onBlockActivated(entityPlayer, side, vx, vy, vz);
	}
	
	@Override
    @SideOnly(Side.CLIENT)
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new EnergyConverterElnToOtherGui(player, this);
	}
	
	
	float inPowerFactor;
	boolean hasChanges = false;
	public float inPowerMax;
	@Override
	public void serverPublishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.serverPublishUnserialize(stream);
		try {
			inPowerFactor = stream.readFloat();
			inPowerMax = stream.readFloat();
			 
			hasChanges = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String getNodeUuid() {
		// TODO Auto-generated method stub
		return EnergyConverterElnToOtherNode.getNodeUuidStatic();
	}

	

    // ********************IC2********************
	
	

    @Optional.Method(modid = Other.modIdIc2)
	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
		if(worldObj.isRemote) return false;
		SimpleNode n = getNode();
		return n.getFront().back() == Direction.from(direction);
	}

    @Optional.Method(modid = Other.modIdIc2)
    @Override
	public double getOfferedEnergy() {
		if(worldObj.isRemote)return 0;
		EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
		double pMax = node.getOtherModOutMax(node.descriptor.ic2.outMax,Other.getElnToIc2ConversionRatio());
		return pMax;
	}

    @Optional.Method(modid = Other.modIdIc2)
    @Override
	public void drawEnergy(double amount) {
		if(worldObj.isRemote);
		EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
		node.drawEnergy(amount,Other.getElnToIc2ConversionRatio());
	}
	
    @Optional.Method(modid = Other.modIdIc2)
	//@Override
	public int getSourceTier() {
		// TODO Auto-generated method stub
    	EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
		return node.descriptor.ic2.tier;
	}
	
    
    
    
    // *****************   OC **********************
    
    EnergyConverterElnToOtherFireWallOc oc;
    
    
    @Optional.Method(modid = Other.modIdOc)
    EnergyConverterElnToOtherFireWallOc getOc(){
    	if(oc == null) oc = new EnergyConverterElnToOtherFireWallOc(this);
    	return oc;
    }
    
	@Override
    @Optional.Method(modid = Other.modIdOc)
	public Node node() {
		return getOc().node;
	}

	@Override
	@Optional.Method(modid = Other.modIdOc)
	public void onConnect(Node node) {
	
	}

	@Override
	@Optional.Method(modid = Other.modIdOc)
	public void onDisconnect(Node node) {
		
	}

	@Override
	@Optional.Method(modid = Other.modIdOc)
	public void onMessage(Message message) {
		
	}
    
	/*
	@Override
	@Optional.Method(modid = Other.modIdOc)
	public Node sidedNode(ForgeDirection side) {
		if(worldObj.isRemote){
			if(front.back() == Direction.from(side)) return node();
			return null;
		}else{
			if(getNode().getFront().back() == Direction.from(side)) return node();
			return null;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	@Optional.Method(modid = Other.modIdOc)
	public boolean canConnect(ForgeDirection side) {
		if(front == null) return false;
		if(front.back() == Direction.from(side)) return true;
		return false;
	}*/
    // *****************     Bridges ****************
    
    

	@Override
	public void updateEntity() {
		super.updateEntity();
		if(Other.ic2Loaded) EnergyConverterElnToOtherFireWallIc2.updateEntity(this);
		if(Other.ocLoaded) getOc().updateEntity();
	}


	public void onLoaded() {
		if(Other.ic2Loaded) EnergyConverterElnToOtherFireWallIc2.onLoaded(this);
	}


	@Override
	public void invalidate() {
		super.invalidate();
		if(Other.ic2Loaded) EnergyConverterElnToOtherFireWallIc2.invalidate(this);
		if(Other.ocLoaded) getOc().invalidate();
	}


	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if(Other.ic2Loaded) EnergyConverterElnToOtherFireWallIc2.onChunkUnload(this);
		if(Other.ocLoaded) getOc().onChunkUnload();
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt);
		if(Other.ocLoaded) getOc().readFromNBT(nbt);

	}
	
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		if(Other.ocLoaded) getOc().writeToNBT(nbt);
	}
	
	protected boolean addedToEnet;


}
