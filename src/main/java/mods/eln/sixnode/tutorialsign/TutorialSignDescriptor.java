package mods.eln.sixnode.tutorialsign;


import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.world.item.ItemStack;

public class TutorialSignDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    private Obj3DPart main, light, halo;

    public TutorialSignDescriptor(String name, Obj3D obj) {
        super(name, TutorialSignElement.class, null);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            light = obj.getPart("light");
            halo = obj.getPart("halo");
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }
// 26.x port: setupColor() removed (client rendering; redone in P3)
    // 26.x port: draw() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)
}
