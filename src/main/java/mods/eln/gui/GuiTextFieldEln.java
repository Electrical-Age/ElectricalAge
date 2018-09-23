package mods.eln.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.util.ArrayList;

public class GuiTextFieldEln extends GuiTextField implements IGuiObject {

    int xPos, yPos, width, height;
    GuiTextFieldElnObserver observer;
    GuiHelper helper;

    private boolean enabled = true;

    IGuiObjectObserver iGuiObjectObserver;

    public GuiTextFieldEln(FontRenderer par1FontRenderer, int x, int y, int w, int h, GuiHelper helper) {
        super(par1FontRenderer, x, y, w, h);
        setTextColor(-1);
        setDisabledTextColour(-1);
        setEnableBackgroundDrawing(true);
        setMaxStringLength(150);
        xPos = x;
        yPos = y;
        width = w;
        height = h;
        this.helper = helper;
    }

    public GuiTextFieldEln(FontRenderer par1FontRenderer, int x, int y, int w, GuiHelper helper) {
        this(par1FontRenderer, x, y, w, 12, helper);
    }

    @Override
    public int getYMax() {
        return yPos + height;
    }

    public void setObserver(GuiTextFieldElnObserver observer) {
        this.observer = observer;
    }

    public interface GuiTextFieldElnObserver {
        void textFieldNewValue(GuiTextFieldEln textField, String value);
    }

    ArrayList<String> comment = new ArrayList<String>();

    public void setComment(String[] comment) {
        for (String str : comment) {
            this.comment.add(str);
        }
    }

    public void setComment(int line, String comment) {
        if (this.comment.size() < line + 1)
            this.comment.add(line, comment);
        else
            this.comment.set(line, comment);
    }

    public void setText(float value) {
        if (Math.abs(value) < 1000)
            setText(String.format("%3.2f", value));
        else
            setText(String.format("%3.0f", value));
    }

    public void setText(int value) {
        setText(String.format("%d", value));
    }

    @Override
    public void setEnabled(boolean par1) {
        enabled = par1;
        super.setEnabled(par1);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public boolean textboxKeyTyped(char par1, int par2) {
        if (getEnabled() && this.isFocused() && par1 == '\r') {
            setFocused(false);
            return true;
        }
        return super.textboxKeyTyped(par1, par2);
    }

    @Override
    public void setFocused(boolean par1) {
        if (isFocused() == true && par1 == false) {
            if (observer != null)
                observer.textFieldNewValue(this, this.getText());
            if (iGuiObjectObserver != null)
                iGuiObjectObserver.guiObjectEvent(this);
        }
        super.setFocused(par1);
    }

    @Override
    public void idraw(int x, int y, float f) {
        this.drawTextBox();
    }

    @Override
    public boolean ikeyTyped(char key, int code) {
        return this.textboxKeyTyped(key, code);
    }

    @Override
    public void imouseClicked(int x, int y, int code) {
        this.mouseClicked(x, y, code);
    }

    @Override
    public void imouseMove(int x, int y) {
    }

    @Override
    public void imouseMovedOrUp(int x, int y, int witch) {
    }

    @Override
    public void idraw2(int x, int y) {
        if (!isFocused() && getVisible() && x >= xPos && y >= yPos && x < xPos + width && y < yPos + height)
            helper.drawHoveringText(comment, x, y, Minecraft.getMinecraft().fontRendererObj);

    }

    @Override
    public void translate(int x, int y) {
        this.xPos += x;
        this.yPos += y;
    }

    public int getHeight() {
        return height;
    }

    public void setGuiObserver(IGuiObjectObserver iGuiObjectObserver) {
        this.iGuiObjectObserver = iGuiObjectObserver;
    }
}
