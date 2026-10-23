package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 26.x (Baughn, Q4): the flare stack burns whatever is piped (or poured) into it, for nothing: the sink that keeps a
 * column from backing up. The flame and smoke depend on the fluid: gas burns clean, heavy oil and residue smoke
 * black, steam and water just vent. Placeholder model.
 */
public class FlareStackDescriptor extends TransparentNodeDescriptor {
    public static final int TANK_MB = 4000;
    /** mB/s it burns at most. */
    public static final double BURN_RATE = 250;
    final Obj3D obj;

    public FlareStackDescriptor(String name, String model) {
        super(name, FlareStackElement.class, FlareStackRender.class);
        obj = Eln.obj.getObj(model);
    }

    void draw() {
        if (obj != null) for (var p : obj.parts()) p.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("Burns any fluid piped in, up to %.0f mB/s, for nothing", BURN_RATE));
    }
}
