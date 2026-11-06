package mods.eln.transparentnode.transformer;

import mods.eln.misc.Obj3D;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The Variable DC-DC Converter (Baughn 2026-11-06): a DC-DC converter whose secondary is tapped by a signal on its front,
 * so a circuit can set its output voltage. Always isolated, so a new tap changes no matrix.
 */
public class VariableTransformerDescriptor extends TransformerDescriptor {
    public VariableTransformerDescriptor(String name, Obj3D obj, Obj3D defaultFeroObj, Obj3D casing, float minimalLoadToHum) {
        super(name, VariableTransformerElement.class, obj, defaultFeroObj, casing, minimalLoadToHum);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("A signal on its front taps the secondary:\n0 V one turn, 5 V every turn.\nAlways isolated.").split("\n"));
    }
}
