package mods.eln.transparentnode.solarpanel;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.wiki.Data;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class SolarPanelDescriptor extends TransparentNodeDescriptor {

    final Coordinate groundCoordinate;
    boolean basicModel;
    private Obj3D obj;
    private Obj3DPart main;
    private Obj3DPart panel;

    public SolarPanelDescriptor(
        String name,
        Obj3D obj, CableRenderDescriptor cableRender,
        GhostGroup ghostGroup, int solarOffsetX, int solarOffsetY, int solarOffsetZ,
        Coordinate groundCoordinate, double electricalUmax, double electricalPmax,
        double electricalDropFactor,
        double alphaMin, double alphaMax

    ) {
        super(name, SolarPanelElement.class, SolarPanelRender.class);
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


    public void setParent(net.minecraft.item.Item item, int damage) {
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

    boolean canRotate;

    public void applyTo(ElectricalLoad load) {
        load.setRs(electricalRs);
    }


    public double alphaTrunk(double alpha) {
        if (alpha > alphaMax) return alphaMax;
        if (alpha < alphaMin) return alphaMin;
        return alpha;
    }

    @Override
    public Direction getFrontFromPlace(Direction side, EntityLivingBase entityLiving) {
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

    // TODO(1.10): Fix item render.
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
//                                         ItemRendererHelper helper) {
//        return type != ItemRenderType.INVENTORY;
//    }
//
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        return true;
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        if (type == ItemRenderType.INVENTORY) {
//            super.renderItem(type, item, data);
//        } else {
//            draw((float) alphaMin, Direction.XN);
//        }
//    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Produces power fromFacing solar radiation."));
        list.add("  " + tr("Max. voltage: %sV", Utils.plotValue(electricalUmax)));
        list.add("  " + tr("Max. power: %sW", Utils.plotValue(electricalPmax)));
        if (canRotate) list.add(tr("Can be geared towards the sun."));
    }

    @Override
    public void addCollisionBoxesToList(AxisAlignedBB par5AxisAlignedBB, List list, BlockPos pos) {
        if (canRotate) {
            super.addCollisionBoxesToList(par5AxisAlignedBB, list, pos);
            return;
        }
        AxisAlignedBB bb = new AxisAlignedBB(pos).setMaxY(0.5);
        if (par5AxisAlignedBB.intersectsWith(bb)) list.add(bb);
    }
}
