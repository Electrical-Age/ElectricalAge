package mods.eln.computercraftio;

import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.TransparentNode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;

public class PeripheralHandler implements IPeripheralProvider {
	@Override
	public IPeripheral getPeripheral(World world, int x, int y, int z, int side) {
		System.out.print("PERIPHERAL ? " + x + " " + y + " " + z + " " + side);
		NodeBase nb = NodeManager.instance
				.getNodeFromCoordonate(new Coordonate(x, y, z, world));
		if (nb instanceof TransparentNode) {
			TransparentNode tn = (TransparentNode) nb;
			if (tn.element != null
					&& tn.element instanceof ComputerCraftIoElement) {
				System.out.println(" YES");
				return (IPeripheral) tn.element;
			}
		}
		System.out.println(" NO");
		return null;
	}
}