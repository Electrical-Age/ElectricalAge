package mods.eln.ore;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.ArrayList;
import java.util.Random;

public class OreDescriptor extends GenericItemBlockUsingDamageDescriptor implements IWorldGenerator {

    int metadata;

    int spawnRate, spawnSizeMax, spawnSizeMin, spawnHeightMin, spawnHeightMax;

    public OreDescriptor(String name, int metadata,
                         int spawnRate, int spawnSizeMin, int spawnSizeMax, int spawnHeightMin, int spawnHeightMax) {
        super(name);
        this.metadata = metadata;
        this.spawnHeightMax = spawnHeightMax;
        this.spawnHeightMin = spawnHeightMin;
        this.spawnRate = spawnRate;
        this.spawnSizeMin = spawnSizeMin;
        this.spawnSizeMax = spawnSizeMax;
    }

    // TODO(1.10): Fix item rendering.
//    public IIcon getBlockIconId(int side, int damage) {
//        return getIcon();
//    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addOre(newItemStack());
    }

    public ArrayList<ItemStack> getBlockDropped(int fortune) {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        list.add(new ItemStack(Eln.oreItem, 1, metadata));
        return list;
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.isSurfaceWorld()) {
            generateSurface(random, chunkX * 16, chunkZ * 16, world); //This makes it gen overworld (the *16 is important)
        }
    }

    public void generateSurface(Random random, int x, int z, World w) {
        if (w.getWorldInfo().getTerrainType() == WorldType.FLAT) return;
        //for(int i = 0;i<4;i++){ //This goes through the ore metadata
        for (int ii = 0; ii < spawnRate; ii++) { //This makes it gen multiple times in each chunk
            int posX = x + random.nextInt(16); //X coordinate to gen at
            int posY = spawnHeightMin + random.nextInt(spawnHeightMax - spawnHeightMin); //Y coordinate less than 40 to gen at
            int posZ = z + random.nextInt(16); //Z coordinate to gen at
            int size = spawnSizeMin + random.nextInt(spawnSizeMax - spawnSizeMin);
            new WorldGenMinable(Eln.oreBlock, metadata, size, Blocks.stone).generate(w, random, posX, posY, posZ); //The gen call
        }
        //}
        //new WorldGenTrees(par1, par2, par3, par4, par5)
    }
}
