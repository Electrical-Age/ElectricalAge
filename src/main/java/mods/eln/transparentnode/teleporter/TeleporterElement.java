package mods.eln.transparentnode.teleporter;


import mods.eln.compat.WorldCompat;
import mods.eln.i18n.I18N;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
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
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import mods.eln.sound.SoundCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeleporterElement extends TransparentNodeElement implements ITeleporter {

    TeleporterDescriptor descriptor;
    NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
    Resistor powerResistor = new Resistor(powerLoad, null);
    TeleporterSlowProcess slowProcess = new TeleporterSlowProcess();

    NodePeriodicPublishProcess publisher;
    static public final ArrayList<ITeleporter> teleporterList = new ArrayList<ITeleporter>();

    public TeleporterElement(TransparentNode transparentNode,
                             TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        this.descriptor = (TeleporterDescriptor) descriptor;
        publisher = new NodePeriodicPublishProcess(node, 2, 1);
        powerLoad.isPrivateSubSystem();
        electricalLoadList.add(powerLoad);
        electricalComponentList.add(powerResistor);
        slowProcessList.add(slowProcess);
        slowProcessList.add(publisher);

        teleporterList.add(this);

        Failure exp = new Failure(this).machine();
        slowProcessList.add(voltageWatchdog.set(powerLoad).setUNominal(this.descriptor.cable.electricalNominalVoltage).set(exp));

    }

    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();

    Coordonate lightCoordonate;

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
        return node.maskElectricalPower;
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

        for (Coordonate c : descriptor.getPowerCoordonate(node.coordonate.world())) {
            TeleporterPowerNode n = new TeleporterPowerNode();
            n.setElement(this);
            c.applyTransformation(front, node.coordonate);
            n.onBlockPlacedBy(c, Direction.XN, null, null);

            powerNodeList.add(n);
        }

        lightCoordonate = new Coordonate(this.descriptor.lightCoordonate);
        lightCoordonate.applyTransformation(front, node.coordonate);

        descriptor.ghostDoorClose.newRotate(front).eraseGeo(node.coordonate);
        descriptor.ghostDoorOpen.newRotate(front).plot(node.coordonate, node.coordonate, descriptor.getGhostGroupUuid());

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

    ArrayList<TeleporterPowerNode> powerNodeList = new ArrayList<TeleporterPowerNode>();

    @Override
    public void unload() {
        super.unload();
        unregister();
    }

    void unregister() {
        teleporterList.remove(this);
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side,
                                    float vx, float vy, float vz) {

        return false;
    }

    String name = "Unnamed", targetName = "Unnamed";
    double energyHit = 0;
    double energyTarget = 0;
    double powerCharge = 2000;

    @Override
    public void writeToNBT(CompoundTag nbt) {

        super.writeToNBT(nbt);

        nbt.putString("name", name);
        nbt.putString("targetName", targetName);
        nbt.putDouble("powerCharge", powerCharge);
        nbt.putBoolean("reset", state != StateIdle);

    }

    @Override
    public void readFromNBT(CompoundTag nbt) {

        super.readFromNBT(nbt);

        name = nbt.getStringOr("name", "");
        targetName = nbt.getStringOr("targetName", "");
        powerCharge = nbt.getDoubleOr("powerCharge", 0.0);

        if (nbt.getBooleanOr("reset", false))
            state = StateReset;
    }

    public static final byte StateIdle = 0;
    public static final byte StateStart = 1;
    public static final byte StateClose = 2;
    public static final byte StateCharge = 3;
    public static final byte StateTeleport = 4;
    public static final byte StateOpen = 5;
    public static final byte StateReserved = 6;
    public static final byte StateReset = 7;

    byte state = StateIdle;
    float timeCounter;

    boolean doorState = true;

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
                    descriptor.ghostDoorOpen.newRotate(front).eraseGeo(node.coordonate);
                    break;
                case StateOpen:
                    descriptor.ghostDoorClose.newRotate(front).eraseGeo(node.coordonate);
                    break;
                default:
                    break;
            }

            this.state = state;

            switch (this.state) {
                case StateClose:
                    doorState = false;
                    descriptor.ghostDoorClose.newRotate(front).plot(node.coordonate, node.coordonate, descriptor.getGhostGroupUuid());
                    break;
                case StateOpen:
                    doorState = true;
                    descriptor.ghostDoorOpen.newRotate(front).plot(node.coordonate, node.coordonate, descriptor.getGhostGroupUuid());
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
        if (this.doorState == false && doorState == true) {
            setState(StateOpen);
            // setState(StateReserved);
        }
        if (this.doorState == true && doorState == false) {
            setState(StateClose);
            // setState(StateReserved);
        }
        this.processRatio = processRatio;
    }

    boolean reservateRefreshed = false;

    float processRatio = 0;
    public static final byte eventNoTargetFind = 1;
    public static final byte eventMultipleoTargetFind = 2;
    public static final byte eventTargetFind = 3;
    public static final byte eventSameTarget = 4;
    public static final byte eventNotSameDimensionTarget = 5;
    public static final byte eventTargetBusy = 6;
    public static final byte eventInstablePowerSupply = 7;
    boolean imMaster = false;

    class TeleporterSlowProcess implements IProcess {

        int dx, dy, dz;

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
                    Coordonate c = getTeleportCoordonate();
                    double distance = getTeleportCoordonate().trueDistanceTo(target.getTeleportCoordonate());
                    AABB bb = descriptor.getBB(node.coordonate, front);
                    int playerCount = c.world().getEntitiesOfClass(Player.class, bb).size();
                    int itemCount = c.world().getEntitiesOfClass(ItemEntity.class, bb).size();
                    int petCount = c.world().getEntitiesOfClass(LivingEntity.class, bb).size() - playerCount;
                    // Object o = c.world().getEntitiesOfClass(ItemEntity.class,bb);
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
                    LightBlockEntity.addLight(lightCoordonate, 12, 11);
            }
            switch (state) {
                case StateReserved:
                    if (reservateRefreshed == false) {
                        if (doorState == false)
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

                        if (target.reservate() == false) {
                            sendIdToAllClient(eventTargetBusy);
                            break;
                        }

                        sendIdToAllClient(eventTargetFind);

					/*
                     * AABB bb = descriptor.getBB(node.coordonate,front); List list = node.coordonate.world().getEntitiesOfClass(ItemEntity.class, bb); for(Object o : list){ Entity e = (Entity)o; if(e instanceof EntityPlayerMP) ((EntityPlayerMP)e).setPositionAndUpdate(e.getX() + dx, e.getY() + dy, e.getZ() + dz); else e.setPosition(e.getX() + dx, e.getY() + dy, e.getZ() + dz); }
					 */
                        imMaster = true;
                        setState(StateStart);

                    }
                    break;

                case StateStart: {
                    int count = node.coordonate.world().getEntitiesOfClass(Entity.class, descriptor.getBB(node.coordonate, front)).size();
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
                        AABB bb = descriptor.getBB(node.coordonate, front);
                        List list = node.coordonate.world().getEntitiesOfClass(Entity.class, bb);
                        for (Object o : list) {
                            Entity e = (Entity) o;
                            double failDistance = 1000;
                            while (true) {
                                int x, y, z;
                                x = (int) (e.getX() + (Math.random() * 2 - 1) * failDistance);
                                z = (int) (e.getZ() + (Math.random() * 2 - 1) * failDistance);
                                y = 20;
                                while (!Utils.isAirBlock(WorldCompat.getBlock(e.level(), x, y, z)) && !Utils.isAirBlock(WorldCompat.getBlock(e.level(), x, y + 1, z))) {
                                    y++;
                                }
                                Utils.serverTeleport(e, x + 0.5, y, z + 0.5);
                                break;
                            }
                        }
                        setState(StateOpen);
                    } else {
                        ITeleporter target = getTarget(targetNameCopy);
                        Coordonate c = getTeleportCoordonate();
                        // double distance = getTeleportCoordonate().trueDistanceTo(c);
                        // AABB bb = descriptor.getBB(node.coordonate, front);
                        // int playerCount = c.world().getEntitiesOfClass(Player.class, bb).size();
                        // int itemCount = c.world().getEntitiesOfClass(ItemEntity.class, bb).size();
                        // int petCount = c.world().getEntitiesOfClass(LivingEntity.class, bb).size() - playerCount;
                        // // Object o = c.world().getEntitiesOfClass(ItemEntity.class,bb);
                        // energyTarget = 10000 +
                        // 40000 * playerCount +
                        // 5000 * petCount +
                        // 5000 * itemCount;
                        //
                        // energyTarget *= 1.0 + Math.pow(distance / 250.0, 0.5);

                        energyHit += powerResistor.getP() * time;
                        processRatio = (float) (energyHit / energyTarget);

                        if (energyHit >= energyTarget) {
                            dx = target.getTeleportCoordonate().x - c.x;
                            dy = target.getTeleportCoordonate().y - c.y;
                            dz = target.getTeleportCoordonate().z - c.z;
                            setState(StateTeleport);
                        }
                    }
                }
                break;

                case StateTeleport: {
                    timeCounter += time;
                    if (timeCounter > 0) {

                        AABB bb = descriptor.getBB(node.coordonate, front);
                        List list = node.coordonate.world().getEntitiesOfClass(Entity.class, bb);
                        for (Object o : list) {
                            Entity e = (Entity) o;
                            Utils.serverTeleport(e, e.getX() + dx, e.getY() + dy, e.getZ() + dz);

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

    int getTargetCount(String str) {
        int count = 0;
        for (ITeleporter t : teleporterList) {
            if (t.getName().equals(str) && node.coordonate.dimention == t.getTeleportCoordonate().dimention) {
                count++;
            }
        }
        return count;
    }

    ITeleporter getTarget(String str) {
        ITeleporter target = null;
        for (ITeleporter t : teleporterList) {
            if (t.getName().equals(str) && node.coordonate.dimention == t.getTeleportCoordonate().dimention) {
                if (target != null) return null;
                target = t;
            }
        }
        return target;
    }

    boolean startFlag = false;

    public static final byte setNameId = 1;
    public static final byte startId = 2;
    public static final byte setTargetNameId = 3;
    public static final byte setChargePowerId = 4;

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
    public Coordonate getTeleportCoordonate() {

        return descriptor.getTeleportCoordonate(front, node.coordonate);
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
            Utils.plotValue(getTeleportCoordonate().trueDistanceTo(getTarget(targetName).getTeleportCoordonate()), "m"));
        info.put(I18N.tr("Required energy"), Utils.plotEnergy("", energyTarget));
        info.put(I18N.tr("Charge power"), Utils.plotPower("", powerCharge));
        return info;
    }
}
