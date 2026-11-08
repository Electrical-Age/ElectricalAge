package mods.eln.transparentnode.waterturbine;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.SoundCommand;
import net.minecraft.world.entity.player.Player;

import java.io.DataInputStream;
import java.io.IOException;

public class WaterTurbineRender extends TransparentNodeElementRender {

    public WaterTurbineRender(TransparentNodeEntity tileEntity,
                              TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (WaterTurbineDescriptor) descriptor;

    }

    Coordonate waterCoord, waterCoordRight;
    /** The flow's direction (-1..1, eased), then the wheel's speed: direction x the root of power, eased again. */
    final mods.eln.anim.Smooth dirFilter = new mods.eln.anim.Smooth(0.5);
    mods.eln.anim.Spin wheel;
    WaterTurbineDescriptor descriptor;
    boolean soundPlaying = false;

    @Override
    public void draw() {
        // front.glRotateXnRef();

        front.glRotateXnRef();
        descriptor.draw(wheel().deg());

    }


    mods.eln.anim.Spin wheel() {
        if (wheel == null) wheel = new mods.eln.anim.Spin(descriptor.speed, 1).randomPhase();
        return wheel;
    }

    public void refresh(float deltaT) {
        float flowDir = waterCoord.getMeta() > waterCoordRight.getMeta() ? 1 : -1;
        if (Utils.isWater(waterCoord) == false)
            flowDir = 0;

        dirFilter.target(flowDir).step(deltaT);
        float before = wheel().deg();
        wheel.speed(dirFilter.get() * Math.sqrt(powerFactor)).step(deltaT);
        float speed = wheel.speedNow();

        if ((int) (wheel.deg() / 45) != (int) (before / 45) && soundPlaying == false) {   // (each paddle passing)
            play(new SoundCommand(descriptor.soundName)
                .mulVolume(descriptor.nominalVolume * (0.007f + 0.2f * speed * speed), 1.1f));
            //SoundClient.playFromBlock(tileEntity.getLevel(),coord.x, coord.y, coord.z, descriptor.soundName,1,1,5,15);
            soundPlaying = true;
        } else
            soundPlaying = false;
    }


    private float water;
    private float powerFactor;

    @Override
    public boolean cameraDrawOptimisation() {

        return false;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {

        super.networkUnserialize(stream);
        try {
            powerFactor = stream.readFloat();
        } catch (IOException e) {

            e.printStackTrace();
        }

        waterCoord = this.descriptor.getWaterCoordonate(tileEntity.getLevel());
        waterCoord.setWorld(tileEntity.getLevel());
        waterCoord.applyTransformation(front, coordonate());
        waterCoordRight = new Coordonate(waterCoord);
        waterCoordRight.setWorld(tileEntity.getLevel());
        waterCoordRight.move(front.right());
    }
}
