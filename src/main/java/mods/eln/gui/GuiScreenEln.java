package mods.eln.gui;

import mods.eln.gui.GuiTextFieldEln.GuiTextFieldElnObserver;
import mods.eln.gui.IGuiObject.IGuiObjectObserver;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public abstract class GuiScreenEln extends GuiScreen implements GuiTextFieldElnObserver, IGuiObjectObserver {

    protected GuiHelper helper;

    protected abstract GuiHelper newHelper();

    @Override
    public void initGui() {
        super.initGui();
        helper = newHelper();
    }

    public GuiTextFieldEln newGuiTextField(int x, int y, int width) {
        GuiTextFieldEln o = helper.newGuiTextField(x, y, width);
        o.setObserver(this);
        return o;
    }

    public GuiButtonEln newGuiButton(int x, int y, int width, String name) {
        GuiButtonEln o = helper.newGuiButton(x, y, width, name);
        o.setObserver(this);
        return o;
    }

    public GuiVerticalTrackBar newGuiVerticalTrackBar(int x, int y, int width, int height) {
        GuiVerticalTrackBar o = helper.newGuiVerticalTrackBar(x, y, width, height);
        o.setObserver(this);
        return o;
    }

    public GuiVerticalTrackBarHeat newGuiVerticalTrackBarHeat(int x, int y, int width, int height) {
        GuiVerticalTrackBarHeat o = helper.newGuiVerticalTrackBarHeat(x, y, width, height);
        o.setObserver(this);
        return o;
    }

    public GuiVerticalCustomValuesBar newGuiVerticalCustomValuesBar(int x, int y, int width, int height, Float[] values) {
        GuiVerticalCustomValuesBar o = helper.newGuiVerticalCustomValuesBar(x, y, width, height, values);
        o.setObserver(this);
        return o;
    }

    public GuiVerticalProgressBar newGuiVerticalProgressBar(int x, int y, int width, int height) {
        GuiVerticalProgressBar o = helper.newGuiVerticalProgressBar(x, y, width, height);

        return o;
    }

    @Override
    protected void keyTyped(char key, int code) throws IOException {
        helper.keyTyped(key, code);
        super.keyTyped(key, code);
    }

    @Override
    protected void mouseClicked(int x, int y, int code) throws IOException {
        helper.mouseClicked(x, y, code);
        super.mouseClicked(x, y, code);
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int witch) {
        helper.mouseMovedOrUp(x, y, witch);
        super.mouseMovedOrUp(x, y, witch);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int x, int y, float f) {
        super.drawScreen(x, y, f);
        preDraw(f, x, y);
        helper.mouseMove(x, y);
        helper.draw(x, y, f);
        postDraw(f, x, y);
        helper.draw2(x, y);
    }

    @Override
    public void textFieldNewValue(GuiTextFieldEln textField, String value) {
        guiObjectEvent(textField);
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
    }

    protected void preDraw(float f, int x, int y) {
    }

    protected void postDraw(float f, int x, int y) {
    }

    protected void drawString(int x, int y, String str) {
        drawString(x, y, 4210752, str);
    }

    protected void drawString(int x, int y, int color, String str) {
        helper.drawString(x, y, color, str);
    }

    protected void add(IGuiObject o) {
        helper.add(o);
    }

    protected void remove(IGuiObject o) {
        helper.remove(o);
    }
}
