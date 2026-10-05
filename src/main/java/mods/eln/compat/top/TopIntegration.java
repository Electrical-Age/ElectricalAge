package mods.eln.compat.top;

import mcjty.theoneprobe.api.IBlockDisplayOverride;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import mods.eln.Eln;
import mods.eln.ghost.GhostBlock;
import mods.eln.ghost.GhostElement;
import mods.eln.ghost.GhostObserver;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeBlock;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInterModComms;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;

/**
 * TheOneProbe info for EA nodes (1.12 port; replaces the 1.7.10 Waila integration).
 * TOP builds probe info on the SERVER, so the node data is read directly (1.7.10 Waila needed request/response
 * packets and a client cache). Same content as 1.7.10 Waila:
 * <ul>
 * <li>Transparent node: the element's getWaila() lines "key: value" (value white).</li>
 * <li>Six node: the element on the hit side: getWaila() lines (null values skipped). TOP's header uses the client
 * pick block, which SixNodeBlock already resolves to the element's item.</li>
 * <li>Ghost block: header = the real device's item (1.7.10 Waila head/stack), body = the real device's lines.</li>
 * </ul>
 * Registered by {@link #register()} (Wp11Content.preInit) through IMC when theoneprobe is loaded.
 */
public class TopIntegration implements Function<ITheOneProbe, Void> {
    public static final String MODID_TOP = "theoneprobe";

    public static void register() {
        FMLInterModComms.sendFunctionMessage(MODID_TOP, "getTheOneProbe", TopIntegration.class.getName());
    }

    @Override
    public Void apply(ITheOneProbe probe) {
        probe.registerProvider(new Provider());
        probe.registerBlockDisplayOverride(new GhostHeader());
        return null;
    }

    /** A real device behind a ghost block: its item and its info lines (null when unknown). */
    public static final class GhostTarget {
        public final ItemStack stack;
        public final Map<String, String> waila;

        GhostTarget(ItemStack stack, Map<String, String> waila) {
            this.stack = stack;
            this.waila = waila;
        }
    }

    /** 1.7.10 GhostNodeWailaRequestPacketHandler: the ghost's observer, transparent node or six-node element. */
    @Nullable
    public static GhostTarget ghostTarget(World world, Coordonate coord) {
        GhostElement ghost = Eln.ghostManager.getGhost(coord);
        Coordonate realCoord = ghost == null ? null : ghost.getObservatorCoordonate();
        if (realCoord == null) return null;
        GhostTarget target = null;
        NodeBase node = NodeManager.instance.getNodeFromCoordonate(realCoord);
        if (node instanceof TransparentNode && ((TransparentNode) node).element != null) {
            TransparentNode t = (TransparentNode) node;
            target = new GhostTarget(t.element.getDescriptor().newItemStack(), waila(t));
        }
        GhostObserver observer = Eln.ghostManager.getObserver(realCoord);
        if (observer instanceof SixNodeElement) {
            SixNodeElement element = (SixNodeElement) observer;
            target = new GhostTarget(element.sixNodeElementDescriptor.newItemStack(), element.getWaila());
        }
        return target;
    }

    @Nullable
    static Map<String, String> waila(TransparentNode node) {
        try {
            return node.element.getWaila();
        } catch (NullPointerException e) {
            // 1.7.10 TransparentNodeRequestPacketHandler: invalid node
            Utils.print("Attempted to get TOP info for an invalid node!");
            return null;
        }
    }

    static void addLines(IProbeInfo info, @Nullable Map<String, String> lines) {
        if (lines == null) return;
        for (Map.Entry<String, String> e : lines.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            info.text(e.getKey() + ": " + TextFormatting.WHITE + e.getValue());
        }
    }

    static final class Provider implements IProbeInfoProvider {
        @Override
        public String getID() {
            return Eln.MODID + ":nodes";
        }

        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo info, EntityPlayer player, World world, IBlockState state, IProbeHitData data) {
            Block block = state.getBlock();
            Coordonate coord = new Coordonate(data.getPos().getX(), data.getPos().getY(), data.getPos().getZ(), world);
            if (block instanceof TransparentNodeBlock) {
                NodeBase node = NodeManager.instance.getNodeFromCoordonate(coord);
                if (node instanceof TransparentNode) addLines(info, waila((TransparentNode) node));
            } else if (block instanceof SixNodeBlock) {
                NodeBase node = NodeManager.instance.getNodeFromCoordonate(coord);
                if (node instanceof SixNode && data.getSideHit() != null) {
                    SixNodeElement element = ((SixNode) node).getElement(Direction.fromEnumFacing(data.getSideHit()));
                    if (element != null) addLines(info, element.getWaila());
                }
            } else if (block instanceof GhostBlock) {
                GhostTarget target = ghostTarget(world, coord);
                if (target != null) addLines(info, target.waila);
            }
        }
    }

    /** Ghost blocks pick as EMPTY: show the real device's item and name instead of TOP's standard header. */
    static final class GhostHeader implements IBlockDisplayOverride {
        @Override
        public boolean overrideStandardInfo(ProbeMode mode, IProbeInfo info, EntityPlayer player, World world, IBlockState state, IProbeHitData data) {
            if (!(state.getBlock() instanceof GhostBlock)) return false;
            GhostTarget target = ghostTarget(world, new Coordonate(data.getPos().getX(), data.getPos().getY(), data.getPos().getZ(), world));
            if (target == null || target.stack.isEmpty()) return false;
            info.horizontal()
                .item(target.stack)
                .vertical()
                .itemLabel(target.stack)
                .text(TextStyleClass.MODNAME + Eln.NAME);
            addLines(info, target.waila);
            return true;
        }
    }
}
