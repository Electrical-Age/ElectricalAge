package mods.eln.sixnode.energymeter;

import java.util.List;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class EnergyMeterDescriptor extends SixNodeDescriptor {

	private Obj3D obj;
	public Obj3DPart base, comma, powerDisk, textMj, textkj,signWheel;
	public Obj3DPart[] numberWheel;
	public float[] pinDistance;

	public EnergyMeterDescriptor(String name, Obj3D obj) {
		super(name, EnergyMeterElement.class, EnergyMeterRender.class);
		this.obj = obj;
		if (obj != null) {
			base = obj.getPart("Base");
			comma = obj.getPart("Comma");
			powerDisk = obj.getPart("PowerDisk");
			textMj = obj.getPart("TextMj");
			textkj = obj.getPart("TextkJ");
			signWheel = obj.getPart("SignWheel");
			numberWheel = new Obj3DPart[8];
			for (int idx = 0; idx < numberWheel.length; idx++) {
				numberWheel[idx] = obj.getPart("NumberWheel" + idx);
			}
		}

		pinDistance = Utils.getSixNodePinDistance(base);
	}

	float alphaOff, alphaOn, speed;

	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addWiring(newItemStack());
	}

	@Override
	public boolean use2DIcon() {
		return false;
	}

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}

	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw(13896,true);
	}

	public void draw(double energy, boolean drawAll) {

		// UtilsClient.disableCulling();
		base.draw();
		textkj.draw();
		comma.draw();
		powerDisk.draw(-(float) energy, 0f, 1f, 0f);

		float ox = 0.20859f, oy = 0.15625f, oz = 0;
		double delta = 0;
		boolean propagate = true;
		double oldRot = 0;
		
		if(drawAll){
			double rot;
			if(energy > 0.5) rot = 0;
			else if(energy < -0.5) rot = 1;
			else rot = 0.5-energy;
			rot *= 36;
			GL11.glPushMatrix();
			GL11.glTranslatef(ox, oy, oz);
			GL11.glRotatef((float) rot, 0f, 0f, 1f);
			GL11.glTranslatef(-ox, -oy, -oz);
			signWheel.draw();
			GL11.glPopMatrix();
		}
		
		energy = Math.max(0.0,Math.abs(energy));
		if(energy < 5) propagate = false;
		/*if (drawAll)*/ {
			for (int idx = 0; idx < numberWheel.length; idx++) {
				if (drawAll) {
					double rot = ((energy) % 10) + 0.0;
					// energy -= rot;
	
					rot += 0.00; // - (((int) energy/10.0)*10)
					if (idx == 1) {
						delta = ((rot) % 1) * 2 - 1;
						delta *= delta * delta;
						// delta *= delta * delta;
						// delta *= delta * delta;
						delta *= 0.5;
					}
					if (idx != 0) {
	
						if (propagate) {
							if (rot < 9.5 && rot > 0.5) {
								propagate = false;
							}
							rot = (int) (rot) + delta;
						}
						else
							rot = (int) (rot);
	
					}
	
					oldRot = rot;
					// energy += rot;
					rot *= 36;
					GL11.glPushMatrix();
					GL11.glTranslatef(ox, oy, oz);
					GL11.glRotatef((float) rot, 0f, 0f, 1f);
					GL11.glTranslatef(-ox, -oy, -oz);
					numberWheel[idx].draw();
					GL11.glPopMatrix();

				energy /= 10.0;
				}
				else{
				//	numberWheel[idx].draw();
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
