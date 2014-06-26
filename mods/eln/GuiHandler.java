package mods.eln;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
	// returns an instance of the Container you made earlier
	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world,
			int x, int y, int z) {
		NodeBlockEntity tileEntity = (NodeBlockEntity) world
				.getTileEntity(x, y, z);
		Direction side = Direction.fromInt(id-nodeBaseOpen);
		Object container = tileEntity.newContainer(side, player);
		if (container == null) {
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				DataOutputStream stream = new DataOutputStream(bos);

				stream.writeByte(Eln.packetOpenLocalGui);
				stream.writeInt(id);
				stream.writeInt(x);
				stream.writeInt(y);
				stream.writeInt(z);

				
				Utils.sendPacketToClient(bos,(EntityPlayerMP)player);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
		}
		return container;

	}
	public static final int genericOpen = 5977;
	public static final int nodeBaseOpen = 6935;
	// returns an instance of the Gui you made earlier
	@Override
	public Object getClientGuiElement(int id, EntityPlayer player, World world,
			int x, int y, int z) {
		if(id == genericOpen){
			return UtilsClient.guiLastOpen;
		}
		
		if(id >= nodeBaseOpen && id <= nodeBaseOpen + 5){		
			NodeBlockEntity tileEntity = (NodeBlockEntity) world
					.getTileEntity(x, y, z);
			Direction side = Direction.fromInt(id-nodeBaseOpen);
			if(tileEntity == null) return null;
			return tileEntity.newGuiDraw(side, player);
		}

		return null;
	}
}