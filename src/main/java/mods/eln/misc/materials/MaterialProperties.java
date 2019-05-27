package mods.eln.misc.materials;

import java.util.HashMap;

/**
 * Generic Material (acts like Copper by default)
 */
public class MaterialProperties {

    private HashMap<MaterialType, MaterialData> materials = new HashMap<>();

    public MaterialProperties() {
        materials.put(MaterialType.COPPER, new MaterialData(
            1.68,
            385.0,
            2530.0));
    }

    /**
     * get Electrical Resistivity
     *
     * @return resistivity (ohms/meter)
     */
    public double getElectricalResistivity(MaterialType type) {
        return materials.get(type).electricalResistivityConstant * Math.pow(10, -8);
    }

/*

Electrical Resistivity

Material    Resistivity ρ (ohm m)

Silver:     1.59    x10^-8
Copper:     1.68    x10^-8
Aluminum:   2.65    x10^-8
Tungsten:   5.6     x10^-8
Iron:       9.71    x10^-8
Platinum    10.6    x10^-8
Lead:       22      x10^-8
Mercury     98      x10^-8
Glass       1-10000 x10^-9
Rubber      1-100   x10^-13

Source: http://hyperphysics.phy-astr.gsu.edu/hbase/Tables/rstiv.html

*/

    /**
     * get Thermal Conductivity
     *
     * @return thermal conductivity (W/m K)
     */
    public double getThermalConductivity(MaterialType type) {
        return materials.get(type).thermalConductivityConstant;
    }

/*

Thermal Conductivity

Material    Thermal conductivity (W/m K)*
Diamond:    1000
Silver:     406.0
Copper:     385.0
Gold:       314
Brass:      109.0
Aluminum:   205.0
Iron:       79.5
Steel:      50.2
Lead:       34.7
Mercury:    8.3
Glass:      0.8
Water:      0.6
Air:        0.024

Source: http://hyperphysics.phy-astr.gsu.edu/hbase/Tables/thrcn.html

*/

// TODO: Get recommended amperage per material type per mm^2. (remember, skin effect on AC signals)

}





