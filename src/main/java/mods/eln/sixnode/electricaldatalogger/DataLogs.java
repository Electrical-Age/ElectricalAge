package mods.eln.sixnode.electricaldatalogger;

import mods.eln.gl.GL11;

import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.nbt.CompoundTag;

public class DataLogs implements INBTTReady {

    byte[] log;
    int sizeMax, size;

    float samplingPeriod = 0.5f;
    float maxValue = 100f, minValue = 0f;
    byte unitType = percentType;
    static final byte voltageType = 0, currentType = 1, powerType = 2, celsiusType = 3, percentType = 4, energyType = 5;

    public DataLogs(int sizeMax) {
        log = new byte[sizeMax];
        this.sizeMax = sizeMax;
        size = 0;
    }

    void write(byte data) {
        int idx;
        if (size != sizeMax) {
            size++;
        }
        if (size != sizeMax)
            idx = size;
        else
            idx = size - 1;

        while (idx > 0) {
            log[idx] = log[idx - 1];
            idx--;
        }
        log[0] = data;
    }

    void reset() {
        size = 0;
    }

    int size() {
        return size;
    }

    byte read(int idx) {
        return log[idx];
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        byte[] cpy = nbt.getByteArray(str + "log").orElse(new byte[0]);
        Utils.println("Datalog readnbt " + cpy.length);
        for (int idx = 0; idx < cpy.length; idx++) {
            write(cpy[cpy.length - 1 - idx]);
        }

        samplingPeriod = nbt.getFloatOr(str + "samplingPeriod", 0f);
        maxValue = nbt.getFloatOr(str + "maxValue", 0f);
        minValue = nbt.getFloatOr(str + "minValue", 0f);
        unitType = nbt.getByteOr(str + "unitType", (byte) 0);
        Utils.println("Datalog readnbt done");
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putByteArray(str + "log", copyLog());
        nbt.putFloat(str + "samplingPeriod", samplingPeriod);
        nbt.putFloat(str + "maxValue", maxValue);
        nbt.putFloat(str + "minValue", minValue);
        nbt.putByte(str + "unitType", unitType);
    }

    public byte[] copyLog() {
        byte[] cpy = new byte[size];
        for (int idx = 0; idx < size; idx++) {
            cpy[idx] = read(idx);
        }
        return cpy;
    }

    @Override
    public String toString() {
        String str = "";
        for (int idx = 0; idx < size; idx++) {
            str += ((int) read(idx) + 128) + " ";
        }
        return str;
    }

    void draw(float margeX, float margeY, String textHeader) {
        draw(log, size, samplingPeriod, maxValue, minValue, unitType, margeX, margeY, textHeader);
    }
    static void draw(byte[] value, int size, float samplingPeriod, float maxValue, float minValue, byte unitType, float margeX, float margeY, String textHeader) {
        if (value == null) return;
        if (size < 2) return;
        //long startT = System.nanoTime();
        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        //L11.glEnable(GL11.GL_LINE_SMOOTH);
        //GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        float dx = 1f / (size - 1);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int idx = 0; idx < size; idx++) {
            GL11.glVertex2f(margeX - dx * idx * margeX, margeY - ((int) value[idx] + 128) / 255f * margeY);
        }
        GL11.glEnd();

        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int idx = 0; idx < size; idx++) {
            float x = margeX - dx * idx * margeX;
            float y = margeY - ((int) value[idx] + 128) / 255f * margeY;
                /*float dy = 0.0f;
				int dyInt; 
				if (idx == 0) dyInt = value[idx] - value[idx + 1];
				else if (idx == size-1) dyInt = value[idx - 1] - value[idx];
				else dyInt = value[idx - 1] - value[idx + 1];
				dy = -(dyInt) / 255f * margeY;
				float norm = (float) Math.sqrt(dy * dy + dx * dx);
				float rx = dy / norm * 0.01f, ry = -dx / norm * 0.01f;
				if ((idx & 1) == 0)
					GL11.glColor3f(1f, 0f, 0f);
				else
					GL11.glColor3f(0f, 1f, 0f);
				
				GL11.glVertex2f(x - rx,y - ry);
				GL11.glVertex2f(x + rx,y + ry);*/

            GL11.glVertex2f(x, y + 0.01f);
            GL11.glVertex2f(x, y - 0.01f);
        }
        GL11.glEnd();

        float temp = 0.01f;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        GL11.glVertex2f(margeX + temp, 0f);
        GL11.glVertex2f(margeX - temp, 0f);
        GL11.glVertex2f(margeX + temp, margeY + temp);
        GL11.glVertex2f(margeX - temp, margeY - temp);
        GL11.glVertex2f(0f, margeY + temp);
        GL11.glVertex2f(0f, margeY - temp);
        GL11.glEnd();

        if ((minValue < 0 && maxValue > 0) || (minValue > 0 && maxValue < 0)) {
            temp = 0.005f;
            float zeroY = (maxValue) / (maxValue - minValue) * margeY;
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            GL11.glVertex2f(margeX, zeroY + temp);
            GL11.glVertex2f(margeX, zeroY - temp);
            GL11.glVertex2f(0f, zeroY + temp);
            GL11.glVertex2f(0f, zeroY - temp);
            GL11.glEnd();
        }
		/*
		GL11.glBegin(GL11.GL_LINE_STRIP);
			GL11.glVertex2f(margeX, 0f);
			GL11.glVertex2f(margeX, margeY);
			GL11.glVertex2f(0f, margeY);
		GL11.glEnd();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		*/
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        
        GL11.glPushMatrix();
        float scale = 0.01f;
        GL11.glScalef(scale, scale, 1f);
        //UtilsClient.drawString("Time", (int)( 0.5f / scale), (int)(0.8f / scale), 0);

        UtilsClient.drawString(textHeader + " " + getYstring(1f, maxValue, minValue, unitType), (int) (margeX / scale), (int) (0f / scale), 0);
        UtilsClient.drawString(textHeader + " " + getYstring(0.5f, maxValue, minValue, unitType), (int) (margeX / scale), (int) ((margeY / 2 - 0.05f) / scale), 0);
        UtilsClient.drawString(textHeader + " " + getYstring(0.0f, maxValue, minValue, unitType), (int) (margeX / scale), (int) ((margeY - 0.08f) / scale), 0);

        UtilsClient.drawString(textHeader + Utils.plotTime(size * samplingPeriod), (int) (0f / scale), (int) ((margeY + 0.03) / scale), 0);
        UtilsClient.drawString(textHeader + Utils.plotTime(0), (int) ((margeX - 0.05) / scale), (int) ((margeY + 0.03) / scale), 0);
        //UtilsClient.drawString("Time", (int)(0.5f / scale), (int)(0.8f / scale), 0);
        GL11.glPopMatrix();
        //startT = System.nanoTime() - startT;
        //Utils.println("startT : " + startT);
    }
    public static String getYstring(float factor, float maxValue, float minValue, byte unitType) {
        String str = "";

        switch (unitType) {
            case celsiusType:
                str = Utils.plotCelsius("", factor * (maxValue - minValue) + minValue - PhysicalConstant.Tamb);
                break;
            case voltageType:
                str = Utils.plotVolt("", factor * (maxValue - minValue) + minValue);
                break;
            case currentType:
                str = Utils.plotAmpere("", factor * (maxValue - minValue) + minValue);
                break;
            case powerType:
                str = Utils.plotPower("", factor * (maxValue - minValue) + minValue);
                break;
            case percentType:
                str = Utils.plotPercent("", (factor * (maxValue - minValue) + minValue) * 0.01);
                break;
            case energyType:
                str = Utils.plotEnergy("", (factor * (maxValue - minValue) + minValue));
                break;
        }
        return str;
    }
    public static void draw(CompoundTag nbt, float margeX, float margeY, String textHeader) {
        if (nbt == null) return;
        byte[] data = nbt.getByteArray("log").orElse(new byte[0]);
        if (data == null) return;
        draw(data, data.length, nbt.getFloatOr("samplingPeriod", 0f), nbt.getFloatOr("maxValue", 0f), nbt.getFloatOr("minValue", 0f), nbt.getByteOr("unitType", (byte) 0), margeX, margeY, textHeader);
    }
}
