package mods.eln.gui;

import mods.eln.misc.UtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GuiHelper {
    public GuiScreen screen;
    public int xSize, ySize;
    ResourceLocation background;
    static final ResourceLocation helperTexture = new ResourceLocation("eln", "sprites/gui/helpertexture.png");

    static final ResourceLocation slotSkin = new ResourceLocation("textures/gui/container/furnace.png");

    public static final Tessellator tessellator = new Tessellator(16);

    public GuiHelper(GuiScreen screen, int xSize, int ySize, String backgroundName) {
        this.screen = screen;
        this.xSize = xSize;
        this.ySize = ySize;
        background = new ResourceLocation("eln", "sprites/gui/" + backgroundName);
    }

    public GuiHelper(GuiScreen screen, int xSize, int ySize) {
        this.screen = screen;
        this.xSize = xSize;
        this.ySize = ySize;
    }

    GuiTextFieldEln newGuiTextField(int x, int y, int width) {
        GuiTextFieldEln o;
        o = new GuiTextFieldEln(Minecraft.getMinecraft().fontRenderer,
            screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, 12, this);
        objectList.add(o);
        return o;
    }

    GuiButtonEln newGuiButton(int x, int y, int width, String name) {
        GuiButtonEln o;
        o = new GuiButtonEln(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, 20, name);
        o.setHelper(this);
        objectList.add(o);
        return o;
    }

    GuiVerticalTrackBar newGuiVerticalTrackBar(int x, int y, int width, int height) {
        GuiVerticalTrackBar o;
        o = new GuiVerticalTrackBar(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, height, this);
        objectList.add(o);
        return o;
    }

    GuiVerticalTrackBarHeat newGuiVerticalTrackBarHeat(int x, int y, int width, int height) {
        GuiVerticalTrackBarHeat o;
        o = new GuiVerticalTrackBarHeat(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, height, this);
        objectList.add(o);
        return o;
    }

    public GuiVerticalProgressBar newGuiVerticalProgressBar(int x, int y, int width, int height) {
        GuiVerticalProgressBar o;
        o = new GuiVerticalProgressBar(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, height, this);
        objectList.add(o);
        return o;
    }

    public GuiVerticalCustomValuesBar newGuiVerticalCustomValuesBar(int x, int y, int width, int height, Float[] values) {
        GuiVerticalCustomValuesBar o;
        o = new GuiVerticalCustomValuesBar(screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, width, height, this, values);
        objectList.add(o);
        return o;
    }

	/*public void drawHoveringText(List list, int x, int y, FontRenderer fontRenderer, GuiContainerEln cont) {
        drawHoveringText(list, screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, Minecraft.getMinecraft().fontRenderer);
	}*/

    public void add(IGuiObject o) {
        o.translate(screen.width / 2 - xSize / 2, screen.height / 2 - ySize / 2);
        objectList.add(o);
    }

    public void remove(IGuiObject o) {
        o.translate(-screen.width / 2 + xSize / 2, -screen.height / 2 + ySize / 2);
        objectList.remove(o);
    }

    /*
    void flushRemove() {
        for(IGuiObject o : removeList) {
            o.translate(-screen.width / 2 + xSize / 2, -screen.height / 2 + ySize / 2);
            objectList.remove(o);
        }
        removeList.clear();
    }

    ArrayList<IGuiObject> removeList = new ArrayList<IGuiObject>();*/
    ArrayList<IGuiObject> objectList = new ArrayList<IGuiObject>();

    void draw(int x, int y, float f) {
        screen.drawDefaultBackground();
        if (background != null)
            UtilsClient.drawGuiBackground(background, screen, xSize, ySize);
        else {
            UtilsClient.bindTexture(helperTexture);
            int px = 0, py = 0;
            px += (screen.width - xSize) / 2;
            py += (screen.height - ySize) / 2;

            screen.drawRect(px + 2, py + 2, px + xSize - 2, py + ySize - 2, 0xFFC6C6C6);

            screen.drawRect(px + 4, py, px + xSize - 4, py + 1, 0xFF000000);
            screen.drawRect(px + 4, py + 1, px + xSize - 4, py + 3, 0xFFFFFFFF);
            screen.drawRect(px + 4, py + ySize - 1, px + xSize - 4, py + ySize - 0, 0xFF000000);
            screen.drawRect(px + 4, py + ySize - 3, px + xSize - 4, py + ySize - 1, 0xFF555555);

            screen.drawRect(px, py + 4, px + 1, py + ySize - 4, 0xFF000000);
            screen.drawRect(px + 1, py + 4, px + 3, py + ySize - 4, 0xFFFFFFFF);
            screen.drawRect(px + xSize - 1, py + 4, px + xSize - 0, py + ySize - 4, 0xFF000000);
            screen.drawRect(px + xSize - 3, py + 4, px + xSize - 1, py + ySize - 4, 0xFF555555);

            GL11.glColor3f(1f, 1f, 1f);

            screen.drawTexturedModalRect(px, py, 0, 0, 4, 4);
            screen.drawTexturedModalRect(px + xSize - 4, py, 4, 0, 4, 4);
            screen.drawTexturedModalRect(px, py + ySize - 4, 0, 4, 4, 4);
            screen.drawTexturedModalRect(px + xSize - 4, py + ySize - 4, 4, 4, 4, 4);
        }

        for (IGuiObject o : objectList) {
            o.idraw(x, y, f);
        }
    }

    public void drawTexturedModalRect(int x, int y, int u, int v, int width, int height) {
        x += (screen.width - xSize) / 2;
        y += (screen.height - ySize) / 2;
        screen.drawTexturedModalRect(x, y, u, v, width, height);
    }

    public void drawRect(int x0, int y0, int x1, int y1, int color) {
        int dx = (screen.width - xSize) / 2;
        int dy = (screen.height - ySize) / 2;
        screen.drawRect(x0 + dx, y0 + dy, x1 + dx, y1 + dy, color);
    }

    IGuiObject[] objectListCopy() {
        IGuiObject[] cpy = new IGuiObject[objectList.size()];
        for (int idx = 0; idx < cpy.length; idx++) {
            cpy[idx] = objectList.get(idx);
        }
        return cpy;
    }

    protected void keyTyped(char key, int code) {
        for (IGuiObject o : objectListCopy()) {
            o.ikeyTyped(key, code);
        }
    }

    protected void mouseClicked(int x, int y, int code) {
        for (IGuiObject o : objectListCopy()) {
            o.imouseClicked(x, y, code);
        }
    }

    protected void mouseMove(int x, int y) {
        for (IGuiObject o : objectList) {
            o.imouseMove(x, y);
        }
    }

    protected void mouseMovedOrUp(int x, int y, int witch) {
        for (IGuiObject o : objectList) {
            o.imouseMovedOrUp(x, y, witch);
        }
    }

    public void drawString(int x, int y, int color, String str) {
        Minecraft.getMinecraft().fontRenderer.drawString(str, screen.width / 2 - xSize / 2 + x, screen.height / 2 - ySize / 2 + y, color);
    }

    public void draw2(int x, int y) {
        for (IGuiObject o : objectList) {
            o.idraw2(x, y);
        }
    }

    public void drawHoveringText(List par1List, int x, int y, FontRenderer font) {
        if (!par1List.isEmpty()) {
            if (screen instanceof GuiContainer) {
                x -= (screen.width - xSize) / 2;
                y -= (screen.height - ySize) / 2;
            }

            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            int textWidth = 0;
            Iterator iterator = par1List.iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();
                int l = font.getStringWidth(s);

                if (l > textWidth) {
                    textWidth = l;
                }
            }

            if (screen instanceof GuiContainer) {
                if (x + (screen.width - xSize) / 2 + textWidth + 30 > screen.width) {
                    x -= textWidth + 24;
                }
            } else {
                if (x + textWidth + 30 > screen.width) {
                    x -= textWidth + 24;
                }
            }

            int i1 = x + 12;
            int j1 = y - 12;
            int k1 = 8;

            if (par1List.size() > 1) {
                k1 += 2 + (par1List.size() - 1) * 10;
            }
/*
            if (i1 + k > this.width) {
                i1 -= 28 + k;
            }

            if (j1 + k1 + 6 > this.height) {
                j1 = this.height - k1 - 6;
            }*/


            int l1 = -267386864;
            drawGradientRect(i1 - 3, j1 - 4, i1 + textWidth + 3, j1 - 3, l1, l1);
            drawGradientRect(i1 - 3, j1 + k1 + 3, i1 + textWidth + 3, j1 + k1 + 4, l1, l1);
            drawGradientRect(i1 - 3, j1 - 3, i1 + textWidth + 3, j1 + k1 + 3, l1, l1);
            drawGradientRect(i1 - 4, j1 - 3, i1 - 3, j1 + k1 + 3, l1, l1);
            drawGradientRect(i1 + textWidth + 3, j1 - 3, i1 + textWidth + 4, j1 + k1 + 3, l1, l1);
            int i2 = 1347420415;
            int j2 = (i2 & 16711422) >> 1 | i2 & -16777216;
            drawGradientRect(i1 - 3, j1 - 3 + 1, i1 - 3 + 1, j1 + k1 + 3 - 1, i2, j2);
            drawGradientRect(i1 + textWidth + 2, j1 - 3 + 1, i1 + textWidth + 3, j1 + k1 + 3 - 1, i2, j2);
            drawGradientRect(i1 - 3, j1 - 3, i1 + textWidth + 3, j1 - 3 + 1, i2, i2);
            drawGradientRect(i1 - 3, j1 + k1 + 2, i1 + textWidth + 3, j1 + k1 + 3, j2, j2);

            for (int k2 = 0; k2 < par1List.size(); ++k2) {
                String s1 = (String) par1List.get(k2);
                font.drawStringWithShadow(s1, i1, j1, -1);

                if (k2 == 0) {
                    j1 += 2;
                }

                j1 += 10;
            }

            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        }
    }

    public void drawGradientRect(int par1, int par2, int par3, int par4, int par5, int par6) {
        float f = (float) (par5 >> 24 & 255) / 255.0F;
        float f1 = (float) (par5 >> 16 & 255) / 255.0F;
        float f2 = (float) (par5 >> 8 & 255) / 255.0F;
        float f3 = (float) (par5 & 255) / 255.0F;
        float f4 = (float) (par6 >> 24 & 255) / 255.0F;
        float f5 = (float) (par6 >> 16 & 255) / 255.0F;
        float f6 = (float) (par6 >> 8 & 255) / 255.0F;
        float f7 = (float) (par6 & 255) / 255.0F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        // TODO(1.10): Just no. Use LibLib.
        //  Tessellator tessellator = Tessellator.instance;
//        tessellator.startDrawingQuads();
//        tessellator.setColorRGBA_F(f1, f2, f3, f);
//        tessellator.addVertex((double) par3, (double) par2, 0);
//        tessellator.addVertex((double) par1, (double) par2, 0);
//        tessellator.setColorRGBA_F(f5, f6, f7, f4);
//        tessellator.addVertex((double) par1, (double) par4, 0);
//        tessellator.addVertex((double) par3, (double) par4, 0);
        tessellator.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public int getHoveringTextWidth(List<String> comment, FontRenderer fontRenderer) {
        int strWidth = 0;
        for (String str : comment) {
            int size = fontRenderer.getStringWidth(str);
            if (size > strWidth) strWidth = size;
        }
        return strWidth + 5;
    }

    public int getHoveringTextHeight(List<String> comment, FontRenderer fontRenderer) {
        return comment.size() * 9 - 4;
    }

    public void drawProcess(int x, int y, float value) {
        UtilsClient.bindTexture(helperTexture);
        drawTexturedModalRect(x, y, 8, 0, (int) (22), 16);
        drawTexturedModalRect(x, y, 8 + 22, 0, (int) (22 * value), 16);
    }

    protected void drawSlot(int x, int y) {
        UtilsClient.bindTexture(slotSkin);

        drawTexturedModalRect(x - 1, y - 1, 55, 16, 73 - 55, 34 - 16);
    }
}
