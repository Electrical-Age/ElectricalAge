package mods.eln.gui;

import java.util.ArrayList;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class SlotWithSkinAndComment extends Slot implements ISlotSkin ,ISlotWithComment{
	
	public SlotWithSkinAndComment(IInventory par1iInventory, int par2, int par3, int par4,SlotSkin skin,String[] comment) {
		super(par1iInventory, par2, par3, par4);
		this.skin = skin;
		this.comment = comment;
	}
	String[] comment;
	SlotSkin skin;
	@Override
	public SlotSkin getSlotSkin() {
		
		return skin;
	}
	@Override
	public void getComment(ArrayList<String> list) {
		for(String str : comment)
			list.add(str);
	}

}
