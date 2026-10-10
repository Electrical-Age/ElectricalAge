package mods.eln.transparentnode.solarpanel;

import mods.eln.gl.GL11;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class SolarPanelDescriptor extends TransparentNodeDescriptor {

    final Coordonate groundCoordinate;
    boolean basicModel;
    private Obj3D obj;
    private Obj3DPart main;
    private Obj3DPart panel;

    public SolarPanelDescriptor(
        String name,
        Obj3D obj, CableRenderDescriptor cableRender,
        GhostGroup ghostGroup, int solarOffsetX, int solarOffsetY, int solarOffsetZ,
        Coordonate groundCoordinate, double electricalUmax, double electricalPmax,
        double electricalDropFactor,
        double alphaMin, double alphaMax

    ) {
        super(name, SolarPanelElement.class, null);
        this.groundCoordinate = groundCoordinate;
        this.ghostGroup = ghostGroup;

        electricalRs = electricalUmax * electricalUmax * electricalDropFactor
            / electricalPmax / 2.0;
        this.electricalPmax = electricalPmax;
        this.solarOffsetX = solarOffsetX;
        this.solarOffsetY = solarOffsetY;
        this.solarOffsetZ = solarOffsetZ;
        this.alphaMax = alphaMax;
        this.alphaMin = alphaMin;
        basicModel = true;
        this.electricalUmax = electricalUmax;

        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            panel = obj.getPart("panel");
        }

        this.cableRender = cableRender;

        canRotate = alphaMax != alphaMin;

        voltageLevelColor = VoltageLevelColor.fromVoltage(electricalUmax);
    }


    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    CableRenderDescriptor cableRender;
    double electricalUmax;
    double electricalPmax;

    int solarOffsetX, solarOffsetY, solarOffsetZ;
    double alphaMin, alphaMax;
    //double efficiency;
    double electricalRs;

    public boolean canRotate;

    public void applyTo(ElectricalLoad load) {
        load.setRs(electricalRs);
    }


    public double alphaTrunk(double alpha) {
        if (alpha > alphaMax) return alphaMax;
        if (alpha < alphaMin) return alphaMin;
        return alpha;
    }

    @Override
    public Direction getFrontFromPlace(Direction side, LivingEntity entityLiving) {
        if (canRotate && groundCoordinate != null) {
            // That is, if this isn't a 1x1 panel.
            return Direction.ZN;
        } else {
            return super.getFrontFromPlace(side, entityLiving);
        }
    }

    void draw(float alpha, Direction front) {
        front.glRotateZnRef();
        GL11.glTranslatef(0, 0, main.getFloat("offZ"));
        if (main != null) main.draw();
        if (panel != null) {
            front.glRotateZnRefInv();
            panel.draw(alpha, 0f, 0f, 1f);
        }
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Produces power from solar radiation."));
        list.add("  " + tr("Max. voltage: %1$V", Utils.plotValue(electricalUmax)));
        list.add("  " + tr("Max. power: %1$W", Utils.plotValue(electricalPmax)));
        if (canRotate) list.add(tr("Can be geared towards the sun."));
    }

    @Override
    public void addCollisionBoxesToList(AABB par5AxisAlignedBB, List list, Level world, int x, int y, int z) {
        if (canRotate) {
            super.addCollisionBoxesToList(par5AxisAlignedBB, list, world, x, y, z);
            return;
        }
        // 1.7.10: stone.getCollisionBoundingBoxFromPool (absolute), then maxY -= 0.5 (AABB is immutable now)
        AABB bb = new AABB(x, y, z, x + 1, y + 1, z + 1);
        bb = bb.setMaxY(bb.maxY - 0.5);
        if (par5AxisAlignedBB.intersects(bb)) list.add(bb);
    }
}
