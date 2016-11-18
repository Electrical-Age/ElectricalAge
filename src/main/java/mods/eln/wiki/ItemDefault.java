package mods.eln.wiki;

import mods.eln.gui.GuiLabel;
import mods.eln.gui.IGuiObject;
import mods.eln.api.recipe.Recipe;
import mods.eln.misc.RecipesList;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ItemDefault extends Default {
    public static interface IPlugIn {
        public int top(int y, GuiVerticalExtender extender, ItemStack stack);

        public int bottom(int y, GuiVerticalExtender extender, ItemStack stack);
    }

    private ItemStack stack;
    private GuiScreen previewScreen;

    public ItemDefault(ItemStack stack, GuiScreen previewScreen) {
        super(previewScreen);
        this.stack = stack;

    }

    GuiItemStack self;

    @Override
    public void initGui() {

        super.initGui();
        try {

            if (stack == null) return;
            int y = 6;

            Object desc = Utils.getItemObject(stack);
            IPlugIn plugIn = null;
            if (desc instanceof IPlugIn) {
                plugIn = (IPlugIn) desc;
            }

            self = new GuiItemStack(6, y, stack, helper);
            extender.add(self);
            extender.add(new GuiLabel(6 + 21, y + 3, stack.getDisplayName()));
            y += 24;

            if (plugIn != null) y = plugIn.top(y, extender, stack);

            List<IRecipe> recipeOutList = new ArrayList<IRecipe>();
            List<IRecipe> recipeInList = new ArrayList<IRecipe>();
            if (stack != null) {
                List list = CraftingManager.getInstance().getRecipeList();
                for (Object o : list) {
                    try {
                        if (o instanceof IRecipe) {
                            IRecipe r = (IRecipe) o;

                            ItemStack out = r.getRecipeOutput();
                            if (out != null && out.getItem() == stack.getItem() && out.getItemDamage() == stack.getItemDamage()) {
                                recipeOutList.add(r);
                            }

                            for (ItemStack rStack : Utils.getRecipeInputs(r)) {
                                if (rStack != null && rStack.getItem() == stack.getItem() && rStack.getItemDamage() == stack.getItemDamage()) {
                                    recipeInList.add(r);
                                    break;
                                }
                            }
                        }

                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                }
            }
            int counter = 0;
            if (recipeOutList.isEmpty()) {
                extender.add(new GuiLabel(6, y, tr("Cannot be crafted!")));
                y += 12;
            } else {
                extender.add(new GuiLabel(6, y, tr("Recipe:")));
                y += 12;

                counter = -1;
                for (IRecipe r : recipeOutList) {
                    if (counter == 0) y += 60;
                    if (counter == -1) counter = 0;
                    ItemStack[][] stacks = Utils.getItemStackGrid(r);

                    if (stacks != null) {
                        for (int idx2 = 0; idx2 < 3; idx2++) {
                            for (int idx = 0; idx < 3; idx++) {
                                GuiItemStack gui = new GuiItemStack(idx * 18 + 6 + counter * 60, idx2 * 18 + y, stacks[idx2][idx], helper);
                                extender.add(gui);
                            }
                        }
                        counter = (counter + 1) % 3;
                    }
                }

                y += 70;
            }

            if (recipeInList.isEmpty()) {
                extender.add(new GuiLabel(6, y, tr("Is not a crafting material!")));
                y += 12;
            } else {
                extender.add(new GuiLabel(6, y, tr("Can be used to craft:")));
                y += 12;
                counter = -1;
                for (IRecipe r : recipeInList) {
                    if (counter == 0) y += 60;
                    if (counter == -1) counter = 0;

                    ItemStack[][] stacks = Utils.getItemStackGrid(r);
                    if (stacks != null) {
                        for (int idx2 = 0; idx2 < 3; idx2++) {
                            for (int idx = 0; idx < 3; idx++) {
                                ItemStack rStack = stacks[idx2][idx];

                                GuiItemStack gui = new GuiItemStack(idx * 18 + 6 + counter * 105, idx2 * 18 + y, rStack, helper);
                                extender.add(gui);
                            }
                        }

                        GuiItemStack gui = new GuiItemStack((int) (3.5 * 18) + 6 + counter * 105, 1 * 18 + y, r.getRecipeOutput(), helper);
                        extender.add(gui);

                        counter = (counter + 1) % 2;
                    }

                }
                y += 70;
            }

            {
                counter = -1;
                List<Recipe> list = RecipesList.getGlobalRecipeWithInput(stack);
                if (list.isEmpty()) {
                    //extender.add(new GuiLabel(6, y, "Can't Product"));
                } else {
                    extender.add(new GuiLabel(6, y, tr("Can create:")));
                    y += 12;
                    for (Recipe r : list) {
                        if (counter == 0) y += (int) (18 * 1.3);
                        if (counter == -1) counter = 0;
                        int x = 6 + counter * 60;
                        extender.add(new GuiItemStack(x, y, r.input, helper));
                        x += 18 * 2;

                        for (ItemStack m : r.machineList) {
                            extender.add(new GuiItemStack(x, y, m, helper));
                            x += 18;
                        }
                        x += 18;
                        extender.add(new GuiItemStack(x, y, r.getOutputCopy()[0], helper));

                        x += 22;
                        extender.add(new GuiLabel(x, y + 4, tr("Cost %1$J", r.energy)));

                        counter = (counter + 1) % 1;
                    }
                    y += (int) (18 * 1.3);
                }
            }
            {
                counter = -1;
                List<Recipe> list = RecipesList.getGlobalRecipeWithOutput(stack);
                if (list.isEmpty()) {
                    //extender.add(new GuiLabel(6, y, "Can't Product"));
                } else {
                    extender.add(new GuiLabel(6, y, tr("Created by:")));
                    y += 12;
                    for (Recipe r : list) {
                        if (counter == 0) y += (int) (18 * 1.3);
                        if (counter == -1) counter = 0;
                        int x = 6 + counter * 60;
                        extender.add(new GuiItemStack(x, y, r.input, helper));
                        x += 18 * 2;

                        for (ItemStack m : r.machineList) {
                            extender.add(new GuiItemStack(x, y, m, helper));
                            x += 18;
                        }
                        x += 18;
                        extender.add(new GuiItemStack(x, y, r.getOutputCopy()[0], helper));

                        x += 22;
                        extender.add(new GuiLabel(x, y + 4, tr("Cost %1$J", r.energy)));

                        counter = (counter + 1) % 1;
                    }
                    y += (int) (18 * 1.3);
                }
            }

            if (plugIn != null) y = plugIn.bottom(y, extender, stack);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {

        super.guiObjectEvent(object);

    }
}
