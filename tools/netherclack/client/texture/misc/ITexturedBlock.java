package netherclack.client.texture.misc;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import netherclack.client.texture.ClientLoader;

public interface ITexturedBlock
{
	public List<IBlockState> getHandledStates();
	
	@SideOnly(Side.CLIENT)
	public void registerTextures(ClientLoader loader);
	
	@SideOnly(Side.CLIENT)
	public TextureAtlasSprite getTexture(IBlockState state, EnumFacing side, ClientLoader loader);
	
	@SideOnly(Side.CLIENT)
	public TextureAtlasSprite getParticleTexture(IBlockState state, ClientLoader loader);
	
	public AxisAlignedBB getRenderBoundingBox(IBlockState state);
	
	
}
