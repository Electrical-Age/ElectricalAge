package mods.eln.transparentnode.autominer;



import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.compat.WorldCompat;
import mods.eln.Eln;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.ore.OreBlock;
import mods.eln.sim.IProcess;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class AutoMinerSlowProcess implements IProcess, INBTTReady {

    private final AutoMinerElement miner;

    int pipeLength = 0;

    private double energyCounter = 0;
    private double energyTarget = 0;

    private boolean oneJobDone = true;
    boolean silkTouch = false;

    enum jobType {none, done, full, chestFull, ore, pipeAdd, pipeRemove}

    jobType job = jobType.none;
    private jobType oldJob = jobType.none;
    private final Coordonate jobCoord = new Coordonate();
    private int blinkCounter = 0;

    private int drillCount = 1;

    ArrayList<ItemStack> itemsToDrop = new ArrayList<ItemStack>(4);

    /**
     * The ghost id of every pipe segment. 1.12 gave each segment its height as id, and the machine's own ghosts have -1
     * (TransparentNodeDescriptor.getGhostGroupUuid): below y = 0 the segment at y = -1 counted as the machine, so
     * breaking it broke the miner. Any id but -1 does.
     */
    static final int PIPE_GHOST_UUID = 1;

    /** Seconds until an idle miner (none, done, full, chestFull) looks for a job again. */
    private double idleWait = 0;

    /** How many times the job was set up (tests: an idle miner looks once a second). */
    int setups = 0;

    /** The pipe ghosts of an older save were renumbered (once, on the first step: ghosts load after the nodes). */
    private boolean pipeIdsChecked = false;

    public AutoMinerSlowProcess(AutoMinerElement autoMiner) {
        this.miner = autoMiner;
    }

    void toggleSilkTouch() {
        silkTouch = !silkTouch;
    }

    private boolean isReadyToDrill() {
        ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.getInventory().getItem(AutoMinerContainer.electricalDrillSlotId));
        if (drill == null) return false;
        return isStorageReady();
    }

    private boolean isStorageReady() {
        ResourceHandler<ItemResource> h = getDropHandler();
        if (h == null) return false;
        for (int idx = 0; idx < h.size(); idx++) {
            if (h.getResource(idx).isEmpty())
                return true;
        }
        return false;
    }

    @Override
    public void process(double time) {
        if (!pipeIdsChecked) {
            pipeIdsChecked = true;
            renumberOldPipe();
        }
        ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.getInventory().getItem(AutoMinerContainer.electricalDrillSlotId));

        if (++blinkCounter >= 9) {
            blinkCounter = 0;
            if ((miner.inPowerLoad.getU() / miner.descriptor.nominalVoltage - 0.5) * 3 > Math.random()) {
                miner.setPowerOk(true);
                LightBlockEntity.addLight(miner.lightCoordonate, 12, 11);
            } else {
                miner.setPowerOk(false);
            }
        }

        energyCounter += miner.powerResistor.getP() * time;

        if (job != jobType.none && job != jobType.full && job != jobType.chestFull && job != jobType.done) {
            if (energyCounter >= energyTarget || (job == jobType.ore && !isReadyToDrill()) || !miner.powerOk) {
                setupJob();
            }

            if (energyCounter >= energyTarget) {
                oneJobDone = true;
                switch (job) {
                    case ore:
                        // read again: the block may have changed in the seconds the drill took
                        if (isProtected(jobCoord)) {
                            energyCounter -= energyTarget;
                            setupJob();
                            break;
                        }
                        drillCount++;

                        Block block = WorldCompat.getBlock(jobCoord.world(), jobCoord.x, jobCoord.y, jobCoord.z);
                        int meta = WorldCompat.getMeta(jobCoord.world(), jobCoord.x, jobCoord.y, jobCoord.z);
                        if (silkTouch) {
                            itemsToDrop.add(new ItemStack(block)); // 1.12: + meta
                        } else {
                            // 1.12: drops go into a NonNullList (was getDrops(world, x, y, z, meta, fortune 0))
                            BlockPos pos = new BlockPos(jobCoord.x, jobCoord.y, jobCoord.z);
                            if (jobCoord.world() instanceof net.minecraft.server.level.ServerLevel sl)
                                itemsToDrop.addAll(Block.getDrops(sl.getBlockState(pos), sl, pos, sl.getBlockEntity(pos)));
                        }

                        // Use cobblestone instead of air, everywhere except the mining shaft.
                        // This is so mobs won't spawn excessively.
                        int xDist = jobCoord.x - miner.node.coordonate.x, zDist = jobCoord.z - miner.node.coordonate.z;
                        if (xDist * xDist + zDist * zDist > 25) {
                            WorldCompat.setBlock(jobCoord.world(), jobCoord.x, jobCoord.y, jobCoord.z, Blocks.COBBLESTONE);
                        } else {
                            WorldCompat.setBlockToAir(jobCoord.world(), jobCoord.x, jobCoord.y, jobCoord.z);
                        }

                        energyCounter -= energyTarget;
                        setupJob();
                        break;
                    case pipeAdd:
                        // miner.pushLog("Pipe " + (pipeLength + 1) + " added");
                        Eln.ghostManager.createGhost(jobCoord, miner.node.coordonate, PIPE_GHOST_UUID);
                        miner.getInventory().removeItem(AutoMinerContainer.MiningPipeSlotId, 1);

                        pipeLength++;
                        miner.needPublish();

                        energyCounter -= energyTarget;
                        setupJob();
                        break;
                    case pipeRemove:
                        // miner.pushLog("Pipe " + pipeLength + " removed");
                        Eln.ghostManager.removeGhostAndBlock(jobCoord);
                        if (Utils.isEmpty(miner.getInventory().getItem(AutoMinerContainer.MiningPipeSlotId))) {
                            miner.getInventory().setItem(AutoMinerContainer.MiningPipeSlotId, ElnDeviceRegistry.miningPipeDescriptor.newItemStack(1));
                        } else {
                            miner.getInventory().getItem(AutoMinerContainer.MiningPipeSlotId).grow(1); // 1.12: decrStackSize(slot, -1)
                            miner.getInventory().setChanged();
                        }

                        pipeLength--;
                        miner.needPublish();

                        energyCounter -= energyTarget;
                        setupJob();
                        break;
                    default:
                        break;
                }
            }
        } else if ((idleWait -= time) <= 0) {
            // idle: look again once a second (1.12 did every tick, and a finished miner rescanned its whole layer, up
            // to 129 x 129 blocks, 20 times a second for as long as it stood there)
            idleWait = 1;
            setupJob();
        }

        switch (job) {
            default:
                miner.powerResistor.highImpedance();
                break;
            case ore:
                if (drill == null) {
                    miner.powerResistor.highImpedance();
                } else {
                    double p = drill.nominalPower;
                    if (silkTouch) p *= 3;
                    miner.powerResistor.setR(Math.pow(miner.descriptor.nominalVoltage, 2.0) / p);
                }
                break;
            case pipeAdd:
                miner.powerResistor.setR(miner.descriptor.pipeOperationRp);
                break;
            case pipeRemove:
                miner.powerResistor.setR(miner.descriptor.pipeOperationRp);
                break;
        }

        if (oldJob != job) {
            miner.needPublish();
        }

        if (oneJobDone || oldJob != job) {
            switch (job) {
                case chestFull:
                    miner.pushLog("* Storage full!");
                    break;
                case done:
                    miner.pushLog("- SLEEP");
                    break;
                case full:
                    miner.pushLog("* Pipe stack full!");
                    break;
                case none:
                    miner.pushLog("* Waiting opcode.");
                    break;
                case ore:
                    miner.pushLog("- DRILL #" + drillCount);
                    break;
                case pipeAdd:
                    miner.pushLog("- ADD PIPE #" + (pipeLength + 1));
                    break;
                case pipeRemove:
                    miner.pushLog("- REMOVE PIPE #" + (pipeLength));
                    break;
                default:
                    break;
            }
        }
        oneJobDone = false;
        oldJob = job;
    }

    /**
     * Where mined items go: the block 1 (else 2) forward and 1 down, through its item capability from above (26.x:
     * modded and EA inventories too), else a plain vanilla Container there as before.
     */
    public ResourceHandler<ItemResource> getDropHandler() {
        ResourceHandler<ItemResource> handler = null;
        for (int x = 2; x >= 1; x--) {
            Coordonate c = new Coordonate(x, -1, 0, miner.world());
            c.applyTransformation(miner.front, miner.coordonate());
            ResourceHandler<ItemResource> h = miner.world().getCapability(Capabilities.Item.BLOCK, c.pos(), net.minecraft.core.Direction.UP);
            if (h == null && c.getTileEntity() instanceof Container chest) h = VanillaContainerWrapper.of(chest);
            if (h != null) handler = h;
        }
        return handler;
    }

    /** Puts the whole stack into the drop inventory, or nothing (the stack stays queued). */
    public boolean drop(ItemStack stack) {
        if (Utils.isEmpty(stack)) return true;
        ResourceHandler<ItemResource> h = getDropHandler();
        if (h == null) return false;
        try (Transaction tx = Transaction.openRoot()) {
            if (ResourceHandlerUtil.insertStacking(h, ItemResource.of(stack), stack.getCount(), tx) < stack.getCount()) return false;
            tx.commit();
            return true;
        }
    }

    private boolean isMinable(Block block) {
        return !Utils.isAirBlock(block)
            && (block) != Blocks.WATER // 1.12 also FLOWING_WATER / FLOWING_LAVA: one block each on 26.x
            && (block) != Blocks.LAVA
            && (block) != Blocks.OBSIDIAN && (block) != Blocks.BEDROCK;
    }

    /**
     * Blocks the miner leaves alone, as it always left bedrock and obsidian: unbreakable ones (destroy speed below 0:
     * end portal frames, barriers), those the wither can't break (reinforced deepslate, command blocks), those world
     * generation must not replace (spawners, trial spawners, vaults, chests), and budding amethyst. 1.12 mined them
     * too, and with silk touch handed them out as items. In the shaft they end it (done), as bedrock does; around it
     * they are skipped.
     */
    public static boolean isProtected(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, BlockPos pos) {
        return state.getDestroySpeed(level, pos) < 0
            || state.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)
            || state.is(net.minecraft.tags.BlockTags.FEATURES_CANNOT_REPLACE)
            || state.is(Blocks.BUDDING_AMETHYST) || state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
    }

    private boolean isProtected(Coordonate c) {
        BlockPos pos = c.pos();
        return isProtected(c.world().getBlockState(pos), c.world(), pos);
    }

    private void setupJob() {
        setups++;
        ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.getInventory().getItem(AutoMinerContainer.electricalDrillSlotId));
        // OreScanner scanner = (OreScanner) ElectricalDrillDescriptor.getDescriptor(miner.inventory.getItem(AutoMinerContainer.OreScannerSlotId));
        MiningPipeDescriptor pipe = (MiningPipeDescriptor) ElectricalDrillDescriptor.getDescriptor(miner.getInventory().getItem(AutoMinerContainer.MiningPipeSlotId));

        int scannerRadius = Eln.autominerRange;
        double scannerEnergy = 0;

        jobCoord.dimention = miner.node.coordonate.dimention;
        jobCoord.x = miner.node.coordonate.x;
        jobCoord.y = miner.node.coordonate.y - pipeLength;
        jobCoord.z = miner.node.coordonate.z;

        // Attempt to drop items. This might not be successful.
        while (itemsToDrop.size() > 0) {
            int index = itemsToDrop.size() - 1;
            if (drop(itemsToDrop.get(index))) {
                itemsToDrop.remove(index);
            } else {
                break; // 1.12 port bug fix: 1.7.10 spun here forever (server hang) when the chest filled up; the rest
                       // stays queued and the job below becomes chestFull until there is room
            }
        }

        boolean jobFind = false;
        if (!miner.node.coordonate.getBlockExist()) {
            setJob(jobType.none);
        } else if (!miner.powerOk) {
            setJob(jobType.none);
        } else if (drill == null) {
            if (jobCoord.y != miner.node.coordonate.y) {
                ItemStack pipeStack = miner.getInventory().getItem(AutoMinerContainer.MiningPipeSlotId);
                if (Utils.isEmpty(pipeStack) || (pipeStack.getCount() != pipeStack.getMaxStackSize() && pipeStack.getCount() != miner.getInventory().getMaxStackSize())) {
                    jobFind = true;
                    setJob(jobType.pipeRemove);
                } else {
                    jobFind = true;
                    setJob(jobType.full);
                }
            }
        } else if (!isStorageReady() || itemsToDrop.size() != 0) {
            setJob(jobType.chestFull);
            jobFind = true;
        } else if (pipe != null) {
            if (jobCoord.y < miner.node.coordonate.y - 2) {
                int depth = (miner.node.coordonate.y - jobCoord.y);
                double miningRay = depth / 10.0 + 0.1;
                miningRay = Math.min(miningRay, 2);
                if (depth < scannerRadius) scannerRadius = depth + 1;
                miningRay = Math.min(miningRay, scannerRadius - 2);
                for (jobCoord.z = miner.node.coordonate.z - scannerRadius; jobCoord.z <= miner.node.coordonate.z + scannerRadius; jobCoord.z++) {
                    for (jobCoord.x = miner.node.coordonate.x - scannerRadius; jobCoord.x <= miner.node.coordonate.x + scannerRadius; jobCoord.x++) {
                        double dx = jobCoord.x - miner.node.coordonate.x;
                        double dy = 0;
                        double dz = jobCoord.z - miner.node.coordonate.z;
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        Block block = WorldCompat.getBlock(jobCoord.world(), jobCoord.x, jobCoord.y, jobCoord.z);
                        if ((checkIsOre(jobCoord) || (distance > 0.1 && distance < miningRay && isMinable(block))) && !isProtected(jobCoord)) {
                            jobFind = true;
                            setJob(jobType.ore);
                            break;
                        }
                    }
                    if (jobFind) break;
                }
            }

            if (!jobFind) {
                if (jobCoord.y < jobCoord.world().getMinY() + 3) { // 1.12: y < 3 (bedrock at 0..4); 26.x worlds go down to the min build height
                    jobFind = true;
                    setJob(jobType.done);
                } else {
                    jobCoord.x = miner.node.coordonate.x;
                    jobCoord.y--;
                    jobCoord.z = miner.node.coordonate.z;

                    Block block = WorldCompat.getBlock(jobCoord.world(), jobCoord.x, jobCoord.y, jobCoord.z);
                    if (!Utils.isAirBlock(block)
                        && block != Blocks.WATER
                        && block != Blocks.LAVA) {
                        if (!isProtected(jobCoord)) {
                            jobFind = true;
                            setJob(jobType.ore);
                        } else {
                            jobFind = true;
                            setJob(jobType.done);
                        }
                    } else {
                        jobFind = true;
                        setJob(jobType.pipeAdd);
                    }
                }
            }
        }
        if (!jobFind) setJob(jobType.none);

        switch (job) {
            case ore:
                energyTarget = drill.OperationEnergy + scannerEnergy;
                // Copied from Mekanism. Note that the power demand is tripled, so in effect this doubles time.
                if (silkTouch) energyTarget *= 6;
                break;
            case pipeAdd:
                energyTarget = miner.descriptor.pipeOperationEnergy;
                break;
            case pipeRemove:
                energyTarget = miner.descriptor.pipeOperationEnergy;
                break;
            default:
                energyTarget = 0;
                break;
        }
    }

    private void setJob(jobType job) {
        if (job != this.job) {
            miner.needPublish();
            energyCounter = 0;
        }
        this.job = job;
    }

    private boolean checkIsOre(Coordonate coordonate) {
        Block block = WorldCompat.getBlock(coordonate.world(), coordonate.x, coordonate.y, coordonate.z);
        if (block instanceof OreBlock) return true;
        // 1.12: vanilla BlockOre / BlockRedstoneOre and the X-ray scanner's ore table (other mods' ores);
        // 26.x: the common ore tag (vanilla and mod ores)
        return coordonate.world().getBlockState(new BlockPos(coordonate.x, coordonate.y, coordonate.z))
            .is(net.neoforged.neoforge.common.Tags.Blocks.ORES);
    }

    public void onBreakElement() {
        destroyPipe();
        // what was mined but had no room in the chest yet
        for (ItemStack s : itemsToDrop) Utils.dropItem(s, miner.node.coordonate);
        itemsToDrop.clear();
    }

    /** A segment an older version made at y = -1 has the machine's ghost id (-1): it gets the pipe id. */
    private void renumberOldPipe() {
        Coordonate c = new Coordonate(miner.node.coordonate);
        for (c.y = miner.node.coordonate.y - 1; c.y >= miner.node.coordonate.y - pipeLength; c.y--)
            Eln.ghostManager.renumberGhost(c, miner.node.coordonate, miner.descriptor.getGhostGroupUuid(), PIPE_GHOST_UUID);
    }

    private void destroyPipe() {
        dropPipe();
        Eln.ghostManager.removeGhostAndBlockWithObserverAndNotUuid(miner.node.coordonate, miner.descriptor.getGhostGroupUuid());
        pipeLength = 0;
        miner.needPublish();
    }

    private void dropPipe() {
        Coordonate coord = new Coordonate(miner.node.coordonate);
        for (coord.y = miner.node.coordonate.y - 1; coord.y >= miner.node.coordonate.y - pipeLength; coord.y--) {
            Utils.dropItem(ElnDeviceRegistry.miningPipeDescriptor.newItemStack(1), coord);
        }
    }

    void ghostDestroyed() {
        destroyPipe();
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        pipeLength = nbt.getIntOr(str + "AMSP" + "pipeLength", 0);
        drillCount = nbt.getIntOr(str + "AMSP" + "drillCount", 0);
        if (drillCount == 0) drillCount++;
        itemsToDrop.clear();
        net.minecraft.nbt.ListTag queued = nbt.getListOrEmpty(str + "AMSP" + "itemsToDrop");
        for (int i = 0; i < queued.size(); i++) {
            ItemStack s = Utils.loadItem(queued.get(i));
            if (!s.isEmpty()) itemsToDrop.add(s);
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putInt(str + "AMSP" + "pipeLength", pipeLength);
        nbt.putInt(str + "AMSP" + "drillCount", drillCount);
        // the mined stacks waiting for room in the chest (new key 2026-11-10; older saves have none: an empty queue)
        if (!itemsToDrop.isEmpty()) {
            net.minecraft.nbt.ListTag queued = new net.minecraft.nbt.ListTag();
            for (ItemStack s : itemsToDrop) {
                net.minecraft.nbt.Tag t = Utils.saveItem(s);
                if (t != null) queued.add(t);
            }
            nbt.put(str + "AMSP" + "itemsToDrop", queued);
        }
    }
}
