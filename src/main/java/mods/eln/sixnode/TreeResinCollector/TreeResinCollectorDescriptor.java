package mods.eln.sixnode.TreeResinCollector;

import mods.eln.gl.GL11;


import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class TreeResinCollectorDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    private Obj3DPart main, fill;

    float emptyS, emptyT;

    public TreeResinCollectorDescriptor(String name, Obj3D obj) {
        super(name, TreeResinCollectorElement.class, null);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            fill = obj.getPart("fill");
            if (fill != null) {
                emptyT = fill.getFloat("emptyT");
                emptyS = fill.getFloat("emptyS");
            }
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }
    void draw(float factor) {
        if (main != null) main.draw();
        if (fill != null) {
            if (factor > 1f) factor = 1f;
            factor = (1f - factor);
            GL11.glTranslatef(0f, 0f, factor * emptyT);
            GL11.glScalef(1f - factor * (1f - emptyS), 1f - factor * (1f - emptyS), 1f);
            fill.draw();
        }
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addMachine(newItemStack());
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(0.0f);

    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Produces Tree Resin over\ntime when put on a tree.").split("\n"));
    }

    // 1.12: ore dictionary treeWood/logWood and treeLeaves; 26.x: the block tags
    public static boolean isWood(Block b) {
        return b.defaultBlockState().is(net.minecraft.tags.BlockTags.LOGS);
    }

    public static boolean isLeaf(Block b) {
        return b.defaultBlockState().is(net.minecraft.tags.BlockTags.LEAVES);
    }

    @Override
    public boolean canBePlacedOnSide(Player player, Coordonate c, Direction side) {
        Block b = c.getBlock();
        if (!isWood(b) || side.isY()) {
            Utils.sendMessage(player, tr("This block can only be placed on the side of a tree!"));
            return false;
        }
        return true;
    }
}
