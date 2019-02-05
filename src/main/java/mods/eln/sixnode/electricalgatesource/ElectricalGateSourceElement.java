package mods.eln.sixnode.electricalgatesource;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sound.SoundCommand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalGateSourceElement extends SixNodeElement {

    public ElectricalGateSourceDescriptor descriptor;
    public NbtElectricalLoad outputGate = new NbtElectricalLoad("outputGate");

    public NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);

    public AutoResetProcess autoResetProcess;

    public static final byte setVoltagerId = 1;

    public ElectricalGateSourceElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.descriptor = (ElectricalGateSourceDescriptor) descriptor;

        electricalLoadList.add(outputGate);
        electricalComponentList.add(outputGateProcess);

        if (this.descriptor.autoReset) {
            slowProcessList.add(autoResetProcess = new AutoResetProcess());
            autoResetProcess.reset();
        }
    }

    class AutoResetProcess implements IProcess {
        double timeout = 0;
        double timeoutDelay = 0.21;

        @Override
        public void process(double time) {
            if (timeout > 0) {
                if (timeout - time < 0) {
                    outputGateProcess.setOutputNormalized(0);
                    needPublish();
                }
                timeout -= time;
            }
        }

        void reset() {
            timeout = timeoutDelay;
        }
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("front", (byte) ((front.toInt() << 0)));
        return nbt;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (front == lrdu) return outputGate;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu) return NodeBase.maskElectricalOutputGate;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotUIP(outputGate.getU(), outputGate.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Output voltage"), Utils.plotVolt("", outputGate.getU()));
        return info;
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(front.toInt() << 4);
            stream.writeFloat((float) outputGateProcess.getU());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        Eln.instance.signalCableDescriptor.applyTo(outputGate);
        computeElectricalLoad();
    }

    @Override
    protected void inventoryChanged() {
        computeElectricalLoad();
    }

    public void computeElectricalLoad() {
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        ItemStack currentItemStack = entityPlayer.getHeldItemMainhand();

        if (onBlockActivatedRotate(entityPlayer)) {
            return true;
        } else if (!Utils.playerHasMeter(entityPlayer) && descriptor.onOffOnly) {
            outputGateProcess.state(!outputGateProcess.getOutputOnOff());
            play(new SoundCommand("random.click").mulVolume(0.3F, 0.6F).smallRange());
            if (autoResetProcess != null)
                autoResetProcess.reset();
            needPublish();
            return true;
        }
        // front = LRDU.fromInt((front.toInt() + 1)&3);
        return false;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case setVoltagerId:
                    outputGateProcess.setU(stream.readFloat());
                    needPublish();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return !descriptor.onOffOnly;
    }
}
