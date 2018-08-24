package netherclack.client.texture.misc;

import java.util.List;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import netherclack.client.texture.ClientLoader;

public interface ITexturedItem
{
	public List<Integer> getHandledMeta();
	
	@SideOnly(Side.CLIENT)
	public void registerTextures(ClientLoader client);
	
	@SideOnly(Side.CLIENT)
	public TextureAtlasSprite getTexture(int meta);
}
