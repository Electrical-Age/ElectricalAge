package mods.eln.sixnode.electricaldatalogger;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class ElectricalDataLoggerDescriptor extends SixNodeDescriptor {

    Obj3D obj;
    Obj3DPart main, led, reflection;
    float sx, sy, sz;
    float tx, ty, tz;
    float rx, ry, rz, ra;
    float mx, my;

    float cr, cg, cb;

    float reflc;

    public boolean onFloor;
    private String textColor;

    public ElectricalDataLoggerDescriptor(String name, boolean onFloor, String objName, float cr, float cg, float cb, String textColor) {
        super(name, ElectricalDataLoggerElement.class, ElectricalDataLoggerRender.class);
        this.cb = cb;
        this.cr = cr;
        this.cg = cg;
        this.onFloor = onFloor;
        this.textColor = textColor;
        obj = Eln.obj.getObj(objName);
        if (obj != null) {
            main = obj.getPart("main");
            reflection = obj.getPart("reflection");
            if (main != null) {
                sx = main.getFloat("sx");
                sy = main.getFloat("sy");
                sz = main.getFloat("sz");
                tx = main.getFloat("tx");
                ty = main.getFloat("ty");
                tz = main.getFloat("tz");
                rx = main.getFloat("rx");
                ry = main.getFloat("ry");
                rz = main.getFloat("rz");
                ra = main.getFloat("ra");
                mx = main.getFloat("mx");
                my = main.getFloat("my");
                reflc = main.getFloat("reflc");
                led = obj.getPart("led");
            }
        }

        if (onFloor) {
            setPlaceDirection(Direction.YN);
        }
    }

    @Override
    public boolean use2DIcon() {
        return false;
    }

    void draw(DataLogs log, LRDU front, int objPosMX, int objPosMZ) {
        if (onFloor) front.glRotateOnX();
        else GL11.glRotatef(90, 1, 0, 0);
        //GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (main != null) main.draw();
        //GL11.glEnable(GL11.GL_TEXTURE_2D);

        //Glass (reflections)
        UtilsClient.enableBlend();
        obj.bindTexture("Reflection.png");
        float rotYaw = Minecraft.getMinecraft().thePlayer.rotationYaw / 360.f;
        float rotPitch = Minecraft.getMinecraft().thePlayer.rotationPitch / 180.f;
        float pos = (((float) Minecraft.getMinecraft().thePlayer.posX) - ((float) (objPosMX * 2)) + ((float) Minecraft.getMinecraft().thePlayer.posZ) - ((float) (objPosMZ * 2))) / 24.f;
        GL11.glColor4f(1, 1, 1, reflc);
        reflection.draw(rotYaw + pos, rotPitch * 0.857f);
        UtilsClient.disableBlend();

        //Plot
        if (log != null) {
            UtilsClient.disableLight();
            // GL11.glPushMatrix();
            UtilsClient.ledOnOffColor(true);
            if (led != null) led.draw();

            UtilsClient.glDefaultColor();

            GL11.glTranslatef(tx, ty, tz);
            GL11.glRotatef(ra, rx, ry, rz);
            GL11.glScalef(sx, sy, sz);
            GL11.glColor4f(cr, cg, cb, 1);
            log.draw(mx, my, textColor);

            UtilsClient.glDefaultColor();

            UtilsClient.enableLight();
        }
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    @Override
    public boolean hasVolume() {
        return onFloor;
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
        if (type == ItemRenderType.INVENTORY) {
            GL11.glRotatef(-90, 0f, 1f, 0f);
            GL11.glTranslatef(-0.4f, 0f, 0f);
        }
        if (main != null) main.draw();
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add("This block measures the electrical");
        list.add("signal and plots a graph with the data.");
        list.add("Can store 256 samples.");
    }
}
/*
 * 	        	GL11.glScalef(1f, -1f, 1f);
	        	GL11.glTranslatef(0.1f, -0.5f, 0.5f);
	        	GL11.glRotatef(90, 0f, 1f, 0f);  */
