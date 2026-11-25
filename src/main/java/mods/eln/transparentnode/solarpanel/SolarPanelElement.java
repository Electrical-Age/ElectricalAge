package mods.eln.transparentnode.solarpanel;

import mods.eln.Eln;
import mods.eln.api.Meters;
import mods.eln.i18n.I18N;
import mods.eln.item.SolarTrackerDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.GhostPowerNode;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.network.ClientValues;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.PvSourceBipole;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SolarPanelElement extends TransparentNodeElement {
    /** 1.12 SolarPanelContainer.trackerSlotId */
    public static final int trackerSlotId = 0;


    SolarPanelDescriptor descriptor;
    NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");
    VoltageSource positiveSrc = new VoltageSource("posSrc", positiveLoad, null);
    VoltageSource negativeSrc = new VoltageSource("negSrc", negativeLoad, null);

    /** The panel's diode curve driving its two terminals (Baughn's option B+, board #79). */
    final PvSourceBipole pv;
    /** The panel's own temperature: sunlight heats it, the power taken out and the air cool it. */
    final NbtThermalLoad panelHeat = new NbtThermalLoad("panel");

    SolarPannelSlowProcess slowProcess = new SolarPannelSlowProcess(this);

    public double panelAlpha = Math.PI / 2;
    private GhostPowerNode groundNode = null;

    public SolarPanelElement(TransparentNode transparentNode,
                             TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        this.descriptor = (SolarPanelDescriptor) descriptor;

        grounded = false;

        pv = new PvSourceBipole(positiveLoad, negativeLoad, positiveSrc, negativeSrc, this.descriptor.newCurve());
        double rp = this.descriptor.panelRp();
        panelHeat.set(1e9, rp, SolarPanelDescriptor.THERMAL_TAU / rp);
        panelHeat.setAsSlow();
        thermalLoadList.add(panelHeat);
        // driven backwards hard (a reversed battery through its bypass) it overheats and burns: 150 C
        slowProcessList.add(new ThermalLoadWatchDog().set(panelHeat).setTMax(130).set(new Failure(this).electronics()));

        electricalLoadList.add(positiveLoad);
        electricalLoadList.add(negativeLoad);


        electricalComponentList.add(positiveSrc);
        electricalComponentList.add(negativeSrc);

        slowProcessList.add(slowProcess);
    }

    @Override
    public void connectJob() {
        Eln.simulator.mna.addProcess(pv);
        super.connectJob();
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        Eln.simulator.mna.removeProcess(pv);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        if (groundNode == null) {
            // Single-tile solar panel.
            if (side == front.left()) return positiveLoad;
            if (side == front.right() && !grounded) return negativeLoad;
        } else {
            return positiveLoad;
        }
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (groundNode == null) {
            // Single-tile solar panel.
            if (side == front.left()) return NodeBase.maskElectricalPower;
            if (side == front.right() && !grounded) return NodeBase.maskElectricalPower;
        } else {
            if (side == front) return NodeBase.maskElectricalPower;
        }
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(positiveLoad.getU() - negativeLoad.getU(), positiveLoad.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T", panelHeat.Tc);
    }

    /** The panel's temperature, absolute C (thermal loads keep K above 20 C). */
    public double panelCelsius() {
        return 20 + panelHeat.Tc;
    }

    /** Air cooling at this light: still air, faster in wind and rain (from the slow process's refresh). */
    void coolPanel(double light) {
        double rain = 0, wind = 0;
        if (node != null && node.coordonate.getWorldExist()) {
            double[] w = Utils.weatherAt(node.coordonate.world(), node.coordonate.pos());
            rain = Math.max(w[0], w[1]);
            wind = Math.min(Utils.getWind(node.coordonate, 0) / 10.0, 1.0);
        }
        panelHeat.Rp = descriptor.panelRp() / (1 + 0.6 * rain + 0.5 * wind);
    }

    @Override
    public void initialize() {
        descriptor.applyTo(positiveLoad);
        descriptor.applyTo(negativeLoad);

        if (descriptor.groundCoordinate != null) {
            GhostPowerNode n = new GhostPowerNode(node.coordonate, front, descriptor.groundCoordinate, negativeLoad);
            n.initialize();
            groundNode = n;
        }

        connect();
    }

    @Override
    public void onBreakElement() {
        super.onBreakElement();
        if (groundNode != null)
            groundNode.onBreakBlock();
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        return descriptor.canRotate && inventory.take(entityPlayer.getMainHandItem(), this, true, false);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        pv.writeToNBT(nbt, "pv");
        nbt.putDouble("panelAlpha", panelAlpha);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        pv.readFromNBT(nbt, "pv");   // (saves before B+ carry powerSourceP/Umax/Imax: re-derived, ignored)
        panelAlpha = ClientValues.load(nbt.getDoubleOr("panelAlpha", 0.0), descriptor.alphaMin, descriptor.alphaMax, descriptor.alphaTrunk(Math.PI / 2));
    }


    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(!Utils.isEmpty(getInventory().getItem(trackerSlotId)));
            stream.writeFloat((float) panelAlpha);
            node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream);
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public static final byte unserializePannelAlpha = 0;

    public byte networkUnserialize(DataInputStream stream) {

        byte packetType = super.networkUnserialize(stream);
        try {
            switch (packetType) {
                case unserializePannelAlpha:
                    panelAlpha = ClientValues.clamp(stream.readFloat(), descriptor.alphaMin, descriptor.alphaMax, panelAlpha);
                    needPublish();
                    break;

                default:
                    return packetType;
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
        return unserializeNulldId;
    }

    private final AutoAcceptInventoryProxy inventory =
        (new AutoAcceptInventoryProxy(new TransparentNodeElementInventory(1, 64, this)))
            .acceptIfEmpty(0, SolarTrackerDescriptor.class);

    @Override
    public Container getInventory() {
        return inventory.getInventory();
    }

    @Override
    public boolean hasGui() {
        return descriptor.canRotate;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new LinkedHashMap<String, String>();
        info.put(I18N.tr("Power"), Meters.watts(Math.max(0, pv.getP())) + " @ " + Meters.volts(pv.getU()));
        double[] mpp = pv.curve().maxPowerPoint();
        info.put(I18N.tr("Best"), mpp[1] > 0 ? Meters.watts(mpp[1]) + " @ " + Meters.volts(mpp[0]) : I18N.tr("no light"));
        info.put(I18N.tr("Panel temperature"), Meters.celsius(panelCelsius()));
        info.put(I18N.tr("Sun angle"), Utils.plotValue(((slowProcess.getSolarAlpha()) * (180 / Math.PI)) - 90, "\u00B0"));
        info.put(I18N.tr("Panel angle"), Utils.plotValue((panelAlpha * (180 / Math.PI)) - 90, "\u00B0"));
        return info;
    }

}
