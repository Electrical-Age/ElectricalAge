package mods.eln.transparentnode.groundradar;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The Ground Radar (Baughn 2026-11-24): placed on the ground, it scans what is under it (GroundScan) on two portable
 * batteries, which stay in it when it is broken, and shows the picture as slices in its screen. Model
 * mods.eln.cad.models.GroundRadar.
 */
public class GroundRadarDescriptor extends TransparentNodeDescriptor {
    /**
     * What a scan costs: J_PER_CELL for each cell (Baughn: energy goes as resolution x range), drawn as fast as the
     * batteries give it, at most SAMPLES_PER_TICK samples of work a tick (~1 ms), and at least MIN_S long. Batteries are
     * drawn at DRAIN times their own discharge rate (Baughn 1575643239036223550: 30x the energy, drained 3x as fast).
     * From sea level the smallest scan is ~10k cells (~9 kJ, ~16 s on two portable batteries' 600 W); range 3 there
     * needs 89-173 kJ, more than two portable batteries hold (80 kJ): two battery packs (240 kJ, 1.8 kW) do.
     */
    public static final double J_PER_CELL = 0.9, MIN_S = 3, DRAIN = 3;
    public static final int SAMPLES_PER_TICK = 16000;
    public static final int MAX_RANGE = 3, MAX_DETAIL = 4;

    /** Cells across at a range setting. */
    public static int cellsAcross(int range) {
        return GroundScan.N * range;
    }

    /** The cells' growth with depth at a detail setting. */
    public static int growth(int detail) {
        return GroundScan.GROWTH * detail;
    }

    /** A scan's energy (J) depth blocks down to the floor at these settings. */
    public static double energy(int depth, int range, int detail) {
        int n = cellsAcross(range);
        return J_PER_CELL * n * n * GroundScan.layers(Math.max(0, depth), growth(detail)).length;
    }
    public static final int BATTERY_SLOTS = 2;
    final Obj3D obj;
    /** The Ground Radar w/Goggles (Baughn: the goggles are a crafting item): its screen can enter the fly-through view. */
    public final boolean goggles;

    public GroundRadarDescriptor(String name, String model, boolean goggles) {
        super(name, GroundRadarElement.class, GroundRadarRender.class);
        obj = Eln.obj.getObj(model);
        this.goggles = goggles;
    }

    /** How long ago, for a player: "12 s", "5 min", "3 h". */
    static String ago(long seconds) {
        if (seconds < 120) return tr("%1$ s", seconds);
        if (seconds < 7200) return tr("%1$ min", seconds / 60);
        return tr("%1$ h", seconds / 3600);
    }

    void draw(boolean powered, boolean scanning) {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
        mods.eln.anim.Led.draw(obj.getPart("ledpower"), powered);
        mods.eln.anim.Led.draw(obj.getPart("ledrun"), scanning);
        if (goggles) {
            var g = obj.getPart("goggles");
            if (g != null) g.draw();
        }
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(false, false);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Scans underground caves, structures, oil and water to bedrock."));
        list.add(tr("Runs on two portable batteries."));
        mods.eln.client.ItemTooltips.detail(list,
            tr("Resolution drops with depth; wet ground weakens the signal."));
    }
}
