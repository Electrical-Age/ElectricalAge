package mods.eln.electricalmath;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.electricalfurnace.ElectricalFurnaceContainer;
import mods.eln.electricalfurnace.ElectricalFurnaceRender;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
import mods.eln.gui.GuiTextFieldEln.GuiTextFieldElnObserver;
import mods.eln.misc.Utils;
import mods.eln.node.TransparentNodeElementInventory;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet250CustomPayload;

public class ElectricalMathGui extends GuiContainerEln {

    public ElectricalMathGui(EntityPlayer player, IInventory inventory, ElectricalMathRender render)
    {
        super(new ElectricalMathContainer(null,player, inventory));
        //this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
    }

	GuiTextFieldEln expression;
	ElectricalMathRender render;
	
	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 176, 166 - 38, 8, 84 - 38);
	}
   
	@Override
	public void initGui() {
		super.initGui();

		expression = newGuiTextField(8, 8, 176 - 16);
		expression.setText(render.expression);
		expression.setObserver(this);
		expression.setComment(new String[]{"Output Voltage Formula"});
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		super.guiObjectEvent(object);
		if(object == expression) {
			render.clientSetString(ElectricalMathElement.setExpressionId, expression.getText());
		}
	}
	
	@Override
	protected void postDraw(float f, int x, int y) {
		super.postDraw(f, x, y);
		int c;
		int redNbr = 0;
		ItemStack stack = render.inventory.getStackInSlot(ElectricalMathContainer.restoneSlotId);
		if(stack != null)
			redNbr = stack.stackSize;
		if(!expression.getText().equals(render.expression)) {
			c = 0xFF404040;
			helper.drawString(8, 29, c, "Waiting for completion...");
		} else if(expression.getText().equals("")) {
			c = 0xFF404040;
			helper.drawString(8, 29, c, "Needs an equation!");
		} else if(render.equationIsValid) {
			if(redNbr >= render.redstoneRequired)
				c = 0xFF108F00;
			else
				c = 0xFFFF0000;
			helper.drawString(8, 29, c, "Redstone required : " + render.redstoneRequired);
		} else {
			c = 0xFFFF0000;
			helper.drawString(8, 29, c, "Invalid equation!");
		}
	}
}
