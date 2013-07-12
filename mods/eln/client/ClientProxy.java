package mods.eln.client;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import cpw.mods.fml.client.registry.ClientRegistry;
import mods.eln.CommonProxy;
import mods.eln.Eln;
import mods.eln.misc.ItemRender;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3DFolder;
import mods.eln.node.SixNodeEntity;
import mods.eln.node.SixNodeRender;
import mods.eln.node.TransparentNodeEntity;
import mods.eln.node.TransparentNodeRender;
import net.minecraftforge.client.MinecraftForgeClient;








public class ClientProxy extends CommonProxy {

   
	@Override
	public void registerRenderers() {

		ClientRegistry.bindTileEntitySpecialRenderer(SixNodeEntity.class, new SixNodeRender());
		ClientRegistry.bindTileEntitySpecialRenderer(TransparentNodeEntity.class, new TransparentNodeRender());
       	
      	MinecraftForgeClient.registerItemRenderer(Eln.transparentNodeItem.itemID, Eln.transparentNodeItem);
      	MinecraftForgeClient.registerItemRenderer(Eln.sixNodeItem.itemID, Eln.sixNodeItem);
      	MinecraftForgeClient.registerItemRenderer(Eln.sharedItem.itemID, Eln.sharedItem);
      	       

		Eln.clientKeyHandler = new ClientKeyHandler();
	}
	
	
}