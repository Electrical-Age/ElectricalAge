package mods.eln.misc;

import mods.eln.Eln;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The 1.12 Utils, ported a piece at a time (P0: what the simulator needs). With -Deln.deterministic=true (the unit
 * tests), rand returns the midpoint, like the 1.12 test stub did.
 */
public class Utils {
    static final boolean DETERMINISTIC = Boolean.getBoolean("eln.deterministic");

    public static void println(Object str) {
        String s = String.valueOf(str);
        // 1.12 code reports broken invariants as println("ASSERT ..."): make those visible (tools/gt-summary.sh)
        if (s.startsWith("ASSERT")) Eln.LOG.warn(s);
        else Eln.LOG.debug(s);
    }

    public static void print(String format, Object... data) {
        Eln.LOG.debug(String.format(format, data));
    }

    public static void print(Object str) {
        Eln.LOG.debug(String.valueOf(str));
    }

    /** Uniform in [min, max). */
    public static double rand(double min, double max) {
        if (DETERMINISTIC) return (min + max) / 2;
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }

    public static boolean isTheClass(Object o, Class c) {
        for (Class k = o.getClass(); k != null; k = k.getSuperclass()) if (k == c) return true;
        return false;
    }

    public static boolean isEmpty(net.minecraft.world.item.ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    /**
     * Same item, or both in a common tag (1.12: same item + meta, or a shared ore dictionary name). Without damage
     * metadata on 26.x, item identity replaces item + meta.
     */
    public static boolean areSame(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemStack output) {
        if (stack.getItem() == output.getItem()) return true;
        return stack.typeHolder().tags().anyMatch(output::is);
    }

    public static void sendPacketToClient(java.io.ByteArrayOutputStream bos, net.minecraft.server.level.ServerPlayer player) {
        mods.eln.network.ElnNet.sendToPlayer(player, bos);
    }

    public static void sendMessage(net.minecraft.world.entity.player.Player player, String text) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(text));
    }

    public static void notifyNeighbours(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState s = level.getBlockState(pos);
        level.updateNeighborsAt(pos, s.getBlock());
    }

    /** The direction the entity looks along (pitch beyond 45 degrees: down/up). Yaw convention as in 1.12. */
    public static Direction entityLivingViewDirection(net.minecraft.world.entity.LivingEntity e) {
        if (e.getXRot() > 45) return Direction.YN;
        if (e.getXRot() < -45) return Direction.YP;
        return entityLivingHorizontalViewDirection(e);
    }

    public static Direction entityLivingHorizontalViewDirection(net.minecraft.world.entity.LivingEntity e) {
        int dirx = net.minecraft.util.Mth.floor((double) (e.getYRot() * 4.0F / 360.0F) + 0.5D) & 3;
        if (dirx == 3) return Direction.XP;
        if (dirx == 0) return Direction.ZP;
        if (dirx == 1) return Direction.XN;
        return Direction.ZN;
    }

    /** Registry access for item (de)serialization: the running server's (null when there is none). */
    @javax.annotation.Nullable
    public static net.minecraft.core.HolderLookup.Provider registries() {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    }

    /** An ItemStack as NBT (26.x codec), or null for empty stacks or when no registries are available. */
    @javax.annotation.Nullable
    public static net.minecraft.nbt.Tag saveItem(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.core.HolderLookup.Provider r = registries();
        if (r == null || isEmpty(stack)) return null;
        return net.minecraft.world.item.ItemStack.CODEC.encodeStart(r.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
            .result().orElse(null);
    }

    public static net.minecraft.world.item.ItemStack loadItem(net.minecraft.nbt.Tag tag) {
        net.minecraft.core.HolderLookup.Provider r = registries();
        if (r == null || tag == null) return net.minecraft.world.item.ItemStack.EMPTY;
        return net.minecraft.world.item.ItemStack.CODEC.parse(r.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
            .result().orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    /** An inventory as a list of {Slot: byte, item} under key str (1.12 layout: Slot + the stack's own tags). */
    public static void readFromNBT(net.minecraft.nbt.CompoundTag nbt, String str, net.minecraft.world.Container inventory) {
        net.minecraft.nbt.ListTag list = nbt.getListOrEmpty(str);
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.CompoundTag t = list.getCompoundOrEmpty(i);
            int slot = t.getByteOr("Slot", (byte) 0) & 255;
            if (slot < inventory.getContainerSize())
                inventory.setItem(slot, loadItem(t.get("item")));
        }
    }

    public static void writeToNBT(net.minecraft.nbt.CompoundTag nbt, String str, net.minecraft.world.Container inventory) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            net.minecraft.nbt.Tag item = saveItem(inventory.getItem(i));
            if (item == null) continue;
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            t.putByte("Slot", (byte) i);
            t.put("item", item);
            list.add(t);
        }
        nbt.put(str, list);
    }

    /** 1.12 Utils.newNbtTagCompund: a new compound stored under key str of nbt, returned for filling. */
    public static net.minecraft.nbt.CompoundTag newNbtTagCompund(net.minecraft.nbt.CompoundTag nbt, String str) {
        net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
        nbt.put(str, c);
        return c;
    }

    /** The compound tags directly inside nbt (1.12 getTags). */
    public static java.util.List<net.minecraft.nbt.CompoundTag> getTags(net.minecraft.nbt.CompoundTag nbt) {
        java.util.List<net.minecraft.nbt.CompoundTag> out = new java.util.ArrayList<>();
        for (String k : nbt.keySet()) {
            net.minecraft.nbt.Tag t = nbt.get(k);
            if (t instanceof net.minecraft.nbt.CompoundTag) out.add((net.minecraft.nbt.CompoundTag) t);
        }
        return out;
    }

    /** 1.12 mustDropItem: drops are on unless the player is in creative. */
    public static boolean mustDropItem(net.minecraft.world.entity.player.Player player) {
        return player == null || !player.isCreative();
    }

    public static boolean isCreative(net.minecraft.world.entity.player.Player player) {
        return player != null && player.isCreative();
    }

    private static int uuid = 1;

    /** Server-side unique ids (sound tracks, element uuids), as in 1.12. */
    public static int getUuid() {
        if (uuid < 1) uuid = 1;
        return uuid++;
    }

    /**
     * An item for the client in an element packet: its registry id ("" for empty). 1.12 wrote two shorts (numeric item
     * id, damage); 26.x has neither, and both ends are EA code.
     */
    public static void serialiseItemStack(java.io.DataOutputStream stream, net.minecraft.world.item.ItemStack stack) throws java.io.IOException {
        if (isEmpty(stack)) stream.writeUTF("");
        else stream.writeUTF(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static net.minecraft.world.item.ItemStack unserialiseItemStack(java.io.DataInputStream stream) throws java.io.IOException {
        String id = stream.readUTF();
        if (id.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.resources.Identifier key = net.minecraft.resources.Identifier.tryParse(id);
        if (key == null) return net.minecraft.world.item.ItemStack.EMPTY;
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(key)
            .map(net.minecraft.world.item.ItemStack::new).orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    /** The player holds an EA wrench (filled when the wrench item is ported). */
    public static java.util.function.Predicate<net.minecraft.world.entity.player.Player> wrenchCheck = p -> false;

    public static boolean isPlayerUsingWrench(net.minecraft.world.entity.player.Player player) {
        return player != null && wrenchCheck.test(player);
    }

    // ---- 1.12 text formatting (meters, waila, tooltips) ----
    public static String plotValue(double value) {
        double valueAbs = Math.abs(value);
        if (valueAbs < 0.0001) {
            return "0";
        } else if (valueAbs < 0.000999) {
            return String.format("%1.2fµ",value * 10000);
        } else if (valueAbs < 0.00999) {
            return String.format("%1.2fm", value * 1000);
        } else if (valueAbs < 0.0999) {
            return String.format("%2.1fm", value * 1000);
        } else if (valueAbs < 0.999) {
            return String.format("%3.0fm", value * 1000);
        } else if (valueAbs < 9.99) {
            return String.format("%1.2f", value);
        } else if (valueAbs < 99.9) {
            return String.format("%2.1f", value);
        } else if (valueAbs < 999) {
            return String.format("%3.0f", value);
        } else if (valueAbs < 9999) {
            return String.format("%1.2fk", value / 1000.0);
        } else if (valueAbs < 99999) {
            return String.format("%2.1fk", value / 1000.0);
        } else { // if(value < 1000000)
            return String.format("%3.0fk", value / 1000.0);
        }
    }

    public static String plotValue(double value, String unit) {
        return plotValue(value) + unit;
    }

    public static String plotVolt(double value) {
        return plotValue(value, "V  ");
    }

    public static String plotVolt(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotVolt(value);
    }

    public static String plotAmpere(double value) {
        return plotValue(value, "A  ");
    }

    public static String plotAmpere(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotAmpere(value);
    }

    public static String plotCelsius(String header, double value) {
        value += mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius;
        if (!header.equals(""))
            header += " ";
        return header + plotValue(value, "\u00B0C ");
    }

    public static String plotPercent(String header, double value) {
        if (!header.equals(""))
            header += " ";
        if (value >= 1.0)
            return header + String.format("%3.0f", value * 100.0) + "%   ";
        else
            return header + String.format("%3.1f", value * 100.0) + "%   ";
    }

    public static String plotEnergy(double value) {
        return plotValue(value, "J  ");
    }

    public static String plotEnergy(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotEnergy(value);
    }

    public static String plotRads(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotValue(value, "rad/s ");
    }

    public static String plotER(double E, double R) {
        return plotEnergy("E", E) + plotRads("R", R);
    }

    public static String plotPower(double value) {
        return plotValue(value, "W  ");
    }

    public static String plotPower(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotPower(value);
    }

    public static String plotOhm(double value) {
        return plotValue(value, "\u2126 ");
    }

    public static String plotOhm(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotOhm(value);
    }

    public static String plotUIP(double U, double I) {
        return plotVolt("U", U) + plotAmpere("I", I) + plotPower("P", Math.abs(U * I));
    }

    public static String plotTime(double value) {
        String str = "";
        int h, mn, s;

        if (value == 0.0)
            return str + "0''";

        h = (int) (value / 3600);
        value = value % 3600;
        mn = (int) (value / 60);
        value = value % 60;
        s = (int) (value / 1);

        if (h != 0)
            str += h + "h";
        if (mn != 0)
            str += mn + "'";
        if (s != 0)
            str += s + "''";
        return str;
    }

    public static String plotTime(String header, double value) {
        if (!header.equals(""))
            header += " ";
        return header + plotTime(value);
    }

    public static String plotBuckets(String header, double buckets) {
        if (!header.equals(""))
            header += " ";
        return header + plotValue(buckets, "B ");
    }

    public static String plotSignal(double U, double I) {
        return plotVolt("U", U) + plotAmpere("I", I) + plotPercent("Value", U / Eln.SVU);
    }

    public static float limit(float value, float min, float max) {
        return Math.max(Math.min(value, max), min);
    }

    public static double limit(double value, double min, double max) {
        return Math.max(Math.min(value, max), min);
    }

    public static void printFunction(FunctionTable func, double start, double end, double step) {
        Utils.println("********");
        double x;
        for (int idx = 0; (x = start + step * idx) < end + 0.00001; idx++) {
            Utils.println(func.getValue(x));
        }
        Utils.println("********");
    }

    /** One Minecraft day in seconds (1.12 Utils.minecraftDay). */
    public static final double minecraftDay = 60 * 24;

    public static net.minecraft.world.phys.Vec3 getVec05(Coordonate c) {
        return new net.minecraft.world.phys.Vec3(c.x + (c.x < 0 ? -1 : 1) * 0.5, c.y + (c.y < 0 ? -1 : 1) * 0.5, c.z + (c.z < 0 ? -1 : 1) * 0.5);
    }

    /**
     * The sun's angle in radians, 0 at noon (1.12 World.getCelestialAngleRadians(0)); 26.x keeps it as the SUN_ANGLE
     * environment attribute in degrees (same convention: the daylight detector takes its cosine).
     */
    public static double sunAngleRadians(net.minecraft.world.level.Level world) {
        return world.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, net.minecraft.core.BlockPos.ZERO)
            * Math.PI / 180.0;
    }

    /** The EA descriptor behind a stack (1.12: of a damage sub-item), else the Item itself; null for empty stacks. */
    public static Object getItemObject(net.minecraft.world.item.ItemStack stack) {
        if (isEmpty(stack)) return null;
        net.minecraft.world.item.Item i = stack.getItem();
        if (i instanceof mods.eln.generic.IDescriptorItem) return ((mods.eln.generic.IDescriptorItem) i).getEaDescriptor();
        return i;
    }

    public static float[] getSixNodePinDistance(Obj3D.Obj3DPart obj) {
        return new float[]{Math.abs(obj.zMin * 16), Math.abs(obj.zMax * 16), Math.abs(obj.yMin * 16), Math.abs(obj.yMax * 16)};
    }

    public static double getWind(int worldId, int y) {
        net.minecraft.world.level.Level world = Dimensions.level(worldId);
        if (world == null) {
            return Math.max(0.0, Eln.wind.getWind(y));
        } else {
            float factor = 1f + world.getRainLevel(0) * 0.2f + world.getThunderLevel(0) * 0.2f;
            return Math.max(0.0, Eln.wind.getWind(y) * factor + world.getRainLevel(0) * 1f + world.getThunderLevel(0) * 2f);
        }
    }

    public static int getLight(net.minecraft.world.level.Level w, net.minecraft.world.level.LightLayer e, int x, int y, int z) {
        return mods.eln.compat.WorldCompat.getSavedLightValue(w, e, x, y, z);
    }

    public static int getRedstoneLevelAround(Coordonate coord, Direction side) {
        int level = mods.eln.compat.WorldCompat.getStrongestIndirectPower(coord.world(), coord.x, coord.y, coord.z);
        if (level >= 15) return 15;

        side = side.getInverse();
        switch (side) {
            case YN:
            case YP:
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x + 1, coord.y, coord.z, side.toSideValue()));
                if (level >= 15) return 15;
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x - 1, coord.y, coord.z, side.toSideValue()));
                if (level >= 15) return 15;
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x, coord.y, coord.z + 1, side.toSideValue()));
                if (level >= 15) return 15;
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x, coord.y, coord.z - 1, side.toSideValue()));

            case XN:
            case XP:
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x, coord.y + 1, coord.z, side.toSideValue()));
                if (level >= 15) return 15;
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x, coord.y - 1, coord.z, side.toSideValue()));
                if (level >= 15) return 15;
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x, coord.y, coord.z + 1, side.toSideValue()));
                if (level >= 15) return 15;
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x, coord.y, coord.z - 1, side.toSideValue()));

            case ZN:
            case ZP:
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x + 1, coord.y, coord.z, side.toSideValue()));
                if (level >= 15) return 15;
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x - 1, coord.y, coord.z, side.toSideValue()));
                if (level >= 15) return 15;
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x, coord.y + 1, coord.z, side.toSideValue()));
                if (level >= 15) return 15;
                level = Math.max(level, mods.eln.compat.WorldCompat.getIndirectPowerLevelTo(coord.world(), coord.x, coord.y - 1, coord.z, side.toSideValue()));
        }

        return level;
    }

    public static java.util.ArrayList<net.minecraft.world.level.block.Block> traceRay(net.minecraft.world.level.Level world, double x, double y,
                                            double z, double tx, double ty, double tz) {
        java.util.ArrayList<net.minecraft.world.level.block.Block> blockList = new java.util.ArrayList<net.minecraft.world.level.block.Block>();

        double dx, dy, dz;
        dx = tx - x;
        dy = ty - y;
        dz = tz - z;
        double norm = (Math.sqrt(dx * dx + dy * dy + dz * dz));
        double normInv = 1 / (norm + 0.000000001);
        dx *= normInv;
        dy *= normInv;
        dz *= normInv;
        double d = 0;

        while (d < norm) {
            if (Utils.isBlockLoaded(world, x, y, z)) {
                net.minecraft.world.level.block.Block b = Utils.getBlock(world, x, y, z);
                if (b != null)
                    blockList.add(b);
            }

            x += dx;
            y += dy;
            z += dz;
            d += 1;
        }

        return blockList;
    }

    public static boolean isBlockLoaded(net.minecraft.world.level.Level world, double x, double y, double z) {
        return mods.eln.compat.WorldCompat.blockExists(world, net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(y), net.minecraft.util.Mth.floor(z));
    }

    public static net.minecraft.world.level.block.Block getBlock(net.minecraft.world.level.Level world, double x, double y, double z) {
        return mods.eln.compat.WorldCompat.getBlock(world, net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(y), net.minecraft.util.Mth.floor(z));
    }

    public static double getLength(double x, double y,
                                   double z, double tx, double ty, double tz) {
        double dx, dy, dz;
        dx = tx - x;
        dy = ty - y;
        dz = tz - z;
        double norm = (Math.sqrt(dx * dx + dy * dy + dz * dz));
        return norm;
    }

    public static short modbusToShort(double outputNormalized, int i) {
        int bit = Float.floatToRawIntBits((float) outputNormalized);
        if (i == 1)
            return (short) bit;
        else
            return (short) (bit >>> 16);
    }
    public static float modbusToFloat(short first, short second) {
        int bit = ((((int) first) & 0xFFFF) << 16) + (((int) second) & 0xFFFF);
        return Float.intBitsToFloat(bit);
    }

    /** The world save folder, with a trailing slash (1.12: DimensionManager save root). */
    public static String getMapFolder() {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toString() + "/";
    }

    public static java.io.File getMapFile(String name) {
        return new java.io.File(getMapFolder() + name);
    }

    public static String readMapFile(String name) throws java.io.IOException {
        return new String(java.nio.file.Files.readAllBytes(getMapFile(name).toPath()), java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Smelting result of a single input item (1.12 FurnaceRecipes.getSmeltingResult); EMPTY without a server/recipe. */
    public static net.minecraft.world.item.ItemStack smeltResult(net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack input) {
        if (isEmpty(input) || !(level instanceof net.minecraft.server.level.ServerLevel sl)) return net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.world.item.crafting.SingleRecipeInput in = new net.minecraft.world.item.crafting.SingleRecipeInput(input);
        return sl.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, in, sl)
            .map(r -> r.value().assemble(in))
            .orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    /** Per-slot limit: the container's, capped by the item's own max stack size (1.12 used the container's only). */
    static int slotLimit(net.minecraft.world.Container inventory, net.minecraft.world.item.ItemStack stack) {
        return Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
    }

    public static boolean canPutStackInInventory(net.minecraft.world.item.ItemStack[] stackList, net.minecraft.world.Container inventory, int[] slotsIdList) {
        net.minecraft.world.item.ItemStack[] outputStack = new net.minecraft.world.item.ItemStack[slotsIdList.length];
        net.minecraft.world.item.ItemStack[] inputStack = new net.minecraft.world.item.ItemStack[stackList.length];

        for (int idx = 0; idx < outputStack.length; idx++) {
            if (!Utils.isEmpty(inventory.getItem(slotsIdList[idx])))
                outputStack[idx] = inventory.getItem(slotsIdList[idx]).copy();
        }
        for (int idx = 0; idx < stackList.length; idx++) {
            inputStack[idx] = stackList[idx].copy();
        }

        boolean oneStackDone;
        for (net.minecraft.world.item.ItemStack stack : inputStack) {
            oneStackDone = false;
            for (int idx = 0; idx < slotsIdList.length; idx++) {
                net.minecraft.world.item.ItemStack targetStack = outputStack[idx];

                if (Utils.isEmpty(targetStack)) {
                    outputStack[idx] = stack;
                    oneStackDone = true;
                    break;
                } else if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(targetStack, stack)) {
                    int transferMax = slotLimit(inventory, targetStack) - targetStack.getCount();
                    if (transferMax > 0) {
                        int transfer = Math.min(stack.getCount(), transferMax);
                        outputStack[idx].grow(transfer);
                        stack.shrink(transfer);
                    }

                    if (stack.getCount() == 0) {
                        oneStackDone = true;
                        break;
                    }
                }
            }

            if (!oneStackDone)
                return false;
        }
        return true;
    }

    public static boolean tryPutStackInInventory(net.minecraft.world.item.ItemStack[] stackList, net.minecraft.world.Container inventory, int[] slotsIdList) {
        for (net.minecraft.world.item.ItemStack stack : stackList) {
            for (int idx = 0; idx < slotsIdList.length; idx++) {
                net.minecraft.world.item.ItemStack targetStack = inventory.getItem(slotsIdList[idx]);
                if (Utils.isEmpty(targetStack)) {
                    inventory.setItem(slotsIdList[idx], stack.copy());
                    stack.setCount(0);
                    break;
                } else if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(targetStack, stack)) {
                    int transferMax = slotLimit(inventory, targetStack) - targetStack.getCount();
                    if (transferMax > 0) {
                        int transfer = Math.min(stack.getCount(), transferMax);
                        targetStack.grow(transfer); // 1.12: decrStackSize(slot, -transfer)
                        inventory.setChanged();
                        stack.shrink(transfer);
                    }

                    if (stack.getCount() == 0) {
                        break;
                    }
                }
            }
        }
        return true;
    }

    /** 1.12: WATER or FLOWING_WATER; 26.x has one water block (flowing is a state). */
    public static boolean isWater(Coordonate waterCoord) {
        return waterCoord.getBlock() == net.minecraft.world.level.block.Blocks.WATER;
    }

    public static final double burnTimeToEnergyFactor = 1.0;

    /** Fuel energy of a stack [J] (1.12: 80 kJ per 1600 ticks of furnace burn time). 26.x: the COOKING_FUEL component. */
    public static double getItemEnergie(net.minecraft.world.item.ItemStack stack) {
        return burnTimeToEnergyFactor * 80000.0 / 1600 * burnTime(stack);
    }

    public static double getCoalEnergyReference() {
        return burnTimeToEnergyFactor * 80000.0;
    }

    /** Furnace burn time in ticks (0 if not a fuel). */
    public static int burnTime(net.minecraft.world.item.ItemStack stack) {
        if (isEmpty(stack)) return 0;
        net.minecraft.world.item.component.CookingFuel fuel = stack.get(net.minecraft.core.component.DataComponents.COOKING_FUEL);
        if (fuel == null) return 0;
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        try {
            net.minecraft.world.level.storage.loot.LootContext ctx = new net.minecraft.world.level.storage.loot.LootContext.Builder(
                new net.minecraft.world.level.storage.loot.LootParams.Builder(server.overworld())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY))
                .create(java.util.Optional.empty());
            return fuel.burnTime().get(ctx, 0);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** Day fraction 0..1 (1.12: world time / 23999). */
    public static double getWorldTime(net.minecraft.world.level.Level world) {
        return world.getOverworldClockTime() / (23999.0);
    }

    public static double getWeatherNoLoad(int dim) {
        net.minecraft.world.level.Level world = Dimensions.level(dim);
        if (world == null) return 0.0;
        if (world.isThundering())
            return 1.0;
        if (world.isRaining())
            return 0.5;
        return 0.0;
    }

    public static void serverTeleport(net.minecraft.world.entity.Entity e, double x, double y, double z) {
        if (e instanceof net.minecraft.server.level.ServerPlayer)
            ((net.minecraft.server.level.ServerPlayer) e).teleportTo(x, y, z);
        else
            e.setPos(x, y, z);
    }

    /** 1.12 `block == Blocks.AIR`: on 26.x also cave_air and void_air. */
    public static boolean isAirBlock(net.minecraft.world.level.block.Block block) {
        return block != null && block.defaultBlockState().isAir();
    }
}
