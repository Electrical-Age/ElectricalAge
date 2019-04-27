package mods.eln.sixnode.batterycharger;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class BatteryChargerRender extends SixNodeElementRender {

    BatteryChargerDescriptor descriptor;

    Coordinate coord;
    boolean[] charged = new boolean[]{false, false, false, false};
    boolean[] batteryPresence = new boolean[]{false, false, false, false};

    float alpha = 0;

    EntityItem[] b = new EntityItem[4];
    boolean powerOn;
    private float voltage;

    public BatteryChargerRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (BatteryChargerDescriptor) descriptor;

        coord = new Coordinate(tileEntity);
    }

    @Override
    public void draw() {
        super.draw();

        drawPowerPin(descriptor.pinDistance);

        if (side.isY()) {
            front.right().glRotateOnX();
        }

        drawEntityItem(b[0], 0.1875, 0.15625, 0.15625, alpha, 0.2f);
        drawEntityItem(b[1], 0.1875, 0.15625, -0.15625, alpha, 0.2f);
        drawEntityItem(b[2], 0.1875, -0.15625, 0.15625, alpha, 0.2f);
        drawEntityItem(b[3], 0.1875, -0.15625, -0.15625, alpha, 0.2f);

        descriptor.draw(batteryPresence, charged);
    }

    @Override
    public void refresh(float deltaT) {
        alpha += 90 * deltaT;
        if (alpha > 360) alpha -= 360;
    }

    public void drawEntityItem(EntityItem entityItem, double x, double y, double z, float roty, float scale) {
        if (entityItem == null) return;

        entityItem.hoverStart = 0.0f;
        entityItem.rotationYaw = 0.0f;
        entityItem.motionX = 0.0;
        entityItem.motionY = 0.0;
        entityItem.motionZ = 0.0;
        //scale *= 10;
        Render var10;
        // TODO(1.10): Render items.
//        var10 = RenderManager.instance.getEntityRenderObject(entityItem);
//        GL11.glPushMatrix();
//        GL11.glTranslatef((float) x, (float) y, (float) z);
//        GL11.glRotatef(90, 0f, 1f, 0f);
//        GL11.glRotatef(roty, 0, 1, 0);
//        GL11.glScalef(scale, scale, scale);
//        GL11.glTranslatef(0.0f, -0.25f, 0.0f);
//        var10.doRender(entityItem, 0, 0, 0, 0, 0);
//        GL11.glPopMatrix();
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return descriptor.cable.render;
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new BatteryChargerGui(this, player, inventory);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            powerOn = stream.readBoolean();
            voltage = stream.readFloat();

            for (int idx = 0; idx < 4; idx++) {
                b[idx] = Utils.unserializeItemStackToEntityItem(stream, b[idx], tileEntity);
            }

            byte temp = stream.readByte();
            for (int idx = 0; idx < 4; idx++) {
                charged[idx] = (temp & 1) != 0;
                temp = (byte) (temp >> 1);
            }
            temp = stream.readByte();
            for (int idx = 0; idx < 4; idx++) {
                batteryPresence[idx] = (temp & 1) != 0;
                temp = (byte) (temp >> 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    SixNodeElementInventory inventory = new SixNodeElementInventory(5, 64, this);
}
