package mods.eln.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

/**
 * Checkbox item for GUI (WIP)
 *
 * @author lambdaShade
 * @date   15.04.2015
 */

public class GuiCheckbox extends Gui implements IGuiObject {

	/*
    static ResourceLocation reslocBitmapCheckboxOff = new ResourceLocation("eln","sprites/gui/checkbox_off.png");
    static ResourceLocation reslocBitmapCheckboxOffSel = new ResourceLocation("eln","sprites/gui/checkbox_offsel.png");
    static ResourceLocation reslocBitmapCheckboxOn = new ResourceLocation("eln","sprites/gui/checkbox_on.png");
    static ResourceLocation reslocBitmapCheckboxOnSel = new ResourceLocation("eln","sprites/gui/checkbox_onsel.png");
	*/
	
    @Override
    public void idraw(int x, int y, float f) {

    }

    @Override
    public void idraw2(int x, int y) {

    }

    @Override
    public boolean ikeyTyped(char key, int code) {
        return false;
    }

    @Override
    public void imouseClicked(int x, int y, int code) {

    }

    @Override
    public void imouseMove(int x, int y) {

    }

    @Override
    public void imouseMovedOrUp(int x, int y, int witch) {

    }

    @Override
    public void translate(int x, int y) {

    }

    @Override
    public int getYMax() {
        return 0;
    }
}
