package mods.eln.sixnode.powerinductorsix;

import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.world.entity.player.Player;

public class PowerInductorSixRender extends SixNodeElementRender {

    public PowerInductorSixDescriptor descriptor;
    private CableRenderType renderPreProcess;


    public PowerInductorSixRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (PowerInductorSixDescriptor) descriptor;
    }

    @Override
    public void draw() {
        front.left().glRotateOnX();
        descriptor.draw();
    }

}
