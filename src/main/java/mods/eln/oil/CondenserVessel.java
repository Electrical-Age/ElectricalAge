package mods.eln.oil;

import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.nbt.NbtThermalLoad;
import net.minecraft.nbt.CompoundTag;

/**
 * A condenser's vessel and what works on it, shared by the 50V/200V turbines' condensers (CondenserElement) and the
 * Plant Steam Condenser (PlantCondenserElement; Baughn's standing rule: share code rather than copy it). The vessel sits
 * at the saturation pressure of its water; its shell (an EA thermal load) takes its heat; an air ejector pulls the air
 * out; a condensate pump (CondensatePump's curve) pushes water from its bottom to an outlet node; makeup water comes in through a float valve,
 * open below MAKEUP_OPEN of its volume as water, shut by MAKEUP_SHUT. How hard the pump and the ejector run is the
 * owner's (a turbine's shaft, or a shaft motor).
 */
public final class CondenserVessel {
    /** The float valve: open below this share of the vessel as water, shut at the other. */
    public static final double MAKEUP_OPEN = 0.15, MAKEUP_SHUT = 0.25;
    public static final double OUTLET_VOLUME = 0.00002, PORT_K = BoilerElement.PORT_K;
    public final NbtThermalLoad shell;
    public final double volume, nominalHeat;
    /** The condensate pump's curve (sized to the owner's turbine). */
    public final CondensatePump curve;
    public FluidNode vessel, outlet, inlet;
    public FluidDevice.Link pump, makeup;
    double[] loaded, loadedOut, loadedIn;
    /** Saved before the tanks shrank (CondenserElement: no "tank2" tag): its water is trimmed once on load. */
    boolean oldTank;
    /** Heat the shell took from the vessel (W, averaged) and condensate pumped (mB/s), for WAILA. */
    public double heatW, pumped;
    /** Totals since loaded (GameTests): condensate pumped (kg), air the ejector took (kg), steam exhausted into it (kg). */
    public double totalPumpedKg, totalAirKg, totalSteamKg;
    double meterHeat, meterPump, meterTime;
    /** The pump's back-pressure (Pa over the vessel's bottom) and its outlet's pressure (Pa), at the last step. */
    public double backPa, outletPa;
    /** The pump's speed (0..1.2 of its rating). */
    public double speed;

    public CondenserVessel(NbtThermalLoad shell, double volume, double nominalHeat, CondensatePump curve) {
        this.shell = shell;
        this.volume = volume;
        this.nominalHeat = nominalHeat;
        this.curve = curve;
    }

    /** Liquid as a share of the vessel. */
    public double floodShare() {
        return vessel == null ? 0 : vessel.mLiquid() / Water.RHO_L / vessel.volume;
    }

    /** A turbine's exhaust straight into it: m kg of steam (and air kg of air) with its enthalpy per kg (J/kg). */
    public void exhaust(double m, double air, double hSteam, double tAir) {
        if (vessel == null || m <= 0 && air <= 0) return;
        vessel.mWater += m;
        vessel.mAir += air;
        totalSteamKg += Math.max(0, m);
        vessel.energy += m * hSteam + air * Water.hAir(tAir);
        vessel.update();
    }

    /** Make the nodes and links at height y (restoring what was saved), ambient T for new ones. */
    public void initialize(double y, double ambientK) {
        // the shell: its air cooler takes the nominal heat DT_SHELL over the ambient
        shell.set(0.01, CondenserDescriptor.DT_SHELL / nominalHeat, nominalHeat * 10 / CondenserDescriptor.DT_SHELL);
        vessel = new FluidNode(volume, y, 1.0, false);
        restore(vessel, loaded, ambientK, true);
        outlet = new FluidNode(OUTLET_VOLUME, y, 0.25, false);
        restore(outlet, loadedOut, ambientK, false);
        outlet.lumpable = true;
        outlet.resistance = 1 / PORT_K;
        inlet = new FluidNode(OUTLET_VOLUME, y, 0.25, false);
        restore(inlet, loadedIn, ambientK, false);
        inlet.lumpable = true;
        inlet.resistance = 1 / PORT_K;
        // (into the vessel's top: the makeup falls in, it doesn't run back out)
        makeup = new FluidDevice.Link(inlet, 0, vessel, 1.0, PORT_K).kind(FluidEdge.Kind.CHECK);
        // g = rho k / mu x open: k for Qmax / H0; open carries the speed and the curve's chord (before)
        pump = new FluidDevice.Link(vessel, 0, outlet, 0, curve.freeKgPerS / curve.shutoffPa * Water.MU_L / Water.RHO_L).kind(FluidEdge.Kind.CHECK);
        pump.boost = curve.boost(0);
        pump.open = 0;
    }

    private void restore(FluidNode n, double[] saved, double ambientK, boolean isVessel) {
        if (saved != null) {
            // (a vessel saved before Baughn's smaller tanks, fuller than the float valve's level of its new one: the excess
            // water goes, with its share of the energy; else it was over FLOOD at once, and the turbine took water damage)
            double max = isVessel && oldTank ? MAKEUP_SHUT * volume * Water.RHO_L : Double.MAX_VALUE, keep = saved[0] > max ? max / saved[0] : 1;
            n.mWater = saved[0] * keep;
            n.mAir = saved[1];
            n.energy = saved[2] * keep;
            n.update();
        } else n.fill(0, Water.P_ATM, ambientK);
    }

    /**
     * Before a fluid step: the shell takes the vessel's heat (scaled by `cooling`, 0..1: a flooded plant condenser's
     * tubes are under water), the ejector pulls air (at `ejector`, 0..1), the float valve, the pump (open `pumpOpen`).
     */
    public void before(double dt, double cooling, double ejector, double pumpOpen) {
        if (vessel == null) return;
        // the shell takes the vessel's heat (EA thermal temperatures are on the 20 C scale)
        double shellK = shell.Tc + mods.eln.sim.PhysicalConstant.Tref;
        double q = cooling * (vessel.T() - shellK) / (CondenserDescriptor.DT_VESSEL / nominalHeat);
        // (at most half the way to the shell's temperature in a step: a vessel of thin air has next to no heat capacity,
        // and the explicit step overshot to -193 C: AdventurAgent)
        double toShell = (vessel.energy - mods.eln.sim.fluid.Flash.energyAt(vessel.mWater, vessel.mAir, vessel.volume, shellK)) / dt;
        q = toShell >= 0 ? Math.max(0, Math.min(q, 0.5 * toShell)) : Math.min(0, Math.max(q, 0.5 * toShell));
        vessel.heat -= q;
        shell.moveEnergyTo(q * dt);
        meterHeat += q * dt;
        // the air ejector, down to EJECTOR_FLOOR_PA of air (an empty vessel with nothing in it has no temperature:
        // AdventurAgent saw -193 C)
        double tNow = vessel.T(), floor = CondenserDescriptor.EJECTOR_FLOOR_PA * vessel.volume / (Water.R_AIR * Math.max(250, tNow));
        double air = Math.max(0, vessel.mAir - floor) * (1 - Math.exp(-dt * Math.max(0, ejector) / CondenserDescriptor.EJECTOR_TAU));
        if (air > 0) {
            double T = vessel.T();
            vessel.mAir -= air;
            vessel.energy -= air * Water.uAir(T);
            vessel.update();
            totalAirKg += air;
        }
        // the makeup's float valve
        if (makeup != null) makeup.open = Math.max(0, Math.min(1, (MAKEUP_SHUT - floodShare()) / (MAKEUP_SHUT - MAKEUP_OPEN)));
        if (pump != null) {
            // the curve's chord through where it runs now (CondensatePump): speed scales the flow, the head stays
            backPa = backPressure();
            speed = Math.max(0, Math.min(1.2, pumpOpen));
            pump.open = speed * curve.chordShare(backPa);
            pump.boost = curve.boost(backPa);
        }
    }

    /**
     * The pressure the pump works against now (Pa): its outlet's run over the vessel's bottom, less the fall between
     * them (both lumped as the network steps them: the outlet's own node is stale once it is part of a pipe run).
     */
    double backPressure() {
        FluidNode a = mods.eln.fluid.FluidWorld.nodeFor(vessel), b = mods.eln.fluid.FluidWorld.nodeFor(outlet);
        double pa = a.pressureAt(pump.drawA), pb = b.pressureAt(pump.drawB);
        outletPa = pb;
        return pb - pa - Water.RHO_L * mods.eln.sim.fluid.FluidNetwork.G * (a.z + pump.drawA * a.height - b.z - pump.drawB * b.height);
    }

    /** The pump's flow now against its back-pressure, at its speed, as a share of its rated (free) flow; 0: shut off. */
    public double flowShare() {
        return speed * curve.share(backPa);
    }

    /** After a fluid step: the meters; returns the condensate pumped this step (kg). */
    public double after(double dt) {
        double kg = pump != null && pump.edge != null ? Math.max(0, pump.edge.fLiquid) * dt : 0;
        totalPumpedKg += kg;
        meterPump += kg / EaUnits.KG_PER_MB_WATER;
        meterTime += dt;
        if (meterTime >= 1) {
            heatW = meterHeat / meterTime;
            pumped = meterPump / meterTime;
            meterHeat = meterPump = meterTime = 0;
        }
        return kg;
    }

    public void write(CompoundTag nbt) {
        if (vessel == null) return;
        put(nbt, "vessel", vessel);
        put(nbt, "outlet", outlet);
        put(nbt, "inlet", inlet);
    }

    static void put(CompoundTag nbt, String key, FluidNode n) {
        nbt.putDouble(key + "W", n.mWater);
        nbt.putDouble(key + "A", n.mAir);
        nbt.putDouble(key + "U", n.energy);
    }

    static double[] get(CompoundTag nbt, String key) {
        return nbt.contains(key + "W") ? new double[]{nbt.getDoubleOr(key + "W", 0), nbt.getDoubleOr(key + "A", 0), nbt.getDoubleOr(key + "U", 0)} : null;
    }

    public void read(CompoundTag nbt, boolean oldTank) {
        this.oldTank = oldTank;
        loaded = get(nbt, "vessel");
        loadedOut = get(nbt, "outlet");
        loadedIn = get(nbt, "inlet");
    }
}
