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
        mods.eln.sound.SoundCommand.clientPlayer = mods.eln.sound.SoundClient::play;
        UtilsClient.hooks = new ClientHooks();
        ClientRenders.registerAll();
        // oil: crude (still/flowing) and lake oil, opaque (AdventurAgent: tinted water textures let the lake bed show through)
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent.class, e -> {
            var tint = net.neoforged.neoforge.client.fluid.FluidTintSources.constant(0xFFFFFFFF);
            var still = new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "block/oil_still"));
            var flow = new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "block/oil_flow"));
            e.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(still, flow, null, tint),
                mods.eln.oil.Oil.CRUDE, mods.eln.oil.Oil.CRUDE_FLOWING);
            e.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(still, flow, null, tint), mods.eln.oil.Oil.RESERVOIR_OIL);
            // the column's cuts: water's (greyscale) textures tinted, so each bucket differs from crude and the others
            var wStill = new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_still"));
            var wFlow = new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_flow"));
            for (var c : mods.eln.oil.Oil.Cut.values()) {
                var t = net.neoforged.neoforge.client.fluid.FluidTintSources.constant(c.color);
                e.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(wStill, wFlow, null, t), c.source, c.flowing);
            }
            e.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(wStill, wFlow, null,
                net.neoforged.neoforge.client.fluid.FluidTintSources.constant(0xFFE8ECF0)), mods.eln.oil.Oil.STEAM, mods.eln.oil.Oil.STEAM_FLOWING);
        });
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, e -> {
            e.registerBlockEntityRenderer(ElnRegistry.SIX_NODE_ENTITY.get(), ctx -> new SixNodeRender<>());
            e.registerBlockEntityRenderer(ElnRegistry.TRANSPARENT_NODE_ENTITY.get(), ctx -> new TransparentNodeRender());
            // voxel nodes: parts baked in the chunk mesh by VoxelModel; six-node elements hosted there drawn as in six-node blocks
            e.registerBlockEntityRenderer(ElnRegistry.VOXEL_NODE_ENTITY.get(), ctx -> new SixNodeRender<>());
            // every entity type needs a renderer: the client crashes on one without (thunderstorm replicators)
            e.registerEntityRenderer(ElnRegistry.REPLICATOR.get(), mods.eln.client.entity.ReplicatorRenderer::new);
        });
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent.class,
            e -> e.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "device"),
                mods.eln.client.render.ElnItemRenderer.Unbaked.MAP_CODEC));
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterBlockStateModels.class,
            e -> e.registerModel(mods.eln.client.render.VoxelModel.ID, mods.eln.client.render.VoxelModel.Unbaked.CODEC));
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent.class,
            e -> e.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "lit"), mods.eln.client.render.LitProperty.MAP_CODEC));
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent.class, e -> e.registerAbove(
            net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR, net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "battery_belt"), BeltHud::render));
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent.class,
            e -> e.registerAboveAll(net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "tutorial_sign"),
                mods.eln.sixnode.tutorialsign.TutorialSignHud::render));
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class,
            e -> e.registerLayerDefinition(mods.eln.client.entity.ReplicatorModel.LAYER, mods.eln.client.entity.ReplicatorModel::createLayer));
        if (mods.eln.handbook.Handbook.guideAvailable())
            modBus.addListener(net.neoforged.neoforge.client.event.AddClientReloadListenersEvent.class, HandbookGuide::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, FrameTime::onFrame);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent.class, mods.eln.client.render.ChiselOutline::onExtract);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent.class, mods.eln.client.render.KitOutline::onExtract);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent.class, mods.eln.client.render.VoxelOutline::onExtract);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent.class, mods.eln.client.render.DevicePreviewRender::onExtract);
        ClientScreens.registerAll();
        mods.eln.gui.ElnNodeMenu.clientLayout = mods.eln.client.gui.ElnLayouts::apply;
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent.class, ClientKeys::register);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.client.event.ClientTickEvent.Post.class, ClientKeys::tick);
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent.class,
            e -> {
                e.register(ElnRegistry.NODE_MENU.get(), mods.eln.client.gui.ElnScreens::create);
                e.register(ElnRegistry.XRAY_MENU.get(), mods.eln.client.gui.XRayScannerScreen::new);
                e.register(ElnRegistry.BELT_MENU.get(), mods.eln.client.gui.BeltScreen::new);
            });
        XRayScan.install();
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent.class, ItemTooltips::onTooltip);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.client.event.ClientTickEvent.Post.class, e -> XRayScan.tick());
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent.class,
            e -> mods.eln.client.render.ElnPipelines.ALL.forEach(e::registerPipeline));
        // the optional schematic icon set (Options -> Resource Packs): overrides textures/item/<id>.png
        modBus.addListener(net.neoforged.neoforge.event.AddPackFindersEvent.class,
            e -> e.addPackFinders(net.minecraft.resources.Identifier.fromNamespaceAndPath(Eln.MODID, "resourcepacks/schematic_icons"),
                net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                net.minecraft.network.chat.Component.literal("Electrical Age: schematic icons"),
                net.minecraft.server.packs.repository.PackSource.BUILT_IN, false,
                net.minecraft.server.packs.repository.Pack.Position.TOP));
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
