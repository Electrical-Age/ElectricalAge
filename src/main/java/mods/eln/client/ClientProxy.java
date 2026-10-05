package mods.eln.client;

import net.minecraftforge.fml.client.registry.ClientRegistry;
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
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    public static UuidManager uuidManager;
    public static SoundClientEventListener soundClientEventListener;
    public static ClientPacketHandler clientPacketHandler;

    @Override
    public void handleDescriptionPacket(byte[] data) {
        if (clientPacketHandler != null) clientPacketHandler.packetRx(data, null);
    }

    @Override
    public void registerRenderers() {
        clientPacketHandler = new ClientPacketHandler();
        ClientRegistry.bindTileEntitySpecialRenderer(SixNodeEntity.class, new SixNodeRender());
        ClientRegistry.bindTileEntitySpecialRenderer(TransparentNodeEntity.class, new TransparentNodeRender());

        // TODO(1.12 WP5): IItemRenderer bridge (TEISR + perspective-capturing IBakedModel) for transparentNodeItem,
        // sixNodeItem, sharedItem, sharedItemStackOne (were MinecraftForgeClient.registerItemRenderer).

        RenderingRegistry.registerEntityRenderingHandler(ReplicatorEntity.class, new ReplicatorRender(new ModelSilverfish(), (float) 0.3));

        Eln.clientKeyHandler = new ClientKeyHandler();
        FMLCommonHandler.instance().bus().register(Eln.clientKeyHandler);
        MinecraftForge.EVENT_BUS.register(new TutorialSignOverlay());
        uuidManager = new UuidManager();
        soundClientEventListener = new SoundClientEventListener(uuidManager);

        new FrameTime();
        new ConnectionListener();
    }
}
