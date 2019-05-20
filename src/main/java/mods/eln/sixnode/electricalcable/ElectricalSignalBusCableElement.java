package mods.eln.sixnode.electricalcable;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeConnection;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.util.EnumChatFormatting;

import java.util.LinkedHashMap;
import java.util.Map;

public class ElectricalSignalBusCableElement extends ElectricalCableElement {
    public NbtElectricalLoad coloredElectricalLoads[];

    public ElectricalSignalBusCableElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        colorCare = 0;
        electricalLoadList.remove(electricalLoad);
        electricalLoad = null;

        coloredElectricalLoads = new NbtElectricalLoad[16];
        for(int i = 0; i < 16; i++) {
            NbtElectricalLoad load = new NbtElectricalLoad("color" + i);
            load.setCanBeSimplifiedByLine(true);
            electricalLoadList.add(load);
            coloredElectricalLoads[i] = load;
        }
    }

    @Override
    public void initialize() {
        for(NbtElectricalLoad load : coloredElectricalLoads) {
            this.descriptor.applyTo(load);
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        int color = (mask >> NodeBase.MASK_COLOR_SHIFT) & 0xF;
        ElectricalLoad load = coloredElectricalLoads[color];
        Utils.println("ESBCE.gEL: mask " + mask + ", color " + color + ", load " + load);
        return load;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new LinkedHashMap<>();

        String[] arry = new String[coloredElectricalLoads.length / 4];

        String t = "";

        for (int i = 0; i < coloredElectricalLoads.length; i++) {
            t += wool_to_chat[15-i] + Utils.plotVolt("",  coloredElectricalLoads[i].getU()).trim() + "\u00A7r, ";
            if ((i+1)% 4 == 0) {
                arry[(i-3) / 4 ] = t.substring(0, t.length() - 2);
                t = "";
            }
        }

        for (int i = 0; i < arry.length; i++) {
            info.put(String.format("%02d-%02d", 4*i, 4*i + 3), arry[i]);
        }


        return info;
    }

    public static EnumChatFormatting[] wool_to_chat = {
        EnumChatFormatting.WHITE,
        EnumChatFormatting.GOLD,
        EnumChatFormatting.LIGHT_PURPLE,
        EnumChatFormatting.BLUE,
        EnumChatFormatting.YELLOW,
        EnumChatFormatting.GREEN,
        EnumChatFormatting.RED,
        EnumChatFormatting.DARK_GRAY,
        EnumChatFormatting.GRAY,
        EnumChatFormatting.DARK_AQUA,
        EnumChatFormatting.DARK_PURPLE,
        EnumChatFormatting.DARK_BLUE,
        EnumChatFormatting.AQUA,  // FIXME: supposed to be brown
        EnumChatFormatting.DARK_GREEN,
        EnumChatFormatting.DARK_RED,
        EnumChatFormatting.BLACK,
    };

    @Override
    public String multiMeterString() {
        String t = "";
        for(int i = 0; i < 16; i++) {
            t += (wool_to_chat[15 - i] + Utils.plotVolt("", coloredElectricalLoads[i].getU()).trim()) + " ";
        }
        return t;
    }

    @Override
    public void newConnectionAt(NodeConnection connection, boolean isA) {
        if(!isA) return;  // Only run for one of the connection attempts between two ESBCEs; choose A arbitrarily.
        Utils.println("ESBCE.nCA:");
        NodeBase other = connection.N2;
        Utils.println("\tother is: " + other);
        if(other instanceof SixNode) {
            Utils.println("\tother is SixNode");
            SixNode sixother = (SixNode) other;
            SixNodeElement el = sixother.getElement(connection.dir2.applyLRDU(connection.lrdu2));
            Utils.println("\tel is: " + el);
            if(el instanceof ElectricalSignalBusCableElement) {
                Utils.println("\tel is ESBCE too");
                // Connect the other 15 colors, too
                for(int i = 1; i < 16; i++) {
                    ElectricalConnection econ = new ElectricalConnection(
                        this.coloredElectricalLoads[i],
                        ((ElectricalSignalBusCableElement) el).coloredElectricalLoads[i]
                    );
                    Eln.simulator.addElectricalComponent(econ);
                    connection.addConnection(econ);
                }
            }
        }
        Utils.println("ESBCE.nCA ends.");
    }
}
