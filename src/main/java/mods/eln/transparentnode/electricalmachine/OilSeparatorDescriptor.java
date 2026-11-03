package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.RecipesList;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

/**
 * 26.x oil (design: notes/oil): the tar sand separator, hot-water extraction. A macerator-style machine (for now on
 * the macerator's model) whose recipe turns tar sand into sand and puts 100 mB of crude into its tank; it stops when
 * the tank can't take the next 100 mB. Pipes (Pipez, Energized Power...) drain the tank through the fluid capability,
 * and its outlet on the back pushes the crude into whatever takes it there (OUTLET_MB_PER_S).
 */
public class OilSeparatorDescriptor extends MaceratorDescriptor {
    public static final int CRUDE_PER_ITEM = 100;
    public static final int TANK_MB = 4000;
    /** How fast its outlet pushes crude out (mB/s): far more than it makes. */
    public static final double OUTLET_MB_PER_S = 200;

    public OilSeparatorDescriptor(String name, String modelName, double nominalU, double nominalP, double maximalU,
                                  ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, modelName, nominalU, nominalP, maximalU, thermal, cable, recipe);
        ElementClass = OilSeparatorElement.class;
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;   // mods.eln.cad.models.TarSandSeparator terminal plates
        var obj = mods.eln.Eln.obj.getObj(modelName);
        oil = obj == null ? null : obj.getPart("oil");
    }

    /** The oil in its basin (the model's part oil, full, its origin at the basin's floor). */
    private final mods.eln.misc.Obj3D.Obj3DPart oil;

    /** The machine, its basin's oil risen to the crude tank's fill (Baughn 2026-11-03), published as render.basin. */
    @Override
    void draw(ElectricalMachineRender render, Object handle, net.minecraft.world.entity.item.ItemEntity inEntity,
              net.minecraft.world.entity.item.ItemEntity outEntity, float powerFactor, float processState) {
        if (oil != null && render != null && render.basin > 0.005f) {   // (no render: the item)
            // (a sliver over the floor at the least, not in its plane)
            float share = 0.05f + 0.95f * Math.min(1, render.basin), oy = oil.getFloat("originY");
            mods.eln.gl.GL11.glPushMatrix();
            mods.eln.gl.GL11.glTranslatef(0, oy, 0);
            mods.eln.gl.GL11.glScalef(1, share, 1);
            mods.eln.gl.GL11.glTranslatef(0, -oy, 0);
            oil.draw();
            mods.eln.gl.GL11.glPopMatrix();
        }
        super.draw(render, handle, inEntity, outEntity, powerFactor, processState);
    }

    @Override
    boolean publishesBasin() {
        return true;
    }
}
