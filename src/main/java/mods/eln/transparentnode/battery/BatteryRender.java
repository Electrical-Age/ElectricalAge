package mods.eln.transparentnode.battery;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDUMask;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataInputStream;
import java.io.IOException;

public class BatteryRender extends TransparentNodeElementRender {

    //public double voltagePositive = 0, voltageNegative, current = 0, temperature = 0;
    public float energy, life;

    public BatteryDescriptor descriptor;

    TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(2, 64, this);

    boolean plus, minus;

    LRDUMask lrdu = new LRDUMask();

    float power;

    public BatteryRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (BatteryDescriptor) descriptor;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        descriptor.draw(plus, minus);
    }

	/*
    @Override
	public GuiContainer newGuiDraw(Direction side, EntityPlayer player) {
		return new TransformatorGuiDraw(player, inventory, this);
	}*/

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
			/*voltagePositive = stream.readShort() / Node.networkSerializeUFactor;
			voltageNegative = stream.readShort() / Node.networkSerializeUFactor;
			current = stream.readShort() / Node.networkSerializeIFactor;
			temperature = stream.readShort() / Node.networkSerializeTFactor;
			*/
            power = stream.readFloat();
            energy = stream.readFloat();
            life = stream.readShort() / 1000.0f;

            lrdu.deserialize(stream);

            plus = true;
            minus = true;
            //plus = lrdu.get(lrdu)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new BatteryGuiDraw(player, inventory, this);
    }
}
