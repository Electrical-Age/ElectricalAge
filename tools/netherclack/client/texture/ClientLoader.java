package netherclack.client.texture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import netherclack.client.texture.misc.BaseModel;
import netherclack.client.texture.misc.BasicBlockModel;
import netherclack.client.texture.misc.BasicItemModel;
import netherclack.client.texture.misc.IAdvancedTexturedBlock;
import netherclack.client.texture.misc.ITexturedBlock;
import netherclack.client.texture.misc.ITexturedItem;

import org.apache.commons.codec.Charsets;

import com.google.common.collect.ImmutableMap;

/**
 * 
 * @author Speiger
 * 
 * Texture storage & Loader etc.
 * Since we are using a Json Destroyer to load the models we need a separate texture loader
 */
@SideOnly(Side.CLIENT)
public class ClientLoader
{
	public static ClientLoader instance = new ClientLoader();
	
	private Minecraft minecraft = Minecraft.getMinecraft();
	
	private Map<String, TextureAtlasSprite[]> textures = new LinkedHashMap<String, TextureAtlasSprite[]>();
	private Map<String, ResourceLocation[]> texturePaths = new LinkedHashMap<String, ResourceLocation[]>();
	
	private Map<Block, Map<IBlockState, ModelResourceLocation>> states = new HashMap<Block, Map<IBlockState, ModelResourceLocation>>();
	private IStateMapper mapper = new StateHelper(this);
	private ItemMeshDefinition blockMesher = new BlockMeshHelper(this);
	
	private Map<Item, Map<Integer, ModelResourceLocation>> itemStates = new HashMap<Item, Map<Integer, ModelResourceLocation>>();
	private ItemMeshDefinition definetion = new MeshHelper(this);
	
	/**
	 * Function just add it to forge. thats simply it.
	 * Static would work but i still would have to call the class
	 * So i add a function and then i can control it
	 */
	public void addToForge()
	{
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	/**
	 * Function to say the storage: Please provide so many slots for me
	 * @param id the textureID
	 * @param size how many slots should be provided
	 */
	public void provideSize(String id, int size)
	{
		if(texturePaths.containsKey(id))
		{
			return;
		}
		texturePaths.put(id, new ResourceLocation[size]);
		textures.put(id, new TextureAtlasSprite[size]);
	}
	
	/**
	 * Function to register a Texture to a slot or overriding a texture for a slot
	 * @param id of the texture map
	 * @param slot the slot where you want to put it
	 * @param texture the texture you want to load it
	 */
	public void registerTexture(String id, int slot, ResourceLocation texture)
	{
		texturePaths.get(id)[slot] = texture;
	}
	
	/**
	 * Bulk Texture registration if you know how many textures you want to load
	 * @param id the id of the textures
	 * @param textureList the textures you want to load
	 */
	public void registerTextureMap(String id, ResourceLocation... textureList)
	{
		texturePaths.put(id, textureList);
		textures.put(id, new TextureAtlasSprite[textureList.length]);
	}
	
	/**
	 * Functions to get the textures for a id
	 * @param id the id you provide
	 * @return the textures if they are there. can be null
	 */
	public TextureAtlasSprite[] getTextures(String id)
	{
		return textures.get(id);
	}
	
	/**
	 * Function to access a texture from a array
	 * @param id the id you provide
	 * @param slot the slot which you want
	 * @return the texture that you will get. can be null
	 */
	public TextureAtlasSprite getTexture(String id, int slot)
	{
		return textures.get(id)[slot];
	}
	
	public TextureAtlasSprite getIconSafe(TextureAtlasSprite sprite)
	{
		if(sprite == null)
		{
			sprite = minecraft.getTextureMapBlocks().getMissingSprite();
		}
		return sprite;
	}
	
	/**
	 * Function to load the textures as paths
	 * Supports resets
	 */
	private void init()
	{
		texturePaths.clear();
		textures.clear();
		for(Block block : Block.REGISTRY)
		{
			if(block instanceof ITexturedBlock)
			{
				((ITexturedBlock)block).registerTextures(this);
			}
		}
		for(Item item : Item.REGISTRY)
		{
			if(item instanceof ITexturedItem)
			{
				((ITexturedItem)item).registerTextures(this);
			}
		}
	}
	
	/**
	 * Function to load the textures.
	 * Since its only for this Mod i shouldn't do so many safety checks.
	 * this is just for a future proof system...
	 * But yeah this function reloads the items/blocks so they get changes applied to
	 * and also it reloads the textures
	 */
	@SubscribeEvent
	public void onTextureLoad(TextureStitchEvent.Pre event)
	{
		init();
		if(texturePaths.isEmpty())
		{
			return;
		}
		TextureMap map = event.getMap();
		for(Entry<String, ResourceLocation[]> entry : texturePaths.entrySet())
		{
			ResourceLocation[] value = entry.getValue();
			if(value == null)
			{
				continue;
			}
			TextureAtlasSprite[] array = textures.get(entry.getKey());
			if(array == null)
			{
				array = new TextureAtlasSprite[value.length];
				textures.put(entry.getKey(), array);
			}
			for(int i = 0;i<value.length;i++)
			{
				ResourceLocation location = value[i];
				if(location == null)
				{
					continue;
				}
				array[i] = map.registerSprite(location);
			}
			textures.put(entry.getKey(), array);
		}
	}
	
	@SubscribeEvent
	public void onModelReload(ModelBakeEvent event)
	{
		states.clear();
		itemStates.clear();
		handleModels(event);
	}
	
	private void handleModels(ModelBakeEvent event)
	{
		ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> itemMap = getMap(new ResourceLocation("minecraft:models/item/generated"));
		ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> blockMap = getMap(new ResourceLocation("minecraft:models/block/block"));
		IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();
		for(Block block : Block.REGISTRY)
		{
			Map<IBlockState, ModelResourceLocation> resources = new HashMap<IBlockState, ModelResourceLocation>();
			if(block instanceof ITexturedBlock)
			{
				ITexturedBlock texture = (ITexturedBlock)block;
				boolean adv = texture instanceof IAdvancedTexturedBlock;
				for(IBlockState state : texture.getHandledStates())
				{
					BaseModel model = new BasicBlockModel(texture, state);
					model.setCamera(blockMap);
					model.init(this);
					ModelResourceLocation modelResourceLocation = getModelResourceLocation(state);
					resources.put(state, modelResourceLocation);
					registry.putObject(modelResourceLocation, model);
				}
			}
			if(resources.size() > 0)
			{
				states.put(block, resources);
				ModelLoader.setCustomStateMapper(block, mapper);
				ModelLoader.setCustomMeshDefinition(Item.getItemFromBlock(block), blockMesher);
				registry.putObject(getBlockinventoryResourceLocation(block), registry.getObject(resources.values().iterator().next()));
			}
		}
		for(Item item : Item.REGISTRY)
		{
			Map<Integer, ModelResourceLocation> resources = new HashMap<Integer, ModelResourceLocation>();
			if(item instanceof ITexturedItem)
			{
				ITexturedItem textured = (ITexturedItem)item;
				for(int meta : textured.getHandledMeta())
				{
					ItemStack stack = new ItemStack(item, 1, meta);
					BaseModel model = new BasicItemModel(textured, stack);
					model.setCamera(itemMap);
					model.init(this);
					ModelResourceLocation modelResourceLocation = getItemInventoryResourceLocation(stack);
					ModelBakery.registerItemVariants(item, modelResourceLocation);	
					registry.putObject(modelResourceLocation, model);
					resources.put(meta, modelResourceLocation);
				}
			}
			if(resources.size() > 0)
			{
				ModelLoader.setCustomMeshDefinition(item, definetion);
				itemStates.put(item, resources);
				registry.putObject(getItemInventoryResourceLocation(item), registry.getObject(resources.values().iterator().next()));
			}
		}
	}
	
	public static ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> getMap(ResourceLocation par1)
	{
		return IPerspectiveAwareModel.MapWrapper.getTransforms(getTransformFromJson(par1));
	}
	
	private static ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> getMap(ItemCameraTransforms par1)
	{
		return IPerspectiveAwareModel.MapWrapper.getTransforms(par1);
	}
	
	private static ItemCameraTransforms getTransformFromJson(ResourceLocation par1)
	{
		try
		{
			return ModelBlock.deserialize(getReaderForResource(par1)).getAllTransforms();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return ItemCameraTransforms.DEFAULT;
	}
	
	private static Reader getReaderForResource(ResourceLocation location) throws IOException
	{
		ResourceLocation file = new ResourceLocation(location.getResourceDomain(), location.getResourcePath() + ".json");
		IResource iresource = Minecraft.getMinecraft().getResourceManager().getResource(file);
		return new BufferedReader(new InputStreamReader(iresource.getInputStream(), Charsets.UTF_8));
	}
	
	/**
	 * These functions are simple Item/Block & meta versions to ModelResourceLocations
	 * It helps a lot saves a lot of code lines and does its job.
	 * It supports also addon
	 */
	
	public static ModelResourceLocation getModelResourceLocation(IBlockState state)
	{
		return new ModelResourceLocation(Block.REGISTRY.getNameForObject(state.getBlock()), (new DefaultStateMapper()).getPropertyString(state.getProperties()));
	}
	
	public static ModelResourceLocation getBlockinventoryResourceLocation(Block block)
	{
		return new ModelResourceLocation(Block.REGISTRY.getNameForObject(block), "inventory");
	}
	
	public static ModelResourceLocation getItemInventoryResourceLocation(ItemStack item)
	{
		String name = item.getUnlocalizedName();
		ResourceLocation resource = Item.REGISTRY.getNameForObject(item.getItem());
		return new ModelResourceLocation(resource.getResourceDomain() + name.substring(name.indexOf(".") + 1), "inventory");
	}
	
	public static ModelResourceLocation getItemInventoryResourceLocation(Item item)
	{
		return new ModelResourceLocation(Item.REGISTRY.getNameForObject(item), "inventory");
	}
	
	/**
	 * 
	 * @author Speiger
	 *
	 * Class to control the BlockStates.
	 * If something is not added here then it does not want to be added
	 * this is the perfect controller for that
	 */
	public static class StateHelper implements IStateMapper
	{
		ClientLoader loader;
		
		public StateHelper(ClientLoader client)
		{
			loader = client;
		}
		
		@Override
		public Map<IBlockState, ModelResourceLocation> putStateModelLocations(Block blockIn)
		{
			Map<IBlockState, ModelResourceLocation> location = loader.states.get(blockIn);
			if(location == null)
			{
				location = new HashMap<IBlockState, ModelResourceLocation>();
				loader.states.put(blockIn, location);
			}
			return location;
		}
	}
	
	public static class BlockMeshHelper implements ItemMeshDefinition
	{
		ClientLoader loader;
		
		public BlockMeshHelper(ClientLoader client)
		{
			loader = client;
		}
		
		@Override
		public ModelResourceLocation getModelLocation(ItemStack stack)
		{
			Block block = Block.getBlockFromItem(stack.getItem());
			if(block instanceof IAdvancedTexturedBlock)
			{
				Map<IBlockState, ModelResourceLocation> loc = loader.states.get(block);
				if(loc != null)
				{
					return loc.get(((IAdvancedTexturedBlock)block).getBlockState(stack));
				}
			}
			return null;
		}
		
	}
	
	public static class MeshHelper implements ItemMeshDefinition
	{
		ClientLoader loader;
		
		public MeshHelper(ClientLoader client)
		{
			loader = client;
		}
		
		@Override
		public ModelResourceLocation getModelLocation(ItemStack stack)
		{
			Item item = stack.getItem();
			Map<Integer, ModelResourceLocation> map = loader.itemStates.get(item);
			if(map == null || map.isEmpty())
			{
				return null;
			}
			int meta = stack.getMetadata();
			if(!item.getHasSubtypes())
			{
				meta = item.getMetadata(meta);
			}
			return map.get(meta);
		}
		
	}
}
