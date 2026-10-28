package mods.eln.bike;

import mods.eln.item.belt.BatteryBeltItem;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The electric bike (Baughn 2026-10-28: "Not a motorcycle"). Ridden like a horse, but the rider's client moves it
 * (the boat's way: whoever controls it simulates it and the server takes its moves); BikePhysics has the rules. Off
 * the bike it stands on its kickstand where it was left; stopped too long with you on it, it falls over with you; run
 * into a full block or a wall fast and you're thrown (the client says so with BikeCrash). A trunk on the back holds a
 * battery and three things; shift-right-click (or the inventory key while riding) opens it.
 */
public class ElectricBike extends VehicleEntity implements PlayerRideableJumping, HasCustomInventoryScreen {
    public enum Mode { RIDDEN, PARKED, FALLEN }

    static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(ElectricBike.class, EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Boolean> DATA_POWERED = SynchedEntityData.defineId(ElectricBike.class, EntityDataSerializers.BOOLEAN);
    /** The battery's charge for the rider's speedometer: 0..1, NO_BATTERY when the slot is empty (Baughn: show it needs one). */
    static final EntityDataAccessor<Float> DATA_CHARGE = SynchedEntityData.defineId(ElectricBike.class, EntityDataSerializers.FLOAT);
    public static final float NO_BATTERY = -1;

    /** The trunk: slot 0 the battery, 1-3 anything (Baughn). */
    public static final int TRUNK_SLOTS = 4, BATTERY_SLOT = 0;
    /** Stopped this long with a rider (ticks), then each second a TIP_CHANCE it goes over (Baughn: 5 s, 10%/s). */
    public static volatile int STILL_TICKS = 100;
    public static volatile float TIP_CHANCE = 0.1f;
    /** Slower than this (b/t) counts as stopped: 0.05 b/s. */
    static final double STOPPED = 0.0025;
    /** The wheels' radius in blocks, for turning them with the distance rolled. */
    public static final float WHEEL_RADIUS = 0.34f;

    public final SimpleContainer trunk = new SimpleContainer(TRUNK_SLOTS);
    final BikePhysics physics = new BikePhysics();
    /** Client, controlling: the next jump's scale (0 none). */
    float pendingJump;
    int jumpCooldown;
    /** Server: ticks ridden while stopped; the position last tick; this tick's speed (b/t). */
    int stillTicks;
    /** Server: ticks until the no-charge hint, after mounting. */
    int hintTicks;
    static final int HINT_DELAY = 65;
    /** Water deeper than this (blocks) throws the rider off: above the hubs. */
    static final double DEEP_WATER = 0.5;
    double lastX = Double.NaN, lastZ;
    double serverSpeed;
    /** Client: wheel turn (radians) now and last tick; the kickstand's fold (0 down, 1 up) now and last tick. */
    public float wheel, wheelO, stand, standO;

    /** Client, every tick: the bike's motor sound (ElnClient sets it; nothing on a server). */
    public static java.util.function.Consumer<ElectricBike> clientSound = b -> {
    };
    /** Client: tell the server the rider ran into something (ElnClient sets it to send BikeCrash). */
    public static java.util.function.Consumer<ElectricBike> clientCrash = b -> {
    };

    public ElectricBike(EntityType<? extends ElectricBike> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(DATA_MODE, (byte) Mode.PARKED.ordinal());
        data.define(DATA_POWERED, false);
        data.define(DATA_CHARGE, NO_BATTERY);
    }

    public Mode mode() {
        return Mode.values()[Math.floorMod(entityData.get(DATA_MODE), Mode.values().length)];
    }

    public void setMode(Mode m) {
        entityData.set(DATA_MODE, (byte) m.ordinal());
    }

    /** Whether the battery in the trunk has charge to give (the server decides; both sides ride by it). */
    public boolean powered() {
        return entityData.get(DATA_POWERED);
    }

    /** The charge as the server last sent it: 0..1, or NO_BATTERY. */
    public float syncedCharge() {
        return entityData.get(DATA_CHARGE);
    }

    /** Server: send the charge when it moved by half a percent, or crossed empty or the battery came or went. */
    public void syncCharge() {
        float now = accepts(trunk.getItem(BATTERY_SLOT)) ? charge() : NO_BATTERY, was = syncedCharge();
        if (now != was && (Math.abs(now - was) >= 0.005f || now <= 0 || was <= 0 || now >= 1)) entityData.set(DATA_CHARGE, now);
    }

    @Override
    protected Item getDropItem() {
        return ElnRegistry.ELECTRIC_BIKE.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getDropItem());
    }

    @Override
    public float maxUpStep() {
        return BikePhysics.STEP_HEIGHT;
    }

    @Override
    protected double getDefaultGravity() {
        return BikePhysics.GRAVITY;
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return net.minecraft.world.entity.LinearInterpolationHandler.create(this, 3);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    /** It stands where it was left: nobody pushes it about (Baughn: on its block until moved). */
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        if (getHurtTime() > 0) setHurtTime(getHurtTime() - 1);
        if (getDamage() > 0) setDamage(getDamage() - 1);
        super.tick();
        if (jumpCooldown > 0) jumpCooldown--;
        if (isLocalInstanceAuthoritative()) drive();
        else setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide()) clientTick();
        else serverTick();
    }

    /** On the side that moves it: the rider's input into speed, steering and jumps (client); else it stands or lies. */
    void drive() {
        double vy = getDeltaMovement().y, forward = 0;
        if (level().isClientSide() && mode() == Mode.RIDDEN && getControllingPassenger() instanceof Player p) {
            setYRot(BikePhysics.turn(getYRot(), p.getYRot()));
            float in = p.zza > 0.01f ? 1 : p.zza < -0.01f ? -1 : 0;
            forward = physics.speed(in, powered());
            if (isInWater()) forward *= 0.3;
            if (pendingJump > 0 && onGround()) {
                vy = BikePhysics.jumpVelocity(pendingJump);
                jumpCooldown = 10;
                playSound(SoundEvents.CHAIN_PLACE, 0.6f, 0.7f);
            }
            pendingJump = 0;
        } else {
            physics.reset();
        }
        float yr = getYRot() * Mth.DEG_TO_RAD;
        setDeltaMovement(-Mth.sin(yr) * forward, vy, Mth.cos(yr) * forward);
        move(MoverType.SELF, getDeltaMovement());
        if (level().isClientSide() && horizontalCollision && BikePhysics.crashes(forward)) {
            physics.reset();
            clientCrash.accept(this);
        }
        setDeltaMovement(0, (getDeltaMovement().y - BikePhysics.GRAVITY) * BikePhysics.AIR_DRAG, 0);
    }

    void clientTick() {
        // the wheels turn with the ground covered, forwards or back (on every client: positions are what they see)
        wheelO = wheel;
        double dx = getX() - xo, dz = getZ() - zo;
        float yr = getYRot() * Mth.DEG_TO_RAD;
        double along = -Mth.sin(yr) * dx + Mth.cos(yr) * dz;
        wheel += (float) (along / WHEEL_RADIUS);
        standO = stand;
        stand = Mth.approach(stand, mode() == Mode.RIDDEN ? 1 : 0, 0.25f);
        clientSound.accept(this);
    }

    /** The server side of a tick: speed from the moves it was given, the tip-over chance, the motor's draw (public for GameTests). */
    public void serverTick() {
        if (Double.isNaN(lastX)) {
            lastX = getX();
            lastZ = getZ();
        }
        serverSpeed = Math.min(1, Math.hypot(getX() - lastX, getZ() - lastZ));
        lastX = getX();
        lastZ = getZ();
        if (mode() == Mode.RIDDEN && getControllingPassenger() == null) setMode(Mode.PARKED);
        // getting on without charge: a quiet word over the hotbar (the speedometer shows it too), once vanilla's
        // "press shift to dismount" (60 ticks) is gone, or it overwrote ours (AdventurAgent)
        if (hintTicks > 0 && --hintTicks == 0 && mode() == Mode.RIDDEN && getControllingPassenger() instanceof Player p
            && syncedCharge() <= 0)
            p.sendOverlayMessage(Component.translatable(syncedCharge() < 0 ? "message.eln.electric_bike.no_battery"
                : "message.eln.electric_bike.flat_battery").withStyle(net.minecraft.ChatFormatting.GRAY));
        // water over the hubs: you come off and it goes down (AdventurAgent found it riding along a pool's bottom)
        if (mode() == Mode.RIDDEN && getFluidHeight(net.minecraft.tags.FluidTags.WATER) > DEEP_WATER) fall();
        // stopped with you on it: after 5 s, 10% a second that it goes over (Baughn)
        if (mode() == Mode.RIDDEN && serverSpeed < STOPPED) {
            stillTicks++;
            if (stillTicks >= STILL_TICKS && (stillTicks - STILL_TICKS) % 20 == 0 && random.nextFloat() < TIP_CHANCE) fall();
        } else {
            stillTicks = 0;
        }
        drawPower();
    }

    /** The motor's draw from the trunk's battery (server), by the speed it went this tick; POWERED for both sides. */
    void drawPower() {
        ItemStack s = trunk.getItem(BATTERY_SLOT);
        IItemEnergyBattery b = accepts(s) ? BatteryBeltItem.battery(s) : null;
        boolean has = b != null && b.getEnergy(s) > 1 && b.getDischagePower(s) > 0;
        if (has && mode() == Mode.RIDDEN && serverSpeed > STOPPED) {
            double joules = Math.min(BikePhysics.motorWatts(serverSpeed), b.getDischagePower(s)) / 20;
            b.setEnergy(s, Math.max(0, b.getEnergy(s) - joules));
            has = b.getEnergy(s) > 1;
        }
        if (has != powered()) entityData.set(DATA_POWERED, has);
        syncCharge();
    }

    /** It goes over (server): the rider comes off and it lies on its side until stood up. */
    public void fall() {
        if (level().isClientSide() || mode() == Mode.FALLEN) return;
        ejectPassengers();
        setMode(Mode.FALLEN);
        stillTicks = 0;
        playSound(SoundEvents.ANVIL_LAND, 0.25f, 1.7f);
        playSound(SoundEvents.CHAIN_BREAK, 0.8f, 0.9f);
    }

    /** BikeCrash from a client: only its rider can throw itself off. */
    public static void onCrash(ServerPlayer player, int entityId) {
        if (player.level().getEntity(entityId) instanceof ElectricBike b && b.getControllingPassenger() == player) b.fall();
    }

    // ---------------------------------------------------------------- riding

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult sup = super.interact(player, hand, location);
        if (sup != InteractionResult.PASS) return sup;
        if (mode() == Mode.FALLEN) {   // stand it back up
            if (!level().isClientSide()) {
                setMode(Mode.PARKED);
                playSound(SoundEvents.CHAIN_PLACE, 0.8f, 1.2f);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            openCustomInventoryScreen(player);
            return InteractionResult.SUCCESS;
        }
        if (isVehicle()) return InteractionResult.PASS;
        if (!level().isClientSide() && !player.startRiding(this)) return InteractionResult.PASS;
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && mode() != Mode.FALLEN;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level().isClientSide()) {
            setMode(Mode.RIDDEN);
            stillTicks = 0;
            playSound(SoundEvents.CHAIN_PLACE, 0.5f, 1.5f);   // (the kickstand folds)
            syncCharge();
            hintTicks = HINT_DELAY;
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        physics.reset();
        if (!level().isClientSide() && mode() == Mode.RIDDEN) {
            setMode(Mode.PARKED);
            playSound(SoundEvents.CHAIN_PLACE, 0.6f, 1.3f);   // (the kickstand goes down)
        }
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    /** On the seat: above the frame, a little behind the middle. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0, SEAT_Y, -0.15).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    /** Where the attachment puts the rider's feet (with the player's 0.6 vehicle attachment): hips on the saddle. */
    public static final double SEAT_Y = 0.8;

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (passenger instanceof LivingEntity l) l.setYBodyRot(getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // to the side the rider looks, as a boat; else on top
        for (float side : new float[]{90, -90, 180}) {
            Vec3 dir = getCollisionHorizontalEscapeVector(getBbWidth() * Mth.SQRT_OF_TWO, passenger.getBbWidth(), getYRot() + side);
            BlockPos at = BlockPos.containing(getX() + dir.x, getBoundingBox().maxY, getZ() + dir.z);
            for (BlockPos p : new BlockPos[]{at, at.below()}) {
                double floor = level().getBlockFloorHeight(p);
                if (!DismountHelper.isBlockFloorValid(floor)) continue;
                Vec3 target = new Vec3(getX() + dir.x, p.getY() + floor, getZ() + dir.z);
                for (Pose pose : passenger.getDismountPoses())
                    if (DismountHelper.canDismountTo(level(), target, passenger, pose)) {
                        passenger.setPose(pose);
                        return target;
                    }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    // ---------------------------------------------------------------- jumping (the horse's charge bar)

    @Override
    public void onPlayerJump(int amount) {
        if (amount > 0 && mode() == Mode.RIDDEN) pendingJump = getPlayerJumpPendingScale(amount);
    }

    @Override
    public boolean canJump() {
        return mode() == Mode.RIDDEN;
    }

    @Override
    public void handleStartJump(int jumpScale) {
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public int getJumpCooldown() {
        return jumpCooldown;
    }

    // ---------------------------------------------------------------- the trunk

    /** The battery slot takes EA's item batteries (portable batteries, packs, condensators, a battery belt). */
    public static boolean accepts(ItemStack s) {
        return BatteryBeltItem.accepts(s) || s.getItem() instanceof BatteryBeltItem;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (player instanceof ServerPlayer sp && isAlive())
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new BikeTrunkMenu(id, inv, this),
                Component.translatable("container.eln.electric_bike")));
    }

    /** The battery's charge, 0..1 (0 without one). */
    public float charge() {
        ItemStack s = trunk.getItem(BATTERY_SLOT);
        IItemEnergyBattery b = accepts(s) ? BatteryBeltItem.battery(s) : null;
        if (b == null || b.getEnergyMax(s) <= 0) return 0;
        return (float) Math.max(0, Math.min(1, b.getEnergy(s) / b.getEnergyMax(s)));
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // broken up (or punched away in creative), it spills its trunk, as a chest boat does
        if (!level().isClientSide() && reason.shouldDestroy()) Containers.dropContents(level(), this, trunk);
        super.remove(reason);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("mode", mode() == Mode.RIDDEN ? Mode.PARKED.ordinal() : mode().ordinal());
        net.minecraft.world.ContainerHelper.saveAllItems(out, trunk.getItems());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        setMode(Mode.values()[Math.floorMod(in.getIntOr("mode", Mode.PARKED.ordinal()), Mode.values().length)]);
        net.minecraft.world.ContainerHelper.loadAllItems(in, trunk.getItems());
    }
}
