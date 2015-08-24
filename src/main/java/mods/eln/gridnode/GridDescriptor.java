package mods.eln.gridnode;

import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

/**
 * Created by svein on 22/08/15.
 */
public class GridDescriptor extends TransparentNodeDescriptor {
    public final ElectricalCableDescriptor cableDescriptor;
    public final ArrayList<Obj3D.Obj3DPart> plus = new ArrayList<Obj3D.Obj3DPart>();
    public final ArrayList<Obj3D.Obj3DPart> gnd = new ArrayList<Obj3D.Obj3DPart>();
    protected final String cableTexture;
    private final Obj3D obj;
    private final Obj3D.Obj3DPart main;

    public GridDescriptor(String name, Obj3D obj, Class ElementClass, Class RenderClass, String cableTexture, ElectricalCableDescriptor cableDescriptor) {
        super(name, ElementClass, RenderClass);
        this.obj = obj;
        this.cableDescriptor = cableDescriptor;
        this.main = obj.getPart("main");
        for (int i = 0; ; i++) {
            Obj3D.Obj3DPart plus = obj.getPart("p" + i);
            Obj3D.Obj3DPart gnd = obj.getPart("g" + i);
            if (plus == null || gnd == null) break;
            this.plus.add(plus);
            this.gnd.add(gnd);
        }
        this.cableTexture = cableTexture;
    }

    public void draw() {
        main.draw();
        for (Obj3D.Obj3DPart p : plus) {
            p.draw();
        }
        for (Obj3D.Obj3DPart m : gnd) {
            m.draw();
        }
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        objItemScale(obj);
        Direction.ZN.glRotateXnRef();
        GL11.glPushMatrix();
        GL11.glTranslatef(0, -1, 0);
        GL11.glScalef(0.6f, 0.6f, 0.6f);
        draw();
        GL11.glPopMatrix();
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
    public boolean use2DIcon() {
        return false;
    }

    public boolean rotationIsFixed() {
        return false;
    }
}
