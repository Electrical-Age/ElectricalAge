package mods.eln.simplenode.energyconverter;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import mods.eln.Other;
import mods.eln.misc.Utils;
import net.minecraft.nbt.NBTTagCompound;

public class EnergyConverterElnToOtherFireWallOc {

    EnergyConverterElnToOtherEntity e;
    Node node;

    protected boolean addedToNetwork = false;

    public EnergyConverterElnToOtherFireWallOc(EnergyConverterElnToOtherEntity e) {
        this.e = e;
    }

    public void updateEntity() {
        // On the first update, try to add our node to nearby networks. We do
        // this in the update logic, not in validate() because we need to access
        // neighboring tile entities, which isn't possible in validate().
        // We could alternatively check node != null && node.network() == null,
        // but this has somewhat better performance, and makes it clearer.
        if (e.getWorld().isRemote) return;
        if (!addedToNetwork) {
            addedToNetwork = true;
            Network.joinOrCreateNetwork(e);
        } else {
            if (node != null) {
                if (e.getNode() == null) return;
                Connector c = ((Connector) node);
                EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) e.getNode();
                double eMax = node.getOtherModEnergyBuffer(Other.getElnToOcConversionRatio());
                eMax = Math.min(Math.min(eMax, c.globalBufferSize() - c.globalBuffer()), node.descriptor.oc.outMax);
                if (c.tryChangeBuffer(eMax)) {
                    node.drawEnergy(eMax, Other.getElnToOcConversionRatio());
                }
            }
        }
    }

    public void onChunkUnload() {
        // Make sure to remove the node from its network when its environment,
        // meaning this tile entity, gets unloaded.
        if (e.getWorld().isRemote) return;
        if (node != null) node.remove();
    }

    public void invalidate() {
        // Make sure to remove the node from its network when its environment,
        // meaning this tile entity, gets unloaded.
        if (e.getWorld().isRemote) return;
        if (node != null) node.remove();
    }

    // ----------------------------------------------------------------------- //
    public void readFromNBT(final NBTTagCompound nbt) {
        // The host check may be superfluous for you. It's just there to allow
        // some special cases, where getNode() returns some node managed by
        // some other instance (for example when you have multiple internal
        // nodes in this tile entity).
        if (node != null && node.host() == this) {
            // This restores the node's address, which is required for networks
            // to continue working without interruption across loads. If the
            // node is a power connector this is also required to restore the
            // internal energy buffer of the node.
            node.load(nbt.getCompoundTag("oc:node"));
        }
    }

    public void writeToNBT(final NBTTagCompound nbt) {
        // See readFromNBT() regarding host check.
        if (node != null && node.host() == this) {
            final NBTTagCompound nodeNbt = new NBTTagCompound();
            node.save(nodeNbt);
            Utils.newNbtTagCompund(nodeNbt, "oc:node");
        }
    }

    public void constructor() {
        node = li.cil.oc.api.Network.newNode(e, Visibility.None).withConnector().create();
        Utils.println("******** C " + node);
    }
}
