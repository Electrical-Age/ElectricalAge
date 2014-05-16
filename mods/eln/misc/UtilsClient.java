package mods.eln.misc;

import java.awt.Color;

import mods.eln.Eln;
import mods.eln.GuiHandler;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.lwjgl.opengl.GL11;

public class UtilsClient {

	public static float distanceFromClientPlayer(World world, int xCoord, int yCoord, int zCoord) {
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

		return (float) Math.sqrt((xCoord - player.posX) * (xCoord - player.posX)
				+ (yCoord - player.posY) * (yCoord - player.posY)
				+ (zCoord - player.posZ) * (zCoord - player.posZ));
	}

	public static float distanceFromClientPlayer(SixNodeEntity tileEntity) {
		return distanceFromClientPlayer(tileEntity.worldObj, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
	}

	public static EntityClientPlayerMP getClientPlayer() {
		// TODO Auto-generated method stub
		return Minecraft.getMinecraft().thePlayer;
	}

	public static void drawHaloNoLightSetup(Obj3DPart halo, float r, float g, float b, World w, int x, int y, int z, boolean bilinear) {
		if (halo == null)
			return;
		if (bilinear)
			enableBilinear();
		int light = Utils.getLight(w, x, y, z) * 19 / 15 - 4;
		Entity e = getClientPlayer();
		float d = (float) (Math.abs(x - e.posX) + Math.abs(y - e.posY) + Math.abs(z - e.posZ));

		GL11.glColor4f(r, g, b, 1f - (light / 15f));
		halo.draw(d * 20, 1, 0, 0);
		GL11.glColor4f(1f, 1f, 1f, 1f);
		if (bilinear)
			disableBilinear();
	}

	public static GuiScreen guiLastOpen;

	public static void clientOpenGui(GuiScreen gui)
	{
		guiLastOpen = gui;
		EntityClientPlayerMP clientPlayer = (EntityClientPlayerMP) getClientPlayer();
		clientPlayer.openGui(Eln.instance, GuiHandler.genericOpen, clientPlayer.worldObj, 0, 0, 0);
	}

	public static void drawHalo(Obj3DPart halo, float r, float g, float b, World w, int x, int y, int z, boolean bilinear) {

		disableLight();
		enableBlend();

		UtilsClient.drawHaloNoLightSetup(halo, r, g, b, w, x, y, z, bilinear);
		enableLight();
		disableBlend();
	}

	public static void drawHaloNoLightSetup(Obj3DPart halo, float r, float g, float b, TileEntity e, boolean bilinear) {
		drawHaloNoLightSetup(halo, r, g, b, e.worldObj, e.xCoord, e.yCoord, e.zCoord, bilinear);
	}

	public static void drawHalo(Obj3DPart halo, float r, float g, float b, TileEntity e, boolean bilinear) {
		drawHalo(halo, r, g, b, e.worldObj, e.xCoord, e.yCoord, e.zCoord, bilinear);
	}

	public static void drawHaloNoLightSetup(Obj3DPart halo, float distance) {
		if (halo == null)
			return;
		halo.faceGroupe.get(0).bindTexture();
		enableBilinear();
		float scale = 1f;

		halo.drawNoBind();
	}

	public static void drawHalo(Obj3DPart halo, float distance) {

		disableLight();
		enableBlend();

		drawHaloNoLightSetup(halo, distance);
		enableLight();
		disableBlend();
	}

	public static void drawHaloNoLightSetup(Obj3DPart halo, float r, float g, float b, Entity e, boolean bilinear) {
		if (halo == null)
			return;
		if (bilinear)
			enableBilinear();
		int light = Utils.getLight(e.worldObj, MathHelper.floor_double(e.posX), MathHelper.floor_double(e.posY), MathHelper.floor_double(e.posZ));
		// light =
		// e.worldObj.getLightBrightnessForSkyBlocks(MathHelper.floor_double(e.posX),
		// MathHelper.floor_double(e.posY), MathHelper.floor_double(e.posZ),0);
		// System.out.println(light);
		GL11.glColor4f(r, g, b, 1f - (light / 15f));
		halo.draw();
		GL11.glColor4f(1f, 1f, 1f, 1f);
		if (bilinear)
			disableBilinear();
	}

	public static void drawHalo(Obj3DPart halo, float r, float g, float b, Entity e, boolean bilinear) {

		disableLight();
		enableBlend();

		drawHaloNoLightSetup(halo, r, g, b, e, bilinear);
		enableLight();
		disableBlend();
	}

	public static void enableBilinear() {
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
	}

	public static void disableBilinear() {
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

	}

	public static void disableCulling() {
		GL11.glDisable(GL11.GL_CULL_FACE);
	}

	public static void enableCulling() {
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

	public static void disableTexture() {
		// TODO Auto-generated method stub
		GL11.glDisable(GL11.GL_TEXTURE_2D);
	}

	public static void enableTexture() {
		// TODO Auto-generated method stub
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	public static void drawIcon(ItemRenderType type) {
		if (type == ItemRenderType.INVENTORY) {

			disableCulling();
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glTexCoord2f(1f, 0f);
			GL11.glVertex3f(16f, 0f, 0f);
			GL11.glTexCoord2f(0f, 0f);
			GL11.glVertex3f(0f, 0f, 0f);
			GL11.glTexCoord2f(0f, 1f);
			GL11.glVertex3f(0f, 16f, 0f);
			GL11.glTexCoord2f(1f, 1f);
			GL11.glVertex3f(16f, 16f, 0f);
			GL11.glEnd();
			enableCulling();
		}
		else if (type == ItemRenderType.ENTITY) {

			disableCulling();
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glTexCoord2f(1f, 1f);
			GL11.glVertex3f(0, 0f, 0.5f);
			GL11.glTexCoord2f(0f, 1f);
			GL11.glVertex3f(0.0f, 0f, -0.5f);
			GL11.glTexCoord2f(0f, 0f);
			GL11.glVertex3f(0.0f, 1f, -0.5f);
			GL11.glTexCoord2f(1f, 0f);
			GL11.glVertex3f(0.0f, 1f, 0.5f);
			GL11.glEnd();
			enableCulling();
		}
		else {
			GL11.glTranslatef(0.5f, -0.3f, 0.5f);

			disableCulling();
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glTexCoord2f(1f, 1f);
			GL11.glVertex3f(0.0f, 0.5f, 0.5f);
			GL11.glTexCoord2f(0f, 1f);
			GL11.glVertex3f(0.0f, 0.5f, -0.5f);
			GL11.glTexCoord2f(0f, 0f);
			GL11.glVertex3f(0.0f, 1.5f, -0.5f);
			GL11.glTexCoord2f(1f, 0f);
			GL11.glVertex3f(0.0f, 1.5f, 0.5f);
			GL11.glEnd();
			enableCulling();
		}
	}

	public static void drawIcon(ItemRenderType type, String icon) {
		bindTextureByName(icon);
		drawIcon(type);
	}

	public static void drawIcon(ItemRenderType type, ResourceLocation icon) {
		bindTexture(icon);
		drawIcon(type);
	}

	public static void drawIcon(ItemRenderType type, Icon icon) {
		drawIcon(type, icon.getIconName());
	}

	public static void drawEnergyBare(ItemRenderType type, float e) {
		float x = 13f, y = 15f - e * 14f;
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GL11.glColor3f(0f, 0f, 0f);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex3f(x + 2f, 1, 0.01f);
		GL11.glVertex3f(x, 1, 0f);
		GL11.glVertex3f(x, 15f, 0f);
		GL11.glVertex3f(x + 2f, 15f, 0.01f);
		GL11.glEnd();

		GL11.glColor3f(1, e, 0f);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex3f(x + 1f, y, 0.01f);
		GL11.glVertex3f(x, y, 0f);
		GL11.glVertex3f(x, 15f, 0f);
		GL11.glVertex3f(x + 1f, 15f, 0.01f);
		GL11.glEnd();

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(1f, 1f, 1f);

	}

	public static void bindTextureByName(String par1Str)
	{
		// Minecraft.getMinecraft().renderEngine.func_110577_a(new
		// ResourceLocation(par1Str));
		Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation("eln", par1Str));
	}

	public static void bindTexture(ResourceLocation ressource)
	{
		// Minecraft.getMinecraft().renderEngine.func_110577_a(new
		// ResourceLocation(par1Str));
		Minecraft.getMinecraft().renderEngine.bindTexture(ressource);
	}

	static boolean lightmapTexUnitTextureEnable;

	public static void disableLight() {
		// TODO Auto-generated method stub
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		lightmapTexUnitTextureEnable = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
		if (lightmapTexUnitTextureEnable)
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL11.glDisable(GL11.GL_LIGHTING);
	}

	public static void enableLight() {
		// TODO Auto-generated method stub
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		if (lightmapTexUnitTextureEnable)
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	public static void enableBlend() {
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDepthMask(false);

	}

	public static void disableBlend() {
		// TODO Auto-generated method stub
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	
	public static void ledOnOffColor(boolean on)
	{
		if(! on)
			GL11.glColor3f(0.7f, 0f, 0f);
		else
			GL11.glColor3f(0f, 0.7f, 0f);
	}
	public static Color ledOnOffColorC(boolean on)
	{
		if(! on)
			return new Color(0.7f, 0f, 0f);
		else
			return new Color(0f, 0.7f, 0f);
	}
	
	public static void drawLight(Obj3DPart part)
	{
		if(part == null) return;
		disableLight();
		enableBlend();
		
		part.draw();
	
		enableLight();
		disableBlend();
		
	}
	
	public static void drawLightNoBind(Obj3DPart part) {

		if(part == null) return;
		disableLight();
		enableBlend();
		
		part.drawNoBind();
	
		enableLight();
		disableBlend();		
	}
	public static void drawGuiBackground(ResourceLocation ressource,GuiScreen guiScreen,int xSize,int ySize) {
		bindTexture(ressource);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int x = (guiScreen.width - xSize) / 2;
		int y = (guiScreen.height - ySize) / 2;
		guiScreen.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
	}

	public static void drawLight(Obj3DPart part,float angle,float x,float y,float z)
	{
		if(part == null) return;
		disableLight();
		enableBlend();
		
		part.draw(angle, x, y, z);

		enableLight();
		disableBlend();
		
	}
	public static void glDefaultColor() {
		GL11.glColor4f(1f,1f, 1f, 1f);
	}
	
	
	static public void drawEntityItem(EntityItem entityItem,double x, double y , double z,float roty,float scale)
	{
		if(entityItem == null) return;
		


		entityItem.hoverStart = 0.0f;
		entityItem.rotationYaw = 0.0f;
		entityItem.motionX = 0.0;
		entityItem.motionY = 0.0;
		entityItem.motionZ =0.0;
		
		Render var10 = null;
		var10 = RenderManager.instance.getEntityRenderObject(entityItem);
		GL11.glPushMatrix();
			GL11.glTranslatef((float)x, (float)y, (float)z);
			GL11.glRotatef(roty, 0, 1, 0);
			GL11.glScalef(scale, scale, scale);
			var10.doRender(entityItem,0, 0, 0, 0, 0);	
		GL11.glPopMatrix();	
		

	}
	
	protected static RenderItem itemRendererr;
	
	static RenderItem getItemRender(){
		if(itemRendererr == null){
			itemRendererr = new RenderItem();
		}
		
		return itemRendererr;
	}
	
	static Minecraft mc()
	{
		return Minecraft.getMinecraft();
	}
	public static void guiScale() {
		GL11.glScalef(16f, 16f, 1f);
		
	}
    public static void drawItemStack(ItemStack par1ItemStack, int x, int y, String par4Str,boolean gui)
    {
    	RenderItem itemRenderer = getItemRender();
    	
       // GL11.glTranslatef(0.0F, 0.0F, 32.0F);
       
        itemRenderer.zLevel = 400.0F;
        FontRenderer font = null;
        if (par1ItemStack != null) font = par1ItemStack.getItem().getFontRenderer(par1ItemStack);
        if (font == null) font = mc().fontRenderer;
        itemRenderer.renderItemAndEffectIntoGUI(font, mc().getTextureManager(), par1ItemStack, x, y);
        itemRenderer.renderItemOverlayIntoGUI(font, mc().getTextureManager(), par1ItemStack, x, y, par4Str);
        
        itemRenderer.zLevel = 0.0F;
        
        if(gui){
            GL11.glDisable(GL11.GL_LIGHTING);
        }
    }

	public static double clientDistanceTo(Entity e) {
		if(e == null) return 100000000.0;
		Entity c = Minecraft.getMinecraft().thePlayer;
		double x = (c.posX-e.posX),y = (c.posY-e.posY),z = (c.posZ-e.posZ);
		return Math.sqrt(x*x+y*y+z*z);
	}

	public static void disableDepthTest() {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
	}

	public static void enableDepthTest() {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		
	}


}
