package mods.eln.transparentnode.turret;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class TurretDescriptor extends TransparentNodeDescriptor {
    class Properties {
        public final float actionAngle;
        public final float detectionDistance;
        public final float aimDistance;
        public final float impulseEnergy;
        public final float gunMinElevation;
        public final float gunMaxElevation;
        public final float turretSeekAnimationSpeed;
        public final float turretAimAnimationSpeed;
        public final float gunArmAnimationSpeed;
        public final float gunDisarmAnimationSpeed;
        public final float gunAimAnimationSpeed;
        public final double minimalVoltage;
        public final double minimalVoltageHysteresisFactor;
        public final double maximalVoltage;
        public final double basePower;
        public final double chargePower;
        public final double entityDetectionInterval;

        public Properties() {
            actionAngle = 70;
            detectionDistance = 12;
            aimDistance = 15;
            impulseEnergy = 1000;
            gunMinElevation = -40;
            gunMaxElevation = 70;
            turretSeekAnimationSpeed = 40;
            turretAimAnimationSpeed = 70;
            gunArmAnimationSpeed = 3;
            gunDisarmAnimationSpeed = 0.5f;
            gunAimAnimationSpeed = 100;
            minimalVoltage = 600;
            minimalVoltageHysteresisFactor = 0.1;
            maximalVoltage = 1050;
            basePower = 25;
            chargePower = 1000;
            entityDetectionInterval = 0.25;
        }
    }

    private final Obj3DPart turret, holder, joint, leftGun, rightGun, sensor, fire;

    private final Properties properties;

    public TurretDescriptor(String name, String modelName) {
        super(name, TurretElement.class, TurretRender.class);

        final Obj3D obj = Eln.obj.getObj(modelName);
        turret = obj.getPart("Turret");
        holder = obj.getPart("Holder");
        joint = obj.getPart("Joint");
        leftGun = obj.getPart("LeftGun");
        rightGun = obj.getPart("RightGun");
        sensor = obj.getPart("Sensor");
        fire = obj.getPart("Fire");

        properties = new Properties();
        voltageLevelColor = VoltageLevelColor.HighVoltage;
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addMachine(newItemStack());
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Scans for entities and shoots if the\nentity matches the configurable filter criteria.").split("\n"));
        list.add(tr("Nominal voltage: %sV", 800));
        list.add(tr("Standby power: %sW", Utils.plotValue(getProperties().basePower)));
        list.add(tr("Laser charge power: %sW...%skW", 100, 10));
        list.add(tr("CAUTION: Cables can get quite hot!"));
    }

    // TODO(1.10): Fix item render.
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        return true;
//    }
//
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
//        return type != ItemRenderType.INVENTORY;
//    }
//
//    @Override
//    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
//        return type != ItemRenderType.INVENTORY;
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        if (type == ItemRenderType.INVENTORY)
//            super.renderItem(type, item, data);
//        else
//            draw(null);
//    }

    public void draw(TurretRender render) {
        float turretAngle = render != null ? render.getTurretAngle() : 0;
        float gunPosition = render != null ? render.getGunPosition() : 0;
        float gunAngle = render != null ? -render.getGunElevation() : 0;
        boolean shooting = render != null && render.isShooting();
        boolean enabled = render == null || render.isEnabled();

        if (holder != null) holder.draw();
        if (joint != null) joint.draw();
        GL11.glPushMatrix();
        GL11.glRotatef(turretAngle, 0f, 1f, 0f);
        if (turret != null) turret.draw();
        if (sensor != null) {
            if (enabled) {
                if (render != null && render.filter != null)
                    if (render.filterIsSpare)
                        render.filter.glInverseColor(0.5f + 0.5f * gunPosition);
                    else
                        render.filter.glColor(0.5f + 0.5f * gunPosition);
                else
                    GL11.glColor3f(0.5f, 0.5f, 0.5f);
                UtilsClient.drawLight(sensor);
                GL11.glColor3f(1f, 1f, 1f);
            } else {
                GL11.glColor3f(0.5f, 0.5f, 0.5f);
                sensor.draw();
            }
        }
        GL11.glRotatef(gunAngle, 0f, 0f, 1f);

        GL11.glColor4f(.6f, .8f, 1f, .4f);
        if (shooting && fire != null) UtilsClient.drawLight(fire);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        GL11.glTranslatef(0f, 0f, gunPosition / 4f);
        if (leftGun != null) leftGun.draw();
        GL11.glTranslatef(0f, 0f, -gunPosition / 2f);
        if (rightGun != null) rightGun.draw();
        GL11.glPopMatrix();
    }
}
