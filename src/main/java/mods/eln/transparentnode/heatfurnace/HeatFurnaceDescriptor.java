package mods.eln.transparentnode.heatfurnace;

import mods.eln.gl.GL11;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
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
    Obj3DPart tiroir, main;
    float alphaClose, alphaOpen;

    double nominalPower;
    double nominalCombustibleEnergy;
    public int combustionChamberMax;
    double combustionChamberPower;
    ThermalLoadInitializerByPowerDrop thermal;

    double flameStartX, flameStartY, flameStartZ;
    double flameEndX, flameEndY, flameEndZ;
    double flameDeltaX, flameDeltaY, flameDeltaZ;
    double flamePopRate;

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
            tiroir = obj.getPart("tiroir");
            main = obj.getPart("main");

            if (tiroir != null) {
                alphaClose = tiroir.getFloat("alphaClose");
                alphaOpen = tiroir.getFloat("alphaOpen");
            }

            if (main != null) {
                flameStartX = main.getFloat("flameStartX");
                flameStartY = main.getFloat("flameStartY");
                flameStartZ = main.getFloat("flameStartZ");

                flameEndX = main.getFloat("flameEndX");
                flameEndY = main.getFloat("flameEndY");
                flameEndZ = main.getFloat("flameEndZ");

                flameDeltaX = flameEndX - flameStartX;
                flameDeltaY = flameEndY - flameStartY;
                flameDeltaZ = flameEndZ - flameStartZ;
                flamePopRate = main.getFloat("flamePopRate");
            }
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

    void draw(float tiroirFactor) {
        //GL11.glDisable(GL11.GL_T4$5EXTURE_2D);
        //GL11.glDisable(GL11.GL_CULL_FACE);
        //GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
        //	GL11.glColor3f(0.5f, 0.5f, 0.5f);
        if (main != null) main.draw();
        if (tiroir != null) tiroir.draw(alphaClose + tiroirFactor * (alphaOpen - alphaClose), 0f, 0f, 1f);
        //GL11.glEnable(GL11.GL_TEXTURE_2D);
        //GL11.glCullFace(GL11.GL_BACK);
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(1.0f);

    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Generates heat when supplied with fuel."));
        list.add("  " + tr("Nominal power: %1$W", Utils.plotValue(nominalPower)));
        list.add("  " + tr("Max. temperature: %1$°C", Utils.plotValue(thermal.warmLimit)));
    }
}
