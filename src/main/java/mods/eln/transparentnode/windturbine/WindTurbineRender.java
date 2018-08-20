package mods.eln.transparentnode.windturbine;

import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.SoundCommand;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

public class WindTurbineRender extends TransparentNodeElementRender {
    private float haloBlink_OnTime = 0.5f;
    private float haloBlink_OffTime = 2.5f;

    private boolean soundPlaying = false;
    private float haloBlinkCounter = 0.f;
    private boolean haloState = false;

    private CableRenderType renderPreProcess;
    private final LRDUMask eConn = new LRDUMask();

    private final RcInterpolator powerFactorFilter = new RcInterpolator(2);
    private final WindTurbineDescriptor descriptor;
    private float alpha = (float) (Math.random() * 360);

    private final TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(0, 64, this);
    private float wind;
    private float powerFactor;

    public WindTurbineRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (WindTurbineDescriptor) descriptor;
        Random rand = new Random();
        this.haloBlinkCounter = rand.nextFloat() * this.haloBlink_OffTime;
        this.haloBlink_OffTime += rand.nextFloat() * this.haloBlink_OffTime / 15.f;
        this.haloBlink_OnTime += rand.nextFloat() * this.haloBlink_OnTime / 15.f;
    }

    @Override
    public void draw() {
        renderPreProcess = drawCable(Direction.YN, descriptor.cable.render, eConn, renderPreProcess);
        front.glRotateXnRef();
        descriptor.draw(alpha, haloState);
    }

    public void refresh(float deltaT) {
        powerFactorFilter.setTarget(powerFactor);
        powerFactorFilter.step(deltaT);
        float alphaN_1 = alpha;
        alpha += deltaT * descriptor.speed * Math.sqrt(powerFactorFilter.get());
        if (alpha > 360) alpha -= 360;
        if (alpha % 120 > 45 && alphaN_1 % 120 <= 45 && !soundPlaying) {
            this.play(new SoundCommand(descriptor.soundName)
                .mediumRange()
                .mulBlockAttenuation(2)
                .applyNominalVolume(descriptor.nominalVolume)
                .mulVolume((0.007f + 1f * (float) Math.sqrt(powerFactorFilter.get())),
                    1f + (float) Math.sqrt(powerFactorFilter.get()) / 1.3f));

            soundPlaying = true;
        } else {
            soundPlaying = false;
        }
        haloBlinkCounter += deltaT;
        if (!haloState) {
            if (haloBlinkCounter > haloBlink_OffTime) {
                haloBlinkCounter -= haloBlink_OffTime;
                haloState = true;
            }
        } else {
            if (haloBlinkCounter > haloBlink_OnTime) {
                haloBlinkCounter -= haloBlink_OnTime;
                haloState = false;
            }
        }
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            wind = stream.readFloat();
            powerFactor = stream.readFloat();
            eConn.deserialize(stream);
        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}
