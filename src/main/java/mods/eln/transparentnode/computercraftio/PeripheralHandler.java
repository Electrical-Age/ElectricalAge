package mods.eln.transparentnode.computercraftio;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import mods.eln.misc.Coordinate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.simplenode.computerprobe.ComputerProbeNode;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PeripheralHandler implements IPeripheralProvider {

    @Override
    public IPeripheral getPeripheral(World world, BlockPos pos, EnumFacing side) {
        NodeBase nb = NodeManager.instance.getNodeFromCoordinate(new Coordinate(pos, world));
    /*	if (nb instanceof TransparentNode) {
			TransparentNode tn = (TransparentNode) nb;
			if (tn.element != null && tn.element instanceof ComputerCraftIoElement) {
				return (IPeripheral) tn.element;
			}
		}*/

        if (nb instanceof ComputerProbeNode) {
            IPeripheral p = (IPeripheral) nb;
            return p;
        }

        return null;
    }

    public static void register() {
        ComputerCraftAPI.registerPeripheralProvider(new PeripheralHandler());
    }
}
