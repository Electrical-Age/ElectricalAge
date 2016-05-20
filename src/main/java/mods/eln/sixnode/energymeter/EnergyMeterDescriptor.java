package mods.eln.sixnode.energymeter;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class EnergyMeterDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	public Obj3DPart base, powerDisk, energySignWheel, timeUnitWheel, energyUnitWheel;
	public Obj3DPart[] energyNumberWheel, timeNumberWheel;
	public float[] pinDistance;

    float alphaOff, alphaOn, speed;

	public EnergyMeterDescriptor(String name, Obj3D obj, int energyWheelCount, int timeWheelCount) {
		super(name, EnergyMeterElement.class, EnergyMeterRender.class);
		this.obj = obj;
		if (obj != null) {
			base = obj.getPart("Base");
			powerDisk = obj.getPart("PowerDisk");
			energySignWheel = obj.getPart("EnergySignWheel");
			timeUnitWheel = obj.getPart("TimeUnitWheel");
			energyUnitWheel = obj.getPart("EnergyUnitWheel");

			energyNumberWheel = new Obj3DPart[energyWheelCount];
			for (int idx = 0; idx < energyNumberWheel.length; idx++) {
				energyNumberWheel[idx] = obj.getPart("EnergyNumberWheel" + idx);
			}
			timeNumberWheel = new Obj3DPart[timeWheelCount];
			for (int idx = 0; idx < timeNumberWheel.length; idx++) {
				timeNumberWheel[idx] = obj.getPart("TimeNumberWheel" + idx);
			}
		}

		pinDistance = Utils.getSixNodePinDistance(base);

		voltageLevelColor = VoltageLevelColor.Neutral;
	}

	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}

	@Override
	public boolean use2DIcon() {
		return true;
	}

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return type != ItemRenderType.INVENTORY;
	}

	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return type != ItemRenderType.INVENTORY;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if (type == ItemRenderType.INVENTORY) {
			super.renderItem(type, item, data);
		/*
			GL11.glRotatef(45, 0, 0, 1);
			GL11.glRotatef(90, 1, 0, 0);
			GL11.glRotatef(30, 0, 0, 1);
			float scal = 2.5f;
			GL11.glScalef(scal, scal, scal);
			draw(13896, 1511, 1, 0, false);*/
		} else {
			draw(13896, 1511, 1, 0, true);
		}
	}

	public void draw(double energy, double time, int energyUnit, int timeUnit, boolean drawAll) {
		// UtilsClient.disableCulling();
		base.draw();
		powerDisk.draw(-(float) energy, 0f, 1f, 0f);

		{// render energy
			float ox = 0.20859f, oy = 0.15625f, oz = 0;
			double delta = 0;
			boolean propagate = true;
			double oldRot = 0;

			if (drawAll) {
				{
					double rot;
					if (energy > 0.5)
						rot = 0;
					else if (energy < -0.5)
						rot = 1;
					else
						rot = 0.5 - energy;
					rot *= 36;
					GL11.glPushMatrix();
					GL11.glTranslatef(ox, oy, oz);
					GL11.glRotatef((float) rot, 0f, 0f, 1f);
					GL11.glTranslatef(-ox, -oy, -oz);
					energySignWheel.draw();
					GL11.glPopMatrix();
				}
				if (energyUnitWheel != null) {
					double rot = energyUnit * 36;
					GL11.glPushMatrix();
					GL11.glTranslatef(ox, oy, oz);
					GL11.glRotatef((float) rot, 0f, 0f, 1f);
					GL11.glTranslatef(-ox, -oy, -oz);
					energyUnitWheel.draw();
					GL11.glPopMatrix();
				}

				energy = Math.max(0.0, Math.abs(energy));
				if (energy < 5) propagate = false;

				for (int idx = 0; idx < energyNumberWheel.length; idx++) {
					double rot = ((energy) % 10) + 0.0;
					rot += 0.00;

					if (idx == 1) {
						delta = ((rot) % 1) * 2 - 1;
						delta *= delta * delta;
						delta *= 0.5;
					}
					if (idx != 0) {
						if (propagate) {
							if (rot < 9.5 && rot > 0.5) {
								propagate = false;
							}
							rot = (int) (rot) + delta;
						} else
							rot = (int) (rot);
					}

					oldRot = rot;
					rot *= 36;
					GL11.glPushMatrix();
					GL11.glTranslatef(ox, oy, oz);
					GL11.glRotatef((float) rot, 0f, 0f, 1f);
					GL11.glTranslatef(-ox, -oy, -oz);
					energyNumberWheel[idx].draw();
					GL11.glPopMatrix();

					energy /= 10.0;
				}
			}
		}

		if (energyNumberWheel.length != 0) { // Render Times
			float ox = 0.20859f, oy = 0.03125f, oz = 0;
			double delta = 0;
			boolean propagate = true;
			double oldRot = 0;

			if (drawAll) {
				if (timeUnitWheel != null) {
					double rot = timeUnit * 36;
					GL11.glPushMatrix();
					GL11.glTranslatef(ox, oy, oz);
					GL11.glRotatef((float) rot, 0f, 0f, 1f);
					GL11.glTranslatef(-ox, -oy, -oz);
					timeUnitWheel.draw();
					GL11.glPopMatrix();
				}

				time = Math.max(0.0, Math.abs(time));
				if (time < 5) propagate = false;

				for (int idx = 0; idx < timeNumberWheel.length; idx++) {
					double rot = ((time) % 10) + 0.0;
					rot += 0.00;

					if (idx == 1) {
						delta = ((rot) % 1) * 2 - 1;
						delta *= delta * delta;
						delta *= delta * delta;
						delta *= delta * delta;
						delta *= 0.5;
					}
					if (idx != 0) {
						if (propagate) {
							if (rot < 9.5 && rot > 0.5) {
								propagate = false;
							}
							rot = (int) (rot) + delta;
						} else
							rot = (int) (rot);
					}

					oldRot = rot;
					rot *= 36;
					GL11.glPushMatrix();
					GL11.glTranslatef(ox, oy, oz);
					GL11.glRotatef((float) rot, 0f, 0f, 1f);
					GL11.glTranslatef(-ox, -oy, -oz);
					timeNumberWheel[idx].draw();
					GL11.glPopMatrix();

					time /= 10.0;
				}
			}
		}
		// UtilsClient.enableCulling();
	}

	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
	}
}
