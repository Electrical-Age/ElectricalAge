package mods.eln.sixnode.powercapacitorsix;

import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class PowerCapacitorSixRender extends SixNodeElementRender {

	public PowerCapacitorSixDescriptor descriptor;
	private CableRenderType renderPreProcess;

    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

    public PowerCapacitorSixRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (PowerCapacitorSixDescriptor) descriptor;
	}

	@Override
	public void draw() {
		descriptor.draw();
	}

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new PowerCapacitorSixGui(player, inventory, this);
	}
}
