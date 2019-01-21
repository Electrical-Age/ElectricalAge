package mods.eln.sixnode.electricalmath;

import mods.eln.i18n.I18N;
import mods.eln.item.ConfigCopyToolDescriptor;
import mods.eln.item.IConfigurable;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.Node;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.solver.Equation;
import mods.eln.solver.ISymbole;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ElectricalMathElement extends SixNodeElement implements IConfigurable {

    NbtElectricalGateOutput gateOutput = new NbtElectricalGateOutput("gateOutput");
    NbtElectricalGateOutputProcess gateOutputProcess = new NbtElectricalGateOutputProcess("gateOutputProcess", gateOutput);

    NbtElectricalGateInput[] gateInput = new NbtElectricalGateInput[]{new NbtElectricalGateInput("gateA"), new NbtElectricalGateInput("gateB"), new NbtElectricalGateInput("gateC")};

    ArrayList<ISymbole> symboleList = new ArrayList<ISymbole>();

    ElectricalMathElectricalProcess electricalProcess = new ElectricalMathElectricalProcess(this);

    boolean firstBoot = true;

    boolean sideConnectionEnable[] = new boolean[3];
    String expression = "";
    Equation equation;
    boolean equationIsValid;

    int redstoneRequired;
    boolean redstoneReady = false;

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    static final byte setExpressionId = 1;

    public ElectricalMathElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        electricalLoadList.add(gateOutput);
        electricalLoadList.add(gateInput[0]);
        electricalLoadList.add(gateInput[1]);
        electricalLoadList.add(gateInput[2]);

        electricalComponentList.add(gateOutputProcess);

        electricalProcessList.add(electricalProcess);

        symboleList.add(new GateInputSymbol("A", gateInput[0]));
        symboleList.add(new GateInputSymbol("B", gateInput[1]));
        symboleList.add(new GateInputSymbol("C", gateInput[2]));
        symboleList.add(new DayTime());
    }

    class GateInputSymbol implements ISymbole {
        private String name;
        private NbtElectricalGateInput gate;

        public GateInputSymbol(String name, NbtElectricalGateInput gate) {
            this.name = name;
            this.gate = gate;
        }

        @Override
        public double getValue() {
            return gate.getNormalized();
        }

        @Override
        public String getName() {
            return name;
        }
    }

    class ElectricalMathElectricalProcess implements IProcess {
        private ElectricalMathElement e;

        ElectricalMathElectricalProcess(ElectricalMathElement e) {
            this.e = e;
        }

        @Override
        public void process(double time) {
            if (e.redstoneReady)
                e.gateOutputProcess.setOutputNormalizedSafe(e.equation.getValue(time));
            else
                e.gateOutputProcess.setOutputNormalized(0.0);
        }
    }

    void preProcessEquation(String expression) {
        this.expression = expression;
        equation = new Equation(); //expression, symboleList, 100);
        equation.setUpDefaultOperatorAndMapper();
        equation.setIterationLimit(100);
        equation.addSymbol(symboleList);
        equation.preProcess(expression);

        for (int idx = 0; idx < 3; idx++) {
            sideConnectionEnable[idx] = equation.isSymboleUsed(symboleList.get(idx));
        }
        this.expression = expression;

        redstoneRequired = 0;
        if (equationIsValid = equation.isValid()) {
            redstoneRequired = equation.getOperatorCount();
        }
        checkRedstone();
    }

    public class DayTime implements ISymbole {

        @Override
        public double getValue() {
            return sixNode.coordonate.world().getWorldTime() / (24000.0 - 1.0);
        }

        @Override
        public String getName() {
            return "daytime";
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (lrdu == front) return gateOutput;
        if (lrdu == front.left() && sideConnectionEnable[2]) return gateInput[2];
        if (lrdu == front.inverse() && sideConnectionEnable[1]) return gateInput[1];
        if (lrdu == front.right() && sideConnectionEnable[0]) return gateInput[0];
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (lrdu == front) return Node.maskElectricalOutputGate;
        if (lrdu == front.left() && sideConnectionEnable[2]) return Node.maskElectricalInputGate;
        if (lrdu == front.inverse() && sideConnectionEnable[1]) return Node.maskElectricalInputGate;
        if (lrdu == front.right() && sideConnectionEnable[0]) return Node.maskElectricalInputGate;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("Uout:", gateOutput.getU()) + Utils.plotAmpere("Iout:", gateOutput.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Equation"), expression);
        info.put(I18N.tr("Input voltages"),
            Utils.plotVolt("\u00A7c", gateInput[0].getU()) +
                Utils.plotVolt("\u00A7a", gateInput[1].getU()) +
                Utils.plotVolt("\u00A79", gateInput[2].getU()));
        info.put(I18N.tr("Output voltage"), Utils.plotVolt("", gateOutput.getU()));
        return info;
    }

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void initialize() {
        if (firstBoot) preProcessEquation(expression);
    }

    @Override
    protected void inventoryChanged() {
        super.inventoryChanged();
        checkRedstone();
    }

    void checkRedstone() {
        int redstoneInStack = 0;

        ItemStack stack = inventory.getStackInSlot(ElectricalMathContainer.restoneSlotId);
        if (stack != null) redstoneInStack = stack.stackSize;

        redstoneReady = redstoneRequired <= redstoneInStack;
        needPublish();
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new ElectricalMathContainer(sixNode, player, inventory);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("expression", expression);
        equation.writeToNBT(nbt, "equation");
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        expression = nbt.getString("expression");
        preProcessEquation(expression);
        equation.readFromNBT(nbt, "equation");

        firstBoot = false;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeUTF(expression);
            stream.writeInt(redstoneRequired);
            stream.writeBoolean(equationIsValid);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {
        super.networkUnserialize(stream, player);
        try {
            switch (stream.readByte()) {
                case setExpressionId:
                    preProcessEquation(stream.readUTF());
                    reconnect();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        if(compound.hasKey("expression")) {
            preProcessEquation(compound.getString("expression"));
            reconnect();
        }
        if(ConfigCopyToolDescriptor.readVanillaStack(compound, "redstone", inventory, 0, invoker)) {
            checkRedstone();
        }
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        compound.setString("expression", expression);
        ConfigCopyToolDescriptor.writeVanillaStack(compound, "redstone", inventory.getStackInSlot(0));
    }
}
