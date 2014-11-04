package mods.eln.sixnode.lampsocket;

import mods.eln.Translator;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.HelperStdContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.node.six.SixNodeElementInventory;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class LampSocketGuiDraw extends GuiContainerEln {

	
    private SixNodeElementInventory inventory;
    LampSocketRender lampRender;
    GuiButton buttonGrounded,buttonSupplyType;
    GuiTextFieldEln channel;
    GuiVerticalTrackBar alphaZ;
    public LampSocketGuiDraw(EntityPlayer player, IInventory inventory,LampSocketRender lampRender)
    {
        super(new LampSocketContainer(player, inventory,lampRender.lampSocketDescriptor));
        this.inventory = (SixNodeElementInventory) inventory;
        this.lampRender = lampRender;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	int x = 0;
    	if(lampRender.descriptor.alphaZMax == lampRender.descriptor.alphaZMin)
    	{
    		x = - 0;
    		buttonSupplyType = newGuiButton(x+176/2-140/2,8,140,"");
    		channel = newGuiTextField(x+176/2-140/2 +1, 34, 140);
    	}
    	else
    	{
    		buttonSupplyType = newGuiButton(x+176/2-140/2-12,8,136,"");
    		channel = newGuiTextField(x+176/2-140/2-11, 34, 135);
    	}
    
    	buttonGrounded = newGuiButton(x+176/2-30,-2000,60,"");
    	
    	channel.setComment(0,Translator.translate("eln.core.tile.supply.specifychannel"));
    	
    	channel.setText(lampRender.channel);
    	alphaZ  = newGuiVerticalTrackBar(176 - 8 - 20, 8, 20, 69);
    	alphaZ.setRange(lampRender.descriptor.alphaZMin, lampRender.descriptor.alphaZMax);
    	alphaZ.setStepIdMax(200);
    	alphaZ.setValue(lampRender.alphaZ);
    	
    	if(lampRender.descriptor.alphaZMax == lampRender.descriptor.alphaZMin)
    	{
    		alphaZ.setVisible(false);
    	}
    }
    

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
    	if(object == buttonGrounded)
    	{
    		lampRender.clientSetGrounded(!lampRender.getGrounded());
    	}
    	else if(object == buttonSupplyType){
    		lampRender.clientSend(LampSocketElement.tooglePowerSupplyType);
    	}
    	else if(object == channel){
    		lampRender.clientSetString((byte) LampSocketElement.setChannel, channel.getText());
    	}
    	else if(object == alphaZ)
    	{
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
		if(lampRender.grounded)
			buttonGrounded.displayString = Translator.translate("eln.core.tile.lamp.status.parallel");
		else
			buttonGrounded.displayString = Translator.translate("eln.core.tile.lamp.status.serial");
		
		if(lampRender.poweredByLampSupply){
			buttonSupplyType.displayString = Translator.translate("eln.core.tile.lamp.poweredbysupply");
			channel.setVisible(true);
			if(inventory.getStackInSlot(LampSocketContainer.cableSlotId) == null)
				channel.setComment(1,"\u00a74"+Translator.translate("eln.core.tile.lamp.cableslotempty"));	
			else if(lampRender.isConnectedToLampSupply)
				channel.setComment(1,"\u00a72"+Translator.translate("eln.core.tile.lamp.connectedto")+" " + lampRender.channel);
			else
				channel.setComment(1,"\u00a74" +  lampRender.channel + Translator.translate("eln.core.tile.lamp.notinrange"));	
		}
		else{
			channel.setVisible(false);
			buttonSupplyType.displayString = Translator.translate("eln.core.tile.lamp.poweredbycable");
		}
		
		alphaZ.setComment(0, Translator.translate("eln.core.tile.lamp.orientation")+": " + (int)alphaZ.getValue()+"\u00B0");
	}
}
