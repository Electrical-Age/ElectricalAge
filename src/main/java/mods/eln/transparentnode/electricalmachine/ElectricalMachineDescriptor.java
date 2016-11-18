package mods.eln.transparentnode.electricalmachine;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.gui.GuiLabel;
import mods.eln.misc.*;
import mods.eln.api.recipe.Recipe;
import mods.eln.misc.RecipesList;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import mods.eln.wiki.Data;
import mods.eln.wiki.GuiItemStack;
import mods.eln.wiki.GuiVerticalExtender;
import mods.eln.wiki.ItemDefault.IPlugIn;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalMachineDescriptor extends TransparentNodeDescriptor implements IPlugIn {
    public RecipesList recipe = new RecipesList();

    final double nominalU;
    final double nominalP;
    private final ThermalLoadInitializer thermal;
    final ElectricalCableDescriptor cable;
    final int outStackCount;

    private final double resistorR;

    final double boosterEfficiency = 1.0 / 1.1;
    final double boosterSpeedUp = 1.25 / boosterEfficiency;

    SoundCommand endSound;
    String runningSound;

    private Object defaultHandle = null;

    ElectricalMachineDescriptor(String name, double nominalU, double nominalP, double maximalU,
                                ThermalLoadInitializer thermal, ElectricalCableDescriptor cable,
                                RecipesList recipe) {
        super(name, ElectricalMachineElement.class, ElectricalMachineRender.class);
        outStackCount = 4;
        this.nominalP = nominalP;
        this.nominalU = nominalU;
        this.cable = cable;
        this.thermal = thermal;
        resistorR = nominalU * nominalU / nominalP;
        this.recipe = recipe;

        voltageLevelColor = VoltageLevelColor.fromCable(cable);
    }

    public ElectricalMachineDescriptor setRunningSound(String runningSound) {
        this.runningSound = runningSound;
        return this;
    }

    public ElectricalMachineDescriptor setEndSound(SoundCommand endSound) {
        this.endSound = endSound;
        return this;
    }

    public float volumeForRunningSound(float processState, float powerFactor) {
        if (powerFactor >= 0.3)
            return 0.3f * powerFactor;
        else
            return 0f;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        recipe.addMachine(newItemStack(1));
        Data.addMachine(newItemStack(1));
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Nominal voltage: %1V$", Utils.plotValue(nominalU)));
        list.add(tr("Nominal power: %1$W", Utils.plotValue(nominalP)));
    }

    public void applyTo(ElectricalLoad load) {
        cable.applyTo(load);
    }

    public void applyTo(Resistor resistor) {
        resistor.setR(resistorR);
    }

    public void applyTo(ElectricalStackMachineProcess machine) {
        machine.setResistorValue(resistorR);
    }

    public void applyTo(ThermalLoad load) {
        thermal.applyTo(load);
    }

    Object newDrawHandle() {
        return null;
    }

    void draw(ElectricalMachineRender render, Object handleO, EntityItem inEntity, EntityItem outEntity,
              float powerFactor, float processState) {
    }

    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, EntityItem inEntity,
                 EntityItem outEntity, float powerFactor, float processState) {
    }

    public boolean powerLrdu(Direction side, Direction front) {
        return true;
    }

    public boolean drawCable() {
        return false;
    }

    CableRenderDescriptor getPowerCableRender() {
        return null;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        } else {
            draw(null, getDefaultHandle(), null, null, 0f, 0f);
        }
    }

    private Object getDefaultHandle() {
        if (defaultHandle == null)
            defaultHandle = newDrawHandle();
        return defaultHandle;
    }

    @Override
    public int top(int y, GuiVerticalExtender extender, ItemStack stack) {
        return y;
    }

    @Override
    public int bottom(int y, GuiVerticalExtender extender, ItemStack stack) {
        int counter = -1;

        extender.add(new GuiLabel(6, y, tr("Can create:")));
        y += 12;
        for (Recipe r : recipe.getRecipes()) {
            if (counter == 0)
                y += (int) (18 * 1.3);
            if (counter == -1)
                counter = 0;
            int x = 6 + counter * 60;

            extender.add(new GuiItemStack(x, y, r.input, extender.helper));
            x += 18 * 2;

            for (ItemStack m : recipe.getMachines()) {
                extender.add(new GuiItemStack(x, y, m, extender.helper));
                x += 18;
            }
            x += 18;
            extender.add(new GuiItemStack(x, y, r.getOutputCopy()[0], extender.helper));

            x += 22;
            extender.add(new GuiLabel(x, y + 4, Utils.plotEnergy(tr("Cost"), r.energy)));

            counter = (counter + 1) % 1; // WTF ? (% 1 is afaik always 0...)
        }
        y += (int) (18 * 1.3);

        return y;
    }
}
