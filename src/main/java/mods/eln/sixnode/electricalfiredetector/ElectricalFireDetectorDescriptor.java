package mods.eln.sixnode.electricalfiredetector;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalFireDetectorDescriptor extends SixNodeDescriptor {

	private Obj3DPart detector;
    private Obj3DPart led;
    boolean batteryPowered;
	double maxRange;
	public float[] pinDistance;
    final double updateInterval = 0.5;

	public ElectricalFireDetectorDescriptor(String name, Obj3D obj, double maxRange, boolean batteryPowered) {
		super(name, ElectricalFireDetectorElement.class, ElectricalFireDetectorRender.class);
        this.batteryPowered = batteryPowered;
		this.maxRange = maxRange;
		if (obj != null) {
			detector = obj.getPart("Detector");
            led = obj.getPart("Led");

			pinDistance = Utils.getSixNodePinDistance(detector);
		}

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
	}

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

	@Override
	public boolean use2DIcon() {
		return true;
	}

	void draw(boolean firePresent) {
		if (detector != null) detector.draw();
        if (led != null) {
            if (firePresent) {
                UtilsClient.drawLight(led);
            } else {
                GL11.glColor3f(0.5f, 0.5f, 0.5f);
                led.draw();
                GL11.glColor3f(1, 1, 1);
            }
        }
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Output voltage increases\nif a fire has been detected.").split("\n"));
        list.add(tr("Range: %1$ blocks", (int) maxRange));
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
        } else {
            GL11.glScalef(2f, 2f, 2f);
            draw(false);
        }
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).right();
    }
}
