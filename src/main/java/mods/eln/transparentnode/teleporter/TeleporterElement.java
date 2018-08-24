package mods.eln.transparentnode.teleporter;

import mods.eln.i18n.I18N;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import mods.eln.sound.SoundCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeleporterElement extends TransparentNodeElement implements ITeleporter {

    private TeleporterDescriptor descriptor;
    NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
    private Resistor powerResistor = new Resistor(powerLoad, null);
    private TeleporterSlowProcess slowProcess = new TeleporterSlowProcess();

    private NodePeriodicPublishProcess publisher;
    static public final ArrayList<ITeleporter> teleporterList = new ArrayList<ITeleporter>();

    public TeleporterElement(TransparentNode transparentNode,
                             TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        this.descriptor = (TeleporterDescriptor) descriptor;
        publisher = new NodePeriodicPublishProcess(node, 2, 1);
        electricalLoadList.add(powerLoad);
        electricalComponentList.add(powerResistor);
        slowProcessList.add(slowProcess);
        slowProcessList.add(publisher);

        teleporterList.add(this);

        WorldExplosion exp = new WorldExplosion(this).machineExplosion();
        slowProcessList.add(voltageWatchdog.set(powerLoad).setUNominal(this.descriptor.cable.electricalNominalVoltage).set(exp));

    }

    private VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();

    private Coordinate lightCoordinate;

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {

        return powerLoad;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {

        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {

        if (side == Direction.YP || side == Direction.YN) return 0;
        if (lrdu != LRDU.Down) return 0;
        return NodeBase.maskElectricalPower;
    }

    @Override
    public String multiMeterString(Direction side) {

        return null;
    }

    @Override
    public String thermoMeterString(Direction side) {

        return null;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void initialize() {

        descriptor.cable.applyTo(powerLoad);

        powerResistor.highImpedance();

        for (Coordinate c : descriptor.getPowerCoordinate(node.coordinate.world())) {
            TeleporterPowerNode n = new TeleporterPowerNode();
            n.setElement(this);
            c.applyTransformation(front, node.coordinate);
            n.onBlockPlacedBy(c, Direction.XN, null, null);

            powerNodeList.add(n);
        }

        lightCoordinate = new Coordinate(this.descriptor.lightCoordinate);
        lightCoordinate.applyTransformation(front, node.coordinate);

        descriptor.ghostDoorClose.newRotate(front).eraseGeo(node.coordinate);
        descriptor.ghostDoorOpen.newRotate(front).plot(node.coordinate, node.coordinate, descriptor.getGhostGroupUuid());

        connect();
    }

    @Override
    public void onBreakElement() {

        super.onBreakElement();

        for (TeleporterPowerNode n : powerNodeList) {
            n.onBreakBlock();
        }
        powerNodeList.clear();
        unregister();
    }

    private ArrayList<TeleporterPowerNode> powerNodeList = new ArrayList<TeleporterPowerNode>();

    @Override
    public void unload() {
        super.unload();
        unregister();
    }

    void unregister() {
        teleporterList.remove(this);
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
                                    float vx, float vy, float vz) {

        return false;
    }

    private String name = "Unnamed", targetName = "Unnamed";
    private double energyHit = 0;
    private double energyTarget = 0;
    private double powerCharge = 2000;

    @Override
    public void writeToNBT(NBTTagCompound nbt) {

        super.writeToNBT(nbt);

        nbt.setString("name", name);
        nbt.setString("targetName", targetName);
        nbt.setDouble("powerCharge", powerCharge);
        nbt.setBoolean("reset", state != StateIdle);

    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {

        super.readFromNBT(nbt);

        name = nbt.getString("name");
        targetName = nbt.getString("targetName");
        powerCharge = nbt.getDouble("powerCharge");

        if (nbt.getBoolean("reset"))
            state = StateReset;
    }

    private static final byte StateIdle = 0;
    private static final byte StateStart = 1;
    private static final byte StateClose = 2;
    private static final byte StateCharge = 3;
    private static final byte StateTeleport = 4;
    private static final byte StateOpen = 5;
    private static final byte StateReserved = 6;
    private static final byte StateReset = 7;

    private byte state = StateIdle;
    private float timeCounter;

    private boolean doorState = true;

    void setState(byte state) {
        if (state != this.state) {
            timeCounter = 0;
            switch (this.state) {
                case StateCharge:
                    powerResistor.highImpedance();
                    publisher.reconfigure(2, 1);
                    energyHit = 0;
                    processRatio = 0;
                    break;

                case StateReserved:
                    publisher.reconfigure(2, 1);
                    doorState = true;
                    processRatio = 0;
                    break;
                case StateClose:
                    descriptor.ghostDoorOpen.newRotate(front).eraseGeo(node.coordinate);
                    break;
                case StateOpen:
                    descriptor.ghostDoorClose.newRotate(front).eraseGeo(node.coordinate);
                    break;
                default:
                    break;
            }

            this.state = state;

            switch (this.state) {
                case StateClose:
                    doorState = false;
                    descriptor.ghostDoorClose.newRotate(front).plot(node.coordinate, node.coordinate, descriptor.getGhostGroupUuid());
                    break;
                case StateOpen:
                    doorState = true;
                    descriptor.ghostDoorOpen.newRotate(front).plot(node.coordinate, node.coordinate, descriptor.getGhostGroupUuid());
                    break;
                case StateCharge:
                    powerResistor.setR(Math.pow(descriptor.cable.electricalNominalVoltage, 2) / powerCharge);
                    publisher.reconfigure(0.4, 0);
                    break;
                case StateReserved:
                    publisher.reconfigure(0.4, 0);
                    break;
                default:
                    break;
            }

            Utils.println("Teleporter state=" + state);
            needPublish();

        }
    }

    @Override
    public boolean reservate() {
        if (state != StateIdle) return false;
        if (powerLoad.getU() < descriptor.cable.electricalNominalVoltage * 0.8) return false;

        setState(StateReserved);
        imMaster = false;
        return true;
    }

    @Override
    public void reservateRefresh(boolean doorState, float processRatio) {
        reservateRefreshed = true;
        if (!this.doorState && doorState) {
            setState(StateOpen);
            // setState(StateReserved);
        }
        if (this.doorState && !doorState) {
            setState(StateClose);
            // setState(StateReserved);
        }
        this.processRatio = processRatio;
    }

    private boolean reservateRefreshed = false;

    private float processRatio = 0;
    private static final byte eventNoTargetFind = 1;
    private static final byte eventMultipleoTargetFind = 2;
    private static final byte eventTargetFind = 3;
    private static final byte eventSameTarget = 4;
    private static final byte eventNotSameDimensionTarget = 5;
    private static final byte eventTargetBusy = 6;
    private static final byte eventInstablePowerSupply = 7;
    private boolean imMaster = false;

    class TeleporterSlowProcess implements IProcess {

        BlockPos delta;  // Offset to teleport by. Not really a position.

        int blinkCounter = 0;
        int soundCounter = 0;
        String targetNameCopy = "";

        @Override
        public void process(double time) {
            {
                ITeleporter target = getTarget(state == StateIdle ? targetName : targetNameCopy);
                if (target == null) {
                    energyTarget = 0;
                } else {
                    Coordinate c = getTeleportCoordinate();
                    double distance = getTeleportCoordinate().trueDistanceTo(target.getTeleportCoordinate());
                    AxisAlignedBB bb = descriptor.getBB(node.coordinate, front);
                    int playerCount = c.world().getEntitiesWithinAABB(EntityPlayer.class, bb).size();
                    int itemCount = c.world().getEntitiesWithinAABB(EntityItem.class, bb).size();
                    int petCount = c.world().getEntitiesWithinAABB(EntityLivingBase.class, bb).size() - playerCount;
                    // Object o = c.world().getEntitiesWithinAABB(EntityItem.class,bb);
                    energyTarget = 10000 +
                        40000 * playerCount +
                        5000 * petCount +
                        5000 * itemCount;

                    energyTarget *= 1.0 + Math.pow(distance / 250.0, 0.5);
                }
            }

            if (++blinkCounter >= 9) {
                blinkCounter = 0;
                if ((powerLoad.getU() / descriptor.cable.electricalNominalVoltage - 0.5) * 3 > Math.random())
                    LightBlockEntity.addLight(lightCoordinate, 12, 11);
            }
            switch (state) {
                case StateReserved:
                    if (!reservateRefreshed) {
                        if (!doorState)
                            setState(StateOpen);
                        else
                            setState(StateIdle);
                    }
                    break;

                case StateIdle:
                    imMaster = false;
                    if (startFlag) {
                        targetNameCopy = targetName;
                        energyHit = 0;
                        if (targetNameCopy.equals(name)) {
                            sendIdToAllClient(eventSameTarget);
                            break;
                        }
                        int count = getTargetCount(targetNameCopy);
                        if (count == 0) {
                            sendIdToAllClient(eventNoTargetFind);
                            break;
                        }
                        if (count > 1) {
                            sendIdToAllClient(eventMultipleoTargetFind);
                            break;
                        }
                        if (powerLoad.getU() < descriptor.cable.electricalNominalVoltage * 0.8) {
                            break;
                        }
                        ITeleporter target = getTarget(targetNameCopy);

                        if (!target.reservate()) {
                            sendIdToAllClient(eventTargetBusy);
                            break;
                        }

                        sendIdToAllClient(eventTargetFind);

					/*
                     * AxisAlignedBB bb = descriptor.getBB(node.coordinate,front); List list = node.coordinate.world().getEntitiesWithinAABB(EntityItem.class, bb); for(Object o : list){ Entity e = (Entity)o; if(e instanceof EntityPlayerMP) ((EntityPlayerMP)e).setPositionAndUpdate(e.posX + dx, e.posY + dy, e.posZ + dz); else e.setPosition(e.posX + dx, e.posY + dy, e.posZ + dz); }
					 */
                        imMaster = true;
                        setState(StateStart);

                    }
                    break;

                case StateStart: {
                    int count = node.coordinate.world().getEntitiesWithinAABB(Entity.class, descriptor.getBB(node.coordinate, front)).size();
                    if (count == 0) {
                        timeCounter = 0;
                    } else {
                        timeCounter += time;
                        if (timeCounter > 2) {
                            setState(StateClose);
                        }
                    }
                }
                break;

                case StateClose:
                    timeCounter += time;
                    if (timeCounter > 3) {
                        if (reservateRefreshed)
                            setState(StateReserved);
                        else {
                            setState(StateCharge);
                            soundCounter = 0;
                        }
                    }
                    break;

                case StateCharge: {
                    if (soundCounter++ % 18 == 0)
                        play(new SoundCommand(descriptor.chargeSound).mulVolume(descriptor.chargeVolume, 1f));

                    if (targetNameCopy.equals(name)) {
                        sendIdToAllClient(eventSameTarget);
                        setState(StateOpen);
                        break;
                    }
                    int count = getTargetCount(targetNameCopy);
                    if (count == 0) {
                        sendIdToAllClient(eventNoTargetFind);
                        setState(StateOpen);
                        break;
                    }
                    if (count > 1) {
                        sendIdToAllClient(eventMultipleoTargetFind);
                        setState(StateOpen);
                        break;
                    }
                    if (powerLoad.getU() < descriptor.cable.electricalNominalVoltage * 0.8) {
                        sendIdToAllClient(eventInstablePowerSupply);
                        AxisAlignedBB bb = descriptor.getBB(node.coordinate, front);
                        List list = node.coordinate.world().getEntitiesWithinAABB(Entity.class, bb);
                        for (Object o : list) {
                            Entity e = (Entity) o;
                            double failDistance = 1000;
                            {
                                // Find a not-immediately-fatal position to teleport to.
                                // ...well, it may be a lava lake. But you can survive that!
                                int x, y, z;
                                x = (int) (e.posX + (Math.random() * 2 - 1) * failDistance);
                                z = (int) (e.posZ + (Math.random() * 2 - 1) * failDistance);
                                y = 20;
                                BlockPos pos = new BlockPos(x, y, z);
                                while (!(e.worldObj.isAirBlock(pos) && e.worldObj.isAirBlock(pos.up()))) {
                                    y++;
                                }
                                Utils.serverTeleport(e, x + 0.5, y, z + 0.5);
                            }
                        }
                        setState(StateOpen);
                    } else {
                        ITeleporter target = getTarget(targetNameCopy);
                        Coordinate c = getTeleportCoordinate();
                        energyHit += powerResistor.getP() * time;
                        processRatio = (float) (energyHit / energyTarget);

                        if (energyHit >= energyTarget) {
                            delta = target.getTeleportCoordinate().pos.subtract(c.pos);
                            setState(StateTeleport);
                        }
                    }
                }
                break;

                case StateTeleport: {
                    timeCounter += time;
                    if (timeCounter > 0) {

                        AxisAlignedBB bb = descriptor.getBB(node.coordinate, front);
                        List list = node.coordinate.world().getEntitiesWithinAABB(Entity.class, bb);
                        for (Object o : list) {
                            Entity e = (Entity) o;
                            Utils.serverTeleport(e, e.posX + delta.getX(), e.posY + delta.getY(), e.posZ + delta.getZ());

                        }
                        setState(StateOpen);
                    }
                }
                break;

                case StateOpen:
                    timeCounter += time;
                    if (timeCounter > 3) {
                        if (reservateRefreshed)
                            setState(StateReserved);
                        else
                            setState(StateIdle);
                    }
                    break;
                case StateReset:
                    setState(StateIdle);
                    break;

            }

            if (state != StateIdle && imMaster) {
                ITeleporter target = getTarget(targetNameCopy);
                if (target != null) target.reservateRefresh(doorState, processRatio);
            }

            reservateRefreshed = false;

            startFlag = false;
        }

    }

    private int getTargetCount(String str) {
        int count = 0;
        for (ITeleporter t : teleporterList) {
            if (t.getName().equals(str) && node.coordinate.getDimension() == t.getTeleportCoordinate().getDimension()) {
                count++;
            }
        }
        return count;
    }

    private ITeleporter getTarget(String str) {
        ITeleporter target = null;
        for (ITeleporter t : teleporterList) {
            if (t.getName().equals(str) && node.coordinate.getDimension() == t.getTeleportCoordinate().getDimension()) {
                if (target != null) return null;
                target = t;
            }
        }
        return target;
    }

    private boolean startFlag = false;

    static final byte setNameId = 1;
    static final byte startId = 2;
    static final byte setTargetNameId = 3;
    static final byte setChargePowerId = 4;

    @Override
    public byte networkUnserialize(DataInputStream stream) {

        switch (super.networkUnserialize(stream)) {
            case setNameId:
                if (state != StateIdle) break;
                try {
                    name = stream.readUTF();
                } catch (IOException e) {

                    e.printStackTrace();
                }
                needPublish();
                break;
            case setTargetNameId:
                if (state != StateIdle) break;
                try {
                    targetName = stream.readUTF();
                } catch (IOException e) {

                    e.printStackTrace();
                }
                needPublish();
                break;
            case startId:
                startFlag = true;
                break;
            case setChargePowerId:
                if (state != StateIdle) break;
                try {
                    powerCharge = stream.readFloat();
                } catch (IOException e) {

                    e.printStackTrace();
                }
                needPublish();
                break;
        }
        return unserializeNulldId;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {

        super.networkSerialize(stream);
        try {
            stream.writeUTF(name);
            stream.writeUTF(targetName);
            stream.writeFloat((float) powerCharge);
            stream.writeByte(state);
            stream.writeByte(
                (doorState ? 0x1 : 0x0)
            );
            stream.writeFloat(processRatio);
            stream.writeFloat((float) powerLoad.getU());
            stream.writeFloat((float) energyHit);
            stream.writeFloat((float) energyTarget);
            // stream.writeFloat((float) energyHit);
            // stream.writeFloat((float) energyTarget);

        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    @Override
    public Coordinate getTeleportCoordinate() {
        return descriptor.getTeleportCoordinate(front, node.coordinate);
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Destination"), targetName);
        info.put(I18N.tr("Distance"),
            Utils.plotValue(getTeleportCoordinate().trueDistanceTo(getTarget(targetName).getTeleportCoordinate()), "m"));
        info.put(I18N.tr("Required energy"), Utils.plotEnergy("", energyTarget));
        info.put(I18N.tr("Charge power"), Utils.plotPower("", powerCharge));
        return info;
    }
}
