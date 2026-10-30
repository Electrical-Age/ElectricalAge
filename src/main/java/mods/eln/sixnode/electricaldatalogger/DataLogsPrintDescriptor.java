package mods.eln.sixnode.electricaldatalogger;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import mods.eln.gl.GL11;
import mods.eln.misc.UtilsClient;

public class DataLogsPrintDescriptor extends GenericItemUsingDamageDescriptor {

    public DataLogsPrintDescriptor(String name) {
        super(name);
    }

    public void initializeStack(ItemStack stack, DataLogs logs) {
        CompoundTag nbt = new CompoundTag();
        logs.writeToNBT(nbt);
        mods.eln.generic.ItemNbt.set(stack, nbt);
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    /** The printed graph on a sheet of paper (1.12 drew the red graph alone, over the icon). */
    @Override
    public void drawItem(ItemStack stack, boolean entity) {
        UtilsClient.disableTexture();
        UtilsClient.disableCulling();
        GL11.glColor4f(0.95f, 0.95f, 0.9f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glNormal3f(0, 0, 1);
        GL11.glVertex3f(0, 0, 0.5f);
        GL11.glVertex3f(1, 0, 0.5f);
        GL11.glVertex3f(1, 1, 0.5f);
        GL11.glVertex3f(0, 1, 0.5f);
        GL11.glEnd();
        UtilsClient.enableCulling();
        UtilsClient.enableTexture();
        // DataLogs draws y down in [0, marge]: flip into the sheet, in front of it, with a margin
        GL11.glPushMatrix();
        GL11.glTranslatef(0.08f, 0.92f, 0.51f);
        GL11.glScalef(0.84f, -0.84f, 1f);
        GL11.glColor4f(0.8f, 0f, 0f, 1f);
        UtilsClient.disableLight();
        DataLogs.draw(mods.eln.generic.ItemNbt.get(stack), 0.75f, 0.85f, "");
        UtilsClient.enableLight();
        GL11.glPopMatrix();
        UtilsClient.glDefaultColor();
    }
}
