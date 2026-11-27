package mods.eln.item.regulator;

import mods.eln.sim.RegulatorProcess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;
import static mods.eln.item.regulator.IRegulatorDescriptor.RegulatorType.OnOff;

public class RegulatorOnOffDescriptor extends IRegulatorDescriptor {

    private double hysteresis;

    public RegulatorOnOffDescriptor(String name, String iconName, double hysteresis) {
        super(name);
        setDefaultIcon(iconName);
        this.hysteresis = hysteresis;
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("On/off temperature or pressure regulator (%1$% hysteresis).", Math.round(hysteresis * 100)));
    }

    @Override
    public RegulatorType getType() {
        return OnOff;
    }

    @Override
    public void applyTo(RegulatorProcess regulator, double workingPoint, double P, double I, double D) {
        regulator.setOnOff(hysteresis, workingPoint);
    }
}
