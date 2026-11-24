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
    /** What a scan draws from the batteries, and for how long at least (the picture builds over it). */
    public static final double SCAN_W = 100, SCAN_S = 3;
    /** Samples read per tick: a whole scan (~300k samples to bedrock from sea level) in about 2-3 s. */
    public static final int SAMPLES_PER_TICK = 8000;
    public static final int BATTERY_SLOTS = 2;
    final Obj3D obj;

    public GroundRadarDescriptor(String name, String model) {
        super(name, GroundRadarElement.class, GroundRadarRender.class);
        obj = Eln.obj.getObj(model);
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
        Collections.addAll(list, tr("Shows what is under it, down to bedrock:\ncaves, buried structures, oil, water.\nCoarser the deeper it looks; wet ground\nfades its signal.\nRuns on two portable batteries.").split("\n"));
    }
}
