package mods.eln.sixnode.electricalsource;

import java.util.List;

import mods.eln.misc.UtilsClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import org.lwjgl.opengl.GL11;

public class ElectricalSourceDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	private Obj3DPart main;
	private Obj3DPart led;
	private boolean signalSource = false;

	public ElectricalSourceDescriptor(String name, Obj3D obj, boolean signalSource) {
		super(name, ElectricalSourceElement.class, ElectricalSourceRender.class);
		this.obj = obj;
		if (obj != null) {
			main = obj.getPart("main");
			led = obj.getPart("led");
		}
		this.signalSource = signalSource;
	}

	public boolean isSignalSource() {
		return signalSource;
	}

	void draw(boolean ledOn) {
		if (main != null) main.draw();
		if (led != null) {
			if (ledOn)
				UtilsClient.drawLight(led);
			else {
				GL11.glPushMatrix();
				GL11.glColor3f(0.1f, 0.1f, 0.1f);
				led.draw();
				GL11.glPopMatrix();
			}
		}
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		list.add("Provides a stable voltage source");
		list.add("without energy/power limitation.");
        list.add("");
		list.add(Utils.plotOhm("Internal resistance :", Eln.instance.lowVoltageCableDescriptor.electricalRs));
        list.add("");
		list.add("Creative block.");
	}
}
