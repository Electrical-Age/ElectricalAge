package mods.eln.hub;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

import org.lwjgl.opengl.GL11;

import mods.eln.electricaltimeout.ElectricalTimeoutElement;
import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.HelperStdContainer;
import mods.eln.gui.HelperStdContainerSmall;
import mods.eln.gui.IGuiObject;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

public class HubGui extends GuiContainerEln {

	int w, w2;
	int h, v0, v1, v2, v3, v4, v5, v6, v7;
	int v, h0, h1, h2, h3, h4, h5, h6, h7;

	public HubGui(EntityPlayer player, IInventory inventory, HubRender render) {
		super(new HubContainer(player, inventory));
		this.render = render;

	}

	GuiButtonEln connectionGridToggle[] = new GuiButtonEln[6];

	HubRender render;

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

		/*for (int idx = 0; idx < 6; idx++) {
			connectionGridToggle[idx] = newGuiButton(6, 6 + idx * 20, 50, "");
		}*/

		w = 5;
		h = 80;
		v = 80;
		w2 = (v - 5 * w) / 2;
		v0 = 134/2-v/2;
		h0 = 176/2-h/2;
		v1 = v0 + w2;
		v2 = v1 + w;
		v3 = v2 + w;
		v4 = v3 + w;
		v5 = v4 + w;
		v6 = v5 + w;
		v7 = v6 + w2;
		h1 = h0 + w2;
		h2 = h1 + w;
		h3 = h2 + w;
		h4 = h3 + w;
		h5 = h4 + w;
		h6 = h5 + w;
		h7 = h6 + w2;

	}

	@Override
	public void guiObjectEvent(IGuiObject object) {
		super.guiObjectEvent(object);
		/*for (int idx = 0; idx < 6; idx++) {
			if (object == connectionGridToggle[idx]) {
				render.clientSetByte(HubElement.clientConnectionGridToggle,
						(byte) idx);
			}
		}*/
	}

	@Override
	protected void preDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.preDraw(f, x, y);
	/*	for (int idx = 0; idx < 6; idx++) {
			connectionGridToggle[idx].displayString = render.connectionGrid[idx] ? "is on"
					: "is off";
		}*/
	}

	@Override
	protected void postDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.postDraw(f, x, y);

		for (int idx = 0; idx < 6; idx++) {
			if (!render.connectionGrid[idx]) {
				drawConnection(idx, 0xFF808080);
			}
		}

		for (int idx = 0; idx < 6; idx++) {
			if (render.connectionGrid[idx]) {
				drawConnection(idx, 0xFF000000);
			}
		}

	}

	void drawConnection(int id, int color) {
		switch (id) {
		case 2:
			helper.drawRect(h0, v5, h2, v6, color);
			helper.drawRect(h1, v5, h2, v7, color);
			break;
		case 3:
			helper.drawRect(h5, v0, h6, v2, color);
			helper.drawRect(h5, v1, h7, v2, color);
			break;
		case 1:
			helper.drawRect(h0, v1, h2, v2, color);
			helper.drawRect(h1, v0, h2, v2, color);
			break;
		case 0:
			helper.drawRect(h5, v5, h6, v7, color);
			helper.drawRect(h5, v5, h7, v6, color);
			break;
		case 4:
			helper.drawRect(h0, v3, h7, v4, color);
			break;
		case 5:
			helper.drawRect(h3, v0, h4, v7, color);
			break;
		}

	}

    protected void mouseClicked(int x, int y, int code)
    {    	
        super.mouseClicked(x, y, code);
    	x -= width/2 -xSize/2;
    	y -= height/2 -ySize/2;
    	
    	if(isInto(x,y,h0, v5, h2, v6) || isInto(x,y,h1, v5, h2, v7))
		render.clientSetByte(HubElement.clientConnectionGridToggle,(byte) 2);
    	if(isInto(x,y,h5, v0, h6, v2) || isInto(x,y,h5, v1, h7, v2))
    		render.clientSetByte(HubElement.clientConnectionGridToggle,(byte) 3);
    	if(isInto(x,y,h0, v1, h2, v2) || isInto(x,y,h1, v0, h2, v2))
    		render.clientSetByte(HubElement.clientConnectionGridToggle,(byte) 1);
    	if(isInto(x,y,h5, v5, h6, v7) || isInto(x,y,h5, v5, h7, v6))
    		render.clientSetByte(HubElement.clientConnectionGridToggle,(byte) 0);
    	if(isInto(x,y,h0, v3, h7, v4))
    		render.clientSetByte(HubElement.clientConnectionGridToggle,(byte) 4);
    	if(isInto(x,y,h3, v0, h4, v7))
    		render.clientSetByte(HubElement.clientConnectionGridToggle,(byte) 5);

  
    }
	
    
    boolean isInto(int x, int y,int x0,int y0,int x1,int y1){
    	return x >= x0 && x < x1 && y >= y0 && y < y1;
    }
	
	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 176, 216, 8, 134);
	}

}
