package mods.eln.sixnode.electricalsource;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

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
		Collections.addAll(list, tr("Provides an ideal voltage source\nwithout energy or power limitation.").split("\\\n"));
		list.add("");
		list.add(tr("Internal resistance: %1$Ω", Eln.instance.lowVoltageCableDescriptor.electricalRs));
        list.add("");
		list.add(tr("Creative block."));
	}

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		switch (type) {
			case ENTITY:
				draw(false);
				break;

			case EQUIPPED:
			case EQUIPPED_FIRST_PERSON:
				GL11.glPushMatrix();
				GL11.glTranslatef(0.8f,0.3f,0.2f);
				GL11.glRotatef(150, 0,0,1);
				draw(false);
				GL11.glPopMatrix();
				break;

			case INVENTORY:
			case FIRST_PERSON_MAP:
				if (signalSource) {
					VoltageLevelColor.SignalVoltage.drawIconBackground(type);
				}
				super.renderItem(type, item, data);
				break;
		}
	}
}
