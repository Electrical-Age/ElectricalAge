package netherclack.client.texture.misc;

import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import netherclack.client.texture.ClientLoader;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.vector.Vector3f;

/**
 * 
 * @author Speiger
 * 
 * Basic model class which Pre-generates the Baked Quads.
 * If a Quad side is empty then its just showing an empty list
 * I have it done before with a Hash-map but since a array is way faster this shouldn't be a problem
 * Its validating if the block is Opaque and then decides the renderer
 */
public class BasicBlockModel extends BaseModel
{
	boolean full;
	List<BakedQuad>[] quads = createList(7);
	ITexturedBlock textured;
	IBlockState reference;
	TextureAtlasSprite partical;
	
	public BasicBlockModel(ITexturedBlock block, IBlockState state)
	{
		textured = block;
		reference = state;
	}
	
	@Override
	public void init(ClientLoader loader)
	{
		Block block = (Block)textured;
		full = block.isOpaqueCube(reference);
		partical = loader.getIconSafe(textured.getParticleTexture(reference, loader));
		AxisAlignedBB box = textured.getRenderBoundingBox(reference);
		if(full)
		{
			BlockPartFace face = new BlockPartFace(null, -1, "", new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0));
			for(EnumFacing side : EnumFacing.VALUES)
			{
				quads[6].add(getBakery().makeBakedQuad(new Vector3f(0F, 0F, 0F), new Vector3f(16F, 16F, 16F), face, loader.getIconSafe(textured.getTexture(reference, side, loader)), side, ModelRotation.X0_Y0, null, true, true));
			}
		}
		else
		{
			for(EnumFacing side : EnumFacing.VALUES)
			{
				Vector3f min = faces[side.getIndex()][0].applyBoundingBox(box); //I know this needs to changed
				Vector3f max = faces[side.getIndex()][1].applyBoundingBox(box);
				BlockPartFace face = new BlockPartFace(null, -1, "", new BlockFaceUV(new float[]{0F, 0F, 16F, 16F}, 0));
				quads[side.getIndex()].add(getBakery().makeBakedQuad(min, max, face, loader.getIconSafe(textured.getTexture(reference, side, loader)), side, ModelRotation.X0_Y0, null, true, true));
			}
		}
	}
	
	@Override
	public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand)
	{
		return quads[side == null ? 6 : side.getIndex()];
	}

	@Override
	public boolean isAmbientOcclusion()
	{
		return true;
	}

	@Override
	public boolean isGui3d()
	{
		return true;
	}

	@Override
	public boolean isBuiltInRenderer()
	{
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleTexture()
	{
		return partical;
	}
	
	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType)
	{
		return Pair.of(this, IPerspectiveAwareModel.MapWrapper.handlePerspective(this, camera, cameraTransformType).getRight());
	}
}
