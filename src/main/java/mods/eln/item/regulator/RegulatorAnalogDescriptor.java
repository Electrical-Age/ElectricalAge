package mods.eln.item.regulator;

import mods.eln.sim.RegulatorProcess;

import static mods.eln.item.regulator.IRegulatorDescriptor.RegulatorType.Analog;

public class RegulatorAnalogDescriptor extends IRegulatorDescriptor {

  public RegulatorAnalogDescriptor(String name, String iconName) {
    super(name);
    changeDefaultIcon(iconName);
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
