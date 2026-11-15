package mods.eln.sixnode.lampsocket;


import mods.eln.compat.WorldCompat;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.item.LampDescriptor;
import mods.eln.item.LampDescriptor.Type;
import mods.eln.misc.*;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import mods.eln.gl.GL11;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LampSocketRender extends SixNodeElementRender {

    LampSocketDescriptor lampSocketDescriptor = null;
    LampSocketDescriptor descriptor;

    boolean grounded = true;
    public boolean poweredByLampSupply;

    /** The hanging lamp's swing; at rest (indoors, nothing passing) the chain and head bake into the chunk. */
    final LampSwing swing = new LampSwing();
    /** Under the sky or right next to it (LampSwing.OUTDOOR_SKY_LIGHT); looked up about once a second, not per frame. */
    boolean outdoors = false;
    float skyTimeout = 0;

    public LampSwing swing() {
        return swing;
    }

    public boolean outdoors() {
        return outdoors;
    }

    List entityList = new ArrayList();
    float entityTimout = 0;

    public String channel;
    LampDescriptor lampDescriptor = null;
    float alphaZ;
    byte light, oldLight = -1;
    int paintColor = 15;

    public boolean isConnectedToLampSupply;


    /** Blocks the lamp's model reaches out of its block (SixNodeRender's bounding box). */
    @Override
    public int renderReach() {
        // (a street light stands on ghost blocks above its own)
        return Math.max(descriptor.render.reach(), descriptor.hasGhostGroup() ? 3 : 0);
    }

    public LampSocketRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (LampSocketDescriptor) descriptor;
        lampSocketDescriptor = (LampSocketDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw(); //Colored cable only

        GL11.glRotatef(descriptor.initialRotateDeg, 1.f, 0.f, 0.f);
        descriptor.render.draw(this, UtilsClient.distanceFromClientPlayer(this.tileEntity));
    }
    @Override
    public void refresh(float deltaT) {
        if (descriptor.render instanceof LampSocketSuspendedObjRender) {
            float dt = deltaT;

            entityTimout -= dt;
            if (entityTimout < 0) {
                entityList = tileEntity.getLevel().getEntitiesOfClass(Entity.class, new Coordonate(tileEntity.getBlockPos().getX(), tileEntity.getBlockPos().getY() - 2, tileEntity.getBlockPos().getZ(), tileEntity.getLevel()).getAxisAlignedBB(2));
                entityTimout = 0.1f;
            }

            for (Object o : entityList) {
                Entity e = (Entity) o;
                float eFactor = 0;
                if (e instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow)
                    eFactor = 1f;
                if (e instanceof net.minecraft.world.entity.LivingEntity)
                    eFactor = 4f;

                if (eFactor == 0)
                    continue;
                swing.push(e.getDeltaMovement().x, e.getDeltaMovement().z, eFactor, dt);
            }

            skyTimeout -= dt;
            if (skyTimeout < 0) {
                outdoors = WorldCompat.getSavedLightValue(tileEntity.getLevel(), net.minecraft.world.level.LightLayer.SKY, tileEntity.getBlockPos().getX(), tileEntity.getBlockPos().getY(), tileEntity.getBlockPos().getZ()) >= LampSwing.OUTDOOR_SKY_LIGHT;
                skyTimeout = LampSwing.SKY_CHECK_SECONDS * (1f + 0.25f * (float) Math.random());   // (lamps loaded together do not all look in one frame)
            }

            // Indoors a lamp hangs still (Baughn, todo #473): no wind, so a swing dies out and the lamp bakes.
            if (outdoors) swing.wind((float) UtilsClient.getWeather(tileEntity.getLevel()) * 0.9f + 0.1f, dt);
            swing.step(dt, outdoors);
        }
    }
    void setLight(byte newLight) {
        light = newLight;
        if (lampDescriptor != null && lampDescriptor.type == Type.eco && oldLight != -1 && oldLight < 9 && light >= 9) {
            float rand = (float) Math.random();
            if (rand > 0.1f)
                play(new SoundCommand("eln:neon_lamp").mulVolume(0.7f, 1.0f + (rand / 6.0f)).smallRange());
            else
                play(new SoundCommand("eln:neon_lfnoise").mulVolume(0.2f, 1f).verySmallRange());
        }
        oldLight = light;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            Byte b;
            b = stream.readByte();
            grounded = (b & (1 << 6)) != 0;

            ItemStack lampStack = Utils.unserialiseItemStack(stream);
            lampDescriptor = (LampDescriptor) Utils.getItemObject(lampStack);
            alphaZ = stream.readFloat();

            poweredByLampSupply = stream.readBoolean();
            channel = stream.readUTF();

            isConnectedToLampSupply = stream.readBoolean();

            setLight(stream.readByte());
            paintColor = stream.readByte();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) throws IOException {
        super.serverPacketUnserialize(stream);
        setLight(stream.readByte());
    }

    public boolean getGrounded() {
        return grounded;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        if ((lrdu == front && !descriptor.cableFront)
            || (lrdu == front.left() && !descriptor.cableLeft)
            || (lrdu == front.right() && !descriptor.cableRight)
            || (lrdu == front.inverse() && !descriptor.cableBack))
            return null;
        return mods.eln.registry.ElnDeviceRegistry.lowVoltageCableDescriptor.render; // built-in cable
    }

    public void clientSetGrounded(boolean value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            preparePacketForServer(stream);

            stream.writeByte(LampSocketElement.setGroundedId);
            stream.writeByte(value ? 1 : 0);

            sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return descriptor.cameraOpt;
    }
}
