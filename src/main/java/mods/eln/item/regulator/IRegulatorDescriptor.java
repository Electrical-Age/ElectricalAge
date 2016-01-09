package mods.eln.item.regulator;

import mods.eln.item.GenericItemUsingDamageDescriptorUpgrade;
import mods.eln.sim.RegulatorProcess;

public abstract class IRegulatorDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

  public IRegulatorDescriptor(String name) {
    super(name);
  }

  public enum RegulatorType {Manual, None, OnOff, Analog}

  public abstract RegulatorType getType();

  public abstract void applyTo(RegulatorProcess regulator, double workingPoint, double P, double I, double D);
}
