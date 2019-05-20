package mods.eln.misc.materials;

public class MaterialData {

    protected double electricalResistivityConstant = 1.68;
    protected double thermalConductivityConstant = 385.0;
    protected double fusingCurrentConstant = 2530.0;

    public MaterialData(double electricalResistivityConstant, double thermalConductivityConstant, double fusingCurrentConstant) {
        this.electricalResistivityConstant = electricalResistivityConstant;
        this.thermalConductivityConstant = thermalConductivityConstant;
        this.fusingCurrentConstant = fusingCurrentConstant;
    }
}
