package mods.eln;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.Node;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.NodeManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;


public class GuiHandler implements IGuiHandler {
    //returns an instance of the Container you made earlier
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world,int x, int y, int z) {
    	NodeBlockEntity tileEntity = (NodeBlockEntity) world.getBlockTileEntity(x, y, z);
    	Direction side = Direction.fromInt(id);
    	Object container = tileEntity.newContainer(side,player);
    	if(container == null)
    	{
            try {
            	ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream stream = new DataOutputStream(bos);   	
                
    	        stream.writeByte(Eln.packetOpenLocalGui);
    	        stream.writeInt(id);
    	        stream.writeInt(x);
    	        stream.writeInt(y);
    	        stream.writeInt(z);

    	        Packet250CustomPayload packet = new Packet250CustomPayload();
    	        packet.channel = Eln.channelName;
    	        packet.data = bos.toByteArray();
    	        packet.length = bos.size();  
    	        PacketDispatcher.sendPacketToPlayer(packet,(Player)player);  
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			
    		}
    	}
		return container;

    }

    //returns an instance of the Gui you made earlier
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
    	NodeBlockEntity tileEntity = (NodeBlockEntity) world.getBlockTileEntity(x, y, z);
    	Direction side = Direction.fromInt(id);
    
    	return tileEntity.newGuiDraw(side,player);
    }
}