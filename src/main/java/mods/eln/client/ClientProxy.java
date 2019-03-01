package mods.eln.client;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import mods.eln.CommonProxy;
import mods.eln.Eln;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.entity.ReplicatorRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.six.SixNodeRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.node.transparent.TransparentNodeRender;
import mods.eln.sixnode.tutorialsign.TutorialSignOverlay;
import mods.eln.sound.SoundClientEventListener;
import net.minecraft.client.model.ModelSilverfish;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    public static UuidManager uuidManager;
    public static SoundClientEventListener soundClientEventListener;

    //TODO: FIX ITEM RENDERING 1.10
    @Override
    public void registerRenderers() {
        new ClientPacketHandler();
        ClientRegistry.bindTileEntitySpecialRenderer(SixNodeEntity.class, new SixNodeRender());
        ClientRegistry.bindTileEntitySpecialRenderer(TransparentNodeEntity.class, new TransparentNodeRender());

//        MinecraftForgeClient.registerItemRenderer(Eln.transparentNodeItem, Eln.transparentNodeItem);
//        MinecraftForgeClient.registerItemRenderer(Eln.sixNodeItem, Eln.sixNodeItem);
//        MinecraftForgeClient.registerItemRenderer(Eln.sharedItem, Eln.sharedItem);
//        MinecraftForgeClient.registerItemRenderer(Eln.sharedItemStackOne, Eln.sharedItemStackOne);

        RenderingRegistry.registerEntityRenderingHandler(
            ReplicatorEntity.class,
            manager -> new ReplicatorRender(manager, new ModelSilverfish(), 0.3f));

        Eln.clientKeyHandler = new ClientKeyHandler();
        MinecraftForge.EVENT_BUS.register(Eln.clientKeyHandler);
        MinecraftForge.EVENT_BUS.register(new TutorialSignOverlay());
        uuidManager = new UuidManager();
        soundClientEventListener = new SoundClientEventListener(uuidManager);

        if (Eln.versionCheckEnabled)
            MinecraftForge.EVENT_BUS.register(VersionCheckerHandler.getInstance());

        if (Eln.analyticsEnabled)
            MinecraftForge.EVENT_BUS.register(AnalyticsHandler.getInstance());

        new FrameTime();
        new ConnectionListener();
    }
}
