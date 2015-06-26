package mods.eln.transparentnode.teleporter;

import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class TeleporterRender extends TransparentNodeElementRender {

    TeleporterDescriptor d;
    Coordonate c;

    public TeleporterRender(TransparentNodeEntity tileEntity,
                            TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.d = (TeleporterDescriptor) descriptor;
        doorInterpolator.setMaxSpeed(0.3f);
        c = new Coordonate(tileEntity);
    }

    public static final float doorAlphaOpen = -90;
    boolean doorState;

    PhysicalInterpolator doorInterpolator = new PhysicalInterpolator(0.2f, 8.0f, 5f, 0.0f);
    RcInterpolator processRatioInterpolator = new RcInterpolator(1);
    RcInterpolator blueInterpolator = new RcInterpolator(0.5f);

    boolean[] ledState = new boolean[10];

    float counter = 0, ledCounter;
    boolean blink = false;
    float gyroAlpha = 0;

    @Override
    public void draw() {
        Coordonate lightCoordonate = new Coordonate(this.d.lightCoordonate);
        lightCoordonate.applyTransformation(front, c);

        boolean lightEnable = tileEntity.getWorldObj().getBlock(lightCoordonate.x, lightCoordonate.y, lightCoordonate.z) == Eln.lightBlock;


        front.glRotateXnRef();
        GL11.glTranslatef(-1, 0, 0);

        GL11.glColor3f(1f, 1f, 1f);

        if (!lightEnable) {
            d.door_in.draw(doorInterpolator.get() * doorAlphaOpen, 0, 0, 1);
            d.indoor_open.draw();
        }

        d.main.draw();
        d.ext_control.draw();
        d.ext_power.draw();
        UtilsClient.disableCulling();
        d.door_out.draw(doorInterpolator.get() * doorAlphaOpen, 0, 0, 1);
        UtilsClient.enableCulling();

        //	d.outlampline0.draw();
        GL11.glColor3f(1f, 1f, 1f);
        if (doorState == true) {
            UtilsClient.disableCulling();
            d.gyro.draw(doorInterpolator.get() * doorAlphaOpen, 0, 0, 1, gyroAlpha, -0.11746f, 0.04275f, 0);
            UtilsClient.enableCulling();
        }

        UtilsClient.disableLight();
        GL11.glColor3f(1f, 0.5f, 0f);
        if (doorState == false) {
            UtilsClient.disableCulling();
            d.gyro.draw(doorInterpolator.get() * doorAlphaOpen, 0, 0, 1, gyroAlpha, -0.11746f, 0.04275f, 0);
            UtilsClient.enableCulling();
            GL11.glColor4f(1f, 0.5f, 0f, 0.4f);
            UtilsClient.enableBlend();
            d.gyro_alpha.draw(doorInterpolator.get() * doorAlphaOpen, 0, 0, 1, gyroAlpha, -0.11746f, 0.04275f, 0);
            UtilsClient.disableBlend();
        }

        GL11.glColor3f(1f, 1f, 1f);

        if (lightEnable) {
            d.door_in.draw(doorInterpolator.get() * doorAlphaOpen, 0, 0, 1);
            d.indoor_open.draw();
        }
        if (voltage > 0.6f) {

            GL11.glColor3f(1f, 0f, 0f);
            d.leds[0].draw();
            d.leds[1].draw();

            GL11.glColor3f(0f, 1f, 0f);


            for (int idx = 2; idx < 10; idx++) {
                if (ledCounter == 0) {
                    if (Math.random() < 0.3)
                        ledState[idx] = !ledState[idx];
                }
                if (ledState[idx])
                    d.leds[idx].draw();
            }

            GL11.glColor4f(1f, 0.5f, 0.0f, 1f);


            if ((voltage > 0.875f && voltage < 1.2f) || blink)
                d.scr0_electrictity.draw();

            d.scr1_cables.draw();
            d.scr2_transporter.draw();

            if (tileEntity.getWorldObj().getEntitiesWithinAABB(Entity.class, d.getBB(c, front)).size() != 0)
                d.scr3_userin.draw();

            if (doorState)
                d.scr5_dooropen.draw();
            else
                d.src4_doorclosed.draw();
        }

        if (processRatioInterpolator.get() > 0.005) {
            UtilsClient.enableBlend();
            GL11.glColor4f(1f, 1f, 1f, blueInterpolator.get());

            d.indoor_closed.draw();
            d.door_in_charge.draw(doorInterpolator.get() * doorAlphaOpen, 0, 0, 1);
            //GL11.glColor4f(0f, 0.5f, 1f,blueInterpolator.get());
            //d.outlampline0_alpha.draw();
            d.whiteblur.draw(doorInterpolator.get() * doorAlphaOpen, 0, 0, 1);
            GL11.glColor4f(1f, 1f, 1f, 1f);

            UtilsClient.disableBlend();
        }

        UtilsClient.enableLight();


        //	GL11.glColor4f(1f, 1f, 1f,1f);

	
		
		
		/*
        door_in_charge = obj.getPart("door_in_charge");
		door_in = obj.getPart("door_in");
		indoor_closed = obj.getPart("indoor_closed");
		indoor_open = obj.getPart("indoor_open");
		*/

    }


    @Override
    public void refresh(float deltaT) {

        doorInterpolator.step(deltaT);
        processRatioInterpolator.step(deltaT);
        blueInterpolator.setTarget(processRatioInterpolator.get() > 0.1 && (!doorState) ? 1f : 0f);
        blueInterpolator.step(deltaT);

        counter += deltaT;
        if (counter > 0.4) {
            counter = 0;
            blink = !blink;
        }
        ledCounter += deltaT;
        if (ledCounter > 0.1) {
            ledCounter = 0;
        }
        gyroAlpha += 360 * deltaT * (1.0f - doorInterpolator.get());
        if (gyroAlpha >= 360) gyroAlpha -= 360;
    }

    float voltage;
    String name, targetName;
    float chargePower, chargePowerLast, energyHit, energyTarget;
    boolean chargePowerNew;
    byte state;
    float processRatio;

    @Override
    public void networkUnserialize(DataInputStream stream) {

        super.networkUnserialize(stream);

        try {
            name = stream.readUTF();
            targetName = stream.readUTF();
            chargePower = stream.readFloat();
            state = stream.readByte();
            byte b = stream.readByte();

            doorState = (b & 1) != 0;

            processRatio = stream.readFloat();
            voltage = (float) (stream.readFloat() / d.cable.electricalNominalVoltage);
            energyHit = stream.readFloat();
            energyTarget = stream.readFloat();

        } catch (IOException e) {

            e.printStackTrace();
        }

        if (chargePower != chargePowerLast) {
            chargePowerNew = true;
        }
        chargePowerLast = chargePower;

        if (doorState)
            doorInterpolator.setTarget(1f);
        else
            doorInterpolator.setTarget(0f);
		/*
		if(energyTarget == 0){
			chargeRatio = 0;
		}
		else{
			chargeRatio = Math.min(1.0f,energyHit / energyTarget);
		}*/
        processRatioInterpolator.setTarget(processRatio);


    }


    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {

        return new TeleporterGui(player, this);
    }


    @Override
    public void serverPacketUnserialize(DataInputStream stream) {

        super.serverPacketUnserialize(stream);
        try {
            stream.readByte();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @Override
    public boolean cameraDrawOptimisation() {

        return false;
    }
}
