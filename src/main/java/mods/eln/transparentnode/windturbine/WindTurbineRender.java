package mods.eln.transparentnode.windturbine;

import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDUMask;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.SoundCommand;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

public class WindTurbineRender extends TransparentNodeElementRender {
    private boolean soundPlaying = false;

    private CableRenderType renderPreProcess;
    private final LRDUMask eConn = new LRDUMask();

    /** The power share, eased over two seconds; the rotor turns at the descriptor's speed times its square root. */
    private final mods.eln.anim.Smooth powerFactorFilter = new mods.eln.anim.Smooth(2);
    private final mods.eln.anim.Spin rotor;
    /** The halo on the nacelle: half a second on, two and a half off, each turbine a little off the others. */
    private final mods.eln.anim.Blink halo;
    private final WindTurbineDescriptor descriptor;

    private float wind;
    private float powerFactor;

    public WindTurbineRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (WindTurbineDescriptor) descriptor;
        rotor = new mods.eln.anim.Spin(this.descriptor.speed, 0).randomPhase();
        Random rand = new Random();
        float off = 2.5f + rand.nextFloat() * 2.5f / 15f, on = 0.5f + rand.nextFloat() * 0.5f / 15f;
        halo = mods.eln.anim.Blink.random(on, on, off, off);
    }

    @Override
    public void draw() {
        renderPreProcess = drawCable(Direction.YN, descriptor.cable.render, eConn, renderPreProcess);
        front.glRotateXnRef();
        descriptor.draw(rotor.deg(), halo.on());
    }

    public void refresh(float deltaT) {
        powerFactorFilter.target(powerFactor).step(deltaT);
        float root = (float) Math.sqrt(Math.max(0, powerFactorFilter.get()));
        float before = rotor.deg();
        rotor.speed(root).step(deltaT);
        // a blade passing (every 120 degrees) swishes
        if (rotor.deg() % 120 > 45 && before % 120 <= 45 && !soundPlaying) {
            this.play(new SoundCommand(descriptor.soundName)
                .mediumRange()
                .mulBlockAttenuation(2)
                .applyNominalVolume(descriptor.nominalVolume)
                .mulVolume(0.007f + root, 1f + root / 1.3f));
            soundPlaying = true;
        } else {
            soundPlaying = false;
        }
        halo.step(deltaT);
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
