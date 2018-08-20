package mods.eln.simplenode.computerprobe;

import net.minecraftforge.fml.common.Optional;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import mods.eln.Other;
import mods.eln.node.simple.SimpleNodeEntity;

@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = Other.modIdOc)
public class ComputerProbeEntity extends SimpleNodeEntity implements SimpleComponent {

    @Override
    public String getComponentName() {
        return "ElnProbe";
    }

  /*  @Callback
    @Optional.Method(modid = Other.modIdOc)
    public Object[] greet(Context context, Arguments args) {
        return new Object[]{String.format("Hello, %s!", args.checkString(0))};
    }*/

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    public Object[] signalSetDir(Context context, Arguments args) {
        ComputerProbeNode n = getNode();
        if (n == null) return null;
        return n.signalSetDir(context, args);
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    public Object[] signalGetDir(Context context, Arguments args) {
        ComputerProbeNode n = getNode();
        if (n == null) return null;
        return n.signalGetDir(context, args);
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    public Object[] signalSetOut(Context context, Arguments args) {
        ComputerProbeNode n = getNode();
        if (n == null) return null;
        return n.signalSetOut(context, args);
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    public Object[] signalGetOut(Context context, Arguments args) {
        ComputerProbeNode n = getNode();
        if (n == null) return null;
        return n.signalGetOut(context, args);
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    public Object[] signalGetIn(Context context, Arguments args) {
        ComputerProbeNode n = getNode();
        if (n == null) return null;
        return n.signalGetIn(context, args);
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    public Object[] wirelessSet(Context context, Arguments args) {
        ComputerProbeNode n = getNode();
        if (n == null) return null;
        return n.wirelessSet(context, args);
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    public Object[] wirelessRemove(Context context, Arguments args) {
        ComputerProbeNode n = getNode();
        if (n == null) return null;
        return n.wirelessRemove(context, args);
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    public Object[] wirelessRemoveAll(Context context, Arguments args) {
        ComputerProbeNode n = getNode();
        if (n == null) return null;
        return n.wirelessRemoveAll(context, args);
    }

    @Callback
    @Optional.Method(modid = Other.modIdOc)
    public Object[] wirelessGet(Context context, Arguments args) {
        ComputerProbeNode n = getNode();
        if (n == null) return null;
        return n.wirelessGet(context, args);
    }

    public ComputerProbeNode getNode() {
        return (ComputerProbeNode) super.getNode();
    }

    //return new String[]{"writeDir", "readDir", "writeOut", "readOut", "readIn"};

    @Override
    public String getNodeUuid() {
        return ComputerProbeNode.getNodeUuidStatic();
    }
}
