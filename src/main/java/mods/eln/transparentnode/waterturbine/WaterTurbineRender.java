package mods.eln.transparentnode.waterturbine;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.RcInterpolator;
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
    RcInterpolator powerFactorFilter = new RcInterpolator(1);
    RcInterpolator dirFilter = new RcInterpolator(0.5f);
    WaterTurbineDescriptor descriptor;
    float alpha = (float) (Math.random() * 360);
    boolean soundPlaying = false;

    @Override
    public void draw() {
        // front.glRotateXnRef();

        front.glRotateXnRef();
        descriptor.draw(alpha);

    }


    public void refresh(float deltaT) {
        float flowDir = waterCoord.getMeta() > waterCoordRight.getMeta() ? 1 : -1;
        if (Utils.isWater(waterCoord) == false)
            flowDir = 0;

        dirFilter.setTarget(flowDir);
        dirFilter.step(deltaT);
        powerFactorFilter.setTarget((float) (dirFilter.get() * Math.sqrt(powerFactor)));
        powerFactorFilter.step(deltaT);

        // Utils.println(powerFactorFilter.get());
        float alphaN_1 = alpha;
        alpha += deltaT * descriptor.speed * powerFactorFilter.get();
        if (alpha > 360)
            alpha -= 360;
        if (alpha < 0)
            alpha += 360;

        if ((int) (alpha / 45) != (int) (alphaN_1 / 45) && soundPlaying == false) {
            Coordonate coord = coordonate();
            play(new SoundCommand(descriptor.soundName)
                .mulVolume(descriptor.nominalVolume * (0.007f + 0.2f * (float) powerFactorFilter.get() * (float) powerFactorFilter.get()),
                    1.1f));
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
