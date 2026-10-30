package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Plant Steam Condenser (Baughn 2026-10-29, the shaft redo): a separate block for the shaft turbines, fed by pipe
 * (the 50V/200V turbines keep their add-on condensers). Exhaust steam in on top, its back or its right (an LP turbine's
 * exhaust set straight against it: low-pressure steam is too thin for a long pipe); its vessel sits at
 * the saturation pressure of its water, cooled by its shell's air cooler and thermal cables on its sides. Its
 * condensate pump and air ejector run on a Shaft Motor against its side: no motor, no pump and no vacuum (a cold start:
 * pull the vacuum with the motor, warm the lines, then roll the turbines). Air leaks in while it holds a vacuum, so
 * without the ejector the vacuum is lost and an LP turbine's power sags (no bang). Over FLOOD of water its tubes drown
 * and it cools less; full, water backs up its exhaust pipe into the turbine. Model: mods.eln.cad.models.PlantCondenser.
 */
public class PlantCondenserDescriptor extends TransparentNodeDescriptor {
    public final double volume, nominalHeat, pumpKgPerS;
    /** Air leaking in at full vacuum (kg/s), the ejector's and pump's shaft power (W at full speed; the pump's per its
     *  flow), and the flooded share past which its tubes drown. */
    public static final double AIR_LEAK_KG_PER_S = 2e-6, EJECTOR_W = 150, PUMP_EFFICIENCY = 0.6, FLOOD = 0.5;
    final Obj3D obj;

    public PlantCondenserDescriptor(String name, double volume, double nominalHeat, double pumpKgPerS, String model) {
        super(name, PlantCondenserElement.class, PlantCondenserRender.class);
        this.volume = volume;
        this.nominalHeat = nominalHeat;
        this.pumpKgPerS = pumpKgPerS;
        this.obj = Eln.obj.getObj(model);
    }

    private mods.eln.node.transparent.ModelHitbox hitbox;

    /** Its model's hitbox (mods.eln.cad.models.PlantCondenser), turned with its front. */
    @Override
    public mods.eln.node.transparent.ModelHitbox modelHitbox() {
        if (hitbox == null && obj != null) hitbox = mods.eln.node.transparent.ModelHitbox.of(obj);
        return hitbox;
    }

    /** The condenser, and its drive coupling on the side a motor drives it from (1 left, 2 front; mods.eln.cad.models.PlantCondenser). */
    void draw(int motorSide) {
        if (obj == null) return;
        var p = obj.getPart("main");
        if (p != null) p.draw();
        var d = motorSide == 1 ? obj.getPart("drive_left") : motorSide == 2 ? obj.getPart("drive_front") : null;
        if (d != null) d.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add("Exhaust steam in on top, its back or its right; set an LP turbine against it.");
        list.add("Condensate out low on its front; makeup water in high on its left.");
        list.add("Its pump and air ejector run on a Shaft Motor against its side: no motor, no vacuum.");
        list.add(String.format("Rejects %.0f kW at a 20 K rise of its shell; more with thermal cables on its sides.", nominalHeat / 1000));
    }
}
