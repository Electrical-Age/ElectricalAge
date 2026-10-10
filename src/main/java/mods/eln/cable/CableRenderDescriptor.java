package mods.eln.cable;

import mods.eln.misc.UtilsClient;

import net.minecraft.resources.Identifier;

public class CableRenderDescriptor {

    float width, height, widthDiv2;
    float widthPixel, heightPixel;

    public CableRenderDescriptor(String modName, String cableTexture, float width, float height) {
        this.widthPixel = width;
        this.heightPixel = height;
        this.width = width / 16;
        this.height = height / 16;
        this.widthDiv2 = width / 16 / 2;
        this.cableTexture = Identifier.fromNamespaceAndPath(modName, cableTexture.toLowerCase(java.util.Locale.ROOT));
    }

    public Identifier cableTexture;

    public void bindCableTexture() {
        UtilsClient.bindTexture(cableTexture);
    }
}
