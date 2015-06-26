package mods.eln.sixnode.groundcable;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class GroundCableDescriptor extends SixNodeDescriptor {

    Obj3D obj;
    Obj3DPart main;

    public GroundCableDescriptor(String name, Obj3D obj) {
        super(name, GroundCableElement.class, GroundCableRender.class);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
        }
    }

    void draw() {
        if (main != null) main.draw();
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add("Provides a zero volt reference.");
        list.add("Can be used to ground negative");
        list.add("battery pins.");
        list.add(Utils.plotOhm("Internal resistance : ", Eln.getSmallRs()));
    }
}
