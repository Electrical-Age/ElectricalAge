package mods.eln.gridnode;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashSet;
import java.util.Optional;

/**
 * Created by svein on 23/08/15.
 */
public class GridLink implements INBTTReady {

    Coordonate a = new Coordonate(), b = new Coordonate();

    NbtElectricalLoad lineResistor = new NbtElectricalLoad("lr");

    boolean connected = false;
    // Drop this if the link is broken.
    ItemStack cable;
    private Direction as;
    private Direction bs;
    private Optional<GridElement> ae = Optional.empty();
    private Optional<GridElement> be = Optional.empty();
    private ElectricalConnection ar;
    private ElectricalConnection rb;

    public GridLink(Coordonate a, Coordonate b, Direction as, Direction bs, ItemStack cable) {
        assert a != null && b != null && as != null && bs != null && cable != null;
        this.a = a;
        this.b = b;
        this.as = as;
        this.bs = bs;
        this.cable = cable;
    }

    public GridLink(NBTTagCompound nbt, String str) {
        readFromNBT(nbt, str);
    }

    public static GridElement getElementFromCoordinate(Coordonate coord) {
        if (coord == null) return null;
        NodeBase base = NodeManager.instance.getNodeFromCoordonate(coord);
        TransparentNode node = (TransparentNode) base;
        if (node != null && node.element instanceof GridElement) {
            return (GridElement) node.element;
        } else {
            return null;
        }
    }

    static public boolean addLink(GridElement a, GridElement b, Direction as, Direction bs, ElectricalCableDescriptor cable, int cableLength) {
        // Check if these two nodes are already linked.
        for (GridLink link : a.gridLinkList) {
            if (link.links(a, b)) {
                return false;
            }
        }
        for (GridLink link : b.gridLinkList) {
            if (link.links(a, b)) {
                return false;
            }
        }

        // Makin' a Link. Where'd Zelda go?
        GridLink link = new GridLink(a.coordonate(), b.coordonate(), as, bs, cable.newItemStack(cableLength));
        // Yes, a metre's worth of resistance happens in the element itself.
        // This calculation is deliberate; there's also slack in the catenary, you see.
        cable.applyTo(link.lineResistor, cableLength);
        link.connect();

        return true;
    }

    public GridElement elementA() {
        if (!ae.isPresent()) {
            ae = Optional.of(getElementFromCoordinate(a));
        }
        return ae.get();
    }

    public GridElement elementB() {
        if (!be.isPresent()) {
            be = Optional.of(getElementFromCoordinate(b));
        }
        return be.get();
    }

    public boolean connect() {
        GridElement a = getElementFromCoordinate(this.a);
        GridElement b = getElementFromCoordinate(this.b);

        if (a == null || b == null || connected) {
            return false;
        }

        // Add link to simulator.
        ElectricalLoad aLoad = a.getGridElectricalLoad(as);
        ElectricalLoad bLoad = b.getGridElectricalLoad(bs);
        assert aLoad != null;
        assert bLoad != null;
        lineResistor.setCanBeSimplifiedByLine(true);

        ar = new ElectricalConnection(aLoad, lineResistor);
        rb = new ElectricalConnection(lineResistor, bLoad);
        Eln.simulator.addElectricalComponent(ar);
        Eln.simulator.addElectricalComponent(rb);

        // Add link to link lists.
        a.gridLinkList.add(this);
        b.gridLinkList.add(this);
        updateElement(a);
        updateElement(b);

        connected = true;
        return true;
    }

    private void updateElement(GridElement e) {
        e.updateIdealRenderAngle();
        // Need to also publish everything connected to this.
        HashSet<GridElement> s = new HashSet<GridElement>();
        s.add(e);
        for (GridLink link : e.gridLinkList) {
            s.add(link.elementA());
            s.add(link.elementB());
        }
        for (GridElement element : s) {
            element.needPublish();
        }
    }

    public void disconnect() {
        if (!connected)
            return;

        GridElement a = getElementFromCoordinate(this.a);
        GridElement b = getElementFromCoordinate(this.b);

        Eln.simulator.removeElectricalComponent(ar);
        Eln.simulator.removeElectricalComponent(rb);

        updateElement(a);
        updateElement(b);

        connected = false;
    }

    private boolean links(GridElement a, GridElement b) {
        if (this.a.equals(a.coordonate())) {
            return this.b.equals(b.coordonate());
        }
        if (this.a.equals(b.coordonate())) {
            return this.b.equals(a.coordonate());
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        a.readFromNBT(nbt, str + "a");
        b.readFromNBT(nbt, str + "b");
        as = Direction.readFromNBT(nbt, str + "as");
        bs = Direction.readFromNBT(nbt, str + "bs");
        lineResistor.readFromNBT(nbt, str + "lr");
        lineResistor.setRs(nbt.getDouble(str + "lrR"));
        cable = ItemStack.loadItemStackFromNBT(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        a.writeToNBT(nbt, str + "a");
        b.writeToNBT(nbt, str + "b");
        as.writeToNBT(nbt, str + "as");
        bs.writeToNBT(nbt, str + "bs");
        lineResistor.writeToNBT(nbt, str + "lr");
        nbt.setDouble(str + "lrR", lineResistor.getRs());
        cable.writeToNBT(nbt);
    }

    public void selfDestroy() {
        onBreakElement();
    }

    public ItemStack onBreakElement() {
        GridElement a = getElementFromCoordinate(this.a);
        GridElement b = getElementFromCoordinate(this.b);
        a.gridLinkList.remove(this);
        b.gridLinkList.remove(this);
        disconnect();
        return cable;
    }

    public Direction getSide(GridElement gridElement) {
        if (gridElement == elementA()) {
            return as;
        } else {
            return bs;
        }
    }

    public GridElement getOtherElement(GridElement gridElement) {
        if (gridElement == elementA()) {
            return elementB();
        } else {
            return elementA();
        }
    }
}
