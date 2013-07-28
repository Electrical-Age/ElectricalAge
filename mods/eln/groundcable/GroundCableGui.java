package mods.eln.groundcable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
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
import net.minecraft.network.packet.Packet250CustomPayload;

public class GroundCableGui extends GuiContainerEln{

	public GroundCableGui(EntityPlayer player, IInventory inventory,GroundCableRender render) {
		super(new GroundCableContainer(player, inventory));
		this.render = render;
	}


	GuiButton toogleSwitch;
	GuiTextFieldEln setUmin,setUmax;
	GroundCableRender render;
	
	enum SelectedType{none,min,max};

	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();


	}
	


	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelperContainer(this, 176, 166-52,8,84-52);
	}


}
