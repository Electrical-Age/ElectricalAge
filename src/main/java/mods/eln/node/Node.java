package mods.eln.node;


import mods.eln.compat.WorldCompat;
import mods.eln.misc.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LightLayer;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Node extends NodeBase {


    private int lastLight = 0;

    /** Middle-click (pick block): the item for what the player points at (hit: in this block, or null). */
    public net.minecraft.world.item.ItemStack pickItem(@org.jetbrains.annotations.Nullable net.minecraft.world.phys.BlockHitResult hit, net.minecraft.world.level.LevelReader level) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    public void setLightValue(int light) {
        if (light > 15) light = 15;
        if (light < 0) light = 0;
        if (lastLight != light) {
            lastLight = light;
            NodeBlock.setLight(coordonate.world(), coordonate.pos(), light);
            // 26.x: the block's light emission comes from the BE (NodeBlock.getLightEmission); re-check it
            if (coordonate.getBlockExist()) coordonate.world().getLightEngine().checkBlock(coordonate.pos());
            setNeedPublish(true);
        }

    }

    public int getLightValue() {
        return lastLight;
    }


    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);

        lastLight = nbt.getByteOr("lastLight", (byte) 0);
        if (lastLight > 0 && coordonate.world() != null) NodeBlock.setLight(coordonate.world(), coordonate.pos(), lastLight);
    }


    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putByte("lastLight", (byte) lastLight);
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
        net.minecraft.world.level.block.entity.BlockEntity e = WorldCompat.getBlockEntity(coordonate.world(), coordonate.x, coordonate.y, coordonate.z);
        return e instanceof NodeBlockEntity ? (NodeBlockEntity) e : null;
    }

    public int isProvidingWeakPower(Direction side) {
        return 0;
    }

    public boolean canConnectRedstone() {
        return false;
    }


}
