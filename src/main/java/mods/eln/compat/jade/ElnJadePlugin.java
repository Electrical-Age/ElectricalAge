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
        if (node instanceof mods.eln.node.voxel.VoxelNode v) {
            // an element hosted there under the crosshair, else the parts
            var h = acc.getHitResult().getLocation();
            var p = acc.getPosition();
            double x = h.x - p.getX(), y = h.y - p.getY(), z = h.z - p.getZ();
            int el = v.elementAt(x, y, z, 0), pi = v.partAt(x, y, z);
            if (el < 0 && pi < 0) el = v.elementAt(x, y, z, 2);
            if (el >= 0 && v.sideElementList[el] != null) return v.sideElementList[el].getWaila();
            return v.getWaila(details, pi);   // (the pipe or cable under the crosshair: Baughn)
        }
        if (node instanceof mods.eln.node.transparent.TransparentNode t && t.element != null) return t.element.getWaila();
        if (node instanceof mods.eln.node.six.SixNode s && be instanceof mods.eln.node.six.SixNodeEntity se) {
            var side = mods.eln.node.six.SixNodeBlock.sideAt(se, acc.getPosition(), acc.getHitResult());
            if (side != null && s.sideElementList[side.getInt()] != null) return s.sideElementList[side.getInt()].getWaila();
        }
        return Map.of();
    }

    /** Does the device under the crosshair have thermal loads (so its ambient matters)? */
    static boolean hasHeat(BlockAccessor acc) {
        if (!(acc.getBlockEntity() instanceof NodeBlockEntity be)) return false;
        var node = be.getNode();
        if (node instanceof mods.eln.node.voxel.VoxelNode) return true;   // (cables and pipes)
        if (node instanceof mods.eln.node.transparent.TransparentNode t) return t.element != null && !t.element.thermalLoadList.isEmpty();
        if (node instanceof mods.eln.node.six.SixNode s) {
            for (var e : s.sideElementList) if (e != null && !e.thermalLoadList.isEmpty()) return true;
        }
        return false;
    }

    /** A voxel block's header: the device, pipe or cable under the crosshair (the block is "Pipes and Cables"). */
    static String title(BlockAccessor acc) {
        if (!(acc.getBlockEntity() instanceof NodeBlockEntity be) || !(be.getNode() instanceof mods.eln.node.voxel.VoxelNode v)) return null;
        var h = acc.getHitResult().getLocation();
        var p = acc.getPosition();
        double x = h.x - p.getX(), y = h.y - p.getY(), z = h.z - p.getZ();
        int pi = v.partAt(x, y, z);
        int el = v.elementAt(x, y, z, 0);
        if (el < 0 && pi < 0) el = v.elementAt(x, y, z, 2);
        if (el >= 0 && v.sideElementList[el] != null) return v.sideElementList[el].sixNodeElementDescriptor.name;
        if (pi < 0) return null;
        var part = v.parts.get(pi);
        if (part instanceof mods.eln.node.voxel.CablePart c) return mods.eln.node.voxel.CablePart.TIERS[c.tier].name();
        if (part instanceof mods.eln.node.voxel.PipePart pp) return (pp.tier == 0 ? "Iron" : "Alloy") + " Pipe";
        return null;
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
            String title = title(acc);
            if (title != null) tag.putString("elnTitle", title);
            if (m == null || m.isEmpty()) return;
            if (hasHeat(acc)) {   // (the ambient it cools towards: by biome, height, depth; Baughn 2026-10-26)
                m = new java.util.LinkedHashMap<>(m);
                m.put(mods.eln.i18n.I18N.tr("Ambient"), mods.eln.misc.Utils.plotCelsius("", mods.eln.sim.Ambient.relative(acc.getLevel(), acc.getPosition())));
            }
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
            String title = acc.getServerData().getStringOr("elnTitle", "");
            if (!title.isEmpty()) tooltip.replace(snownee.jade.api.JadeIds.CORE_OBJECT_NAME, Component.literal(title));
            var list = acc.getServerData().getListOrEmpty("elnProbe");
            boolean details = mods.eln.Eln.wailaEasyMode || acc.showDetails() || acc.getPlayer() != null && acc.getPlayer().isShiftKeyDown();
            int n = details ? list.size() : Math.min(HEADLINE, list.size());
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
