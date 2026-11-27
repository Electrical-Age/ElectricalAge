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
 * at the saturation pressure of its water; its shell (an EA thermal load) takes its heat through a heat link in the
 * fluid core's step (a radiator: the shell cools to the air, never below it); an air ejector, a gas pump to the air,
 * pulls the vessel's gas out (air, and some steam with it); a condensate pump (CondensatePump's curve) pushes water from its bottom to an outlet node; makeup water comes in through a float valve,
 * open below MAKEUP_OPEN of its volume as water, shut by MAKEUP_SHUT. How hard the pump and the ejector run is the
 * owner's (a turbine's shaft, or a shaft motor).
 */
public final class CondenserVessel {
    /** The float valve: open below this share of the vessel as water, shut at the other (floatOpening). */
    public static final double MAKEUP_OPEN = 0.15, MAKEUP_SHUT = 0.25;

    /**
     * The makeup float valve's opening (0..1) at this hotwell level (share of the vessel as water): the cube of the
     * level's distance under MAKEUP_SHUT across the band, as the boiler's feed float (BoilerElement.feedFloat). The valve
     * is set once a tick from the level, and its port passes ~23 kg/s a bar wide open, so a straight line opened so
     * steeply at the shut mark that one tick's opening took a band's worth of water and shut it again. On a player's
     * small plant (2026-11-17) whose makeup inlet was teed into its condensate pump's discharge, the float then passed
     * the pump's recirculation in gulps of ~150 g/s one tick in ten; each gulp emptied the feed line from 10 to ~1.5
     * bar, refilling it cost the fluid core 100-300 Newton iterations a step, and the network never slept. With the cube
     * it holds a steady opening a few tenths of a percent under the shut mark; wide open it fills as fast as before.
     */
    public static double floatOpening(double flood) {
        double x = Math.max(0, Math.min(1, (MAKEUP_SHUT - flood) / (MAKEUP_SHUT - MAKEUP_OPEN)));
        return x * x * x;
    }
    /** The ejector's air offtake: this share up the vessel (over the tube bundle; flooding past it drowns it). */
    public static final double AIR_OFFTAKE = 0.6;
    public static final double OUTLET_VOLUME = 0.00002, PORT_K = BoilerElement.PORT_K;
    public final NbtThermalLoad shell;
    public final double volume, nominalHeat;
    /** The condensate pump's curve (sized to the owner's turbine). */
    public final CondensatePump curve;
    public FluidNode vessel, outlet, inlet;
    public FluidDevice.Link pump, makeup, ejector;
    /** Where the ejector discharges: the air, a fixed node at 1 atm. */
    FluidNode air;
    double[] loaded, loadedOut, loadedIn;
    /** The volume it was saved with, if bigger than its own (m^3; 0: saved as it is): trimmed once on load. */
    double savedVolume;
    /** Heat the shell took from the vessel (W, averaged) and condensate pumped (mB/s), for WAILA. */
    public double heatW, pumped;
    /** Totals since loaded (GameTests): condensate pumped (kg), air the ejector took (kg), steam exhausted into it (kg). */
    public double totalPumpedKg, totalAirKg, totalSteamKg, totalEjectedSteamKg;
    double meterHeat, meterPump, meterTime;
    /** The pump's back-pressure (Pa over the vessel's bottom) and its outlet's pressure (Pa), at the last step. */
    public double backPa, outletPa;
    /** What the owner lets the makeup take (0..1, a signal: 0 shut, 1 as the float valve has it), and the float valve's
     *  own opening (0..1). */
    public double makeupGate = 1, floatOpen;
    /** The pump's speed (0..1.2 of its rating). */
    public double speed;
    /**
     * The condensate pump's level control (Baughn 2026-11-14, from a player's plant: two condensers on one condensate
     * line, one pump fed the boilers on makeup water while the other delivered nothing and its hotwell flooded its
     * turbine). At or under the makeup's shut level the pump has LOW_HEAD of its shut-off head and LOW_FLOW of its
     * flow; both rise with the water to the whole curve at LEVEL_FULL of the vessel. So against boilers whose feed
     * floats throttle, a low hotwell's pump shuts off first and the fuller one delivers; a low hotwell still fills
     * cold boilers and makes up losses into a boiler up to LOW_HEAD of the shut-off. While the line is under that
     * head a hotwell at LEVEL_FULL passes more than its turbine's condensate at full power, so it can't fill further.
     * LOW_FLOW is 1 (2026-11-15, a player's small plant on makeup water ran its boiler dry at 10 bar): the makeup holds a
     * plant that loses water at the makeup's shut level, and with 0.3 of its flow there the pump returned ~18 % of its
     * rated flow against 10 bar, less than its turbine's steam; the water the plant lacked was missed in the boiler, which
     * drained until its bare heating surface made no more steam than that. The head alone shares a line: a low hotwell's
     * pump stops once the fuller one holds the line over LOW_HEAD of the shut-off.
     * LOW_HEAD is the pump's rated head, RATED_OVER_NOMINAL (15 bar for a 10 bar turbine; 2026-11-15, it was 1.6 / 1.8,
     * 16 bar). With head alone a makeup-level pump passes water whenever the line is under its head, so on a shared line
     * the busy hotwell has to rise until its head holds the line there; at 16 bar a full hotwell's pump passed only
     * ~1.2 times its rated condensate, and a busy unit beside an idling one (whose pump turns full on next to no
     * condensate) flooded its turbine (GameTest fluid_two_unit_plant_long_idle). Against a 15 bar line a hotwell at
     * LEVEL_FULL passes speed x RATED_MARGIN times its rated condensate, so the busy one settles under LEVEL_FULL unless
     * it condenses more than that. The cost: a makeup-level hotwell feeds a boiler only under 15 bar (still more than
     * the old 0.3 of the flow at 16 bar up to ~14.9 bar).
     */
    public static final double LEVEL_FULL = 0.40, LOW_HEAD = CondensatePump.RATED_OVER_NOMINAL / CondensatePump.SHUTOFF_OVER_NOMINAL, LOW_FLOW = 1.0;
    /** The level control's share of the curve now (0 at the makeup's shut level and under, 1 from LEVEL_FULL), and
     *  the shut-off head it leaves the pump (Pa). */
    public double levelShare, headPa;
    /** The share of its flow the pump's suction leaves it now (suction). */
    public double suctionShare = 1;

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
        // (its curve is a pump in the fluid core's solve: speed scales the flow, the shut-off head stays)
        pump = new FluidDevice.Link(vessel, 0, outlet, 0, PORT_K).pump(curve.freeKgPerS, curve.shutoffPa);
        pump.speed = 0;
        // (the air ejector, a gas pump in the fluid core's step from the vessel's top to the air, Baughn 2026-11-10:
        // make it real. It sweeps the vessel's volume every EJECTOR_TAU seconds and lifts to 1 atm from
        // EJECTOR_FLOOR_PA; what it draws is the vessel's gas, steam with the air, so a plant loses a little water to it;
        // its air offtake is AIR_OFFTAKE up the shell, so a flooded condenser drowns it and loses its vacuum)
        air = new FluidNode(1.0, y, 1.0, true);
        air.fill(0, Water.P_ATM, ambientK);
        ejector = new FluidDevice.Link(vessel, AIR_OFFTAKE, air, 0.5, PORT_K)
            .gasPump(volume / CondenserDescriptor.EJECTOR_TAU, Water.P_ATM - CondenserDescriptor.EJECTOR_FLOOR_PA);
        ejector.speed = 0;
    }

    private void restore(FluidNode n, double[] saved, double ambientK, boolean isVessel) {
        if (saved != null) {
            if (isVessel && savedVolume > volume) {
                // (a vessel saved bigger, before Baughn's smaller tanks: its water over the float valve's shut level of the
                // new one goes, and its air over 1 atm's worth, at the temperature it had; else it was over FLOOD at once
                // and the turbine took water damage, or its air stood at many bar against the turbine's exhaust)
                // (a corrupt save, NaN or negative: its temperature the ambient, its masses healed below)
                boolean sound = saved[0] >= 0 && saved[1] >= 0 && Double.isFinite(saved[0]) && Double.isFinite(saved[1]) && Double.isFinite(saved[2]);
                double T = sound ? new mods.eln.sim.fluid.Flash().solve(saved[0], saved[1], saved[2], savedVolume, 300).T : ambientK;
                if (!(T > 120 && T < 4000)) T = ambientK;
                n.mWater = Math.min(saved[0], MAKEUP_SHUT * volume * Water.RHO_L);
                n.mAir = Math.min(saved[1], Water.P_ATM * (volume - n.mWater / Water.RHO_L) / (Water.R_AIR * T));
                n.extra = null;
                n.energy = n.energyAt(T);
                mods.eln.fluid.FluidSaves.heal(n, ambientK, null);
            } else {
                mods.eln.fluid.FluidSaves.restore(n, saved, ambientK, null);
            }
            n.update();
        } else n.fill(0, Water.P_ATM, ambientK);
    }

    /**
     * Before a fluid step: the shell takes the vessel's heat (scaled by `cooling`, 0..1: a flooded plant condenser's
     * tubes are under water), the ejector pulls air (at `ejector`, 0..1), the float valve, the pump (open `pumpOpen`).
     */
    public void before(double dt, double cooling, double ejector, double pumpOpen) {
        if (vessel == null) return;
        // the shell takes the vessel's heat through a heat link inside the fluid core's step (EA thermal temperatures
        // are on the 20 C scale): implicit, so a vessel of thin air settles at the shell's temperature instead of
        // overshooting it (the explicit heat needed a limiter: AdventurAgent saw -193 C)
        vessel.wallT = shell.Tc + mods.eln.sim.PhysicalConstant.Tref;
        vessel.wallUA = Math.max(0, cooling) * nominalHeat / CondenserDescriptor.DT_VESSEL;
        if (this.ejector != null) this.ejector.speed = Math.max(0, Math.min(1, ejector));
        // the makeup's float valve
        floatOpen = floatOpening(floodShare());
        // (an unpiped makeup flange is blind: open, its few mL of air leaked into the vacuum until the node held next to
        // nothing, colder than the core's EOS goes, and the step stopped converging)
        if (makeup != null) makeup.open = piped(inlet) ? floatOpen * Math.max(0, Math.min(1, makeupGate)) : 0;
        if (pump != null) {
            backPa = backPressure();
            speed = Math.max(0, Math.min(1.2, pumpOpen));
            levelShare = levelShare(floodShare());
            headPa = curve.shutoffPa * headLevel(levelShare);
            pump.h0 = headPa;
            suctionShare = suction(floodShare());
            pump.speed = speed * flowLevel(levelShare) * suctionShare;
        }
    }

    /** Whether a port's node is coupled to anything: part of a pipe run, or with an edge besides its own link. */
    static boolean piped(FluidNode n) {
        return mods.eln.fluid.FluidWorld.runSize(n) > 1 || mods.eln.fluid.FluidWorld.edgesOf(n).size() > 1;
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

    /** The level control's share of the curve for a vessel this full of water (0..1). */
    public static double levelShare(double flood) {
        return Math.max(0, Math.min(1, (flood - MAKEUP_SHUT) / (LEVEL_FULL - MAKEUP_SHUT)));
    }

    /** The share of the pump's shut-off head, and of its flow, the level control leaves it. */
    public static double headLevel(double levelShare) {
        return LOW_HEAD + (1 - LOW_HEAD) * levelShare;
    }

    /**
     * The hotwell level (share of the vessel as water) under which the pump has lost its suction. From MAKEUP_OPEN down
     * to it the pump's flow falls to nothing, as a condensate pump's does with too little water over its suction.
     * With head-only level control (LOW_FLOW 1) a plant without makeup water otherwise pumped its hotwell dry into
     * the boiler until the boiler's feed float shut. A flue-heated exchanger on that feed line then held water
     * shut in, which boiled, burst the iron pipe at 25 bar and cycled the plant (GameTest heat_exchanger_condensate_reheat).
     * The water is kept in the hotwell instead, and the boiler stays under its float. Over MAKEUP_OPEN, where a makeup
     * float holds a plant that loses water, it changes nothing.
     */
    public static final double SUCTION_LOW = 0.05;

    /** The share of its flow the pump's suction leaves it at this hotwell level (0..1). */
    public static double suction(double flood) {
        return Math.max(0, Math.min(1, (flood - SUCTION_LOW) / (MAKEUP_OPEN - SUCTION_LOW)));
    }

    public static double flowLevel(double levelShare) {
        return LOW_FLOW + (1 - LOW_FLOW) * levelShare;
    }

    /** The pump's flow now against its back-pressure, at its speed and its hotwell's level, as a share of its rated
     *  (free) flow; 0: shut off. */
    public double flowShare() {
        return headPa <= 0 ? 0 : speed * flowLevel(levelShare) * suctionShare * Math.sqrt(1 - Math.max(0, Math.min(1, backPa / headPa)));
    }

    /**
     * Jade's line on the pump's outlet (Baughn 2026-11-03): the pressure it works against and its flow there as a share
     * of its rated (free) flow at its speed now, or that it is shut off and needs a pipe pump after it.
     */
    public String outletStatus() {
        double bar = outletPa / 1e5;
        if (curve.share(backPa) < 0.05) return String.format(mods.eln.i18n.I18N.tr("%.1f bar (stalled — head too high)"), bar);
        if (speed < 0.01) return String.format(mods.eln.i18n.I18N.tr("%.1f bar (idle)"), bar);
        if (flowShare() < 0.05 * speed) return String.format(mods.eln.i18n.I18N.tr("%.1f bar (low hotwell)"), bar);
        return String.format(mods.eln.i18n.I18N.tr("%.1f bar (%.0f%% flow)"), bar, flowShare() * 100);
    }

    /** After a fluid step: the meters; returns the condensate pumped this step (kg). */
    public double after(double dt) {
        // the heat that went out through the shell this step goes into its thermal load
        if (ejector != null && ejector.edge != null) {
            totalAirKg += Math.max(0, ejector.edge.fAir) * dt;
            totalEjectedSteamKg += Math.max(0, ejector.edge.fVapour) * dt;
        }
        double q = vessel == null ? 0 : -vessel.wallJ;
        shell.moveEnergyTo(q);
        meterHeat += q;
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
        mods.eln.fluid.FluidSaves.put(nbt, key, n);
    }

    static double[] get(CompoundTag nbt, String key) {
        return nbt.contains(key + "W") ? mods.eln.fluid.FluidSaves.array(nbt, key) : null;
    }

    /** Read what was saved; savedVolume: the vessel's volume then (m^3), or 0 if it is its own. */
    public void read(CompoundTag nbt, double savedVolume) {
        this.savedVolume = savedVolume;
        loaded = get(nbt, "vessel");
        loadedOut = get(nbt, "outlet");
        loadedIn = get(nbt, "inlet");
    }
}
