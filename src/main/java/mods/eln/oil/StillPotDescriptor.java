package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.FrontDrawnRender;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Still Pot (refinery M2, todo #591): a 2x2x2 kettle for batch distillation, heated from outside like the Large
 * Boiler (StillPotElement). Its model (mods.eln.cad.models.StillPot) is drawn full size from its origin block, the
 * front bottom left; the other seven blocks are ghosts (GhostCells).
 */
public class StillPotDescriptor extends TransparentNodeDescriptor implements FrontDrawnRender.Drawn {
    final Obj3D obj;

    public StillPotDescriptor(String name, String model) {
        super(name, StillPotElement.class, FrontDrawnRender.class);
        obj = Eln.obj.getObj(model);
        var g = new mods.eln.ghost.GhostGroup();
        g.addRectangle(0, 1, 0, 1, 0, 1);
        g.removeElement(0, 0, 0);
        setGhostGroup(g);
    }

    @Override
    public void draw() {
        if (obj != null) for (var p : obj.parts()) p.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.addAll(StillPotElement.tooltip());
    }
}
