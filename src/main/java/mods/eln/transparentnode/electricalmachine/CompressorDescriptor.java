package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.item.ItemEntity;

public class CompressorDescriptor extends ElectricalMachineDescriptor {
    private float tyOn;
    private float tyOff;
    /** Where the input and output items are drawn (block units from the centre; the model's move.txt may say). */
    private float inX = -0.35f, inY = 0.04f, inZ = 0.3f, outX = 0.35f, outY = 0.04f, outZ = 0.3f;

    private Obj3DPart main;
    private Obj3DPart move;

    public CompressorDescriptor(String name, Obj3D obj, double nominalU, double nominalP, double maximalU,
                                ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;   // (the restyled models' floor terminal plates, 2026-11-07)

        if (obj != null) {
            main = obj.getPart("main");
            move = obj.getPart("move");
            initLeds(obj);
            if (move != null) {
                tyOn = move.getFloat("tyon");
                tyOff = move.getFloat("tyoff");
                // (the restyled presses, 2026-11-07: the input on the die, the output on a tray beside it)
                if (move.getFloat("inx") != 0 || move.getFloat("iny") != 0) {
                    inX = move.getFloat("inx");
                    inY = move.getFloat("iny");
                    inZ = move.getFloat("inz");
                    outX = move.getFloat("outx");
                    outY = move.getFloat("outy");
                    outZ = move.getFloat("outz");
                }
            }
        }
    }

    class CompressorDescriptorHandle {
        final RcInterpolator interpolator = new RcInterpolator(0.25f);
        float itemCounter = 0f;
        /** The piece's turn when the ram met it: it stops turning while pressed. */
        float pressedAt = 0f;
    }

    @Override
    Object newDrawHandle() {
        return new CompressorDescriptorHandle();
    }

    @Override
    public float volumeForRunningSound(float processState, float powerFactor) {
        if (processState < 0.1)
            return 0f;
        else if (processState < 0.3)
            return super.volumeForRunningSound(processState, powerFactor) * (processState - 0.1f) * 5f;
        else
            return super.volumeForRunningSound(processState, powerFactor);
    }

    @Override
    void draw(ElectricalMachineRender render, Object handleO, ItemEntity inEntity, ItemEntity outEntity,
              float powerFactor, float processState) {
        CompressorDescriptorHandle handle = (CompressorDescriptorHandle) handleO;

        // the piece on the die squashes under the ram as it comes down (Baughn 2026-11-07: more than a turning item):
        // flatter and wider with the ram's travel, still while pressed; it springs back as the ram lifts for the next
        float ram = (float) Math.sqrt(Math.max(0, handle.interpolator.get()));
        float squash = mods.eln.anim.ItemMotion.ease(ram, 0.15f, 1);
        float sy = 1 - 0.7f * squash, sxz = 1 + 0.35f * squash;
        float turn = squash > 0.01f ? handle.pressedAt : handle.itemCounter;
        mods.eln.anim.ItemMotion.draw(inEntity, inX, inY, inZ, turn, sxz, sy, sxz);
        UtilsClient.drawEntityItem(outEntity, outX, outY, outZ, -handle.itemCounter + 139f, 1f);

        main.draw();
        GL11.glTranslatef(0f, tyOff + ram * (tyOn - tyOff), 0f);
        move.draw();
    }
    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, ItemEntity inEntity, ItemEntity outEntity, float powerFactor, float processState) {
        CompressorDescriptorHandle handle = (CompressorDescriptorHandle) handleO;
        handle.interpolator.setTarget(processState);
        handle.interpolator.step(deltaT);

        if (Math.sqrt(Math.max(0, handle.interpolator.get())) < 0.15) {   // (free: it turns; under the ram: held)
            handle.itemCounter += deltaT * 90;
            while (handle.itemCounter >= 360f) handle.itemCounter -= 360;
            handle.pressedAt = handle.itemCounter;
        }
    }
    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}
