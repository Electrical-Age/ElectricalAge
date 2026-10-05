package mods.eln.client;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import mods.eln.CommonProxy;
import mods.eln.Eln;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.six.SixNodeRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.node.transparent.TransparentNodeRender;
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
    public void loadModels() {
        Eln.obj.loadAllElnModels();
    }

    @Override
    public void registerRenderers() {
        clientPacketHandler = new ClientPacketHandler();
        ClientRegistry.bindTileEntitySpecialRenderer(SixNodeEntity.class, new SixNodeRender());
        ClientRegistry.bindTileEntitySpecialRenderer(TransparentNodeEntity.class, new TransparentNodeRender());

        // TODO(1.12 WP5): IItemRenderer bridge (TEISR + perspective-capturing IBakedModel) for transparentNodeItem,
        // sixNodeItem, sharedItem, sharedItemStackOne (were MinecraftForgeClient.registerItemRenderer).


        Eln.clientKeyHandler = new ClientKeyHandler();
        FMLCommonHandler.instance().bus().register(Eln.clientKeyHandler);
        uuidManager = new UuidManager();
        soundClientEventListener = new SoundClientEventListener(uuidManager);

        Eln.content.clientInit(); // replicator renderer, tutorial sign overlay (device content)
        new FrameTime();
        new ConnectionListener();
    }
}
