package mods.eln.sixnode.lampsocket;

import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class LampSocketDescriptor extends SixNodeDescriptor {

    public LampSocketType socketType;
    LampSocketObjRender render;

    public boolean cameraOpt = true;

    public int range;
    public String modelName;
    float alphaZMin, alphaZMax, alphaZBoot;
    public boolean cableFront = true;
    public boolean cableLeft = true;
    public boolean cableRight = true;
    public boolean cableBack = true;

    public float initialRotateDeg = 0.f;
    public boolean rotateOnlyBy180Deg = false;

    public boolean paintable = false;
    public boolean renderIconInHand = false;

    public LampSocketDescriptor(String name, LampSocketObjRender render,
                                LampSocketType socketType,
                                boolean paintable,
                                int range,
                                float alphaZMin, float alphaZMax,
                                float alphaZBoot) {
        super(name, LampSocketElement.class, LampSocketRender.class);
        this.socketType = socketType;
        this.paintable = paintable;
        this.range = range;
        this.alphaZMin = alphaZMin;
        this.alphaZMax = alphaZMax;
        this.alphaZBoot = alphaZBoot;
        this.render = render;

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    public void setInitialOrientation(float rotateDeg) {
        this.initialRotateDeg = rotateDeg;
    }

    public void setUserRotationLibertyDegrees(boolean only180) {
        this.rotateOnlyBy180Deg = only180;
    }

    boolean noCameraOpt() {
        return cameraOpt;
    }

    public void setParent(net.minecraft.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addLight(newItemStack());
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return !renderIconInHand && type != ItemRenderType.INVENTORY;
    }

    @Override
    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return !renderIconInHand && type != ItemRenderType.INVENTORY;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY || renderIconInHand)
            super.renderItem(type, item, data);
        else {
            GL11.glScalef(1.25f, 1.25f, 1.25f);
            render.draw(this, type, 0.f);
        }
    }

    @Override
    public boolean hasVolume() {
        return hasGhostGroup();
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        //list.add("Socket Type : " + socketType.toString());

        if (range != 0 || alphaZMin != alphaZMax) {
            //list.add("Projector");
            if (range != 0) {
                list.add(tr("Spot range: %1$ blocks", range));
            }
            if (alphaZMin != alphaZMax) {
                list.add(tr("Angle: %1$° to %2$°", ((int) alphaZMin), ((int) alphaZMax)));
            }
        }
    }
}
