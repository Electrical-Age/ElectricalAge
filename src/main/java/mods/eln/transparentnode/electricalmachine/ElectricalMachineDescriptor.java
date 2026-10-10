package mods.eln.transparentnode.electricalmachine;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalMachineDescriptor extends TransparentNodeDescriptor {
    public RecipesList recipe = new RecipesList();

    final double nominalU;
    final double nominalP;
    private final ThermalLoadInitializer thermal;
    final ElectricalCableDescriptor cable;
    public final int outStackCount;

    private final double resistorR;

    final double boosterEfficiency = 1.0 / 1.1;
    final double boosterSpeedUp = 1.25 / boosterEfficiency;

    SoundCommand endSound;
    String runningSound;

    private Object defaultHandle = null;

    ElectricalMachineDescriptor(String name, double nominalU, double nominalP, double maximalU,
                                ThermalLoadInitializer thermal, ElectricalCableDescriptor cable,
                                RecipesList recipe) {
        super(name, ElectricalMachineElement.class, null);
        outStackCount = 4;
        this.nominalP = nominalP;
        this.nominalU = nominalU;
        this.cable = cable;
        this.thermal = thermal;
        resistorR = nominalU * nominalU / nominalP;
        this.recipe = recipe;

        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
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
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
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
// 26.x port: draw() removed (client rendering; redone in P3)
// 26.x port: refresh() removed (client rendering; redone in P3)

    public boolean powerLrdu(Direction side, Direction front) {
        return true;
    }

    public boolean drawCable() {
        return false;
    }

    CableRenderDescriptor getPowerCableRender() {
        return null;
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    private Object getDefaultHandle() {
        if (defaultHandle == null)
            defaultHandle = newDrawHandle();
        return defaultHandle;
    }

    // 26.x port: top() removed (client rendering; redone in P3)

    // 26.x port: bottom() removed (client rendering; redone in P3)
}
