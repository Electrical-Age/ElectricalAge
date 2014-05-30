package mods.eln.client;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import mods.eln.CommonProxy;
import mods.eln.Eln;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.entity.ReplicatorModel;
import mods.eln.entity.ReplicatorRender;
import mods.eln.misc.ItemRender;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3DFolder;
import mods.eln.node.SixNodeEntity;
import mods.eln.node.SixNodeRender;
import mods.eln.node.TransparentNodeEntity;
import mods.eln.node.TransparentNodeRender;
import net.minecraft.client.model.ModelSilverfish;
import net.minecraft.client.renderer.entity.RenderSilverfish;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

public class ClientProxy extends CommonProxy {

	@Override
	public void registerRenderers() {
		new ClientPacketHandler();
		ClientRegistry.bindTileEntitySpecialRenderer(SixNodeEntity.class, new SixNodeRender());
		ClientRegistry.bindTileEntitySpecialRenderer(TransparentNodeEntity.class, new TransparentNodeRender());
       	
      	MinecraftForgeClient.registerItemRenderer(Eln.transparentNodeItem, Eln.transparentNodeItem);
      	MinecraftForgeClient.registerItemRenderer(Eln.sixNodeItem, Eln.sixNodeItem);
      	MinecraftForgeClient.registerItemRenderer(Eln.sharedItem, Eln.sharedItem);
      	MinecraftForgeClient.registerItemRenderer(Eln.sharedItemStackOne, Eln.sharedItemStackOne);

      //	RenderingRegistry.registerEntityRenderingHandler(ReplicatorEntity.class, new RenderSilverfish());
      //	RenderingRegistry.registerEntityRenderingHandler(ReplicatorEntity.class, new ReplicatorRender(new ReplicatorModel(),1));
      	RenderingRegistry.registerEntityRenderingHandler(ReplicatorEntity.class, new ReplicatorRender(new ModelSilverfish(),(float) 0.3));
      	      	    	
		Eln.clientKeyHandler = new ClientKeyHandler();
		FMLCommonHandler.instance().bus().register(Eln.clientKeyHandler);
	}
}
