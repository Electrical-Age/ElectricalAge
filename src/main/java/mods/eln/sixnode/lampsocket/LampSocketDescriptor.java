package mods.eln.sixnode.lampsocket;

import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class LampSocketDescriptor extends SixNodeDescriptor {

	public LampSocketType socketType;
	LampSocketObjRender render;

    boolean useIconEnable = false;

    public boolean cameraOpt = true;

    public int range;
    public String modelName;
    float alphaZMin, alphaZMax, alphaZBoot;
    public boolean cableFront = true;
    public boolean cableLeft = true;
    public boolean cableRight = true;
    public boolean cableBack = true;

	public LampSocketDescriptor(String name, LampSocketObjRender render,
								LampSocketType socketType,
								int range,
								float alphaZMin, float alphaZMax,
								float alphaZBoot) {
		super(name, LampSocketElement.class, LampSocketRender.class);
		this.socketType = socketType;
		this.range = range;
		this.alphaZMin = alphaZMin;
		this.alphaZMax = alphaZMax;
		this.alphaZBoot = alphaZBoot;
		this.render = render;
	}

	public void useIcon(boolean enable) {
		useIconEnable = enable;
	}
	
	boolean noCameraOpt() {
		return cameraOpt;
	}

	public void setParent(net.minecraft.item.Item item, int damage) {
		super.setParent(item, damage);
		Data.addLight(newItemStack());
	}

	@Override
	public boolean use2DIcon() {
		return useIconEnable;
	}

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return !useIconEnable;
	}

	@Override
	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return !useIconEnable;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if (useIconEnable)
			super.renderItem(type, item, data);
		else
			render.draw(this, type);
	}

	@Override
	public boolean hasVolume() {
		return hasGhostGroup();
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		//list.add("Socket Type : " + socketType.toString());
		
		if (range != 0 || alphaZMin != alphaZMax) {
			//list.add("Projector");
			if (range != 0){
				list.add(tr("Spot range: %1$ blocks", range));
			}
			if (alphaZMin != alphaZMax) {
				list.add(tr("Angle: %1$° to %2$°", ((int) alphaZMin), ((int) alphaZMax)));
			}
		}
	}
}
