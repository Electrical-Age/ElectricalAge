package netherclack.client.texture.misc;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.ItemLayerModel;
import netherclack.client.texture.ClientLoader;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Optional;

public class BasicItemModel extends BaseModel
{
	List<BakedQuad> quads = new ArrayList<BakedQuad>();
	ITexturedItem textured;
	ItemStack reference;
	TextureAtlasSprite sprite;
	
	BasicItemModel other;
	boolean gui;
	
	
	public BasicItemModel(ITexturedItem item, ItemStack stack)
	{
		textured = item;
		reference = stack;
		gui = false;
	}
	
	public BasicItemModel(BasicItemModel model)
	{
		other = model;
		textured = model.textured;
		reference = model.reference;
		gui = true;
	}
	
	
	@Override
	public void init(ClientLoader loader)
	{
		other = new BasicItemModel(this);
		other.sprite = sprite = loader.getIconSafe(textured.getTexture(reference.getMetadata()));
		quads.addAll(ItemLayerModel.getQuadsForSprite(-1, sprite, DefaultVertexFormats.ITEM, Optional.of(getCamera().get(TransformType.FIXED))));
		for(BakedQuad quad : quads)
		{
			if(quad.getFace() == EnumFacing.SOUTH)
			{
				other.quads.add(quad);
			}
		}
	}
	

	
	@Override
	public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand)
	{
		if(side == null)
		{
			return quads;
		}
		return EMPTYLIST;
	}
	
	@Override
	public boolean isAmbientOcclusion()
	{
		return true;
	}
	
	@Override
	public boolean isGui3d()
	{
		return false;
	}
	
	@Override
	public boolean isBuiltInRenderer()
	{
		return false;
	}
	
	@Override
	public TextureAtlasSprite getParticleTexture()
	{
		return sprite;
	}
	
	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type)
	{
		Pair<? extends IBakedModel, Matrix4f> pair = IPerspectiveAwareModel.MapWrapper.handlePerspective(this, camera, type);
		if(type == TransformType.GUI && !gui && pair.getRight() == null)
		{
			return Pair.of(other, null);
		}
		else if(type != TransformType.GUI && gui)
		{
			return Pair.of(other, pair.getRight());
		}
		return pair;
	}
}
