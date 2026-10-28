package mods.eln.transparentnode.heatfurnace;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HeatFurnaceRender extends TransparentNodeElementRender {
    /** The fire's real burn level (it lags the setting) and whether it's being lit. */
    public float fire;
    public boolean lighting;


    double temperature;
    float gainSyncValue = -1234, temperatureTargetSyncValue = -1234;
    boolean gainSyncNew = false, temperatureTargetSyncNew = false;
    short power;

    public boolean controleExternal, takeFuel;

    HeatFurnaceDescriptor descriptor;

    Coordonate coord;
    PhysicalInterpolator interpolator;

    float counter = 0;


    boolean boot = true;

    ItemEntity entityItemIn;

    public HeatFurnaceRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (HeatFurnaceDescriptor) descriptor;
        interpolator = new PhysicalInterpolator(0.4f, 8.0f, 0.9f, 0.2f);
        coord = new Coordonate(tileEntity);
        // the fire roars and crackles as hard as it burns
        addLoopedSound(new mods.eln.sound.LoopedSound("eln:heat_furnace_fire", coordonate()) {
            @Override
            public float getVolume() {
                return 0.2f * Math.min(1, fire);
            }

            @Override
            public float getPitch() {
                return 0.72f + 0.08f * Math.min(1, fire);
            }
        });
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        descriptor.draw(interpolator.get());

        if (entityItemIn != null)
            drawEntityItem(entityItemIn, -0.1, -0.30, 0, counter, 0.8f);
    }

    @Override
    public void refresh(float deltaT) {
        if (!Utils.isPlayerAround(tileEntity.getLevel(), coord.getAxisAlignedBB(1)))
            interpolator.setTarget(0f);
        else
            interpolator.setTarget(1f);
        interpolator.step(deltaT);

        counter += deltaT * 60;
        if (counter >= 360f)
            counter -= 360;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            controleExternal = stream.readBoolean();
            takeFuel = stream.readBoolean();

            temperature = stream.readShort() / NodeBase.networkSerializeTFactor;
            float readF;
            readF = stream.readFloat();
            if (gainSyncValue != readF || controleExternal) {
                gainSyncValue = readF;
                gainSyncNew = true;
            }
            readF = stream.readFloat();
            if (temperatureTargetSyncValue != readF || controleExternal) {
                temperatureTargetSyncValue = readF;
                temperatureTargetSyncNew = true;
            }

            power = stream.readShort();
            fire = stream.readFloat();
            lighting = stream.readBoolean();

            entityItemIn = unserializeItemStackToEntityItem(stream, entityItemIn);

            if (boot) {
                coord.move(front);
                //coord.move(front);
                boot = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientToogleControl() {
        clientSendId(HeatFurnaceElement.unserializeToogleControlExternalId);
    }

    public void clientToogleTakeFuel() {
        clientSendId(HeatFurnaceElement.unserializeToogleTakeFuelId);
    }

    public void clientSetGain(float value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(HeatFurnaceElement.unserializeGain);
            stream.writeFloat(value);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientSetTemperatureTarget(float value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(HeatFurnaceElement.unserializeTemperatureTarget);
            stream.writeFloat(value);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }
}
