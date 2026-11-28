package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Still Pot (refinery M2, todo #591): a 2x2x2 kettle for batch distillation, heated from outside like the Large
 * Boiler (StillPotElement). Its model (mods.eln.cad.models.StillPot) is drawn full size from its origin block, the
 * front bottom left; the other seven blocks are ghosts (GhostCells). Its sight glass shows the pot's liquid
 * (SightGlassRender).
 */
public class StillPotDescriptor extends TransparentNodeDescriptor implements SightGlassRender.Glazed {
    final Obj3D obj;

    public StillPotDescriptor(String name, String model) {
        super(name, StillPotElement.class, SightGlassRender.class);
        obj = Eln.obj.getObj(model);
        var g = new mods.eln.ghost.GhostGroup();
        g.addRectangle(0, 1, 0, 1, 0, 1);
        g.removeElement(0, 0, 0);
        setGhostGroup(g);
    }

    @Override
    public void draw() {
        var main = obj == null ? null : obj.getPart("main");
        if (main != null) main.draw();
    }

    /** The liquid in its sight glass, to the pot's real level (Lashtear 2026-11-28; Baughn: do it properly). */
    @Override
    public void drawLevel(SightGlass glass) {
        glass.draw(obj);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.addAll(StillPotElement.tooltip());
        mods.eln.client.ItemTooltips.detail(list,
            mods.eln.i18n.I18N.tr("Expands right, back, and up from placement. Working charge: up to 24 B."),
            mods.eln.i18n.I18N.tr("Front temperature signal: 1 V per 100 °C."));
    }
}
