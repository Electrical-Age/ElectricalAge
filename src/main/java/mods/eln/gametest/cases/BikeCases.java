package mods.eln.gametest.cases;

import mods.eln.bike.BikePhysics;
import mods.eln.bike.ElectricBike;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.item.belt.BatteryBeltItem;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * The electric bike (Baughn 2026-10-28): its trunk takes a battery in slot 0 and anything in the rest, keeps it over a
 * save, and spills it when the bike is broken up; it parks on its kickstand when you get off and goes over with you
 * when stopped too long; a half block is a step, a full one isn't; the motor draws on the battery as it rides.
 * (Riding itself is client-side: BikePhysicsTest covers its rules.)
 */
public final class BikeCases {
    private BikeCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Trunk());
        cases.add(new ParkAndFall());
        cases.add(new Steps());
        cases.add(new Motor());
        cases.add(new Recipe());
        cases.add(new Charge());
    }

    /** The speedometer's battery (Baughn: show it needs one): the charge the server syncs, NO_BATTERY without one. */
    static final class Charge implements TestCase {
        public String id() {
            return "bike_synced_charge_for_the_speedometer";
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            return 1;
        }

        ElectricBike bike;

        public void build(TestContext ctx) {
            bike = spawn(ctx, ctx.at(1, 1, 1), 0);
        }

        public void measure(TestContext ctx) {
            try {
                bike.serverTick();
                ctx.check("no battery: NO_BATTERY", bike.syncedCharge() == ElectricBike.NO_BATTERY, "" + bike.syncedCharge());
                bike.trunk.setItem(ElectricBike.BATTERY_SLOT, battery(0));
                bike.serverTick();
                ctx.check("a flat battery: 0", bike.syncedCharge() == 0, "" + bike.syncedCharge());
                bike.trunk.setItem(ElectricBike.BATTERY_SLOT, battery(0.62));
                bike.serverTick();
                ctx.check("a battery at 62%: 0.62", Math.abs(bike.syncedCharge() - 0.62f) < 1e-3, "" + bike.syncedCharge());
                var b = BatteryBeltItem.battery(bike.trunk.getItem(0));
                b.setEnergy(bike.trunk.getItem(0), b.getEnergyMax(bike.trunk.getItem(0)) * 0.618);
                bike.serverTick();
                ctx.check("a change under half a percent isn't sent", Math.abs(bike.syncedCharge() - 0.62f) < 1e-3, "" + bike.syncedCharge());
                bike.trunk.setItem(ElectricBike.BATTERY_SLOT, new ItemStack(Items.DIRT));
                bike.serverTick();
                ctx.check("dirt in the slot: NO_BATTERY", bike.syncedCharge() == ElectricBike.NO_BATTERY, "" + bike.syncedCharge());
            } finally {
                bike.discard();
            }
        }
    }

    /** Baughn's recipe: chest (trunk) and leather (saddle) over an iron frame round an electrical motor, two rubber wheels. */
    static final class Recipe implements TestCase {
        public String id() {
            return "bike_recipe";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            ServerLevel w = ctx.level();
            ItemStack rubber = new ItemStack(mods.eln.Eln.itemsByName.get("Rubber")), motor = new ItemStack(mods.eln.Eln.itemsByName.get("Electrical Motor"));
            ItemStack iron = new ItemStack(Items.IRON_INGOT);
            var grid = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.CHEST), new ItemStack(Items.LEATHER), ItemStack.EMPTY,
                iron, motor, iron.copy(),
                rubber, ItemStack.EMPTY, rubber.copy()));
            var found = w.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, w);
            ItemStack out = found.map(h -> h.value().assemble(grid)).orElse(ItemStack.EMPTY);
            ctx.check("chest, leather, iron, an electrical motor and rubber make an electric bike", out.is(ElnRegistry.ELECTRIC_BIKE.get()),
                found.map(h -> h.id().identifier().toString()).orElse("no recipe") + " -> " + out);
        }
    }

    static ElectricBike spawn(TestContext ctx, BlockPos p, float yaw) {
        ServerLevel w = ctx.level();
        ElectricBike b = ElnRegistry.ELECTRIC_BIKE_ENTITY.get().create(w, EntitySpawnReason.TRIGGERED);
        b.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0);
        w.addFreshEntity(b);
        return b;
    }

    /** A rider: a plain mock player, as vanilla's GameTestHelper.makeMockPlayer (NeoForge's fake players can't ride). */
    static net.minecraft.world.entity.player.Player rider(TestContext ctx, String name, ElectricBike near) {
        var p = new net.minecraft.world.entity.player.Player(ctx.level(), new com.mojang.authlib.GameProfile(
            java.util.UUID.nameUUIDFromBytes(("eln-bike-" + name).getBytes()), "bike_" + name)) {
            @Override
            public net.minecraft.world.level.GameType gameMode() {
                return net.minecraft.world.level.GameType.SURVIVAL;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
        p.snapTo(near.getX() + 1, near.getY(), near.getZ(), 0, 0);
        return p;
    }

    static ItemStack battery(double fraction) {
        ItemStack s = new ItemStack(mods.eln.Eln.itemsByName.get("Portable Battery"));
        var b = BatteryBeltItem.battery(s);
        b.setEnergy(s, b.getEnergyMax(s) * fraction);
        return s;
    }

    static final class Trunk implements TestCase {
        public String id() {
            return "bike_trunk_save_and_break";
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            return 2;
        }

        ElectricBike bike;

        public void build(TestContext ctx) {
            bike = spawn(ctx, ctx.at(1, 1, 1), 0);
        }

        public void measure(TestContext ctx) {
            ServerLevel w = ctx.level();
            ctx.check("the bike stands parked when placed", bike.mode() == ElectricBike.Mode.PARKED, "" + bike.mode());
            ItemStack bat = battery(0.5);
            var menu = new mods.eln.bike.BikeTrunkMenu(1, FakePlayerFactory.getMinecraft(w).getInventory());
            ctx.check("the battery slot takes a portable battery, not dirt; the others take anything",
                ElectricBike.accepts(bat) && !ElectricBike.accepts(new ItemStack(Items.DIRT))
                    && !menu.slots.get(0).mayPlace(new ItemStack(Items.DIRT)) && menu.slots.get(1).mayPlace(new ItemStack(Items.DIRT)), "");
            bike.trunk.setItem(ElectricBike.BATTERY_SLOT, bat);
            bike.trunk.setItem(2, new ItemStack(Items.DIRT, 7));
            ctx.check("charge reads the battery's", Math.abs(bike.charge() - 0.5f) < 1e-3, "" + bike.charge());
            // over a save
            var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, w.registryAccess());
            bike.saveWithoutId(out);
            ElectricBike copy = ElnRegistry.ELECTRIC_BIKE_ENTITY.get().create(w, EntitySpawnReason.LOAD);
            copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, w.registryAccess(), out.buildResult()));
            ctx.check("the trunk survives a save", copy.trunk.getItem(2).getCount() == 7 && ElectricBike.accepts(copy.trunk.getItem(0))
                && Math.abs(copy.charge() - 0.5f) < 1e-3, copy.trunk.getItem(2) + " " + copy.trunk.getItem(0));
            // broken up: the bike and what was in it on the ground
            bike.hurtServer(w, w.damageSources().generic(), 100);
            AABB box = new AABB(bike.blockPosition()).inflate(3);
            List<ItemEntity> drops = w.getEntitiesOfClass(ItemEntity.class, box);
            boolean item = drops.stream().anyMatch(e -> e.getItem().is(ElnRegistry.ELECTRIC_BIKE.get()));
            boolean dirt = drops.stream().anyMatch(e -> e.getItem().is(Items.DIRT) && e.getItem().getCount() == 7);
            boolean b = drops.stream().anyMatch(e -> ElectricBike.accepts(e.getItem()));
            ctx.check("broken up, it drops itself and its trunk", bike.isRemoved() && item && dirt && b,
                "removed " + bike.isRemoved() + ", drops " + drops.stream().map(e -> e.getItem().toString()).toList());
            drops.forEach(net.minecraft.world.entity.Entity::discard);
        }
    }

    static final class ParkAndFall implements TestCase {
        public String id() {
            return "bike_kickstand_and_tip_over";
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            return 1;
        }

        ElectricBike bike;

        public void build(TestContext ctx) {
            bike = spawn(ctx, ctx.at(1, 1, 1), 0);
        }

        public void measure(TestContext ctx) {
            var fp = rider(ctx, "a", bike);
            var fp2 = rider(ctx, "b", bike);
            var fp3 = rider(ctx, "c", bike);
            try {
                boolean rode = fp.startRiding(bike);
                ctx.check("a player gets on; the kickstand comes up", rode && bike.mode() == ElectricBike.Mode.RIDDEN, rode + " " + bike.mode());
                fp.stopRiding();
                ctx.check("off again, it stands on its kickstand", bike.mode() == ElectricBike.Mode.PARKED && !bike.isVehicle(), "" + bike.mode());
                fp = fp2;
                fp.startRiding(bike);
                // standing still with a rider: nothing for 5 s, then 10% a second (Baughn)
                int fell = -1;
                for (int t = 0; t < 2000 && fell < 0; t++) {
                    bike.serverTick();
                    if (bike.mode() == ElectricBike.Mode.FALLEN) fell = t + 1;
                }
                ctx.check("stopped with a rider, it goes over, but not in the first 5 s", fell >= ElectricBike.STILL_TICKS, "after " + fell + " ticks");
                ctx.check("and the rider comes off with it", !bike.isVehicle() && fp.getVehicle() == null, "");
                ctx.check("fallen, nobody can get on", !fp3.startRiding(bike), "");
                bike.interact(fp, InteractionHand.MAIN_HAND, bike.position());
                ctx.check("a right-click stands it up again", bike.mode() == ElectricBike.Mode.PARKED, "" + bike.mode());
            } finally {
                fp.stopRiding();
                fp3.stopRiding();
                bike.discard();
            }
        }
    }

    static final class Steps implements TestCase {
        public String id() {
            return "bike_half_steps_ride_full_blocks_stop";
        }

        // (an existing structure size, floor_10x5x10, with the blocks inside it)
        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
            ctx.level().setBlockAndUpdate(ctx.at(1, 1, 3), Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
            ctx.level().setBlockAndUpdate(ctx.at(4, 1, 3), Blocks.STONE.defaultBlockState());
        }

        String last = "";

        /** Push a bike from z = 1 south over the block at z = 3 for 6 blocks; return how far it got. */
        double ride(TestContext ctx, int x) {
            BlockPos s = ctx.at(x, 1, 1);
            ElectricBike b = spawn(ctx, s, 0);
            try {
                for (int t = 0; t < 30; t++) {
                    b.setOnGround(true);
                    b.move(MoverType.SELF, new Vec3(0, 0, 0.2));
                    b.move(MoverType.SELF, new Vec3(0, -0.1, 0));
                }
                last = "went " + (b.getZ() - s.getZ() - 0.5);
                return b.getZ() - s.getZ() - 0.5;
            } finally {
                b.discard();
            }
        }

        public void measure(TestContext ctx) {
            double slab = ride(ctx, 1);
            String a = last;
            double stone = ride(ctx, 4);
            ctx.check("a half slab is a step it rides over", slab > 5.9, a);
            ctx.check("a full block stops it", stone < 1.2, last);
            ctx.check("its step height is under a block", BikePhysics.STEP_HEIGHT < 1, "");
        }
    }

    static final class Motor implements TestCase {
        public String id() {
            return "bike_motor_draws_on_its_battery";
        }

        public int width() {
            return 4;
        }

        public int ticks() {
            return 1;
        }

        ElectricBike bike;

        public void build(TestContext ctx) {
            bike = spawn(ctx, ctx.at(1, 1, 1), 0);
        }

        public void measure(TestContext ctx) {
            var fp = rider(ctx, "motor", bike);
            try {
                bike.serverTick();
                ctx.check("no battery, no power", !bike.powered(), "");
                ItemStack bat = battery(1);
                var bb = BatteryBeltItem.battery(bat);
                bike.trunk.setItem(ElectricBike.BATTERY_SLOT, bat);
                bike.serverTick();
                ctx.check("a charged battery powers it", bike.powered(), "");
                fp.startRiding(bike);
                double e0 = bb.getEnergy(bike.trunk.getItem(0));
                // 10 s at full speed (the rider's client would move it; here the test does)
                for (int t = 0; t < 200; t++) {
                    bike.setPos(bike.getX(), bike.getY(), bike.getZ() + BikePhysics.VMAX_POWERED);
                    bike.serverTick();
                }
                double used = e0 - bb.getEnergy(bike.trunk.getItem(0));
                ctx.check("10 s at full speed takes ~1 kJ (100 W)", Math.abs(used - 1000) < 60, used + " J");
                bb.setEnergy(bike.trunk.getItem(0), 0);
                bike.serverTick();
                ctx.check("flat, it's pedalling only", !bike.powered(), "");
            } finally {
                fp.stopRiding();
                bike.discard();
            }
        }
    }
}
