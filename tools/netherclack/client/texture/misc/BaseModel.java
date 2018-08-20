package netherclack.client.texture.misc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.common.model.TRSRTransformation;
import netherclack.client.texture.ClientLoader;

import org.lwjgl.util.vector.Vector3f;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public abstract class BaseModel implements IPerspectiveAwareModel
{
	static final FaceBakery bake = new FaceBakery();
	public static final Vec3Helper[][] faces = new Vec3Helper[][]{
		{new Vec3Helper(false, false, false), new Vec3Helper(true, false, true)},//Down
		{new Vec3Helper(false, true, false), new Vec3Helper(true, true, true)},//Up
		{new Vec3Helper(false, false, false), new Vec3Helper(true, true, false)},//North
		{new Vec3Helper(false, false, true), new Vec3Helper(true, true, true)},//South
		{new Vec3Helper(false, false, false), new Vec3Helper(true, true, true)},//East
		{new Vec3Helper(false, false, false), new Vec3Helper(true, true, true)},//West
	}; //Helper for Bounding Box to Baked-Quads. Simply converts a number from 0-1 to 0-16 pixel
	
	ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> camera;
	public static List<BakedQuad> EMPTYLIST = ImmutableList.of();
	
	public ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> getCamera()
	{
		return camera;
	}
	
	public FaceBakery getBakery()
	{
		return bake;
	}
	
	public void setCamera(ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> cam)
	{
		camera = cam;
	}
	
	public void init(ClientLoader loader)
	{
		
	}

	@Override
	@Deprecated
	public ItemCameraTransforms getItemCameraTransforms()
	{
		return ItemCameraTransforms.DEFAULT;
	}
	
	@Override
	public ItemOverrideList getOverrides()
	{
		return ItemOverrideList.NONE;
	}
	
	public List<BakedQuad>[] createList(int count)
	{
		List<BakedQuad>[] quads = new List[count];
		for(int i = 0;i<count;i++)
		{
			quads[i] = new ArrayList<BakedQuad>();
		}
		return quads;
	}
	
	public static class Vec3Helper
	{
		boolean x;
		boolean y;
		boolean z;
		
		public Vec3Helper(boolean par1, boolean par2, boolean par3)
		{
			x = par1;
			y = par2;
			z = par3;
		}
		
		public Vector3f applyBoundingBox(AxisAlignedBB par1)
		{
			float xScale = (float)(x ? par1.maxX : par1.minX);
			float yScale = (float)(y ? par1.maxY : par1.minY);
			float zScale = (float)(z ? par1.maxZ : par1.minZ);
			return new Vector3f(16f * xScale, 16f * yScale, 16f * zScale);
		}
	}
}
