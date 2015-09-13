package mods.eln.sixnode.resistor;

import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class ResistorRender extends SixNodeElementRender {

    public ResistorDescriptor descriptor;
    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);
    private CableRenderType renderPreProcess;

    public ResistorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ResistorDescriptor) descriptor;
    }

    @Override
    public void draw() {
        GL11.glRotatef(90, 1, 0, 0);
        front.glRotateOnX();
        descriptor.draw();
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new ResistorGui(player, inventory, this);
    }
}
