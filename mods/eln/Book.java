package mods.eln;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;




	public class Book {
	public void processCommand(ICommandSender sender, String[] data) {
		       
		       
		       EntityPlayer player = (EntityPlayer) sender;
		       
		
		       ItemStack Book = new ItemStack(Items.written_book);
		      
		       
		       NBTTagCompound tag = new NBTTagCompound();
		       NBTTagList bookPages = new NBTTagList();
		       bookPages.appendTag(new NBTTagString("Test"));
		       bookPages.appendTag(new NBTTagString("Test"));
		       bookPages.appendTag(new NBTTagString("Test"));
		       bookPages.appendTag(new NBTTagString("Test"));
		       bookPages.appendTag(new NBTTagString("Test"));
		       bookPages.appendTag(new NBTTagString("Test"));
		       bookPages.appendTag(new NBTTagString("Test"));
		       Book.setTagInfo("pages", bookPages);
		       Book.setTagInfo("author", new NBTTagString("ShadowWarrior979"));
		       Book.setTagInfo("title", new NBTTagString("Test"));

		       
		       //player.inventory.addItemStackToInventory(Book);
		       player.inventory.setInventorySlotContents(1, new ItemStack(Items.book, 1));
	}
	}