package mods.eln.node;

import mods.eln.misc.Direction;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumSkyBlock;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Node extends NodeBase {


    private int lastLight = 0;

    public void setLightValue(int light) {
        if (light > 15) light = 15;
        if (light < 0) light = 0;
        if (lastLight != light) {
            lastLight = light;
            coordinate.world().setLightFor(EnumSkyBlock.BLOCK, coordinate.pos, light);
            setNeedPublish(true);
        }

    }

    public int getLightValue() {
        return lastLight;
    }


    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        lastLight = nbt.getByte("lastLight");
    }


    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("lastLight", (byte) lastLight);
    }

    boolean oldSendedRedstone = false;

    public void publishSerialize(DataOutputStream stream) {
        super.publishSerialize(stream);

        try {
            boolean redstone = canConnectRedstone();
            stream.writeByte(lastLight | (redstone ? 0x10 : 0x00));
            if (redstone != oldSendedRedstone)
                needNotify = true;
            oldSendedRedstone = redstone;
        } catch (IOException e) {

            e.printStackTrace();
        }
    }


    public NodeBlockEntity getEntity() {
        return (NodeBlockEntity) coordinate.world().getTileEntity(coordinate.pos);
    }

    public int isProvidingWeakPower(Direction side) {
        return 0;
    }

    public boolean canConnectRedstone() {
        return false;
    }


}
