package mods.eln.item.regulator;

import mods.eln.sim.RegulatorProcess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;
import static mods.eln.item.regulator.IRegulatorDescriptor.RegulatorType.Analog;

public class RegulatorAnalogDescriptor extends IRegulatorDescriptor {

    public RegulatorAnalogDescriptor(String name, String iconName) {
        super(name);
        setDefaultIcon(iconName);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Smooth PID temperature or pressure regulator for furnaces, heaters and turbines."));
    }

    @Override
    public RegulatorType getType() {
        return Analog;
    }

    @Override
    public void applyTo(RegulatorProcess regulator, double workingPoint, double P, double I, double D) {
        regulator.setAnalog(P, I, D, workingPoint);
    }
}
