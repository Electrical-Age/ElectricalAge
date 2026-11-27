package mods.eln.oil;

import mods.eln.gl.GL11;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 26.x (notes/oil): the boiler. Water piped in (Baughn: piped only), heat from EA's thermal network on any side
 * (electric heater, fuel heat furnace, heat furnace); above 100 °C the heat above boiling turns water into steam:
 * 175 J per mB of water (67.5 g: 20 -> 100 °C plus 2257 J/g), and 1 mB of water makes 67.5 mB of steam (design v2:
 * EA steam is 1 mg per mB). Steam is drawn off through the fluid capability (pipes) to EA's steam turbine.
 * Models: mods.eln.cad.models.Boiler; the Large Boiler's, mods.eln.cad.models.LargeBoiler.
 */
public class BoilerDescriptor extends TransparentNodeDescriptor {
    public static final double J_PER_MB_WATER = 175, STEAM_PER_WATER = mods.eln.sim.fluid.EaUnits.STEAM_MB_PER_WATER_MB, BOIL_C = 100;
    final Obj3D obj;
    /**
     * What sets a boiler's size (the Boiler's values; large() gives the Large Boiler's): the vessel (m3) and its rated
     * water (mB, the gauge glass's full mark); the shell's heat capacity (J/K), its wall into the water (K/W), its
     * thermal faces' series resistance and its loss to the air (K/W); the shell's limit (K over ambient, its
     * watchdog) and the pressure it bursts at (Pa); the strongest explosion its burst makes (4: TNT).
     */
    public double volume = BoilerElement.VOLUME, shellC = BoilerElement.SHELL_C, shellToWater = BoilerElement.SHELL_TO_WATER_K_PER_W;
    public double faceRs = 0.01, lossRp = 1.0, shellMaxK = 400, burstPa = BoilerElement.BURST_PA;
    public int waterMb = BoilerTanks.WATER_MB;
    public float maxBoom = 4;
    public boolean large;
    /** The vessel's height (blocks): 2 for the 2x2x2 Large Boiler. */
    public double height = 1.0;

    public BoilerDescriptor(String name, String model) {
        this(name, model, BoilerElement.class);
    }

    public BoilerDescriptor(String name, String model, Class<?> elementClass) {
        super(name, elementClass, BoilerRender.class);
        obj = Eln.obj.getObj(model);
        cableTerminals = mods.eln.cable.CableTerminals.FRONT;   // the phase signal's plate (mods.eln.cad.models.Boiler)
    }

    /**
     * The Large Boiler (Baughn 2026-11-15: one block, about four Boilers, of alloy plate, with a higher pressure limit
     * and accordingly a larger boom): four times the vessel, the water, the shell and the wall (166 kW at 20 bar
     * against 41.5), faces of a quarter the resistance (a furnace against it runs cooler, and several furnaces on
     * thermal cable can share a face), 500 bar against 350, and a burst of up to 6 against 4.
     */
    public BoilerDescriptor large() {
        large = true;
        volume *= 4;
        waterMb *= 4;
        shellC *= 4;
        shellToWater /= 4;
        faceRs /= 4;
        lossRp = 0.4;   // (about 2.5 times the Boiler's surface)
        burstPa = 500e5;
        maxBoom = 6;
        return this;
    }

    /**
     * The Large Boiler as Baughn asked for it (2026-11-15: "It needs at least one face free for a signal output. How
     * about making it 2x2x2, physically?"): large()'s ratings in a 2x2x2 body, one node at its front-bottom-left block
     * and seven ghost blocks to the right, back and up (LargeBoilerElement: six thermal faces on its lower blocks, two
     * steam outlets on top, the feed flange and the level signal on the front). Its model
     * (mods.eln.cad.models.LargeBoiler) is drawn full size from the origin block.
     */
    public static BoilerDescriptor twoByTwo(String name, String model) {
        var d = new BoilerDescriptor(name, model, LargeBoilerElement.class).large();
        d.height = 2;
        var g = new mods.eln.ghost.GhostGroup();
        g.addRectangle(0, 1, 0, 1, 0, 1);
        g.removeElement(0, 0, 0);
        d.setGhostGroup(g);
        return d;
    }

    /** Its rated fill (kg of water): the gauge glass's full mark. */
    public double ratedKg() {
        return waterMb * mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER;
    }

    /** The heat (W) its wall passes into water boiling at pa with its shell at its limit: what it can take, sustained. */
    public double heatLimitW(double pa) {
        return (shellMaxK - (mods.eln.sim.fluid.Water.tsat(pa) - mods.eln.sim.fluid.Water.T_AMBIENT)) / shellToWater;
    }

    /** A boiler may also stand on another EA device (a steam turbine: its dome port feeds the turbine's inlet below). */
    @Override
    public boolean isFloor(net.minecraft.world.level.block.Block below) {
        return super.isFloor(below) || onDeviceOrPipe(below);   // (or on a pipe: its dome port feeds the pipe below)
    }

    /** The dial's span: DIAL_MAX_C at the end of its 270 degree sweep. */
    static final float DIAL_MAX_C = 300;

    void draw() {
        draw(0.5f, 20);
    }

    /** The boiler, its gauge glass's water column at fill (0..1), its dial at celsius. */
    void draw(float fill, float celsius) {
        if (obj == null) return;
        for (String part : new String[]{"main", "body", "furnace"}) {
            var p = obj.getPart(part);
            if (p != null) p.draw();
        }
        var level = obj.getPart("level");
        if (level != null && fill > 0.01f) mods.eln.anim.Fill.draw(level, 2 * Math.min(1, fill));   // (the column is modelled half the glass high, about its foot)
        var needle = obj.getPart("needle");
        if (needle != null) needle.draw(-135 + 270 * Math.max(0, Math.min(1, celsius / DIAL_MAX_C)), 1, 0, 0);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(height > 1 ? "Large steam boiler (2×2×2 multiblock)." : "Boils water into steam using external heat.");
        list.add(String.format("Capacity: %d B | Max heat: %.0f kW at 20 bar (%.0f kW at 10 bar).",
            waterMb / 1000, heatLimitW(20e5) / 1000, heatLimitW(10e5) / 1000));
        list.add(String.format("Bursts over %.0f bar.", burstPa / 1e5));
        mods.eln.client.ItemTooltips.detail(list,
            height > 1 ? "Expands right, back, and up from placement. Gauge-glass level output: 0–5 V."
                : "Gauge-glass level signal: 0–5 V.",
            "Boiling temperature rises with pressure.");
    }
}
