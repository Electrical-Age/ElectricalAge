package mods.eln.transparentnode.powercapacitor;

import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataInputStream;

public class PowerCapacitorRender extends TransparentNodeElementRender {

    public PowerCapacitorDescriptor descriptor;
    private CableRenderType renderPreProcess;

    public PowerCapacitorRender(TransparentNodeEntity tileEntity,
                                TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (PowerCapacitorDescriptor) descriptor;

    }


    @Override
    public void draw() {


        descriptor.draw();

    }

    @Override
    public void refresh(float deltaT) {

    }


    @Override
    public void networkUnserialize(DataInputStream stream) {

        super.networkUnserialize(stream);


	/*	try {


		} catch (IOException e) {
			
			e.printStackTrace();
		}*/

    }

    TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(2, 64, this);

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {

        return new PowerCapacitorGui(player, inventory, this);
    }


}
