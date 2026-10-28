package mods.eln.sixnode.batterycharger;

import mods.eln.misc.UtilsClient;


import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import mods.eln.gl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class BatteryChargerRender extends SixNodeElementRender {

    BatteryChargerDescriptor descriptor;

    Coordonate coord;
    boolean[] charged = new boolean[]{false, false, false, false};
    boolean[] batteryPresence = new boolean[]{false, false, false, false};

    ItemEntity[] b = new ItemEntity[4];
    boolean powerOn;
    private float voltage;

    /** A cubicle is 3/16 square: the item fills it less a sliver; four times as thick as an item frame's (0.75 px). */
    static final float ITEM_SIZE = 0.18f, ITEM_THICK = 4, ITEM_X = 0.125f + 0.18f / 16 * 4 / 2 + 0.002f;

    /** Switched on and fed (BatteryChargerElement.powered: a tenth of the rated voltage); else the LEDs are dark. */
    boolean powered() {
        return powerOn && voltage >= descriptor.nominalVoltage * BatteryChargerElement.POWERED_SHARE;
    }

    public BatteryChargerRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (BatteryChargerDescriptor) descriptor;

        coord = new Coordonate(tileEntity);
    }

    @Override
    public void draw() {
        super.draw();

        drawPowerPin(descriptor.pinDistance);

        if (side.isY()) {
            front.right().glRotateOnX();
        }
        // on a ceiling the frame comes out half a turn round from the floor's, as seen by whoever placed it: batteries read
        // upside down from below, slot 0 nearest (AdventurAgent). Turn the body round (it's symmetric; the LEDs and the
        // batteries turn together, so each stays by its own LED, in the floor's order).
        if (side == Direction.YP) GL11.glRotatef(180, 1, 0, 0);

        // the batteries lie in their cubicles (3 px square, a pixel deep, floor at x = 0.125), as large as fits and
        // thickened (Baughn: they were small, spinning upright)
        for (int i = 0; i < 4; i++)
            if (b[i] != null)
                UtilsClient.drawFlatItem(b[i].getItem(), ITEM_X, i < 2 ? 0.15625 : -0.15625, i % 2 == 0 ? 0.15625 : -0.15625, ITEM_SIZE, ITEM_THICK);

        descriptor.draw(batteryPresence, charged, powered(), (System.currentTimeMillis() / 500) % 2 == 0);
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return descriptor.cable.render;
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

}
