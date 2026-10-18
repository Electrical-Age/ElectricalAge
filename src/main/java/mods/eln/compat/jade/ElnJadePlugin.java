package mods.eln.compat.jade;

import mods.eln.node.NodeBlock;
import mods.eln.node.NodeBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import java.util.Map;

/**
 * Jade (26.x's WAILA; Baughn: on Incognito) for every EA node: the device's own probe lines (getWaila, as 1.12's WAILA
 * showed them), from the server. A headline always; the full details while sneaking, or always with the config
 * probe.details (ElnConfig). Loaded by Jade only when it is installed.
 */
@WailaPlugin
public class ElnJadePlugin implements IWailaPlugin {
    static final Identifier UID = Identifier.fromNamespaceAndPath("eln", "node");
    /** Lines shown without details. */
    static final int HEADLINE = 3;

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Server.INSTANCE, NodeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Client.INSTANCE, NodeBlock.class);
    }

    /** The device's lines, ordered: key TAB value. */
    static Map<String, String> lines(BlockAccessor acc, boolean details) {
        if (!(acc.getBlockEntity() instanceof NodeBlockEntity be) || !(be.getNode() instanceof mods.eln.node.NodeBase node)) return Map.of();
        if (node instanceof mods.eln.node.voxel.VoxelNode v) return v.getWaila(details);
        if (node instanceof mods.eln.node.transparent.TransparentNode t && t.element != null) return t.element.getWaila();
        if (node instanceof mods.eln.node.six.SixNode s && be instanceof mods.eln.node.six.SixNodeEntity se) {
            var side = mods.eln.node.six.SixNodeBlock.sideAt(se, acc.getPosition(), acc.getHitResult());
            if (side != null && s.sideElementList[side.getInt()] != null) return s.sideElementList[side.getInt()].getWaila();
        }
        return Map.of();
    }

    enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor acc) {
            // (all of it: the client cuts it to the headline unless the player sneaks or the config asks for details)
            Map<String, String> m;
            try {
                m = lines(acc, true);
            } catch (RuntimeException e) {
                return;   // (a device mid-change: no lines this time)
            }
            if (m == null || m.isEmpty()) return;
            ListTag list = new ListTag();
            for (var e : m.entrySet()) list.add(StringTag.valueOf(e.getKey() + "\t" + e.getValue()));
            tag.put("elnProbe", list);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor acc, IPluginConfig config) {
            var list = acc.getServerData().getListOrEmpty("elnProbe");
            int n = mods.eln.Eln.wailaEasyMode || acc.showDetails() ? list.size() : Math.min(HEADLINE, list.size());
            for (int i = 0; i < n; i++) {
                String s = list.getStringOr(i, "");
                int t = s.indexOf('\t');
                tooltip.add(t < 0 ? Component.literal(s) : Component.literal(s.substring(0, t) + ": ").append(Component.literal(s.substring(t + 1).trim())));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
