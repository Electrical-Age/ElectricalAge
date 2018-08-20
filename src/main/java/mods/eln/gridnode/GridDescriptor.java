package mods.eln.gridnode;

import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;

/**
 * Created by svein on 22/08/15.
 */
public class GridDescriptor extends TransparentNodeDescriptor {
    public final ElectricalCableDescriptor cableDescriptor;
    public final ArrayList<Obj3D.Obj3DPart> plus = new ArrayList<Obj3D.Obj3DPart>();
    public final ArrayList<Obj3D.Obj3DPart> gnd = new ArrayList<Obj3D.Obj3DPart>();
    protected final String cableTexture;
    private final Obj3D obj;
//    private final Obj3D.Obj3DPart main;

    protected ArrayList<Obj3D.Obj3DPart> static_parts = new ArrayList<Obj3D.Obj3DPart>();
    protected ArrayList<Obj3D.Obj3DPart> rotating_parts = new ArrayList<Obj3D.Obj3DPart>();

    public GridDescriptor(String name, Obj3D obj, Class ElementClass, Class RenderClass, String cableTexture, ElectricalCableDescriptor cableDescriptor) {
        super(name, ElementClass, RenderClass);
        this.obj = obj;
        this.cableDescriptor = cableDescriptor;

        rotating_parts.add(obj.getPart("main"));
        for (int i = 0; ; i++) {
            Obj3D.Obj3DPart plus = obj.getPart("p" + i);
            Obj3D.Obj3DPart gnd = obj.getPart("g" + i);
            if (plus == null || gnd == null) break;
            rotating_parts.add(plus);
            rotating_parts.add(gnd);
            this.plus.add(plus);
            this.gnd.add(gnd);
        }
        this.cableTexture = cableTexture;
    }

    public void draw(float idealRenderingAngle) {
        final boolean fixed = rotationIsFixed();
        if (!fixed) {
            glPushMatrix();
            glRotatef(idealRenderingAngle, 0, 1, 0);
        }
        for (Obj3D.Obj3DPart part : rotating_parts) {
            part.draw();
        }
        if (!fixed) {
            glPopMatrix();
        }
        for (Obj3D.Obj3DPart part : static_parts) {
            part.draw();
        }
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        } else {
            GL11.glPushMatrix();
            objItemScale(obj);
            Direction.ZN.glRotateXnRef();
            GL11.glTranslatef(0, -1, 0);
            GL11.glScalef(0.6f, 0.6f, 0.6f);
            draw(0);
            GL11.glPopMatrix();
        }
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
                                         ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    public boolean rotationIsFixed() {
        return false;
    }
}
