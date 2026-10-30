package mods.eln.oil;

import mods.eln.gl.GL11;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 26.x (notes/oil): the boiler. Water piped in (Baughn: piped only), heat from EA's thermal network on any side
 * (electric heater, fuel heat furnace, heat furnace); above 100 °C the heat above boiling turns water into steam:
 * 175 J per mB of water (67.5 g: 20 -> 100 °C plus 2257 J/g), and 1 mB of water makes 67.5 mB of steam (design v2:
 * EA steam is 1 mg per mB). Steam is drawn off through the fluid capability (pipes) to EA's steam turbine.
 * Placeholder model: the electric furnace.
 */
public class BoilerDescriptor extends TransparentNodeDescriptor {
    public static final double J_PER_MB_WATER = 175, STEAM_PER_WATER = 67.5, BOIL_C = 100;
    final Obj3D obj;

    public BoilerDescriptor(String name, String model) {
        super(name, BoilerElement.class, BoilerRender.class);
        obj = Eln.obj.getObj(model);
        cableTerminals = mods.eln.cable.CableTerminals.FRONT;   // the phase signal's plate (mods.eln.cad.models.Boiler)
    }

    /** A boiler may also stand on another EA device (a steam turbine: its dome port feeds the turbine's inlet below). */
    @Override
    public boolean isFloor(net.minecraft.world.level.block.Block below) {
        return super.isFloor(below) || below instanceof mods.eln.node.transparent.TransparentNodeBlock
            || below instanceof mods.eln.node.voxel.VoxelNodeBlock;   // (or on a pipe: its dome port feeds the pipe below)
    }

    /** The dial's span: DIAL_MAX_C at the end of its 270 degree sweep. */
    static final float DIAL_MAX_C = 300;

    void draw() {
        draw(0.5f, 20);
    }

    /** The boiler, its gauge glass's water column at fill (0..1), its dial at celsius. */
    void draw(float fill, float celsius) {
        if (obj == null) return;
        for (String part : new String[]{"main", "body", "furnace"}) {
            var p = obj.getPart(part);
            if (p != null) p.draw();
        }
        var level = obj.getPart("level");
        if (level != null && fill > 0.01f) {
            // (the column is modelled half the glass high, about its foot)
            float ox = level.getFloat("originX"), oy = level.getFloat("originY"), oz = level.getFloat("originZ");
            GL11.glPushMatrix();
            GL11.glTranslatef(ox, oy, oz);
            GL11.glScalef(1, 2 * Math.min(1, fill), 1);
            GL11.glTranslatef(-ox, -oy, -oz);
            level.draw();
            GL11.glPopMatrix();
        }
        var needle = obj.getPart("needle");
        if (needle != null) needle.draw(-135 + 270 * Math.max(0, Math.min(1, celsius / DIAL_MAX_C)), 1, 0, 0);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add("Water in by pipe (right flank), heat in from a heater or furnace on the back or a flank, steam out by pipe");
        list.add("Front: signal high while the water is above half the gauge glass");
        list.add("1 bucket of water + 175 kJ: 67.5 buckets of steam");
    }
}
