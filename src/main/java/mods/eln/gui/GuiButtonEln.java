package mods.eln.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

public class GuiButtonEln extends GuiButton implements IGuiObject {

    IGuiObjectObserver observer;

    public GuiButtonEln(int x, int y, int width, int height, String str) {
        super(0, x, y, width, height, str);
    }

    GuiHelper helper;

    public void setHelper(GuiHelper helper) {
        this.helper = helper;
    }

    public void setObserver(IGuiObjectObserver observer) {
        this.observer = observer;
    }

    @Override
    public void idraw(int x, int y, float f) {
        GL11.glColor4f(1f, 1f, 1f, 1f);
        drawButton(Minecraft.getMinecraft(), x, y);
    }

    @Override
    public int getYMax() {
        return yPosition + height;
    }

    @Override
    public boolean ikeyTyped(char key, int code) {
        return false;
    }

    public void onMouseClicked() {
    }

    @Override
    public void imouseClicked(int x, int y, int code) {
//        if (mousePressed(Minecraft.getMinecraft(), x, y)) {
//            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.(new ResourceLocation("gui.button.press"), 1.0F));
//            onMouseClicked();
//            if (observer != null) {
//                observer.guiObjectEvent(this);
//            }
//        }
    }

    @Override
    public void imouseMove(int x, int y) {
    }

    @Override
    public void imouseMovedOrUp(int x, int y, int witch) {
    }

    @Override
    public void idraw2(int x, int y) {
        if (helper != null && visible && x >= xPosition && y >= yPosition && x < xPosition + width && y < yPosition + height)
            helper.drawHoveringText(comment, x, y, Minecraft.getMinecraft().fontRendererObj);
    }

    @Override
    public void translate(int x, int y) {
        this.xPosition += x;
        this.yPosition += y;
    }

    ArrayList<String> comment = new ArrayList<String>();

    public void setComment(int line, String comment) {
        if (this.comment.size() < line + 1)
            this.comment.add(line, comment);
        else
            this.comment.set(line, comment);
    }

    public void clearComment() {
        comment.clear();
    }
}
