package mods.eln.sixnode.electricaldigitaldisplay;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalDigitalDisplayDescriptor extends SixNodeDescriptor {
    protected Obj3D obj;
    protected Obj3D.Obj3DPart digits[] = new Obj3D.Obj3DPart[4];
    protected Obj3D.Obj3DPart dots[] = new Obj3D.Obj3DPart[5];
    protected Obj3D.Obj3DPart colons[] = new Obj3D.Obj3DPart[3];
    protected Obj3D.Obj3DPart base, glass;
    public float pinDistance[];

    public static int DOT_STATES = 256;

    enum Style {LED}

    public static final byte netSetRange = 1;

    public ElectricalDigitalDisplayDescriptor(String name, Obj3D obj_) {
        super(name, ElectricalDigitalDisplayElement.class, ElectricalDigitalDisplayRender.class);
        obj = obj_;
        base = obj.getPart("base");
        pinDistance = Utils.getSixNodePinDistance(base);
        glass = obj.getPart("glass");
        for(int i = 0; i < 4; i++) {
            digits[i] = obj.getPart("digit" + i);
        }
        for(int i = 0; i < 5; i++) {
            dots[i] = obj.getPart("dot" + i);
        }
        for(int i = 1; i < 4; i++) {
            colons[i - 1] = obj.getPart("colon" + i);
        }
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Displays signal value."));
    }

    private void bitToColor(int i) {
        if(i != 0) GL11.glColor3f(0.95f, 0.0f, 0.0f);
        else GL11.glColor3f(0.0f, 0.0f, 0.0f);
    }

    void draw(int value, boolean strobe, Style style) { draw(value, strobe, style, 0, 0); }
    void draw(int value, boolean strobe, Style style, int dye) { draw(value, strobe, style, dye, 0); }

    void draw(int value, boolean strobe, Style style, int dye, int dotconfig) {
        if(value < 0) value = 0;
        if(value > 9999) value = 9999;
        if(dotconfig < 0) dotconfig = 0;
        if(dotconfig > 255) dotconfig = 255;

        switch(style) {
            case LED:
                obj.bindTexture("Digits_LED.png");
                break;
        }

        UtilsClient.disableLight();
        GL11.glColor3f(0.95f, 0.0f, 0.0f);
        int divisor = 1;
        for(int i = 0; i < 4; i++) {
            if(strobe) {
                digits[i].draw(10.0f/16.0f, 0.0f);
            } else {
                digits[i].draw((value / divisor) % 10 / 16.0f, 0.0f);
            }
            divisor *= 10;
        }

        if(!strobe) {
            int i;
            for(i = 0; i < 5; i++) {
                bitToColor(dotconfig & (1 << i));
                dots[i].draw();
            }
            for(i = 0; i < 3; i++) {
                bitToColor(dotconfig & (1 << (5 + i)));
                colons[i].draw();
            }
        }

        Utils.setGlColorFromDye(dye);
        UtilsClient.enableLight();
        base.draw();

        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        UtilsClient.enableBlend();
        obj.bindTexture("Reflection.png");
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        float normYaw = player.rotationYaw / 360.0f;
        float normPitch = player.rotationPitch / 180.0f;
        float offset = (((float) player.posX) + ((float) player.posZ)) / 64.0f;
        glass.draw(normYaw + offset, normPitch * 0.875f);
        UtilsClient.disableBlend();
    }

}
