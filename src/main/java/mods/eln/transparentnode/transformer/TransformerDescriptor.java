package mods.eln.transparentnode.transformer;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
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

public class TransformerDescriptor extends TransparentNodeDescriptor {
    public final float minimalLoadToHum;

    public TransformerDescriptor(String name, Obj3D obj, Obj3D defaultFeroObj, Obj3D casing, float minimalLoadToHum) {
        super(name, TransformerElement.class, TransformerRender.class);
        this.minimalLoadToHum = minimalLoadToHum;

        if (obj != null) {
            main = obj.getPart("main");
            sbire = obj.getPart("sbire");
        }
        if (defaultFeroObj != null) {
            defaultFero = defaultFeroObj.getPart("fero");
        }
        if (casing != null) {
            this.casing = casing.getPart("Case");
            casingDoorR = casing.getPart("DoorR");
            casingDoorL = casing.getPart("DoorL");
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    private Obj3DPart main;
    private Obj3DPart defaultFero;
    private Obj3DPart sbire;
    private Obj3DPart casing;
    private Obj3DPart casingDoorR;
    private Obj3DPart casingDoorL;

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Transforms an input voltage to\nan output voltage.").split("\n"));
        Collections.addAll(list, tr("The voltage ratio is proportional\nto the cable stacks count ratio.").split("\n"));
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        } else {
            draw(defaultFero, 1, 4, false, 0f);
        }
    }

    @Override
    public boolean use2DIcon() {
        return true;
    }

    void draw(Obj3DPart fero, int priCableNbr, int secCableNbr, boolean hasCasing, float doorOpen) {
        if (main != null) main.draw();
        if (fero != null) {
            fero.draw();
            if (priCableNbr != 0) {
                GL11.glPushMatrix();
                float y = (priCableNbr - 1) * 1f / 16f;
                GL11.glTranslatef(0f, -y, 0f);
                for (int idx = 0; idx < priCableNbr; idx++) {
                    sbire.draw();
                    GL11.glTranslatef(0f, 2f / 16f, 0f);
                }
                GL11.glPopMatrix();
            }
            if (secCableNbr != 0) {
                GL11.glPushMatrix();
                GL11.glRotatef(180, 0f, 1f, 0f);
                float y = (secCableNbr - 1) * 1f / 16f;
                GL11.glTranslatef(0f, -y, 0f);
                for (int idx = 0; idx < secCableNbr; idx++) {
                    sbire.draw();
                    GL11.glTranslatef(0f, 2f / 16f, 0f);
                }
                GL11.glPopMatrix();
            }

        }

        if (hasCasing) {
            if (casing != null) casing.draw();
            if (casingDoorR != null) casingDoorR.draw(doorOpen * 90, 0f, 1f, 0f);
            if (casingDoorL != null) casingDoorL.draw(-doorOpen * 90, 0f, 1f, 0f);
        }
    }
}
