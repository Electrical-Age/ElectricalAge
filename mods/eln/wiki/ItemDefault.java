package mods.eln.wiki;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.registry.GameRegistry;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Recipe;
import mods.eln.misc.Utils;

public class ItemDefault extends GuiScreenEln{

	private ItemStack stack;
	private GuiScreen previewScreen;


	public ItemDefault(ItemStack stack,GuiScreen previewScreen) {
		this.stack = stack;
		this.previewScreen = previewScreen;
	}
	GuiHelper helper;
	
	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return helper = new GuiHelper(this, 176, 166);
	}

	GuiButtonEln before;
	GuiItemStack self;
	GuiVerticalExtender extender;
	
	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();
		
		extender = new GuiVerticalExtender(50, 50, 100, 50,helper);
		add(extender);
		
		//next = newGuiButton(70, 0, 50, "next");
		before = newGuiButton(70, 0, 50, "before");
		
		self = new GuiItemStack(0, 0, stack, this,this);
		add(self);
		
		int y = 0;
		
		
		
		ArrayList<IRecipe> recipeList = new ArrayList<IRecipe>();
		if(stack != null){
			List list = CraftingManager.getInstance().getRecipeList();
			for(Object o : list){
				if(o instanceof IRecipe){
					IRecipe r = (IRecipe)o;
					
					ItemStack out = r.getRecipeOutput();
					if(out != null && out.itemID == stack.itemID && out.getItemDamage() == stack.getItemDamage()){
						recipeList.add(r);
					}
				}
			}
		}
	
		for(IRecipe r : recipeList){
			if(r instanceof ShapedRecipes){
				ShapedRecipes s = (ShapedRecipes)r;
				for(int idx2 = 0;idx2 < 3;idx2++){
					for(int idx = 0;idx < 3;idx++){
						ItemStack rStack = null;
						if(idx < s.recipeWidth && idx2 < s.recipeHeight){
							rStack = s.recipeItems[idx + idx2*s.recipeWidth];
						}
						GuiItemStack gui = new GuiItemStack(idx * 18, idx2*18+y	, rStack, this,this);
						extender.add(gui);
					}
					
				}
				
				
				
				
				
				
				y += 64;
			}
		}
		
		
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		// TODO Auto-generated method stub
		super.guiObjectEvent(object);
		
		if(object == before){
			Utils.clientOpenGui(previewScreen);
		}
		
	}
}
