package mods.eln.sixnode.lampsocket;

import mods.eln.gui.*;
import mods.eln.node.six.SixNodeElementInventory;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import static mods.eln.i18n.I18N.tr;

/*
public class MachineGuiClientExample extends GuiScreen {
	MachineBlockEntity tileEntity;
    public MachineGuiClientExample (InventoryPlayer inventoryPlayer,	NodeBlockEntity tileEntity) {
	    //the container is instanciated and passed to the superclass for handling
	    super();
	    this.tileEntity = (MachineBlockEntity) tileEntity;
	}
}
*/

public class LampSocketGuiDraw extends GuiContainerEln {

    private SixNodeElementInventory inventory;
    LampSocketRender lampRender;
    GuiButton buttonGrounded,buttonSupplyType;
    GuiTextFieldEln channel;
    GuiVerticalTrackBar alphaZ;

    public LampSocketGuiDraw(EntityPlayer player, IInventory inventory, LampSocketRender lampRender) {
        super(new LampSocketContainer(player, inventory, lampRender.lampSocketDescriptor));
        this.inventory = (SixNodeElementInventory) inventory;
        this.lampRender = lampRender;
    }
    
    public void initGui() {
    	super.initGui();
    	int x = 0;
    	if (lampRender.descriptor.alphaZMax == lampRender.descriptor.alphaZMin) {
    		x = - 0;
    		buttonSupplyType = newGuiButton(x + 176 / 2 - 140 / 2, 8, 140, "");
    		channel = newGuiTextField(x + 176 / 2 - 140 / 2 + 1, 34, 140);
    	} else {
    		buttonSupplyType = newGuiButton(x + 176 / 2 - 140 / 2 - 12, 8, 136, "");
    		channel = newGuiTextField(x + 176 / 2 - 140 / 2 - 11, 34, 135);
    	}
    
    	buttonGrounded = newGuiButton(x + 176 / 2 - 30, -2000, 60, "");

		channel.setComment(0, tr("Specify the supply channel"));

		channel.setText(lampRender.channel);
    	alphaZ  = newGuiVerticalTrackBar(176 - 8 - 20, 8, 20, 69);
    	alphaZ.setRange(lampRender.descriptor.alphaZMin, lampRender.descriptor.alphaZMax);
    	alphaZ.setStepIdMax(200);
    	alphaZ.setValue(lampRender.alphaZ);
    	
    	if (lampRender.descriptor.alphaZMax == lampRender.descriptor.alphaZMin) {
    		alphaZ.setVisible(false);
    	}
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
    	if (object == buttonGrounded) {
    		lampRender.clientSetGrounded(!lampRender.getGrounded());
    	} else if (object == buttonSupplyType) {
    		lampRender.clientSend(LampSocketElement.tooglePowerSupplyType);
    	} else if (object == channel) {
    		lampRender.clientSetString((byte) LampSocketElement.setChannel, channel.getText());
    	} else if (object == alphaZ) {
    		lampRender.clientSetFloat(LampSocketElement.setAlphaZId, alphaZ.getValue());
    	}
    }

	/*
    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
            fontRenderer.drawString("Tiny", 8, 6, 4210752);
            fontRenderer.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }*/

	@Override
	protected GuiHelperContainer newHelper() {
		return new HelperStdContainer(this);
	}

	@Override
	protected void preDraw(float f, int x, int y) {
		super.preDraw(f, x, y);
		if (lampRender.grounded)
			buttonGrounded.displayString = tr("Parallel");
		else
			buttonGrounded.displayString = tr("Serial");
		
		if (lampRender.poweredByLampSupply) {
			buttonSupplyType.displayString = tr("Powered by Lamp Supply");
			channel.setVisible(true);
			if (inventory.getStackInSlot(LampSocketContainer.cableSlotId) == null)
				channel.setComment(1, "§4" + tr("Cable slot empty"));
			else if (lampRender.isConnectedToLampSupply)
				channel.setComment(1, "§2" + tr("connected to " + lampRender.channel));
			else
				channel.setComment(1, "§4" + tr("%1$ is not in range!", lampRender.channel));
		} else {
			channel.setVisible(false);
			buttonSupplyType.displayString = tr("Powered by cable");
		}

		alphaZ.setComment(0, tr("Orientation: %1$°", (int) alphaZ.getValue()));
	}
}
