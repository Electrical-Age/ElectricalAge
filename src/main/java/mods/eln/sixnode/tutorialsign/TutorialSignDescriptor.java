package mods.eln.sixnode.tutorialsign;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;


import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.world.item.ItemStack;

public class TutorialSignDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    private Obj3DPart main, light, halo;

    public TutorialSignDescriptor(String name, Obj3D obj) {
        super(name, TutorialSignElement.class, null);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            light = obj.getPart("light");
            halo = obj.getPart("halo");
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    void setupColor(float factor, float alpha) {
        if (factor < 0.5) {
            factor *= 2;
            float factorN = 1f - factor;
            GL11.glColor4f(0, 0, 0.4f * factorN, alpha);
        } else {
            factor = (factor - 0.5f) * 2;
            float factorN = 1f - factor;
            GL11.glColor4f(0, 1 * factor, 0, alpha);
        }
    }
    void draw(float factor) {
        //GL11.glColor3f(0.8f, 0.8f, 0.8f);
        if (main != null) main.draw();
        UtilsClient.disableLight();

        setupColor(factor, 1);
        if (light != null) {
            light.draw();
        }

        UtilsClient.enableBlend();
        setupColor(factor, 0.4f);
        if (halo != null) halo.draw();

        UtilsClient.disableBlend();
        UtilsClient.enableLight();
        GL11.glColor3f(1f, 1f, 1f);
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(1f);

    }
}
