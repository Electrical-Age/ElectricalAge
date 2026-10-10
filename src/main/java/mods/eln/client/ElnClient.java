package mods.eln.client;

import mods.eln.Eln;
import mods.eln.misc.UtilsClient;
import mods.eln.network.ElnNet;
import mods.eln.node.six.SixNodeRender;
import mods.eln.node.transparent.TransparentNodeRender;
import mods.eln.registry.ElnRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/** EA's client entry point (1.12 ClientProxy): renderers, the client side of the EA protocol, frame refresh (P3). */
@Mod(value = Eln.MODID, dist = Dist.CLIENT)
public final class ElnClient {
    public ElnClient(IEventBus modBus) {
        ElnNet.clientHandler = ClientPacketHandler::packetRx;
        UtilsClient.hooks = new ClientHooks();
        ClientRenders.registerAll();
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, e -> {
            e.registerBlockEntityRenderer(ElnRegistry.SIX_NODE_ENTITY.get(), ctx -> new SixNodeRender());
            e.registerBlockEntityRenderer(ElnRegistry.TRANSPARENT_NODE_ENTITY.get(), ctx -> new TransparentNodeRender());
        });
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, FrameTime::onFrame);
        ClientScreens.registerAll();
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent.class,
            e -> e.register(ElnRegistry.NODE_MENU.get(), mods.eln.client.gui.ElnScreens::create));
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent.class,
            e -> mods.eln.client.render.ElnPipelines.ALL.forEach(e::registerPipeline));
    }

    static final class ClientHooks implements UtilsClient.Hooks {
        @Override
        public float distanceFromClientPlayer(double x, double y, double z) {
            var p = Minecraft.getInstance().player;
            if (p == null) return 0;
            return (float) (Math.abs(x - p.getX()) + Math.abs(y - p.getY()) + Math.abs(z - p.getZ()));
        }

        @Override
        public int getLight(Level w, int x, int y, int z) {
            if (w == null) return 15;
            BlockPos pos = new BlockPos(x, y, z);
            return Math.max(w.getBrightness(LightLayer.BLOCK, pos), w.getBrightness(LightLayer.SKY, pos) - w.getSkyDarken());
        }

        @Override
        public float playerYaw() {
            var p = Minecraft.getInstance().player;
            return p == null ? 0 : p.getYRot();
        }

        @Override
        public float playerPitch() {
            var p = Minecraft.getInstance().player;
            return p == null ? 0 : p.getXRot();
        }

        @Override
        public double playerX() {
            var p = Minecraft.getInstance().player;
            return p == null ? 0 : p.getX();
        }

        @Override
        public double playerZ() {
            var p = Minecraft.getInstance().player;
            return p == null ? 0 : p.getZ();
        }

        @Override
        public net.minecraft.world.item.ItemStack playerHelmet() {
            var p = Minecraft.getInstance().player;
            return p == null ? net.minecraft.world.item.ItemStack.EMPTY : p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        }

        @Override
        public boolean isGameInPause() {
            return Minecraft.getInstance().isPaused();
        }

        @Override
        public void drawEntityItem(ItemEntity entityItem, double x, double y, double z, float roty, float scale) {
            // R4: items shown on machines (submitItem at the current GL11 matrix)
        }
    }
}
