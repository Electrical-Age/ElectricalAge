package mods.eln.gui;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.INodeContainer;
import mods.eln.node.NodeMenus;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The menu of every EA device GUI (P4; 1.12: the device's Container from GuiHandler, or none). Device slots from the
 * descriptor's {@link MenuSpec}, then the player inventory (27 + 9) as 1.12 BasicContainer. GUI controls keep using EA
 * packets. The open-data buffer carries pos, slot (six-node: SixNodeElement.slot; transparent: side), node kind and descriptor id: the client rebuilds the same slots
 * from the descriptor (it has no nodes).
 */
public class ElnNodeMenu extends AbstractContainerMenu implements INodeContainer {
    public static final byte SIX = 0, TRANSPARENT = 1;

    public final BlockPos pos;
    /** Six-node: the element's slot (0..5: centred on that face); transparent node: the side clicked. */
    public final int slot;
    public final byte kind;
    public final int descriptorId;
    public final MenuSpec spec;
    public final Container deviceInventory;
    /** Server only (null on the client). */
    private final @Nullable NodeBase node;
    public final int deviceSlots;

    public ElnNodeMenu(int containerId, Inventory playerInventory, Container deviceInventory, MenuSpec spec, @Nullable NodeBase node,
                       BlockPos pos, int slot, byte kind, int descriptorId) {
        super(ElnRegistry.NODE_MENU.get(), containerId);
        this.pos = pos;
        this.slot = slot;
        this.kind = kind;
        this.descriptorId = descriptorId;
        this.spec = spec;
        this.deviceInventory = deviceInventory;
        this.node = node;
        for (SlotSpec s : spec.slots()) addSlot(new ElnSlot(deviceInventory, s, playerInventory.player.level()));
        deviceSlots = spec.slots().size();
        if (spec.playerInventory()) {
            int x0 = spec.inventoryX(), y0 = spec.inventoryY();
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 9; j++) addSlot(new Slot(playerInventory, j + i * 9 + 9, x0 + j * 18, y0 + i * 18));
            for (int i = 0; i < 9; i++) addSlot(new Slot(playerInventory, i, x0 + i * 18, y0 + 58));
        }
    }

    /**
     * Client: slot and inventory positions from the device's screen layout (P4b; set by the client). The server keeps
     * MenuSpec's positions: it never uses them.
     */
    public static java.util.function.BiFunction<MenuSpec, Object, MenuSpec> clientLayout = (s, d) -> s;

    /** Client side: rebuilt from the open data; the device inventory is a stand-in the server keeps in sync. */
    public static ElnNodeMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int slot = buf.readByte();
        byte kind = buf.readByte();
        int id = buf.readInt();
        MenuSpec spec = clientLayout.apply(NodeMenus.spec(kind, id), NodeMenus.descriptor(kind, id));
        return new ElnNodeMenu(containerId, playerInventory, new SimpleContainer(Math.max(1, spec.inventorySize())), spec, null,
            pos, slot, kind, id);
    }

    public void writeOpenData(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(slot);
        buf.writeByte(kind);
        buf.writeInt(descriptorId);
    }

    @Override
    public NodeBase getNode() {
        return node;
    }

    @Override
    public int getRefreshRateDivider() {
        // INodeContainer: NodeServer publishes every (1 + divider) ticks; -1 (not a 1.12 node container): rarely
        return spec.refreshRateDivider() < 0 ? 1_000_000 : spec.refreshRateDivider();
    }

    @Override
    public boolean stillValid(Player player) {
        if (node != null) {
            if (node.isDestructing()) return false;
            NodeBase now = NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos, player.level()));
            if (now != node) return false;
        }
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    /**
     * Shift-click: device slot -> player inventory (from the end), player slot -> the device slots, else between the
     * main inventory and the hotbar (1.12 BasicContainer.transferStackInSlot). Returns EMPTY unless something moved:
     * 26.x repeats the click while the result is non-empty.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int total = slots.size();
        boolean moved;
        if (index < deviceSlots) {
            moved = moveItemStackTo(stack, deviceSlots, total, true);
        } else {
            moved = deviceSlots > 0 && moveItemStackTo(stack, 0, deviceSlots, false);
            if (!moved && spec.playerInventory()) {
                if (index < deviceSlots + 27) moved = moveItemStackTo(stack, deviceSlots + 27, total, false);
                else moved = moveItemStackTo(stack, deviceSlots, deviceSlots + 27, false);
            }
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }
}
