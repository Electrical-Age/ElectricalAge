package mods.eln.misc;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;

import mods.eln.Eln;
import mods.eln.GuiHandler;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;

public class UtilsClient {

	public static float distanceFromClientPlayer(World world, int xCoord, int yCoord, int zCoord) {
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

		return (float) Math.sqrt((xCoord - player.posX) * (xCoord - player.posX)
				+ (yCoord - player.posY) * (yCoord - player.posY)
				+ (zCoord - player.posZ) * (zCoord - player.posZ));
	}

	public static float distanceFromClientPlayer(SixNodeEntity tileEntity) {
		return distanceFromClientPlayer(tileEntity.getWorldObj(), tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
	}

	public static EntityClientPlayerMP getClientPlayer() {

		return Minecraft.getMinecraft().thePlayer;
	}

	public static void drawHaloNoLightSetup(Obj3DPart halo, float r, float g, float b, World w, int x, int y, int z, boolean bilinear) {
		if (halo == null)
			return;
		if (bilinear)
			enableBilinear();
		int light = getLight(w, x, y, z) * 19 / 15 - 4;
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
		drawHaloNoLightSetup(halo, r, g, b, e.getWorldObj(), e.xCoord, e.yCoord, e.zCoord, bilinear);
	}

	public static void drawHalo(Obj3DPart halo, float r, float g, float b, TileEntity e, boolean bilinear) {
		drawHalo(halo, r, g, b, e.getWorldObj(), e.xCoord, e.yCoord, e.zCoord, bilinear);
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
		int light = getLight(e.worldObj, MathHelper.floor_double(e.posX), MathHelper.floor_double(e.posY), MathHelper.floor_double(e.posZ));
		// light =
		// e.worldObj.getLightBrightnessForSkyBlocks(MathHelper.floor_double(e.posX),
		// MathHelper.floor_double(e.posY), MathHelper.floor_double(e.posZ),0);
		// Utils.println(light);
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

		GL11.glDisable(GL11.GL_TEXTURE_2D);
	}

	public static void enableTexture() {

		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	public static void disableLight() {
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		lightmapTexUnitTextureEnable = GL11.glGetBoolean(GL11.GL_TEXTURE_2D);
		if (lightmapTexUnitTextureEnable)
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL11.glDisable(GL11.GL_LIGHTING);
	}

	public static void enableLight() {
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		if (lightmapTexUnitTextureEnable)
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	public static void enableBlend() {
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		// GL11.glDepthMask(true);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0.02f);
		// GL11.glDisable(GL11.GL_ALPHA_TEST);
		/*
		 * Utils.println(GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB) + " " + GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA) + " " + GL11.glGetInteger(GL14.GL_BLEND_DST_RGB) + " " + GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA) + " " + GL11.glIsEnabled(GL11.GL_BLEND));
		 */

		// Utils.println(GL11.glGetInteger(GL11.GL_BLEND_SRC) + " " + GL11.glGetInteger(GL11.GL_BLEND_DST) + " " + GL11.glIsEnabled(GL11.GL_BLEND));
		/*
		 * GL11.glEnable(2977); GL11.glEnable(3042);
		 */
		// OpenGlHelper.glBlendFunc(770, 770, 771, 771);
	}

	public static void disableBlend() {
		GL11.glDisable(GL11.GL_BLEND);

		// GL11.glDepthMask(true);
		// GL11.glEnable(GL11.GL_ALPHA_TEST);
		// GL11.glDisable(GL11.GL_BLEND);
		// GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		// Utils.println(GL11.glGetInteger(GL11.GL_BLEND_SRC) + " " + GL11.glGetInteger(GL11.GL_BLEND_DST) + " " + GL11.glIsEnabled(GL11.GL_BLEND));
		// GL11.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
		// GL11.glBlendFunc(1, 1);
		// GL11.glDisable(3042);

		// OpenGlHelper.glBlendFunc(1, 1, 1, 1);
	}

	public static void drawIcon(ItemRenderType type) {
		enableBlend();
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
		disableBlend();
	}

	public static void drawIcon(ItemRenderType type, String icon) {
		bindTextureByName(icon);
		drawIcon(type);
	}

	public static void drawIcon(ItemRenderType type, ResourceLocation icon) {
		bindTexture(icon);
		drawIcon(type);
	}

	/*
	 * public static void drawIcon(ItemRenderType type, Icon icon) { drawIcon(type, icon.getIconName()); }
	 */

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

	public static void ledOnOffColor(boolean on)
	{
		if (!on)
			GL11.glColor3f(0.7f, 0f, 0f);
		else
			GL11.glColor3f(0f, 0.7f, 0f);
	}

	public static Color ledOnOffColorC(boolean on)
	{
		if (!on)
			return new Color(0.7f, 0f, 0f);
		else
			return new Color(0f, 0.7f, 0f);
	}

	public static void drawLight(Obj3DPart part)
	{
		if (part == null)
			return;
		disableLight();
		enableBlend();

		part.draw();

		enableLight();
		disableBlend();

	}

	public static void drawLightNoBind(Obj3DPart part) {

		if (part == null)
			return;
		disableLight();
		enableBlend();

		part.drawNoBind();

		enableLight();
		disableBlend();
	}

	public static void drawGuiBackground(ResourceLocation ressource, GuiScreen guiScreen, int xSize, int ySize) {
		bindTexture(ressource);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int x = (guiScreen.width - xSize) / 2;
		int y = (guiScreen.height - ySize) / 2;
		guiScreen.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
	}

	public static void drawLight(Obj3DPart part, float angle, float x, float y, float z)
	{
		if (part == null)
			return;
		disableLight();
		enableBlend();

		part.draw(angle, x, y, z);

		enableLight();
		disableBlend();

	}

	public static void glDefaultColor() {
		GL11.glColor4f(1f, 1f, 1f, 1f);
	}

	static public void drawEntityItem(EntityItem entityItem, double x, double y, double z, float roty, float scale)
	{
		if (entityItem == null)
			return;

		entityItem.hoverStart = 0.0f;
		entityItem.rotationYaw = 0.0f;
		entityItem.motionX = 0.0;
		entityItem.motionY = 0.0;
		entityItem.motionZ = 0.0;

		Render var10 = null;
		var10 = RenderManager.instance.getEntityRenderObject(entityItem);
		GL11.glPushMatrix();
		GL11.glTranslatef((float) x, (float) y, (float) z);
		GL11.glRotatef(roty, 0, 1, 0);
		GL11.glScalef(scale, scale, scale);
		var10.doRender(entityItem, 0, 0, 0, 0, 0);
		GL11.glPopMatrix();

	}

	static public void drawConnectionPinSixNode(float d, float w, float h) {

		d += 0.1f;
		d *= 0.0625f;
		w *= 0.0625f;
		h *= 0.0625f;
		float w2 = w * 0.5f;
		disableTexture();
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex3f(-w2, d, 0);
		GL11.glVertex3f(w2, d, 0);
		GL11.glVertex3f(w2, d, h);
		GL11.glVertex3f(-w2, d, h);
		GL11.glEnd();
		enableTexture();

	}

	static public void drawConnectionPinSixNode(LRDU front, float[] dList, float w, float h) {
		// front.glRotateOnX();
		// drawConnectionPinSixNode(d[front.toInt()], w, h);
		float d = dList[front.toInt()];
		d += 0.04f;
		d *= 0.0625f;
		w *= 0.0625f;
		h *= 0.0625f;
		float w2 = w * 0.5f;
		disableTexture();
		GL11.glBegin(GL11.GL_QUADS);

		switch (front) {
		case Left:
			GL11.glVertex3f(0, -w2, -d);
			GL11.glVertex3f(0, w2, -d);
			GL11.glVertex3f(h, w2, -d);
			GL11.glVertex3f(h, -w2, -d);
			break;
		case Right:
			GL11.glVertex3f(h, -w2, d);
			GL11.glVertex3f(h, w2, d);
			GL11.glVertex3f(0, w2, d);
			GL11.glVertex3f(0, -w2, d);
			break;
		case Down:
			GL11.glVertex3f(h, -d, -w2);
			GL11.glVertex3f(h, -d, w2);
			GL11.glVertex3f(0, -d, w2);
			GL11.glVertex3f(0, -d, -w2);
			break;
		case Up:
			GL11.glVertex3f(0, d, -w2);
			GL11.glVertex3f(0, d, w2);
			GL11.glVertex3f(h, d, w2);
			GL11.glVertex3f(h, d, -w2);
			break;

		default:
			break;
		}

		GL11.glEnd();
		enableTexture();

	}

	protected static RenderItem itemRendererr;

	static RenderItem getItemRender() {
		if (itemRendererr == null) {
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

	public static void drawItemStack(ItemStack par1ItemStack, int x, int y, String par4Str, boolean gui)
	{
		// Block b = Block.getBlockFromItem(par1ItemStack.getItem());
		// b.rend
		// ForgeHooksClient.renderInventoryItem(new RenderBlocks(),Minecraft.getMinecraft().getTextureManager(),par1ItemStack,false,0,x,y);
		// ForgeHooksClient.renderInventoryItem(Minecraft.getMinecraft().bl, engine, item, inColor, zLevel, x, y)

		RenderItem itemRenderer = getItemRender();
		// GL11.glDisable(3042);
		if (gui) {
			GL11.glEnable(32826);
			RenderHelper.enableGUIStandardItemLighting();
		}
		// GL11.glTranslatef(0.0F, 0.0F, 32.0F);
		// ForgeHooksClient.renderInventoryItem(new RenderBlocks(),Minecraft.getMinecraft().getTextureManager(),par1ItemStack,false,0,x,y);
		itemRenderer.zLevel = 400.0F;
		// ForgeHooksClient.renderInventoryItem(renderBlocks, engine, item, inColor, zLevel, x, y)
		FontRenderer font = null;
		if (par1ItemStack != null) {
			Item i = par1ItemStack.getItem();
			if (i == null)
				return;
			font = i.getFontRenderer(par1ItemStack);
		}
		if (font == null)
			font = mc().fontRenderer;
		itemRenderer.renderItemAndEffectIntoGUI(font, mc().getTextureManager(), par1ItemStack, x, y);
		// itemRenderer.renderItemOverlayIntoGUI(font, mc().getTextureManager(), par1ItemStack, x, y, par4Str);

		itemRenderer.zLevel = 0.0F;

		if (gui) {
			RenderHelper.disableStandardItemLighting();
			GL11.glDisable(32826);
		}

		if (par1ItemStack.stackSize > 1) {
			disableDepthTest();
			// GL11.glPushMatrix();
			// GL
			// GL11.glScalef(0.5f, 0.5f, 0.5f);
			Minecraft.getMinecraft().fontRenderer.drawStringWithShadow("" + par1ItemStack.stackSize, x + 10, y + 9, 0xFFFFFFFF);
			// GL11.glPopMatrix();
			enableDepthTest();
		}
	}

	public static double clientDistanceTo(Entity e) {
		if (e == null)
			return 100000000.0;
		Entity c = Minecraft.getMinecraft().thePlayer;
		double x = (c.posX - e.posX), y = (c.posY - e.posY), z = (c.posZ - e.posZ);
		return Math.sqrt(x * x + y * y + z * z);
	}

	public static double clientDistanceTo(TransparentNodeEntity t) {
		if (t == null)
			return 100000000.0;
		Entity c = Minecraft.getMinecraft().thePlayer;
		double x = (c.posX - t.xCoord), y = (c.posY - t.yCoord), z = (c.posZ - t.zCoord);
		return Math.sqrt(x * x + y * y + z * z);
	}

	public static int getLight(World w, int x, int y, int z) {
		int b = w.getSkyBlockTypeBrightness(EnumSkyBlock.Block, x, y, z);
		int s = w.getSkyBlockTypeBrightness(EnumSkyBlock.Sky, x, y, z) - w.calculateSkylightSubtracted(0f);
		return Math.max(b, s);
	}

	public static void disableDepthTest() {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
	}

	public static void enableDepthTest() {
		GL11.glEnable(GL11.GL_DEPTH_TEST);

	}

	public static void sendPacketToServer(ByteArrayOutputStream bos)
	{
		C17PacketCustomPayload packet = new C17PacketCustomPayload(Eln.channelName, bos.toByteArray());
		Eln.eventChannel.sendToServer(new FMLProxyPacket(packet));
		// Minecraft.getMinecraft().thePlayer.sendQueue.addToSendQueue(new FMLProxyPacket(packet));
	}

	private static int uuid = Integer.MIN_VALUE;

	public static int getUuid() {
		if (uuid > -1)
			uuid = Integer.MIN_VALUE;
		return uuid++;
	}

	static HashSet<Integer> glListsAllocated = new HashSet<Integer>();

	public static int glGenListsSafe() {
		int id = GL11.glGenLists(1);
		glListsAllocated.add(id);
		return id;
	}

	public static void glDeleteListsSafe(int id) {
		glListsAllocated.remove(id);
		GL11.glDeleteLists(id, 1);
	}

	public static void glDeleteListsAllSafe() {
		for (Integer id : glListsAllocated) {
			GL11.glDeleteLists(id, 1);
		}
		glListsAllocated.clear();
	}

	public static void showItemTooltip(List src,List dst) {
		if(src.size() == 0) return;
		if(showItemTooltip()){
			dst.addAll(src);
		}else{
			dst.add("\u00a7F\u00a7o<hold shift>");
		}
	}
	
	public static boolean showItemTooltip() {
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}


}
