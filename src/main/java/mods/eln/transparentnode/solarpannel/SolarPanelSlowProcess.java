package mods.eln.transparentnode.solarpannel;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class SolarPanelSlowProcess implements IProcess {
    // How often to update power output, etc.
    final double timeCounterRefresh = 1;
    SolarPanelElement solarPanel;
    // How long since last update.
    double timeCounter = 0;

    public SolarPanelSlowProcess(SolarPanelElement solarPanel) {
        this.solarPanel = solarPanel;
    }

    public static double getSolarAlpha(World world) {
        double alpha = world.getCelestialAngleRadians(0f);
        if (alpha < Math.PI / 2 * 3) {
            alpha += Math.PI / 2;
        } else {
            alpha -= Math.PI / 2 * 3;
        }
        //return ((((world.getWorldTime()%24000)*12.0/13.0 +500 ) / 24000.0) )% 1.0 * Math.PI*2;
        return alpha;
    }

    @Override
    public void process(double time) {
        timeCounter -= time;
        if (timeCounter < 0) {
            //Utils.println("Solar Light : " + getSolarLight());
            /*if(solarPanel.descriptor.basicModel == false)
            {
				solarPanel.currentSource.setI(solarPanel.descriptor.solarIfS.getValue(getSolarLight()));
			}
			else*/
            {
                solarPanel.powerSource.setP(solarPanel.descriptor.electricalPmax * getSolarLight());
            }
            timeCounter = timeCounterRefresh;
        }
    }

    public double getSolarLight() {
        double solarAlpha = getSolarAlpha();
        //	Utils.print("solarAlpha : " + solarAlpha + "  ");
        if (solarAlpha >= Math.PI) return 0.0;

        if (solarPanel.inventory.getStackInSlot(SolarPannelContainer.trackerSlotId) != null) {
            solarPanel.panelAlpha = solarPanel.descriptor.alphaTrunk(solarAlpha);
        }

        Coordonate coordinate = solarPanel.node.coordonate;
        Vec3 v = Utils.getVec05(coordinate);
        double x = v.xCoord + solarPanel.descriptor.solarOffsetX, y = v.yCoord + solarPanel.descriptor.solarOffsetY, z = v.zCoord + solarPanel.descriptor.solarOffsetZ;

        double lightAlpha = solarPanel.panelAlpha - solarAlpha;
        double light = Math.cos(lightAlpha);

        if (light < 0.0) light = 0.0;

        if (!coordinate.getWorldExist()) return light;

        World world = coordinate.world();
        if (world.getWorldInfo().isRaining()) light *= 0.5;
        if (world.getWorldInfo().isThundering()) light *= 0.5;

        double translucency = getTranslucency(solarAlpha, x, y, (int) z, world);
        light *= translucency;
//		Utils.print("count : " + count + "   ");
        return light;
    }

    private double getTranslucency(double solarAlpha, double x, double y, int z, World world) {
        double xD = Math.cos(solarAlpha);
        double yD = Math.sin(solarAlpha);

        if (Math.abs(xD) > yD) {
            xD = Math.signum(xD);
            yD /= Math.abs(xD);
        } else {
            yD = 1.0;
            xD /= yD;
        }
        double translucency = 1.0;
        while (world.getChunkProvider().chunkExists(((int) x) >> 4, z >> 4)) {
            double opacity = world.getBlockLightOpacity((int) x, (int) y, z);
            translucency *= (255 - opacity) / 255;
            if (translucency == 0.0) {
                break;
            }

            x += xD;
            y += yD;
            if (y > 256.0) break;
        }
        return translucency;
    }

    public double getSolarAlpha() {
        return getSolarAlpha(solarPanel.node.coordonate.world());
    }
}
