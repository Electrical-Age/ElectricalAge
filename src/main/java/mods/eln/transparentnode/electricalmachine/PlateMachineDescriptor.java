package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.UtilsClient;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.item.ItemEntity;

public class PlateMachineDescriptor extends ElectricalMachineDescriptor {
    private Obj3DPart main;
    private Obj3DPart rot1;
    private Obj3DPart rot2;

    /** The 230V mill's backup rolls (2026-11-07): rot3 under rot1 turning as rot2, rot4 over rot2 turning as rot1, at the
     *  work rolls' surface speed (their .txt ratio). */
    private Obj3DPart rot3, rot4;
    /** Where the items are drawn (block units; the restyled models' rot1.txt says). */
    private float inX = -0.35f, inY = 0.1f, inZ = 0f, outX = 0.35f, outY = 0.1f, outZ = 0f;

    public PlateMachineDescriptor(String name, Obj3D obj, double nominalU, double nominalP, double maximalU,
                                  ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;   // (the restyled models' floor terminal plates, 2026-11-07)

        if (obj != null) {
            main = obj.getPart("main");
            rot1 = obj.getPart("rot1");
            rot2 = obj.getPart("rot2");
            rot3 = obj.getPart("rot3");
            rot4 = obj.getPart("rot4");
            if (rot1 != null && (rot1.getFloat("inx") != 0 || rot1.getFloat("iny") != 0)) {
                inX = rot1.getFloat("inx");
                inY = rot1.getFloat("iny");
                inZ = rot1.getFloat("inz");
                outX = rot1.getFloat("outx");
                outY = rot1.getFloat("outy");
                outZ = rot1.getFloat("outz");
            }
        }
        initLeds(obj);
        if (rot1 != null) rollX = rot1.getFloat("originX");   // (0 is a place: the 230V's rolls stand at the centre)
    }

    /** The work rolls' x (rot1's .txt origin), where a piece passing through is flattened; null: flattened with progress. */
    private Float rollX;

    /** The items' size (blocks across). */
    static final float ITEM_SIZE = 0.25f;

    class PlateMachineDescriptorHandle {
        /** The work rolls: a turn a second at full power, easing over half a second; the 230V's backup rolls at their
         *  ratio (their own spins: a multiple of a wrapped angle jumped at each wrap); */
        final mods.eln.anim.Spin rolls = new mods.eln.anim.Spin(360, 0.5);
        final mods.eln.anim.Spin backup3 = new mods.eln.anim.Spin(360 * (rot3 == null ? 1 : rot3.getFloat("ratio")), 0.5),
            backup4 = new mods.eln.anim.Spin(360 * (rot4 == null ? 1 : rot4.getFloat("ratio")), 0.5);
    }

    @Override
    Object newDrawHandle() {
        return new PlateMachineDescriptorHandle();
    }

    @Override
    void draw(ElectricalMachineRender render, Object handleO, ItemEntity inEntity, ItemEntity outEntity, float powerFactor, float processState) {
        PlateMachineDescriptorHandle handle = (PlateMachineDescriptorHandle) handleO;

        main.draw();
        handle.rolls.draw(rot1, 0f, 0f, -1f);
        handle.rolls.draw(rot2, 0f, 0f, 1f);
        handle.backup3.draw(rot3, 0f, 0f, 1f);
        handle.backup4.draw(rot4, 0f, 0f, -1f);

        // the piece being rolled (Baughn 2026-11-07: more than a turning item). It slides from the entry table to the roll
        // gap; then the ingot goes in on one side while the plate comes out on the other, at once (Baughn 2026-11-08: it
        // stayed an ingot until it was all through): the ingot shortens toward the gap as the plate grows from it, longer
        // and thinner, toward the exit table, where the plates made lie. Flat (the item-frame view): standing plates
        // turned on edge and through each other (AdventurAgent).
        float p = render == null ? 0 : render.progress.shown();
        float gap = rollX == null ? (inX + outX) / 2 : rollX, dir = Math.signum(outX - inX);
        float slide = mods.eln.anim.ItemMotion.ease(p, 0, 0.15f), t = mods.eln.anim.ItemMotion.ease(p, 0.15f, 0.95f);
        float z = mods.eln.anim.ItemMotion.lerp(inZ, outZ, 0.5f * slide);
        if (t < 0.999f) {   // the ingot: at the entry table, then shortening into the gap
            float len = ITEM_SIZE * (1 - t);
            float x = mods.eln.anim.ItemMotion.lerp(inX, gap - dir * ITEM_SIZE / 2, slide);
            if (t > 0) x = gap - dir * len / 2;
            mods.eln.anim.ItemMotion.drawFlat(inEntity, x, inY, z, 0, ITEM_SIZE, 1 - t, 1, 2f);
        }
        if (t > 0) {   // the plate it becomes, growing out of the gap (the output's item if there is one yet)
            float len = ITEM_SIZE * 1.4f * t;
            ItemEntity plate = outEntity != null && !outEntity.getItem().isEmpty() ? outEntity : inEntity;
            mods.eln.anim.ItemMotion.drawFlat(plate, gap + dir * len / 2, inY, z, 0, ITEM_SIZE, 1.4f * t, 1, 0.6f);
        }
        mods.eln.anim.ItemMotion.drawFlat(outEntity, outX, outY, outZ, 0, ITEM_SIZE, 1.4f, 1, 0.6f);
    }
    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, ItemEntity inEntity, ItemEntity outEntity, float powerFactor, float processState) {
        PlateMachineDescriptorHandle handle = (PlateMachineDescriptorHandle) handleO;
        handle.rolls.speed(powerFactor).step(deltaT);
        handle.backup3.speed(powerFactor).step(deltaT);
        handle.backup4.speed(powerFactor).step(deltaT);
    }
    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}
