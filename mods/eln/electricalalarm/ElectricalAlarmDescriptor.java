package mods.eln.electricalalarm;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.sim.DiodeProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;

import com.google.common.base.Function;


public class ElectricalAlarmDescriptor extends SixNodeDescriptor{


	public ElectricalAlarmDescriptor(
			String name,
			String objName,
			String soundName,double soundTime,float soundLevel
			) {
		super(name, ElectricalAlarmElement.class,ElectricalAlarmRender.class);
		obj = Eln.instance.obj.getObj(objName);
		this.soundName = soundName;
		this.soundTime = soundTime;
		this.soundLevel = soundLevel;
	}

	Obj3D obj;
	String soundName;
	double soundTime;
	float soundLevel;
	void draw(float factor)
	{
		if(obj.getString("type").equals("rot"))
		{
			obj.draw("Vumeter");
			Obj3DPart pointer = obj.getPart("Pointer");
			float alphaOff,alphaOn;
			alphaOff = pointer.getFloat("alphaOff");
			alphaOn = pointer.getFloat("alphaOn");
			pointer.draw((factor*(alphaOn-alphaOff) + alphaOff), 1.0f, 0, 0);
		}
	}
	
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Emit a sonor alarm when");
		list.add("the input signal is high");
	}
}
