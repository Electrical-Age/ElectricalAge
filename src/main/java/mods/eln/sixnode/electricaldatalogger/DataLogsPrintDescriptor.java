package mods.eln.sixnode.electricaldatalogger;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.UtilsClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import org.lwjgl.opengl.GL11;

public class DataLogsPrintDescriptor extends GenericItemUsingDamageDescriptor {

    public DataLogsPrintDescriptor(String name) {
        super(name);
    }

    public void initializeStack(ItemStack stack, DataLogs logs) {
        NBTTagCompound nbt = new NBTTagCompound();
        logs.writeToNBT(nbt, "");//.setByteArray("logs", logs.copyLog());
        stack.setTagCompound(nbt);
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    public final static float margin = 0.05f;
    public final static ResourceLocation backgroundTexture = new ResourceLocation("eln", "sprites/paper.png");

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        NBTTagCompound nbt = item.getTagCompound();
        //byte [] logsArray = nbt.getByteArray("logs");
        //if(logsArray != null) {

        GL11.glPushMatrix();

        // FIXME: (Grissess) The scale and rotate almost make sense, but the translate is entirely unjustified
        GL11.glScalef(0.75f, -0.75f, 0.70f);
        GL11.glTranslatef(-0.12f, 0.0f, 0.15f);
        GL11.glRotatef(90, 0f, 1f, 0f);
        GL11.glTranslatef(-0.5f, -0.5f, 0.1f);

        UtilsClient.bindTexture(backgroundTexture);
        UtilsClient.disableBilinear();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(-margin, -margin);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(-margin, 1.1f + margin);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(1.3f + margin, 1.1f + margin);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(1.3f + margin,  -margin);
        GL11.glEnd();

        GL11.glLineWidth(1f);
        GL11.glColor4f(1f, 0f, 0f, 1f);
        GL11.glTranslatef(0.0f, 0.0f, 0.01f);
        GL11.glDisable(GL11.GL_LIGHTING);
        DataLogs.draw(nbt, 1f, 1f, "");
        //	DataLogs.draw(logsArray, logsArray.length);
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
        //}
    }
}
