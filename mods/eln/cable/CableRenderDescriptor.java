package mods.eln.cable;

import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import net.minecraft.util.ResourceLocation;

public class CableRenderDescriptor {
	
	float width,height, widthDiv2;
	float widthPixel, heightPixel;
	
	public CableRenderDescriptor(String modName, String cableTexture, float width, float height) {
		this.widthPixel = width;
		this.heightPixel = height;
		this.width = width / 16;
		this.height= height / 16;
		this.widthDiv2 = width / 16 / 2;
		this.cableTexture = new ResourceLocation(modName, cableTexture);
	}
	
	public ResourceLocation cableTexture;
	
	public void bindCableTexture() {
		UtilsClient.bindTexture(cableTexture);
	}
}
