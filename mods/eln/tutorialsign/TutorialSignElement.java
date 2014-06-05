package mods.eln.tutorialsign;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.swing.text.MaskFormatter;

import cpw.mods.fml.common.FMLCommonHandler;


import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.NodeThermalWatchdogProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadDynamicProcess;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalSourceRefGroundProcess;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

public class TutorialSignElement extends SixNodeElement {
	static HashMap<String, String> baliseMap = null;

	public static void resetBalise(){
		baliseMap = null;
	}
	public static String getText(String balise){
		if(baliseMap == null){		
			baliseMap = new HashMap<String, String>();
		
			try {
				String file = Utils.readMapFile("EA/tutorialSign.txt");
				String ret;				
				if(file.contains("\r\n"))
					ret = "\r\n";
				else
					ret = "\n";					
				
				file = file.replaceAll("#"+ret, "#");
				file = file.replaceAll(ret + "#", "#");

				String[] split = file.split("#");
				
				boolean first = true;
				int counter = 0;
				String baliseTag = "";
				for(String str : split){
					if(first) {
						first = false;
						continue;
					}
					if(counter == 0){
						baliseTag = str;
					}
					if(counter == 1){
						baliseMap.put(baliseTag, str);
					}
					
					counter = (counter + 1) & 1;
				}

				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
		String text = baliseMap.get(balise);
		if(text == null) return "No balise found";
		return text;	
	}
	
	
	public TutorialSignElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

	}


	public static final int setTextFileId = 1;
	
	String baliseName = "";
	
	void setBalise(String name){
		baliseName = name;
		needPublish();
	}
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		setBalise(nbt.getString("baliseName"));
	}
	
/*
	private void setTextFile(String name) {
		if(name.matches("^[a-zA-Z0-9]*$") == false){
			fileName = "OnlyAlphaNumeric";
			text = "OnlyAlphaNumeric";
		} else {		
			fileName = name;
			try {
				text = Utils.readMapFile("EATuto/" + fileName + ".txt");
			} catch (IOException e) {
				text = "file not found";
			}
		}
		needPublish();
	}*/


	@Override
	public void writeToNBT(NBTTagCompound nbt) {
 		super.writeToNBT(nbt);
 		nbt.setString("baliseName", baliseName);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		return 0;
	}

	@Override
	public String multiMeterString() {
		return "";
	}
	
	@Override
	public String thermoMeterString() {
		return "";
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeUTF(baliseName);
			stream.writeUTF(getText(baliseName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		try {
			switch(stream.readByte()) {
			case setTextFileId:
				setBalise(stream.readUTF());
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasGui() {
		return true;
	}


	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}
}
