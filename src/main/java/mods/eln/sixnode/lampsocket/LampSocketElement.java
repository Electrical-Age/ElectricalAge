package mods.eln.sixnode.lampsocket;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.i18n.I18N;
import mods.eln.item.BrushDescriptor;
import mods.eln.item.IConfigurable;
import mods.eln.item.ItemMovingHelper;
import mods.eln.item.LampDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.MonsterPopFreeProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.WorldSettings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LampSocketElement extends SixNodeElement implements IConfigurable {

    LampSocketDescriptor socketDescriptor = null;

    public MonsterPopFreeProcess monsterPopFreeProcess = new MonsterPopFreeProcess(sixNode.coordonate, Eln.instance.killMonstersAroundLampsRange);
    public NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");

    public LampSocketProcess lampProcess = new LampSocketProcess(this);
    public Resistor lampResistor = new Resistor(positiveLoad, null);

    boolean poweredByLampSupply = true;
    boolean grounded = true;

    private AutoAcceptInventoryProxy acceptingInventory =
        (new AutoAcceptInventoryProxy(new SixNodeElementInventory(2, 64, this)))
            .acceptIfEmpty(0, LampDescriptor.class)
            .acceptIfEmpty(1, ElectricalCableDescriptor.class);

    LampDescriptor lampDescriptor = null;
    public String channel = lastSocketName;

    public static String lastSocketName = "Default channel";

    static final int setGroundedId = 1;
    static final int setAlphaZId = 2;
    static final int tooglePowerSupplyType = 3, setChannel = 4;

    boolean isConnectedToLampSupply = false;

    public int paintColor = 15;

    public LampSocketElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.socketDescriptor = (LampSocketDescriptor) descriptor;

        lampProcess.alphaZ = this.socketDescriptor.alphaZBoot;
        slowProcessList.add(lampProcess);
        slowProcessList.add(monsterPopFreeProcess);
    }


    @Override
    public IInventory getInventory() {
        if (acceptingInventory != null)
            return acceptingInventory.getInventory();
        else
            return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);
        grounded = (value & 4) != 0;

        setPoweredByLampSupply(nbt.getBoolean("poweredByLampSupply"));
        channel = nbt.getString("channel");

        byte b = nbt.getByte("color");
        if (socketDescriptor.paintable)
            paintColor = b & 0xF;
        else {
            //For avoid existing lamps just set paintable to be drawn black (0) by default.
            //Of course, maps need to be loaded with this code before set an already existing lamp paintable.
            paintColor = 0x0F;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("front", (byte) ((front.toInt() << 0) + (grounded ? 4 : 0)));
        nbt.setBoolean("poweredByLampSupply", poweredByLampSupply);
        nbt.setString("channel", channel);
        nbt.setByte("color", (byte) (paintColor));
    }

    public void networkUnserialize(DataInputStream stream) {
        try {
            switch (stream.readByte()) {
                case setGroundedId:
                    grounded = stream.readByte() != 0;
                    computeElectricalLoad();
                    reconnect();
                    break;
                case setAlphaZId:
                    lampProcess.alphaZ = stream.readFloat();
                    needPublish();
                    break;
                case tooglePowerSupplyType:
                    setPoweredByLampSupply(!poweredByLampSupply);

                    reconnect();
                    break;
                case setChannel:
                    channel = stream.readUTF();
                    lastSocketName = channel;
                    needPublish();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setPoweredByLampSupply(boolean b) {
        poweredByLampSupply = b;
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();

        electricalLoadList.remove(positiveLoad);
        electricalComponentList.remove(lampResistor);
        positiveLoad.state = 0;
    }

    @Override
    public void connectJob() {
        if (!poweredByLampSupply) {
            electricalLoadList.add(positiveLoad);
            electricalComponentList.add(lampResistor);
        }
        super.connectJob();
    }

    @Override
    protected void inventoryChanged() {
        computeElectricalLoad();
        reconnect();
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new LampSocketContainer(player, acceptingInventory.getInventory(), socketDescriptor);
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (acceptingInventory.getInventory().getStackInSlot(LampSocketContainer.cableSlotId) == null) return null;
        if (poweredByLampSupply) return null;

        if (grounded) return positiveLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (acceptingInventory.getInventory().getStackInSlot(LampSocketContainer.cableSlotId) == null) return 0;
        if (poweredByLampSupply) return 0;
        if (grounded) return NodeBase.maskElectricalPower;

        if (front == lrdu) return NodeBase.maskElectricalPower;
        if (front == lrdu.inverse()) return NodeBase.maskElectricalPower;

        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("U:", positiveLoad.getU()) + Utils.plotAmpere("I:", lampResistor.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Power consumption"), Utils.plotPower("", lampResistor.getI() * lampResistor.getU()));
        if (lampDescriptor != null) {
            info.put(I18N.tr("Bulb"), lampDescriptor.name);
        } else {
            info.put(I18N.tr("Bulb"), I18N.tr("None"));
        }
        if (Eln.wailaEasyMode) {
            if (poweredByLampSupply) {
                info.put(I18N.tr("Channel"), channel);
            }
            info.put(I18N.tr("Voltage"), Utils.plotVolt("", positiveLoad.getU()));
            ItemStack lampStack = acceptingInventory.getInventory().getStackInSlot(0);
            if (lampStack != null && lampDescriptor != null) {
                info.put(I18N.tr("Life"), Utils.plotValue(lampDescriptor.getLifeInTag(lampStack)));
            }

        }
        return info;
    }

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte((grounded ? (1 << 6) : 0));
            Utils.serialiseItemStack(stream, acceptingInventory.getInventory().getStackInSlot(LampSocketContainer.lampSlotId));
            stream.writeFloat((float) lampProcess.alphaZ);
            Utils.serialiseItemStack(stream, acceptingInventory.getInventory().getStackInSlot(LampSocketContainer.cableSlotId));
            stream.writeBoolean(poweredByLampSupply);
            stream.writeUTF(channel);
            stream.writeBoolean(isConnectedToLampSupply);
            stream.writeByte(lampProcess.light);
            stream.writeByte(paintColor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        computeElectricalLoad();
    }

    public void computeElectricalLoad() {
        ItemStack lamp = acceptingInventory.getInventory().getStackInSlot(LampSocketContainer.lampSlotId);
        ItemStack cable = acceptingInventory.getInventory().getStackInSlot(LampSocketContainer.cableSlotId);

        ElectricalCableDescriptor cableDescriptor = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(cable);

        if (cableDescriptor == null) {
            positiveLoad.highImpedance();
            //negativeLoad.highImpedance();
        } else {
            cableDescriptor.applyTo(positiveLoad);
            //cableDescriptor.applyTo(negativeLoad, grounded,5);
        }

        lampDescriptor = (LampDescriptor) Utils.getItemObject(lamp);

        if (lampDescriptor == null) {
            lampResistor.setR(Double.POSITIVE_INFINITY);
        } else {
            lampDescriptor.applyTo(lampResistor);
        }
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            front = front.getNextClockwise();
            if (socketDescriptor.rotateOnlyBy180Deg)
                front = front.getNextClockwise();
            reconnect();
            return true;
        }

        ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
        if (currentItemStack != null) {
            GenericItemUsingDamageDescriptor itemDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(currentItemStack);
            if (itemDescriptor != null) {
                if (itemDescriptor instanceof BrushDescriptor) {
                    BrushDescriptor brush = (BrushDescriptor) itemDescriptor;
                    int brushColor = brush.getColor(currentItemStack);
                    if (brushColor != paintColor && brush.use(currentItemStack, entityPlayer)) {
                        paintColor = brushColor;
                        needPublish(); //Sync
                    }
                    return true;
                }
            }
        }

        return acceptingInventory.take(entityPlayer.getCurrentEquippedItem(), this, true, false);
    }

    public int getLightValue() {
        return lampProcess.getBlockLight();
    }

    @Override
    public void destroy(EntityPlayerMP entityPlayer) {
        super.destroy(entityPlayer);
        lampProcess.destructor();
    }

    void setIsConnectedToLampSupply(boolean value) {
        if (isConnectedToLampSupply != value) {
            isConnectedToLampSupply = value;
            needPublish();
        }
    }

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        if(compound.hasKey("powerChannels")) {
            String newChannel = compound.getTagList("powerChannels", 8).getStringTagAt(0);
            if(newChannel != null && newChannel != "") {
                channel = newChannel;
                needPublish();
            }
        }
        if(compound.hasKey("lampType")) {
            String type = compound.getString("lampType");
            GenericItemUsingDamageDescriptor thisLampDesc = GenericItemUsingDamageDescriptor.getDescriptor(getInventory().getStackInSlot(0), LampDescriptor.class);
            if(thisLampDesc != null) {
                (new ItemMovingHelper() {
                    @Override
                    public boolean acceptsStack(ItemStack stack) {
                        return thisLampDesc.checkSameItemStack(stack);
                    }

                    @Override
                    public ItemStack newStackOfSize(int items) {
                        return thisLampDesc.newItemStack(items);
                    }
                }).move(invoker.inventory, getInventory(), 0, 0);
            }
            if(!type.equals(GenericItemUsingDamageDescriptor.INVALID_NAME)) {
                GenericItemUsingDamageDescriptor lampDesc = GenericItemUsingDamageDescriptor.getByName(type);
                (new ItemMovingHelper() {
                    @Override
                    public boolean acceptsStack(ItemStack stack) {
                        return lampDesc.checkSameItemStack(stack);
                    }

                    @Override
                    public ItemStack newStackOfSize(int items) {
                        return lampDesc.newItemStack(items);
                    }
                }).move(invoker.inventory, getInventory(), 0, 1);
            }
        }
        if(compound.hasKey("cableType")) {
            int type = compound.getInteger("cableType");
            GenericItemBlockUsingDamageDescriptor thisCableDesc = GenericItemBlockUsingDamageDescriptor.getDescriptor(getInventory().getStackInSlot(1), ElectricalCableDescriptor.class);
            if(thisCableDesc != null) {
                (new ItemMovingHelper() {
                    @Override
                    public boolean acceptsStack(ItemStack stack) {
                        return thisCableDesc.checkSameItemStack(stack);
                    }

                    @Override
                    public ItemStack newStackOfSize(int items) {
                        return thisCableDesc.newItemStack(items);
                    }
                }).move(invoker.inventory, getInventory(), 1, 0);
            }
            if(type != -1) {
                GenericItemBlockUsingDamageDescriptor cableDesc = Eln.sixNodeItem.getDescriptor(type);
                (new ItemMovingHelper() {
                    @Override
                    public boolean acceptsStack(ItemStack stack) {
                        GenericItemBlockUsingDamageDescriptor thisDesc = GenericItemBlockUsingDamageDescriptor.getDescriptor(stack);
                        Utils.println(String.format("LSE.rCT: IMH.aS: cableDesc %s stack %s descriptor %s", cableDesc, stack, GenericItemBlockUsingDamageDescriptor.getDescriptor(stack)));
                        if(thisDesc != null) {
                            Utils.println(String.format("... cable is %s DMG %d", cableDesc.parentItem, cableDesc.parentItemDamage));
                            Utils.println(String.format("... this is %s DMG %d", thisDesc.parentItem, thisDesc.parentItemDamage));
                        }
                        return cableDesc.checkSameItemStack(stack);
                    }

                    @Override
                    public ItemStack newStackOfSize(int items) {
                        return cableDesc.newItemStack(items);
                    }
                }).move(invoker.inventory, getInventory(), 1, 1);
            }
        }
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString(channel));
        compound.setTag("powerChannels", list);
        IInventory inv = getInventory();
        ItemStack lampStack = inv.getStackInSlot(0);
        ItemStack cableStack = inv.getStackInSlot(1);
        GenericItemUsingDamageDescriptor lampDesc = GenericItemUsingDamageDescriptor.getDescriptor(lampStack, LampDescriptor.class);
        GenericItemBlockUsingDamageDescriptor cableDesc = GenericItemBlockUsingDamageDescriptor.getDescriptor(cableStack);
        if(lampDesc != null)
            compound.setString("lampType", lampDesc.name);
        else
            compound.setString("lampType", GenericItemUsingDamageDescriptor.INVALID_NAME);
        if(cableDesc != null)
            compound.setInteger("cableType", cableDesc.parentItemDamage);
        else
            compound.setInteger("cableType", -1);
    }
}
