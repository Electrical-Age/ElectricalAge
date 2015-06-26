package mods.eln.wiki;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public class Search extends GuiScreenEln {

    private String bootText;

    public Search(String text) {
        bootText = text;

    }


    ArrayList<ItemStack> searchList = new ArrayList<ItemStack>();
    ArrayList<GuiItemStack> guiStackList = new ArrayList<GuiItemStack>();

    void searchStack(String text) {
        for (GuiItemStack gui : guiStackList) {
            remove(gui);
        }
        guiStackList.clear();
        searchList.clear();
        Utils.getItemStack(text, searchList);
        int idx = 0;
        ;
        for (ItemStack stack : searchList) {
            GuiItemStack gui = new GuiItemStack((idx % 8) * 21 + 6, idx / 8 * 21 + 24, stack, helper);
            guiStackList.add(gui);
            add(gui);
            idx++;
            if (idx > 8 * 7 - 1) break;
        }
    }


    GuiButton toogleDefaultOutput;
    GuiTextFieldEln searchText;

    @Override
    public void initGui() {

        super.initGui();

        //toogleDefaultOutput = newGuiButton(8, 8,176-16, "toogle switch");
        searchText = newGuiTextField(8, 8, 176 - 16);
        searchText.setText(bootText);


        searchStack(searchText.getText());

    }

    @Override
    public void guiObjectEvent(IGuiObject object) {

        super.guiObjectEvent(object);
        if (object == toogleDefaultOutput) {

        } else if (object == searchText) {

            searchStack(searchText.getText());
        } else if (object instanceof GuiItemStack) {
            GuiItemStack gui = (GuiItemStack) object;
            //Utils.clientOpenGui(new ItemDefault(gui.stack));
        }
    }

    @Override
    protected void preDraw(float f, int x, int y) {

        super.preDraw(f, x, y);

    }


    @Override
    protected void postDraw(float f, int x, int y) {

        super.postDraw(f, x, y);


    }

    @Override
    protected GuiHelper newHelper() {

        return helper = new GuiHelper(this, 176, 166 + 6);
    }


    GuiHelper helper;

}
