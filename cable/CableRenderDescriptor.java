package mods.eln.cable;

public class CableRenderDescriptor {
	float width,height,widthDiv2;
	float widthPixel,heightPixel;
	public CableRenderDescriptor(String cableTexture,float width,float height)
	{
		this.widthPixel =  width;
		this.heightPixel =  height;
		this.width = width/16;
		this.height= height/16;
		this.widthDiv2 = width/16 / 2;
		this.cableTexture = cableTexture;
	}
	
	public String cableTexture;
}
