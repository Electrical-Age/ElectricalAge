package mods.eln.transparentnode.heatfurnace;

import mods.eln.gl.GL11;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.ModelHitbox;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.FurnaceProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializerByPowerDrop;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class HeatFurnaceDescriptor extends TransparentNodeDescriptor {

    Obj3D obj;
    Obj3DPart main, glow, glass;

    double nominalPower;
    double nominalCombustibleEnergy;
    public int combustionChamberMax;
    double combustionChamberPower;
    ThermalLoadInitializerByPowerDrop thermal;

    public HeatFurnaceDescriptor(String name, String modelName,
                                 double nominalPower, double nominalCombustibleEnergy,
                                 int combustionChamberMax, double combustionChamberPower,
                                 ThermalLoadInitializerByPowerDrop thermal) {
        super(name, HeatFurnaceElement.class, null);
        this.thermal = thermal;
        this.nominalCombustibleEnergy = nominalCombustibleEnergy;
        this.combustionChamberMax = combustionChamberMax;
        this.combustionChamberPower = combustionChamberPower;
        this.nominalPower = nominalPower;
        obj = Eln.obj.getObj(modelName);
        if (obj != null) {
            main = obj.getPart("main");
            glow = obj.getPart("glow");
            glass = obj.getPart("glass");
        }
        thermal.setMaximalPower(nominalPower);

        voltageLevelColor = VoltageLevelColor.Thermal;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addThermal(newItemStack());
    }

    public void applyTo(ThermalLoad load) {
        thermal.applyTo(load);
    }

    public void applyTo(FurnaceProcess process) {
        process.nominalCombustibleEnergy = nominalCombustibleEnergy;
        process.nominalPower = nominalPower;
    }

    /** The foot of the firebox window (px; mods.eln.cad.models.HeatFurnace WIN_Y0): the glow is scaled about it. */
    static final float WINDOW_Y0 = 5.25f;
    /** The fire bed's colour by power (0..1): dull dark red to bright orange-yellow; always self-lit. */
    static final mods.eln.anim.Glow.Ramp FIRE = mods.eln.anim.Glow.ramp(
        k -> new float[]{0.45f + 0.55f * Math.min(1, k * 1.6f), 0.08f + 0.77f * k, 0.03f + 0.32f * k * k}, -1);

    /** Its highest power: the furnace's own and every combustion chamber's (the glow's full brightness). */
    public double maxPower() {
        return nominalPower + combustionChamberMax * combustionChamberPower;
    }

    /**
     * The oven (mods.eln.cad.models.HeatFurnace): the body, then the fire bed seen through the firebox window, as high as
     * the fire's level (0..1) and from dull dark red at no power to bright orange-yellow at full (power 0..1), unlit, and
     * the window's pane over it, see-through.
     */
    void draw(float fire, float power) {
        if (main != null) main.draw();
        if (glow != null && fire > 0.01f)
            mods.eln.anim.Glow.draw(() -> mods.eln.anim.Fill.drawAbout(glow, WINDOW_Y0 / 16f - 0.5f, Math.min(1, fire)),
                Math.max(0, Math.min(1, power)), FIRE);
        if (glass != null) {
            UtilsClient.enableBlend();
            GL11.glColor4f(1, 1, 1, 0.3f);
            glass.draw();
            GL11.glColor4f(1, 1, 1, 1);
            UtilsClient.disableBlend();
        }
    }


    @Override
    public boolean hasItemDraw() {
        return true;
    }

    /** The item: a moderate fire behind the window. */
    @Override
    public void drawItem(boolean entity) {
        draw(0.6f, 0.5f);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        // (Erisi's chat review: players didn't know how its temperature, a regulator and combustion chambers relate)
        list.add(tr("Burns fuel into heat, out of its back into a thermal cable or a boiler."));
        list.add("  " + tr("Power: %1$W, +%2$W per Combustion Chamber in its slot (up to %3$: %4$W)", Utils.plotValue(nominalPower),
            Utils.plotValue(combustionChamberPower), combustionChamberMax, Utils.plotValue(maxPower())));
        list.add("  " + tr("Without a regulator its gauge sets the fire; a regulator item holds a set temperature,"));
        list.add("  " + tr("which it reaches only while its power beats the heat taken from it."));
        list.add("  " + tr("Its sides take a control signal (External control). Max. temperature: %1$°C", Utils.plotValue(thermal.warmLimit)));
    }
}
