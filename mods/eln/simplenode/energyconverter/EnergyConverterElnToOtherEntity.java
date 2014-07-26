package mods.eln.simplenode.energyconverter;

import java.io.DataInputStream;
import java.io.IOException;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.info.Info;
import mods.eln.misc.Direction;
import mods.eln.node.simple.SimpleNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.common.FMLCommonHandler;

public abstract class EnergyConverterElnToOtherEntity extends SimpleNodeEntity {
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {

		return super.onBlockActivated(entityPlayer, side, vx, vy, vz);
	}
	
	@Override
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

}
