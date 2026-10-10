package mods.eln.transparentnode.transformer;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class TransformerDescriptor extends TransparentNodeDescriptor {
    public final float minimalLoadToHum;

    public TransformerDescriptor(String name, Obj3D obj, Obj3D defaultFeroObj, Obj3D casing, float minimalLoadToHum) {
        super(name, TransformerElement.class, null);
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
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Transforms an input voltage to\nan output voltage.").split("\n"));
        Collections.addAll(list, tr("The voltage ratio is proportional\nto the cable stacks count ratio.").split("\n"));
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)
// 26.x port: draw() removed (client rendering; redone in P3)
}
