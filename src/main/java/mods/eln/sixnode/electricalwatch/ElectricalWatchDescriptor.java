package mods.eln.sixnode.electricalwatch;


import mods.eln.gl.GL11;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public class ElectricalWatchDescriptor extends SixNodeDescriptor {

    private Obj3DPart base, cHour, cMin; //Analog
    private Obj3DPart digits[] = new Obj3DPart[4]; //Digital
    private Obj3DPart dot, glass; //Digital

    enum Kind {ANALOG, DIGITAL}

    ;
    private Kind kind;
    double powerConsumtion;

    Obj3D obj;

    public ElectricalWatchDescriptor(String name, Obj3D obj, double powerConsumtion) {
        super(name, ElectricalWatchElement.class, null);
        this.obj = obj;
        this.powerConsumtion = powerConsumtion;
        kind = Kind.valueOf(obj.getString("type").toUpperCase(Locale.ROOT));
        if (obj != null) {
            if (kind == Kind.ANALOG) {
                base = obj.getPart("base");
                cHour = obj.getPart("cHour");
                cMin = obj.getPart("cMin");
            } else if (kind == Kind.DIGITAL) {
                base = obj.getPart("base");
                glass = obj.getPart("glass");
                dot = obj.getPart("digitDot");
                digits[3] = obj.getPart("digit3");
                digits[2] = obj.getPart("digit2");
                digits[1] = obj.getPart("digit1");
                digits[0] = obj.getPart("digit0");
            }
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }
    void draw(float hour, float min, boolean isEnergyAvailable) {
        if (kind == Kind.ANALOG) {
            if (base != null) base.draw();
            if (cHour != null) cHour.draw(360 * hour, -1, 0, 0);
            if (cMin != null) cMin.draw(360 * min, -1, 0, 0);
        } else if (kind == Kind.DIGITAL) {
            //Digits
            obj.bindTexture("Digits.png");
            UtilsClient.disableLight();
            GL11.glColor3f(0.95f, 0.f, 0.f);
            if (isEnergyAvailable) {
                int fulltimeMin = (int) (12.0f * 60.0f * hour);
                int timeHour = fulltimeMin / 60;
                int timeMin = fulltimeMin % 60;
                int tmp = timeMin % 10;
                digits[0].draw(tmp / 16.f, 0.0f);
                tmp = timeMin / 10;
                digits[1].draw(tmp / 16.f, 0.0f);
                tmp = timeHour % 10;
                digits[2].draw(tmp / 16.f, 0.0f);
                tmp = timeHour / 10;
                digits[3].draw(tmp / 16.f, 0.0f);
                if ((fulltimeMin & 0x01) != 0x00)
                    GL11.glColor3f(0.05f, 0.f, 0.f);
                dot.draw();
            } else {
                for (int idx = 0; idx < 4; idx++)
                    digits[idx].draw(10.f / 16.f, 0.f);
                GL11.glColor3f(0.05f, 0.f, 0.f);
                dot.draw();
            }
            GL11.glColor3f(1.f, 1.f, 1.f);
            UtilsClient.enableLight();
            //Frame
            base.draw();
            //Glass (reflections)
            UtilsClient.enableBlend();
            //UtilsClient.enableBilinear();
            obj.bindTexture("Reflection.png");
            float rotYaw = UtilsClient.clientPlayerYaw() / 360.f;
            float rotPitch = UtilsClient.clientPlayerPitch() / 180.f;
            float pos = (((float) UtilsClient.clientPlayerX()) + ((float) UtilsClient.clientPlayerZ())) / 64.f;
            glass.draw(rotYaw + pos, rotPitch * 0.875f);
            //UtilsClient.disableBilinear(); //BUG: Not always disabled.
            UtilsClient.disableBlend();
        }
    }
    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        //Data.addSignal(newItemStack());
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)
    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        GL11.glRotatef(90, 1, 0, 0);
        draw(0.1f, 0.2f, true);

    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).left();
    }
}
