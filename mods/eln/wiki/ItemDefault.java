package mods.eln.wiki;

import java.util.ArrayList;
import java.util.List;

import javax.management.Descriptor;

import cpw.mods.fml.common.registry.GameRegistry;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiLabel;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Recipe;
import mods.eln.misc.Utils;

public class ItemDefault extends Default{
	public static interface IPlugIn{
		public int top(int y,GuiVerticalExtender extender,ItemStack stack);
		public int bottom(int y,GuiVerticalExtender extender,ItemStack stack);
	}
	
	private ItemStack stack;
	private GuiScreen previewScreen;


	public ItemDefault(ItemStack stack,GuiScreen previewScreen) {
		super(previewScreen);
		this.stack = stack;

	}




	GuiItemStack self;

	
	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();
		if(stack == null) return;
		int y = 6;
		
		Object desc = Utils.getItemObject(stack);
		IPlugIn plugIn = null;
		if(desc instanceof IPlugIn){
			plugIn = (IPlugIn)desc;
		}
				
		self = new GuiItemStack(6, y, stack, this,helper);
		extender.add(self);
		extender.add(new GuiLabel(6+21, y+3, stack.getDisplayName()));
		y += 24;
		
		
		if(plugIn != null) y = plugIn.top(y, extender, stack);
		
		ArrayList<IRecipe> recipeOutList = new ArrayList<IRecipe>();
		ArrayList<IRecipe> recipeInList = new ArrayList<IRecipe>();
		if(stack != null){
			List list = CraftingManager.getInstance().getRecipeList();
			for(Object o : list){
				if(o instanceof IRecipe){
					IRecipe r = (IRecipe)o;
					
					ItemStack out = r.getRecipeOutput();
					if(out != null && out.itemID == stack.itemID && out.getItemDamage() == stack.getItemDamage()){
						recipeOutList.add(r);
					}
					
					if(r instanceof ShapedRecipes){
						ShapedRecipes s = (ShapedRecipes)r;
						for(ItemStack rStack : s.recipeItems){
							if(rStack != null && rStack.itemID == stack.itemID && rStack.getItemDamage() == stack.getItemDamage()){
								recipeInList.add(r);
								break;
							}						
						}
					}
				}

			}
		}
		int counter = 0;
		if(recipeOutList.size() == 0){
			extender.add(new GuiLabel(6, y, "Is not craftable"));
			y += 12;			
		}
		else{
			extender.add(new GuiLabel(6, y, "Recipe :"));
			y += 12;
			
			counter = -1;
			for(IRecipe r : recipeOutList){
				if(counter == 0) y += 60;
				if(counter == -1) counter = 0;
				if(r instanceof ShapedRecipes){
					ShapedRecipes s = (ShapedRecipes)r;
					for(int idx2 = 0;idx2 < 3;idx2++){
						for(int idx = 0;idx < 3;idx++){
							ItemStack rStack = null;
							if(idx < s.recipeWidth && idx2 < s.recipeHeight){
								rStack = s.recipeItems[idx + idx2*s.recipeWidth];
							}
							GuiItemStack gui = new GuiItemStack(idx * 18+6 + counter*60, idx2*18+y	, rStack, this,helper);
							extender.add(gui);
						}	
					}
					counter = (counter + 1) % 3;
				}
			}
			
			y+=70;
		}
		
		if(recipeInList.size() == 0){
			extender.add(new GuiLabel(6, y, "Is not a crafting material"));
			y += 12;			
		}
		else{
			extender.add(new GuiLabel(6, y, "Can craft :"));
			y += 12;
			counter = -1;
			for(IRecipe r : recipeInList){
				if(counter == 0) y += 60;
				if(counter == -1) counter = 0;
				if(r instanceof ShapedRecipes){
					ShapedRecipes s = (ShapedRecipes)r;
					for(int idx2 = 0;idx2 < 3;idx2++){
						for(int idx = 0;idx < 3;idx++){
							ItemStack rStack = null;
							if(idx < s.recipeWidth && idx2 < s.recipeHeight){
								rStack = s.recipeItems[idx + idx2*s.recipeWidth];
							}
							GuiItemStack gui = new GuiItemStack(idx * 18+6 + counter*105, idx2*18+y	, rStack, this,helper);
							extender.add(gui);
						}	
					}
					
	
					GuiItemStack gui = new GuiItemStack((int)(3.5* 18)+6 + counter*105, 1*18+y	, s.getRecipeOutput(), this,helper);
					extender.add(gui);
					
					counter = (counter + 1) % 2;
				}
			}
			y+=70;
		}
		

		
		if(plugIn != null) y = plugIn.bottom(y, extender, stack);
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		// TODO Auto-generated method stub
		super.guiObjectEvent(object);
		
	}
}
