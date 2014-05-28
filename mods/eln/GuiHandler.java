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
import mods.eln.wiki.Root;
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
		Direction side = Direction.fromInt(id);
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
	public static final int genericOpen = 6;
	// returns an instance of the Gui you made earlier
	@Override
	public Object getClientGuiElement(int id, EntityPlayer player, World world,
			int x, int y, int z) {
		if(id >= 0 && id <= 5){		
			NodeBlockEntity tileEntity = (NodeBlockEntity) world
					.getTileEntity(x, y, z);
			Direction side = Direction.fromInt(id);
			if(tileEntity == null) return null;
			return tileEntity.newGuiDraw(side, player);
		}
		if(id == genericOpen){
			return UtilsClient.guiLastOpen;
		}
		return null;
	}
}