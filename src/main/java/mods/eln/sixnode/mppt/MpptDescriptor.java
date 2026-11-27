package mods.eln.sixnode.mppt;

import mods.eln.api.Meters;
import mods.eln.gl.GL11;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.MpptTracker;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The MPPT charge controller (Baughn 2026-11-25, board #79): a wall-mounted buck converter between solar panels and a
 * battery that holds the panels at their maximum power point and the battery at its charge voltage. Two tiers of one
 * class: the tracker (perturb and observe, or incremental conductance), the input voltage it stands, its power and
 * its efficiency.
 */
public class MpptDescriptor extends SixNodeDescriptor {
    final Obj3D obj;
    final Obj3DPart main, terminals, lcd, stripe;
    /** Input voltage it stands (the panels' open-circuit voltage counts), rated power (it backs off the peak above it). */
    public final double maxInputVolts, maxPower, efficiency;
    /** Incremental conductance (else perturb and observe). */
    public final boolean incremental;

    public MpptDescriptor(String name, Obj3D obj, double maxInputVolts, double maxPower, double efficiency, boolean incremental) {
        super(name, MpptElement.class, null);
        this.obj = obj;
        this.main = obj == null ? null : obj.getPart("main");
        this.terminals = obj == null ? null : obj.getPart("terminals");
        this.lcd = obj == null ? null : obj.getPart("lcd");
        this.stripe = obj == null ? null : obj.getPart("stripe");
        this.maxInputVolts = maxInputVolts;
        this.maxPower = maxPower;
        this.efficiency = efficiency;
        this.incremental = incremental;
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    MpptTracker newTracker() {
        return incremental ? MpptTracker.incrementalConductance() : MpptTracker.perturbAndObserve();
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    /** The model, lit display while it converts (blue backlight). */
    void draw(boolean lit) {
        if (main != null) main.draw();
        if (terminals != null) terminals.draw();
        if (stripe != null) {   // the tiers told apart: orange, gold for the advanced one
            if (incremental) GL11.glColor3f(0.95f, 0.78f, 0.25f);
            else GL11.glColor3f(0.89f, 0.52f, 0.17f);
            stripe.draw();
            GL11.glColor3f(1f, 1f, 1f);
        }
        mods.eln.anim.Led.draw(lcd, lit, 0.45f, 0.75f, 1.0f, 0.12f, 0.16f, 0.2f);
        GL11.glColor3f(1f, 1f, 1f);
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(true);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Solar MPPT battery charger (step-down only): up to %1$ in, %2$, %3$ eff.",
            Meters.volts(maxInputVolts), Meters.watts(maxPower), Utils.plotPercent(efficiency)));
        mods.eln.client.ItemTooltips.detail(list,
            incremental ? tr("Tracker: incremental conductance.") : tr("Tracker: perturb and observe."));
    }
}
