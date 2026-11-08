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
        if (rot1 != null && rot1.getFloat("originX") != 0) rollX = rot1.getFloat("originX");
    }

    /** The work rolls' x (rot1's .txt origin), where a piece passing through is flattened; null: flattened with progress. */
    private Float rollX;

    class PlateMachineDescriptorHandle {
        float counter = 0;
        final RcInterpolator interpolator = new RcInterpolator(0.5f);
        float itemCounter = 0;
    }

    @Override
    Object newDrawHandle() {
        return new PlateMachineDescriptorHandle();
    }

    @Override
    void draw(ElectricalMachineRender render, Object handleO, ItemEntity inEntity, ItemEntity outEntity, float powerFactor, float processState) {
        PlateMachineDescriptorHandle handle = (PlateMachineDescriptorHandle) handleO;

        main.draw();
        rot1.draw(handle.counter, 0f, 0f, -1f);
        rot2.draw(handle.counter, 0f, 0f, 1f);
        if (rot3 != null) rot3.draw(handle.counter * rot3.getFloat("ratio"), 0f, 0f, 1f);
        if (rot4 != null) rot4.draw(handle.counter * rot4.getFloat("ratio"), 0f, 0f, -1f);

        // the piece being rolled (Baughn 2026-11-07: more than a turning item): it runs from the entry table through the
        // rolls toward the exit table as the job goes on, flattened and drawn out where it has passed the roll gap
        float p = render == null ? 0 : render.progress.shown();
        if (p <= 0) UtilsClient.drawEntityItem(inEntity, inX, inY, inZ, handle.itemCounter, 1f);
        else {
            float x = mods.eln.anim.ItemMotion.lerp(inX, outX, mods.eln.anim.ItemMotion.ease(p, 0.05f, 0.95f));
            float rolled = rollX == null ? p : mods.eln.anim.ItemMotion.ease(x, rollX - 0.06f, rollX + 0.06f);
            float z = mods.eln.anim.ItemMotion.lerp(inZ, outZ, mods.eln.anim.ItemMotion.ease(p, 0.05f, 0.95f));
            mods.eln.anim.ItemMotion.draw(inEntity, x, inY, z, 0, 1 + 0.5f * rolled, 1 - 0.7f * rolled, 1);
        }
        UtilsClient.drawEntityItem(outEntity, outX, outY, outZ, -handle.itemCounter + 139f, 1f);
    }
    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, ItemEntity inEntity, ItemEntity outEntity, float powerFactor, float processState) {
        PlateMachineDescriptorHandle handle = (PlateMachineDescriptorHandle) handleO;
        handle.interpolator.setTarget(powerFactor);
        handle.interpolator.step(deltaT);
        handle.counter += deltaT * handle.interpolator.get() * 360;
        while (handle.counter >= 360f) handle.counter -= 360;

        handle.itemCounter += deltaT * 90;
        while (handle.itemCounter >= 360f) handle.itemCounter -= 360;
    }
    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}
