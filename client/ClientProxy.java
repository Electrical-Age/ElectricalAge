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
import net.minecraft.client.texturepacks.TexturePackDefault;
import net.minecraftforge.client.MinecraftForgeClient;








public class ClientProxy extends CommonProxy {
//	public static WireBlockRender wireBlockRender = new WireBlockRender();
	
//	public static Obj3D masterCubeObj = new Obj3D();
//	public static Obj3D electricalFurnaceObj = new Obj3D();
//	public static Obj3D solarPannelObj = new Obj3D();
	

   
	@Override
	public void registerRenderers() {

		//URL lol3 = Eln.class.getResource("/mods/eln/eln.root");
		//String elnRootName = lol3.getPath().replaceAll("eln.root", "").replaceFirst("/", "");
		

		//elnRootName = elnRootName.replaceAll("\\", "/");
		System.out.println("registerRenderers start");
		MinecraftForgeClient.preloadTexture(CABLE_PNG);
		MinecraftForgeClient.preloadTexture(CABLENODE_PNG);
		MinecraftForgeClient.preloadTexture(THERMALCABLE_PNG);

		
		System.out.println("registerRenderers end");
		
	//	masterCubeObj.loadFile("/mods/eln/model/test/MONKEY.obj");
	//	electricalFurnaceObj.loadFile("/mods/eln/model/ELFURNACE.obj");
	//	solarPannelObj.loadFile("/mods/eln/model/SOLARPANEL_2X2.obj");
		
		
		
		
		ClientRegistry.bindTileEntitySpecialRenderer(SixNodeEntity.class, new SixNodeRender());
		ClientRegistry.bindTileEntitySpecialRenderer(TransparentNodeEntity.class, new TransparentNodeRender());
       	
      	MinecraftForgeClient.registerItemRenderer(Eln.transparentNodeItem.itemID, Eln.transparentNodeItem);
      	MinecraftForgeClient.registerItemRenderer(Eln.sixNodeItem.itemID, Eln.sixNodeItem);
      	MinecraftForgeClient.registerItemRenderer(Eln.sharedItem.itemID, Eln.sharedItem);
      	       
		
		
		Eln.clientKeyHandler = new ClientKeyHandler();
	}

}