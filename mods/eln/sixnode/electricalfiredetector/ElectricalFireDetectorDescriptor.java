package mods.eln.sixnode.electricalfiredetector;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class ElectricalFireDetectorDescriptor extends SixNodeDescriptor {

	private Obj3DPart detector;
    private Obj3DPart led;
	double maxRange;
	public float[] pinDistance;
    final double updateInterval = 0.5;

	public ElectricalFireDetectorDescriptor(String name, Obj3D obj, double maxRange) {
		super(name, ElectricalFireDetectorElement.class, ElectricalFireDetectorRender.class);
		this.maxRange = maxRange;
		if(obj != null) {
			detector = obj.getPart("Detector");
            led = obj.getPart("Led");

			pinDistance = Utils.getSixNodePinDistance(detector);
		}
	}
	@Override
	public boolean use2DIcon() {
		return false;
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
		/*list.add("Output value rise when");
		list.add("a fire is in range");
		list.add("Max range : " + (int)maxRange);*/
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
        GL11.glScalef(2f, 2f, 2f);
        draw(false);
    }
}
