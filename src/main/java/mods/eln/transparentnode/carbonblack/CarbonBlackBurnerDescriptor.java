package mods.eln.transparentnode.carbonblack;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The Carbon Black Burner (the rubber chain's oil route, 2026-11-07): heavy oil, residue or crude, fed into its blue IN
 * flange on the back, burns starved of air in a firebrick box (the furnace process); the soot collects as Carbon Black,
 * which it pushes into a hopper under it (it may stand on one), or a right-click takes. It gives no useful heat (LabAgent: starved, less
 * than the fuel furnace), only black smoke up its stack. No power. Model mods.eln.cad.models.CarbonBlackBurner.
 */
public class CarbonBlackBurnerDescriptor extends TransparentNodeDescriptor {
    final Obj3D obj;
    /** Its fuel tank (mB), how fast it burns (mB/s), and fuel a Carbon Black (mB). */
    public static final int TANK_MB = 4000;
    public static final double BURN_MB_PER_S = 20;
    public static final int MB_PER_CARBON = 500;
    /** Carbon Black it holds before it stops (a stack). */
    public static final int MAX_CARBON = 64;
    /** Its IN flange on the back face (px, model frame): mods.eln.cad.models.CarbonBlackBurner. */
    public static final int IN_Y = 4, IN_Z = 8;
    /** Its stack's top (px, model frame), where the smoke comes out. */
    public static final double STACK_X = 9, STACK_Z = 8;

    public CarbonBlackBurnerDescriptor(String name, String model) {
        super(name, CarbonBlackBurnerElement.class, CarbonBlackBurnerRender.class);
        obj = Eln.obj.getObj(model);
    }

    void draw(boolean burning) {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
        mods.eln.anim.Led.draw(obj.getPart("fire"), burning, 1, 1, 1, 0.25f, 0.2f, 0.2f);   // (the sight port's fire)
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(false);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Burns heavy oil, residue or crude\nstarved of air, fed into the blue IN\nflange on its back: Carbon Black,\none a 500 mB. Pushes it into a\nhopper under it; right-click takes it.").split("\n"));
    }
}
