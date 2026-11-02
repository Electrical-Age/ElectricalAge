package mods.eln.api.impl;

import mods.eln.api.ElnApi;
import mods.eln.api.Machine;
import mods.eln.api.MachineRegistry;
import mods.eln.api.MachineSpec;
import mods.eln.api.MachineView;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ElnApi.machines(): the registered block entity types and the machines hosted at their block entities. Block
 * entities joining a server level are queued (ServerLevelMixin: ServerLevel.onBlockEntityAdded, for placements and
 * chunk loads alike, mid chunk load, so nothing else is done there); each tick, before the simulator's step
 * (ServerTickEvent.Pre at high priority), hosted block entities that are gone (isRemoved: broken, replaced or
 * unloaded) are retired, the queue is hosted, and turned blocks reconnect. of() hosts at once if it comes first.
 */
public final class HostedMachines implements MachineRegistry {
    public static final HostedMachines INSTANCE = new HostedMachines();
    private static final Logger LOG = LoggerFactory.getLogger("eln");

    static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "eln");
    /** eln:machine: a hosted machine's saved state and its clients' view (MachineData). */
    public static final Supplier<AttachmentType<MachineData>> DATA = ATTACHMENTS.register("machine",
        () -> AttachmentType.builder(MachineData::new).serialize(MachineData.SERIALIZER).sync(MachineData.SYNC).build());

    /** A placeholder spec for the inert machine of a type never registered. */
    private static final MachineSpec UNREGISTERED = MachineSpec.consumer(1).ratedWatts(1).build();

    private final RegistrationBook<BlockEntityType<?>, Function<?, MachineSpec>> book = new RegistrationBook<>(
        "ElnApi.machines().register", "capabilities are registered; call it in FMLCommonSetupEvent");
    private final Queue<BlockEntity> added = new ConcurrentLinkedQueue<>();
    private final Map<BlockEntity, MachineNode> hosted = new LinkedHashMap<>();
    private final Map<BlockEntityType<?>, HostedMachine> unregistered = new ConcurrentHashMap<>();

    private HostedMachines() {
    }

    /** EA's mod constructor: the attachment, the capability, the tick and the world's start and stop. */
    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(RegisterCapabilitiesEvent.class, INSTANCE::capabilities);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, ServerTickEvent.Pre.class, e -> INSTANCE.tick());
        NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, e -> INSTANCE.clear());
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, e -> INSTANCE.clear());
    }

    // ---- MachineRegistry

    @Override
    public <T extends BlockEntity> void register(BlockEntityType<T> type, Function<? super T, MachineSpec> spec) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(spec, "spec");
        book.register(type, spec);
    }

    @Override
    public boolean registered(BlockEntityType<?> type) {
        return book.get(type) != null;
    }

    @Override
    public Machine of(BlockEntity be) {
        Objects.requireNonNull(be, "be");
        if (book.get(be.getType()) == null)
            return unregistered.computeIfAbsent(be.getType(), t -> new HostedMachine(UNREGISTERED,
                "block entity type " + name(t) + ", which is not registered (call ElnApi.machines().register in FMLCommonSetupEvent)"));
        Level level = be.getLevel();
        if (level != null && level.isClientSide()) return client(be);
        ServerThread.check();
        MachineData d = be.getData(DATA.get());
        if (d.handle == null) d.handle = new HostedMachine(hostedSpec(be), name(be.getType()) + " at " + be.getBlockPos());
        if (d.node == null && !be.isRemoved() && level instanceof ServerLevel) host(be, d);
        return d.handle;
    }

    /** The view at be for the capability: its machine's (client: synced); null for a type not registered. */
    MachineView view(BlockEntity be) {
        return book.get(be.getType()) == null ? null : of(be).view();
    }

    private Machine client(BlockEntity be) {
        MachineData d = be.getData(DATA.get());
        if (d.handle == null) d.handle = new ClientMachine(spec(be), d);
        return d.handle;
    }

    /** The registered function's spec for be (its type is registered: the function takes be's class). */
    @SuppressWarnings("unchecked")
    private MachineSpec spec(BlockEntity be) {
        MachineSpec s = ((Function<BlockEntity, MachineSpec>) book.get(be.getType())).apply(be);
        if (s == null) throw new IllegalStateException("the spec function registered for " + name(be.getType())
            + " returned null for the block entity at " + be.getBlockPos());
        return s;
    }

    /**
     * The server's spec for be: spec(be), refused (IllegalStateException) if it burns without a burn handler. A burn
     * can't be checked at build() or register() (the spec is the registered function's, per block entity), so it is
     * checked here, as the machine is made: of() throws, the hosting queue logs it and leaves the machine detached.
     * Not on the client, whose view never burns anything.
     */
    private MachineSpec hostedSpec(BlockEntity be) {
        MachineSpec s = spec(be);
        if (s.burnsOnOvervoltage() && s.burnHandler().isEmpty())
            throw new IllegalStateException(NO_HANDLER + " (" + name(be.getType()) + " at " + be.getBlockPos() + ", " + s + ")");
        return s;
    }

    /** The message for a hosted spec that burns without a handler. */
    public static final String NO_HANDLER = "a machine at another mod's block entity burns only with a handler: "
        + "use burnsOnOvervoltage(handler) to say what a burn does to the block (EA can't burn it in place)";

    /** A type's registry name, for messages. */
    static String name(BlockEntityType<?> t) {
        return String.valueOf(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(t));
    }

    // ---- lifecycle

    /** ServerLevel.onBlockEntityAdded (ServerLevelMixin): queues a registered block entity to be hosted next tick. */
    public static void added(BlockEntity be) {
        if (INSTANCE.book.get(be.getType()) != null) INSTANCE.added.add(be);
    }

    /** Before each step: retire the gone, host the new, follow turned blocks. */
    void tick() {
        if (NodeManager.instance == null) return;
        for (Iterator<MachineNode> i = hosted.values().iterator(); i.hasNext(); ) {
            MachineNode n = i.next();
            if (n.be.isRemoved() || n.be.getLevel() != n.level) {
                i.remove();
                retire(n);
            }
        }
        for (BlockEntity be; (be = added.poll()) != null; ) {
            if (be.isRemoved() || !(be.getLevel() instanceof ServerLevel)) continue;
            MachineData d = be.getData(DATA.get());
            if (d.node != null) continue;
            try {
                if (d.handle == null) d.handle = new HostedMachine(hostedSpec(be), name(be.getType()) + " at " + be.getBlockPos());
                host(be, d);
            } catch (RuntimeException e) {
                LOG.error("Electrical Age: hosting the machine of {} at {} failed; it stays detached", name(be.getType()), be.getBlockPos(), e);
            }
        }
        for (MachineNode n : new ArrayList<>(hosted.values())) n.followFacing();
        mods.eln.api.impl.lowlevel.CircuitHosts.INSTANCE.tick();   // (the low level's circuits, the same way)
    }

    private void host(BlockEntity be, MachineData d) {
        ServerLevel level = (ServerLevel) be.getLevel();
        Coordonate c = new Coordonate(be.getBlockPos(), level);
        NodeBase old = NodeManager.instance.getNodeFromCoordonate(c);
        if (old instanceof MachineNode h) {
            hosted.remove(h.be);
            retire(h);
        } else if (old != null) {
            LOG.warn("Electrical Age: a stale {} node at {} gives way to the machine of {}", old.getClass().getSimpleName(), c, name(be.getType()));
            mods.eln.node.NodeOrphans.remove(old);
        }
        MachineNode n = new MachineNode(level, be, d.handle.spec(), d);
        d.node = n;
        hosted.put(be, n);
        ((HostedMachine) d.handle).attach(n.host);
        if (ApiTrace.enabled()) ApiTrace.log("host machine %s at %s (%s)", d.handle.spec(), be.getBlockPos().toShortString(), name(be.getType()));
        n.place();
    }

    private static void retire(MachineNode n) {
        if (ApiTrace.enabled()) ApiTrace.log("retire machine %s at %s: %s, buffer %.3f J", n.host.spec(),
            n.be.getBlockPos().toShortString(), n.host.state(), n.host.bufferJoules());
        n.retire();
        if (n.data.handle instanceof HostedMachine m) m.detach();
    }

    /** The node hosting be's machine now, or null (tests). */
    public MachineNode node(BlockEntity be) {
        return hosted.get(be);
    }

    private void clear() {
        hosted.clear();
        added.clear();
        mods.eln.api.impl.lowlevel.CircuitHosts.INSTANCE.clear();
    }

    // ---- the capability

    private void capabilities(RegisterCapabilitiesEvent e) {
        book.close();
        for (BlockEntityType<?> t : book.keys()) registerView(e, t);
        // EA's own elements on the Machine API (server side: elements live there)
        e.registerBlockEntity(ElnApi.MACHINE, mods.eln.registry.ElnRegistry.TRANSPARENT_NODE_ENTITY.get(), (be, side) -> {
            if (be.getLevel() == null || be.getLevel().isClientSide()) return null;
            if (!(be.getNode() instanceof mods.eln.node.transparent.TransparentNode tn) || tn.element == null) return null;
            ElementHost h = ElementHost.find(tn.element.slowProcessList);
            return h == null ? null : h.view();
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerView(RegisterCapabilitiesEvent e, BlockEntityType<?> t) {
        e.registerBlockEntity(ElnApi.MACHINE, (BlockEntityType) t, (be, side) -> view((BlockEntity) be));
    }
}
