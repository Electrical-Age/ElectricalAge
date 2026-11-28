package mods.eln.compat.jade;

import mods.eln.node.NodeBlock;
import mods.eln.node.NodeBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * probe.details (ElnConfig). And for any other block with the eln:machine capability (a Machine API machine at
 * another mod's block): MachineLines.hosted, from the server. Loaded by Jade only when it is installed.
 */
@WailaPlugin
public class ElnJadePlugin implements IWailaPlugin {
    static final Identifier UID = Identifier.fromNamespaceAndPath("eln", "node");
    /** Lines shown without details. */
    public static final int HEADLINE = 3;

    /** The Machine API's machines at other mods' blocks. */
    static final Identifier MACHINE_UID = Identifier.fromNamespaceAndPath("eln", "machine");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Server.INSTANCE, NodeBlockEntity.class);
        // (a multiblock's other cells: its owner's lines, as on the owner's own block)
        registration.registerBlockDataProvider(Server.INSTANCE, mods.eln.ghost.GhostBlock.class);
        registration.registerBlockDataProvider(MachineServer.INSTANCE, BlockEntity.class);
        // (the Tank Loading Station, a rail and no node: its docked cart's lines; the Tank Minecart itself)
        registration.registerBlockDataProvider(Server.INSTANCE, mods.eln.oil.TankStationEntity.class);
        registration.registerEntityDataProvider(CartServer.INSTANCE, mods.eln.oil.TankCart.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Client.INSTANCE, NodeBlock.class);
        registration.registerBlockComponent(Client.INSTANCE, mods.eln.ghost.GhostBlock.class);
        registration.registerBlockComponent(MachineClient.INSTANCE, Block.class);
        registration.registerBlockComponent(Client.INSTANCE, mods.eln.oil.TankStationBlock.class);
        registration.registerEntityComponent(CartClient.INSTANCE, mods.eln.oil.TankCart.class);
    }

    /** The device's lines, ordered: key TAB value (a voxel block's are VoxelProbe's, one set per thing in it). */
    static Map<String, String> lines(BlockAccessor acc, boolean details) {
        if (acc.getBlockEntity() instanceof mods.eln.oil.TankStationEntity station) return station.waila();
        if (!(acc.getBlockEntity() instanceof NodeBlockEntity be) || !(be.getNode() instanceof mods.eln.node.NodeBase node)) return Map.of();
        if (node instanceof mods.eln.node.transparent.TransparentNode t && t.element != null) return t.element.getWaila();
        if (node instanceof mods.eln.node.six.SixNode s && be instanceof mods.eln.node.six.SixNodeEntity se) {
            var side = mods.eln.node.six.SixNodeBlock.sideAt(se, acc.getPosition(), acc.getHitResult());
            if (side != null && s.sideElementList[side.getInt()] != null) return s.sideElementList[side.getInt()].getWaila();
        }
        return Map.of();
    }

    /** The device under the crosshair, if it can burn out (a transparent node's element, a six-node's in the aimed slot;
     *  not a voxel block's: VoxelProbe). */
    static mods.eln.sim.process.destruct.IBurnable burnable(BlockAccessor acc) {
        if (!(acc.getBlockEntity() instanceof NodeBlockEntity be)) return null;
        var node = be.getNode();
        if (node instanceof mods.eln.node.transparent.TransparentNode t) return t.element;
        if (node instanceof mods.eln.node.six.SixNode s && be instanceof mods.eln.node.six.SixNodeEntity se) {
            var side = mods.eln.node.six.SixNodeBlock.sideAt(se, acc.getPosition(), acc.getHitResult());
            if (side != null) return s.sideElementList[side.getInt()];
        }
        return null;
    }

    /** Does the device under the crosshair have thermal loads (so its ambient matters)? */
    static boolean hasHeat(BlockAccessor acc) {
        if (!(acc.getBlockEntity() instanceof NodeBlockEntity be)) return false;
        var node = be.getNode();
        if (node instanceof mods.eln.node.transparent.TransparentNode t) return t.element != null && !t.element.thermalLoadList.isEmpty();
        if (node instanceof mods.eln.node.six.SixNode s) {
            for (var e : s.sideElementList) if (e != null && !e.thermalLoadList.isEmpty()) return true;
        }
        return false;
    }

    /**
     * The machine a multiblock's other cell (a ghost block) belongs to, on the server: its transparent node's element
     * (the Large Boiler's seven ghost cells showed only "Electrical Age Multiblock Part"), or null.
     */
    static mods.eln.node.transparent.TransparentNodeElement ghostOwner(BlockAccessor acc) {
        if (!(acc.getBlock() instanceof mods.eln.ghost.GhostBlock) || !(acc.getLevel() instanceof net.minecraft.world.level.Level w)) return null;
        return mods.eln.ghost.GhostBlock.owner(w, acc.getPosition());
    }

    /**
     * The blocks whose fluid warnings this block shows (FluidWorld records them at port blocks): itself, a multiblock's
     * origin, and every port block of the device (a ghost cell or the origin of the Still Pot, whose ports are all on
     * other cells, showed none).
     */
    static java.util.List<Long> alarmBlocks(BlockAccessor acc, mods.eln.node.transparent.TransparentNodeElement owner) {
        java.util.List<Long> out = new java.util.ArrayList<>();
        out.add(acc.getPosition().asLong());
        var e = owner;
        if (e == null && acc.getBlockEntity() instanceof NodeBlockEntity be && be.getNode() instanceof mods.eln.node.transparent.TransparentNode t) e = t.element;
        if (owner != null) out.add(owner.node.coordonate.pos().asLong());
        if (e instanceof mods.eln.sim.fluid.FluidDevice d)
            for (var p : d.fluidPorts()) if (!out.contains(p.pos())) out.add(p.pos());
        return out;
    }

    enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor acc) {
            // (all of it: the client cuts to elnHead unless the player sneaks or probe.details is enabled)
            var owner = ghostOwner(acc);
            // (first of all, in red: keep exact callout as requested)
            var at = alarmBlocks(acc, owner);
            if (at.stream().anyMatch(mods.eln.fluid.FluidWorld::solverFailedAt))
                tag.putString("elnAlarm", mods.eln.i18n.I18N.tr("SOLVER FAILED: CALL BAUGHN"));
            else {
                String heavy = null;
                for (long q : at) if (heavy == null) heavy = mods.eln.fluid.FluidWorld.heavyAt(q);
                if (heavy != null) tag.putString("elnNote", heavy);
            }
            String ambient = mods.eln.misc.Utils.plotCelsius("", mods.eln.sim.Ambient.relative(acc.getLevel(), acc.getPosition()));
            // a voxel block: everything in it, the one under the asker's crosshair first; the client chooses (VoxelProbe)
            if (owner == null && acc.getBlockEntity() instanceof NodeBlockEntity be && be.getNode() instanceof mods.eln.node.voxel.VoxelNode v) {
                tag.put(VoxelProbe.KEY, VoxelProbe.entries(v, mods.eln.node.voxel.VoxelNode.eyeOf(acc.getPlayer()), acc.getHitResult().getLocation(), HEADLINE, ambient));
                return;
            }
            Map<String, String> m;
            try {
                m = owner != null ? owner.getWaila() : lines(acc, true);
            } catch (RuntimeException e) {
                return;   // (a device mid-change: no lines this time)
            }
            String title = owner != null ? owner.node.pickItem(null, acc.getLevel()).getHoverName().getString() : null;
            // (burnt: only cause + repair in headline; dead-device lines in details)
            ProbeLines p = VoxelProbe.lines(m, owner != null ? owner : burnable(acc), HEADLINE);
            VoxelProbe.put(tag, title, p, (owner != null ? !owner.thermalLoadList.isEmpty() : hasHeat(acc)) ? ambient : null);
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
            // a voxel block: of the entries sent, the one for what the crosshair is on now, picked as the outline picks it
            CompoundTag data = acc.getServerData();
            if (data.contains(VoxelProbe.KEY) && acc.getBlockEntity() instanceof mods.eln.node.voxel.VoxelNodeEntity v) {
                data = VoxelProbe.choose(data.getListOrEmpty(VoxelProbe.KEY),
                    VoxelProbe.clientAimed.apply(v, acc.getHitResult().getLocation()), v.parts());
                if (data == null) data = new CompoundTag();
            }
            String title = data.getStringOr("elnTitle", "");
            if (!title.isEmpty()) tooltip.replace(snownee.jade.api.JadeIds.CORE_OBJECT_NAME, Component.literal(title));
            String alarm = acc.getServerData().getStringOr("elnAlarm", "");
            if (!alarm.isEmpty()) tooltip.add(Component.literal(alarm).withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));
            String note = acc.getServerData().getStringOr("elnNote", "");
            if (!note.isEmpty()) tooltip.add(Component.literal(note).withStyle(net.minecraft.ChatFormatting.YELLOW));
            var list = data.getListOrEmpty("elnProbe");
            int head = data.getIntOr("elnHead", HEADLINE);
            boolean details = details(acc);
            int n = details ? list.size() : Math.min(head, list.size());
            addLines(tooltip, list, n);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    /**
     * A machine at another mod's block, by the eln:machine capability on the server (the live machine): its lines
     * under elnMachine. EA's own blocks are the node provider's (an element machine's lines are in its getWaila).
     */
    enum MachineServer implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor acc) {
            if (acc.getBlockEntity() == null || acc.getBlockEntity() instanceof NodeBlockEntity) return;
            Map<String, String> m;
            try {
                m = mods.eln.api.impl.MachineLines.at(acc.getLevel(), acc.getPosition());
            } catch (RuntimeException e) {
                return;   // (a block mid-change: no lines this time)
            }
            if (m.isEmpty()) return;
            ProbeLines p = ProbeLines.from(m, HEADLINE);
            tag.put("elnMachine", VoxelProbe.list(p));
            tag.putInt("elnMachineHead", p.headlineCount());
        }

        @Override
        public Identifier getUid() {
            return MACHINE_UID;
        }
    }

    /** The lines MachineServer sent: headline always, buffer & temperature while sneaking or with probe.details. */
    enum MachineClient implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor acc, IPluginConfig config) {
            var list = acc.getServerData().getListOrEmpty("elnMachine");
            int head = acc.getServerData().getIntOr("elnMachineHead", HEADLINE);
            boolean details = details(acc);
            addLines(tooltip, list, details ? list.size() : Math.min(head, list.size()));
        }

        @Override
        public Identifier getUid() {
            return MACHINE_UID;
        }
    }

    /** All the lines, not only the headline: sneaking, Jade's details key, or the config probe.details. */
    static boolean details(snownee.jade.api.Accessor<?> acc) {
        return mods.eln.ElnConfig.probeDetails || acc.showDetails() || acc.getPlayer() != null && acc.getPlayer().isShiftKeyDown();
    }

    /** The first n lines sent (key TAB value) as "key: value". */
    static void addLines(ITooltip tooltip, ListTag list, int n) {
        for (int i = 0; i < n; i++) {
            String s = list.getStringOr(i, "");
            int t = s.indexOf('\t');
            tooltip.add(t < 0 ? Component.literal(s) : Component.literal(s.substring(0, t) + ": ").append(Component.literal(s.substring(t + 1).trim())));
        }
    }

    /** A Tank Minecart: its tank's lines (TankCart.waila), from the server. */
    enum CartServer implements IServerDataProvider<snownee.jade.api.EntityAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag tag, snownee.jade.api.EntityAccessor acc) {
            if (!(acc.getEntity() instanceof mods.eln.oil.TankCart c)) return;
            try {
                ProbeLines p = ProbeLines.from(c.waila(), HEADLINE);
                tag.put("elnProbe", VoxelProbe.list(p));
                tag.putInt("elnHead", p.headlineCount());
            } catch (RuntimeException e) {
                // (a cart mid-change: no lines this time)
            }
        }

        @Override
        public Identifier getUid() {
            return CART_UID;
        }
    }

    /** The cart's lines: a headline, all of them while sneaking or with probe.details, as on a node. */
    enum CartClient implements snownee.jade.api.IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, snownee.jade.api.EntityAccessor acc, IPluginConfig config) {
            var list = acc.getServerData().getListOrEmpty("elnProbe");
            int head = acc.getServerData().getIntOr("elnHead", HEADLINE);
            boolean details = details(acc);
            addLines(tooltip, list, details ? list.size() : Math.min(head, list.size()));
        }

        @Override
        public Identifier getUid() {
            return CART_UID;
        }
    }

    static final Identifier CART_UID = Identifier.fromNamespaceAndPath("eln", "tank_minecart");
}
