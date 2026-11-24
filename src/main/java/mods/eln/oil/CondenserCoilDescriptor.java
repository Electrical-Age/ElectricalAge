package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.FrontDrawnRender;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Condenser Coil (refinery M2, CondenserCoilElement): a 1x1x2 vertical drum, its node the lower block and the upper
 * one a ghost. Its model (mods.eln.cad.models.CondenserCoil) is drawn full height from the lower block.
 */
public class CondenserCoilDescriptor extends TransparentNodeDescriptor implements FrontDrawnRender.Drawn {
    final Obj3D obj;

    public CondenserCoilDescriptor(String name, String model) {
        super(name, CondenserCoilElement.class, FrontDrawnRender.class);
        obj = Eln.obj.getObj(model);
        var g = new mods.eln.ghost.GhostGroup();
        g.addElement(0, 1, 0);
        setGhostGroup(g);
    }

    @Override
    public void draw() {
        if (obj != null) for (var p : obj.parts()) p.draw();
    }

    /** It may stand on a pipe (its condensate drains down through its underside), or on another EA device. */
    @Override
    public boolean isFloor(net.minecraft.world.level.block.Block below) {
        return super.isFloor(below) || onDeviceOrPipe(below);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.addAll(CondenserCoilElement.tooltip());
    }
}
