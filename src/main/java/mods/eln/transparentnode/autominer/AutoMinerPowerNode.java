package mods.eln.transparentnode.autominer;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.GhostNode;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public class AutoMinerPowerNode extends GhostNode {
    private Direction front;

    private AutoMinerElement element;

    @Override
    public void initializeFromThat(Direction front, EntityLivingBase entityLiving, ItemStack itemStack) {
        this.front = front;

        connect();
    }

    @Override
    public int getSideConnectionMask(Direction directionA, LRDU lrduA) {
        if (element == null) return 0;
        if (directionA != front) return 0;
        if (lrduA != LRDU.Down) return 0;
        return maskElectricalPower;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA) {
        return null;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB) {
        if (element == null) return null;
        return element.inPowerLoad;
    }

    @Override
    public void initializeFromNBT() {
    }

    void setElement(AutoMinerElement e) {
        this.element = e;
    }

    public void writeToNBT(net.minecraft.nbt.NBTTagCompound nbt, String str) {
        front.writeToNBT(nbt, str + "front");
    }

    public void readFromNBT(net.minecraft.nbt.NBTTagCompound nbt, String str) {
        front = Direction.readFromNBT(nbt, str + "front");
    }
}
