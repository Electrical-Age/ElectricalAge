package mods.eln.sixnode.electricalmath;

import mods.eln.i18n.I18N;
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
import mods.eln.solver.Program;
import mods.eln.solver.ISymbole;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ElectricalMathElement extends SixNodeElement {

    NbtElectricalGateOutput gateOutput = new NbtElectricalGateOutput("gateOutput");
    NbtElectricalGateOutputProcess gateOutputProcess = new NbtElectricalGateOutputProcess("gateOutputProcess", gateOutput);

    NbtElectricalGateInput[] gateInput = new NbtElectricalGateInput[]{new NbtElectricalGateInput("gateA"), new NbtElectricalGateInput("gateB"), new NbtElectricalGateInput("gateC")};

    ArrayList<ISymbole> symboleList = new ArrayList<ISymbole>();

    ElectricalMathElectricalProcess electricalProcess = new ElectricalMathElectricalProcess(this);

    boolean firstBoot = true;

    boolean sideConnectionEnable[] = new boolean[3];
    String expression = "";
    Program program;
    boolean equationIsValid;
    /** Why the program doesn't compile (for the screen), or "". */
    String error = "";

    /** Cheap chips the program needs (Baughn 2026-10-30: ceil(N / 4) for N operators), and whether the slot has them. */
    int chipsRequired;
    boolean chipsReady = false;

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    static final byte setExpressionId = 1;
    /** Packets for the logger (Logger's ids follow), both ways. */
    public static final byte LOG_HEADER = 20;

    /**
     * The logger it carries (Baughn 2026-10-30, board #45): A, B and C as the program reads them (defaults included)
     * and the output, all as signals (0..1), shown 0 to 50 V unless set otherwise.
     */
    public final mods.eln.sixnode.electricaldatalogger.Logger logger = new mods.eln.sixnode.electricaldatalogger.Logger(this, LOG_HEADER,
        channel("A"), channel("B"), channel("C"), channel("out"));

    public static mods.eln.sixnode.electricaldatalogger.LogChannel channel(String name) {
        var c = new mods.eln.sixnode.electricaldatalogger.LogChannel(0, 50, "V");
        c.name = name;
        return c;
    }

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

        /** The input, or the program's default for it (`A = 0.5`) while no cable is connected (Baughn 2026-10-30). */
        @Override
        public double getValue() {
            double d = program == null ? Double.NaN : program.defaultOf(name);
            if (!Double.isNaN(d) && !connected(gate)) return d;
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
            double out = e.chipsReady ? e.program.getValue(time) : 0;
            if (e.chipsReady)
                e.gateOutputProcess.setOutputNormalizedSafe(out);
            else
                e.gateOutputProcess.setOutputNormalized(0.0);
            float[] v = new float[4];
            for (int i = 0; i < 3; i++) v[i] = (float) e.symboleList.get(i).getValue();
            v[3] = (float) (Double.isFinite(out) ? out : 0);
            e.logger.record(v, time);
        }
    }

    /** Is a cable (an electrical connection to a neighbour) on this input? */
    static boolean connected(NbtElectricalGateInput gate) {
        for (var c : gate.getConnectedComponents()) if (c instanceof mods.eln.sim.ElectricalConnection) return true;
        return false;
    }

    /** The inputs a program may give a default (`A = 0.5`). */
    public static final java.util.Set<String> DEFAULTABLE = java.util.Set.of("A", "B", "C");

    /** Operators a cheap chip pays for (Baughn 2026-10-30; it was one redstone an operator). */
    public static final int OPERATORS_PER_CHIP = 4;

    public static int chipsFor(int operators) {
        return (operators + OPERATORS_PER_CHIP - 1) / OPERATORS_PER_CHIP;
    }

    /** Is this the cheap chip, the processor slot's only item? */
    public static boolean isChip(ItemStack s) {
        return !Utils.isEmpty(s) && Utils.getItemObject(s) instanceof mods.eln.generic.GenericItemUsingDamageDescriptor d && "Cheap Chip".equals(d.name);
    }

    void preProcessEquation(String expression) {
        this.expression = expression;
        program = Program.compile(expression, symboleList, DEFAULTABLE);
        // the inputs connect whether the program reads them or not (Baughn 2026-10-30), so cables can go in first
        for (int idx = 0; idx < 3; idx++) sideConnectionEnable[idx] = true;
        equationIsValid = program.isValid();
        error = program.error == null || expression.isEmpty() ? "" : program.error;
        chipsRequired = equationIsValid ? chipsFor(program.getOperatorCount()) : 0;
        checkChips();
    }

    public class DayTime implements ISymbole {

        @Override
        public double getValue() {
            return Utils.getWorldTime(sixNode.coordonate.world());
        }

        @Override
        public String getName() {
            return "daytime";
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (lrdu == front) return gateOutput;
        if (lrdu == front.left() && sideConnectionEnable[2]) return gateInput[2];
        if (lrdu == front.inverse() && sideConnectionEnable[1]) return gateInput[1];
        if (lrdu == front.right() && sideConnectionEnable[0]) return gateInput[0];
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
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
        // (A red, B blue, C green: Baughn 2026-10-30)
        info.put(I18N.tr("Input voltages"),
            Utils.plotVolt("\u00A7c", gateInput[0].getU()) +
                Utils.plotVolt("\u00A79", gateInput[1].getU()) +
                Utils.plotVolt("\u00A7a", gateInput[2].getU()));
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
        checkChips();
    }

    void checkChips() {
        ItemStack stack = inventory.getItem(ElectricalMathContainer.restoneSlotId);
        int chips = isChip(stack) ? stack.getCount() : 0;
        chipsReady = chipsRequired <= chips;
        needPublish();
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putString("expression", expression);
        nbt.putInt("programVersion", mods.eln.solver.ProgramUpgrade.VERSION);
        program.writeToNBT(nbt, "equation");
        logger.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        // a processor from before 2026-10-30 holds redstone (one an operator): it becomes cheap chips, ceil(count / 4)
        ItemStack old = inventory.getItem(ElectricalMathContainer.restoneSlotId);
        if (!Utils.isEmpty(old) && old.is(net.minecraft.world.item.Items.REDSTONE)) {
            ItemStack chips = mods.eln.Eln.findItemStack("Cheap Chip", chipsFor(old.getCount()));
            inventory.setItem(ElectricalMathContainer.restoneSlotId, chips);
        }
        // a program saved by an older version is rewritten once to today's language, doing the same (rs and rc took
        // their arguments the other way round, = ^ ! were on/off comparisons, the operators' order was 1.12's), and its
        // functions' saved state renumbered to the order they stand in the text
        int version = nbt.getIntOr("programVersion", 0);
        var up = mods.eln.solver.ProgramUpgrade.upgradeAll(nbt.getStringOr("expression", ""), version);
        expression = up.program();
        if (version < mods.eln.solver.ProgramUpgrade.VERSION) {
            nbt = nbt.copy();
            mods.eln.solver.ProgramUpgrade.remapState(nbt, "equation", up.remaps());
        }
        preProcessEquation(expression);
        program.readFromNBT(nbt, "equation");
        logger.readFromNBT(nbt);

        firstBoot = false;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeUTF(expression);
            stream.writeInt(chipsRequired);
            stream.writeBoolean(equationIsValid);
            stream.writeUTF(error);
            logger.publish(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream, ServerPlayer player) {
        super.networkUnserialize(stream, player);
        try {
            byte header = stream.readByte();
            if (header == LOG_HEADER) {
                logger.handle(stream.readByte(), stream, player);
                return;
            }
            switch (header) {
                case setExpressionId:
                    String source = stream.readUTF();
                    if (Program.tooBig(source) != null) break;   // (the screen caps it too; a bigger one is refused here)
                    preProcessEquation(source);
                    reconnect();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
