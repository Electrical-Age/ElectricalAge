package mods.eln.transparentnode.fuelgenerator;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class FuelGeneratorDescriptor extends TransparentNodeDescriptor {

    public ElectricalCableDescriptor cable;
    public double nominalPower;
    public double maxVoltage;
    public float rpm;
    public boolean hasFuel = false;
    public String soundName;
    public float nominalVolume;
    Obj3D obj;
    private Obj3DPart main;

    public FuelGeneratorDescriptor(
            String name, Obj3D obj,
            ElectricalCableDescriptor cable,
            double nominalPower,
            double maxVoltage,
            String soundName,
            float nominalVolume
    ) {
        super(name, FuelGeneratorElement.class, FuelGeneratorRender.class);

        this.cable = cable;
        this.nominalPower = nominalPower;
        this.maxVoltage = maxVoltage;
        this.soundName = soundName;
        this.nominalVolume = nominalVolume;

        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
        }
    }

    public void setParent(net.minecraft.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    public void draw() {

        if (main != null) main.draw();
    }

    public CableRenderDescriptor getCableRenderDescriptor() {
        return cable.render;
    }

    @Override
    public boolean use2DIcon() {
        return false;
    }

    @Override
    public Direction getFrontFromPlace(Direction side,
                                       EntityLivingBase entityLiving) {
        return super.getFrontFromPlace(side.left(), entityLiving);
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
                                         ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        objItemScale(obj);
        GL11.glPushMatrix();
        Direction.ZP.glRotateXnRef();
        GL11.glTranslatef(0, -1, 0);
        GL11.glScalef(0.6f, 0.6f, 0.6f);
        draw();
        GL11.glPopMatrix();
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
                               List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add("Produces power using all kinds of burnable fuel.");
        list.add(Utils.plotVolt("Voltage:", cable.electricalNominalVoltage));
        list.add(Utils.plotPower("Power:", nominalPower));
    }
}
