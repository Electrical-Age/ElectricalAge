package mods.eln.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
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

    @Override
    public void registerRenderers() {
        new ClientPacketHandler();
        ClientRegistry.bindTileEntitySpecialRenderer(SixNodeEntity.class, new SixNodeRender());
        ClientRegistry.bindTileEntitySpecialRenderer(TransparentNodeEntity.class, new TransparentNodeRender());

        MinecraftForgeClient.registerItemRenderer(Eln.transparentNodeItem, Eln.transparentNodeItem);
        MinecraftForgeClient.registerItemRenderer(Eln.sixNodeItem, Eln.sixNodeItem);
        MinecraftForgeClient.registerItemRenderer(Eln.sharedItem, Eln.sharedItem);
        MinecraftForgeClient.registerItemRenderer(Eln.sharedItemStackOne, Eln.sharedItemStackOne);

        RenderingRegistry.registerEntityRenderingHandler(ReplicatorEntity.class, new ReplicatorRender(new ModelSilverfish(), (float) 0.3));

        Eln.clientKeyHandler = new ClientKeyHandler();
        FMLCommonHandler.instance().bus().register(Eln.clientKeyHandler);
        MinecraftForge.EVENT_BUS.register(new TutorialSignOverlay());
        uuidManager = new UuidManager();
        soundClientEventListener = new SoundClientEventListener(uuidManager);

        if (Eln.versionCheckEnabled)
            FMLCommonHandler.instance().bus().register(VersionCheckerHandler.getInstance());

        if (Eln.analyticsEnabled)
            FMLCommonHandler.instance().bus().register(AnalyticsHandler.getInstance());

        new FrameTime();
        new ConnectionListener();
    }
}
