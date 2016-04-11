package mods.eln.sixnode.diode;

import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class DiodeDescriptor extends SixNodeDescriptor {

    private Obj3DPart base;
    private Obj3DPart diodeCables;
    private Obj3DPart diodeCore;

    double stdI, stdU;
    ElectricalCableDescriptor cable;
    String descriptor;
    IFunction IfU;

    ThermalLoadInitializer thermal;

	public DiodeDescriptor(String name,
                           IFunction IfU,
                           double Imax,
                           double stdU, double stdI,
                           ThermalLoadInitializer thermal,
                           ElectricalCableDescriptor cable, Obj3D obj) {
		super(name, DiodeElement.class, DiodeRender.class);

        this.IfU = IfU;
		
		//double Umax = 0;
		//while(IfU.getValue(Umax) < Imax) Umax += 0.01;
		//double Pmax = Umax * IfU.getValue(Umax);
		this.cable = cable;
		this.thermal = thermal;
		thermal.setMaximalPower(stdU * stdI * 1.2);
		this.stdI = stdI;
		this.stdU = stdU;

        base = obj.getPart("Base");
        diodeCables = obj.getPart("DiodeCables");
        diodeCore = obj.getPart("DiodeCore");

        if (cable.signalWire) {
            voltageLevelColor = VoltageLevelColor.SignalVoltage;
        } else {
            voltageLevelColor = VoltageLevelColor.Neutral;
        }
	}

	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addEnergy(newItemStack());
	}

    @Override
    public boolean use2DIcon() {
        return true;
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
    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        } else {
            GL11.glTranslatef(0.0f, 0.0f, -0.2f);
            GL11.glScalef(1.25f,1.25f,1.25f);
            GL11.glRotatef(-90.f,0.f,1.f,0.f);
            draw();
        }
    }

/*	public void applyTo(DiodeProcess diode) {
		diode.IfU = IfU;
	}
	*/
	public void applyTo(ThermalLoad load) {
		thermal.applyTo(load);
	}
	
	public void applyTo(ElectricalLoad load) {
		cable.applyTo(load);
	}
	
	public void applyTo(ResistorSwitch resistorSwitch) {
		resistorSwitch.setR(stdU/stdI);
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Electrical current can only\nflow through the diode\nfrom anode to cathode").split("\\\n"));
    }

    void draw() {
        if (base != null) base.draw();
        if (diodeCables != null) diodeCables.draw();
        if (diodeCore != null) diodeCore.draw();
    }
}
