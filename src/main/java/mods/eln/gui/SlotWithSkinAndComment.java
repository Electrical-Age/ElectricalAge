package mods.eln.gui;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import java.util.ArrayList;

public class SlotWithSkinAndComment extends Slot implements ISlotSkin, ISlotWithComment {

    String[] comment;
    SlotSkin skin;

    public SlotWithSkinAndComment(IInventory par1iInventory, int par2, int par3, int par4, SlotSkin skin, String[] comment) {
        super(par1iInventory, par2, par3, par4);
        this.skin = skin;
        this.comment = comment;
    }

    @Override
    public SlotSkin getSlotSkin() {
        return skin;
    }

    @Override
    public void getComment(ArrayList<String> list) {
        for (String str : comment)
            list.add(str);
    }
}
