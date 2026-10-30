package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A pipe tank (Baughn 2026-10-29, the shaft redo): one vessel with a vapour port on top and on each side up high, and a
 * liquid port underneath and on each side down low, so it serves as a steam drum (boiler steam in, dry steam off the
 * top, water back down), a separator (wet steam in, steam off the top, water off the bottom) or a hotwell (condensate
 * in, a pump drawing from the bottom). A sight glass on its front shows the water. Over its burst pressure it bursts.
 * Model: mods.eln.cad.models.Tank.
 */
public class TankDescriptor extends TransparentNodeDescriptor {
    final double volume, workingPa, burstPa;
    final Obj3D obj;

    public TankDescriptor(String name, double volume, double workingPa, double burstPa, String model) {
        super(name, TankElement.class, TankRender.class);
        this.volume = volume;
        this.workingPa = workingPa;
        this.burstPa = burstPa;
        this.obj = Eln.obj.getObj(model);
    }

    /** Volume (m^3) of so many buckets of water (EA units). */
    public static double bucketsVolume(double buckets) {
        return buckets * 1000 * mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER / mods.eln.sim.fluid.Water.RHO_L;
    }

    /** The vessel, and the water in its sight glass filled to `fill` (0..1). */
    void draw(float fill) {
        if (obj == null) return;
        for (String part : new String[]{"shell", "glass", "ports"}) {
            var m = obj.getPart(part);
            if (m != null) m.draw();
        }
        drawLevel(fill);
    }

    /** A box of tanks, scaled to it by the render: its shell and sight glass, not the ports (they'd stretch). */
    void drawBox(float fill) {
        if (obj == null) return;
        for (String part : new String[]{"shell", "glass"}) {
            var m = obj.getPart(part);
            if (m != null) m.draw();
        }
        drawLevel(fill);
    }

    void drawPart(String name) {
        var p = obj == null ? null : obj.getPart(name);
        if (p != null) p.draw();
    }

    void drawLevel(float fill) {
        if (obj == null) return;
        var l = obj.getPart("level");
        if (l == null || fill <= 0.005f) return;
        // (the glass runs 2..14 px: scale the water about its foot)
        float foot = 2 / 16f - 0.5f;
        mods.eln.gl.GL11.glPushMatrix();
        mods.eln.gl.GL11.glTranslatef(0, foot, 0);
        mods.eln.gl.GL11.glScalef(1, Math.min(1, fill), 1);
        mods.eln.gl.GL11.glTranslatef(0, -foot, 0);
        l.draw();
        mods.eln.gl.GL11.glPopMatrix();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("Holds %.0f buckets of water. Works at up to %.0f bar; bursts over %.0f.",
            volume * mods.eln.sim.fluid.Water.RHO_L / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER / 1000, workingPa / 1e5, burstPa / 1e5));
        list.add("Steam off the top and the high side ports; water off the bottom and the low ones.");
        list.add("A steam drum, a separator for wet steam, or a hotwell for a pump to draw from.");
    }
}
