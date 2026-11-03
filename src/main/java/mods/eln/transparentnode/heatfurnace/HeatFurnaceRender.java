package mods.eln.transparentnode.heatfurnace;

import mods.eln.misc.Direction;
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
    /** Smoke up its stack (burning, nothing piped on its flue's flange). */
    boolean smoking;
    private float smokeDue;


    double temperature;
    float gainSyncValue = -1234, temperatureTargetSyncValue = -1234;
    boolean gainSyncNew = false, temperatureTargetSyncNew = false;
    short power;

    public boolean controleExternal, takeFuel;

    HeatFurnaceDescriptor descriptor;

    /** The fuel slot's stack (sent by the element; not drawn: the firebox is closed). */
    ItemEntity entityItemIn;

    /** A slow flicker of the fire's brightness (radians). */
    float flicker = 0;

    public HeatFurnaceRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (HeatFurnaceDescriptor) descriptor;
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
        // the fire's brightness by its power against the furnace's highest (all chambers), with a slight flicker
        float k = (float) (power / descriptor.maxPower()) * (0.94f + 0.06f * (float) Math.sin(flicker) * (float) Math.sin(flicker * 2.3f));
        descriptor.draw(fire, k);
    }

    @Override
    public void refresh(float deltaT) {
        flicker = (flicker + deltaT * 7) % (float) (20 * Math.PI);
        var level = tileEntity.getLevel();
        if (!smoking || level == null) return;
        // (smoke out of the stack's open flange, more of it the harder it burns)
        smokeDue += deltaT * (2 + 10 * fire);
        var p = tileEntity.getBlockPos();
        while (smokeDue >= 1) {
            smokeDue -= 1;
            double jx = (Math.random() - 0.5) * 0.12, jz = (Math.random() - 0.5) * 0.12;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, p.getX() + 0.5 + jx, p.getY() + 1.02, p.getZ() + 0.5 + jz, 0, 0.06, 0);
        }
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
            smoking = stream.readBoolean();
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
