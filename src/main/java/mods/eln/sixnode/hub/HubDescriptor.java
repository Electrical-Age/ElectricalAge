package mods.eln.sixnode.hub;


import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class HubDescriptor extends SixNodeDescriptor {

    Obj3D obj;
    Obj3DPart main;
    Obj3DPart[] connection = new Obj3DPart[6];

    public HubDescriptor(String name, Obj3D obj) {
        super(name, HubElement.class, null);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            for (int idx = 0; idx < 6; idx++) {
                connection[idx] = obj.getPart("con" + idx);
            }
        }
        voltageLevelColor = VoltageLevelColor.Neutral;
    }
    // 26.x port: draw() removed (client rendering; redone in P3)

/*	@Override
	// 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

	// 26.x port: handleRenderType() removed (client rendering; redone in P3)
*/

	/*@Override
	// 26.x port: renderItem() removed (client rendering; redone in P3)
*/

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Allows crossing cables\non one single block.").split("\n"));
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return LRDU.Up;
    }
}
