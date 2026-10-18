package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.Direction;
import mods.eln.transparentnode.turret.TurretContainer;
import mods.eln.transparentnode.turret.TurretElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * The sentry turret as intended (Baughn 2026-10-18: a GameTest that it works): powered at its nominal 800 V it arms,
 * sweeps its +-70 degree arc, and shoots (5 damage per 1 kJ shot) the living things its filter names; with "spare" it
 * shoots everything except them; with no filter it shoots nothing; never through a solid block, never behind it, and
 * never without power. Six turrets in the 58 x 42 floor, far enough apart (> 12 blocks, the detection range) that each
 * only sees its own targets. Targets are husks (monsters that don't burn in daylight) and cows, without AI.
 */
public final class TurretCases {
    private TurretCases() {
    }

    static final int TURRET = 0 + (66 << 6);
    static final int SOURCE = 0 + (3 << 6);

    public static void addTo(List<TestCase> cases) {
        cases.add(new SentryCase());
    }

    static EntitySensorFilterDescriptor filter(Class<?> c) {
        for (int i = 0; i < 64 * 128; i++)
            if (Eln.sharedItem.getDescriptor(i) instanceof EntitySensorFilterDescriptor f && f.entityClass == c) return f;
        throw new IllegalStateException("no entity filter for " + c);
    }

    static double u(TurretElement t) {
        mods.eln.sim.ElectricalLoad l = Batch1Cases.field(t, "load");
        return l.getU();
    }

    /** One turret and its targets. */
    static final class Post {
        final String what;
        TurretElement turret;
        final List<Mob> shoot = new ArrayList<>(), spare = new ArrayList<>();

        Post(String what) {
            this.what = what;
        }
    }

    static final class SentryCase implements TestCase {
        final List<Post> posts = new ArrayList<>();

        public String id() {
            return "turret_sentry";
        }

        public int width() {
            return 58;
        }

        public int depth() {
            return 42;
        }

        public int floorDepth() {
            return 42;
        }

        public int ticks() {
            return 400;   // (arming, a sweep of the arc at 40 deg/s, a 1 kJ charge at 1 kW: a few seconds per shot)
        }

        /** A mob without AI at the turret's position + forward * f + left * l (blocks). */
        Mob mob(TestContext ctx, Post p, BlockPos at, Direction front, int f, int l, boolean husk) {
            net.minecraft.core.Direction fw = front.toEnumFacing(), left = fw.getCounterClockWise();
            BlockPos q = at.relative(fw, f).relative(left, l);
            Mob m = (husk ? EntityTypes.HUSK : EntityTypes.COW).create(ctx.level(), EntitySpawnReason.COMMAND);
            m.setPos(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
            m.setNoAi(true);
            m.setPersistenceRequired();
            ctx.level().addFreshEntity(m);
            return m;
        }

        Post post(TestContext ctx, String what, int x, int z, boolean powered, Class<?> filterClass, boolean spare) {
            Post p = new Post(what);
            BlockPos at = ctx.at(x, 1, z);
            p.turret = (TurretElement) ctx.placeTransparent(TURRET, at);
            Direction front = p.turret.front;
            if (powered) {
                mods.eln.sixnode.electricalsource.ElectricalSourceElement s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement)
                    ctx.placeSix(SOURCE, at.relative(front.back().toEnumFacing()));
                s.networkUnserialize(ctx.stream(out -> {
                    out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                    out.writeFloat(800);
                }));
            }
            if (filterClass != null) {
                p.turret.getInventory().setItem(TurretContainer.filterId, filter(filterClass).newItemStack());
                p.turret.getInventory().setChanged();
            }
            p.turret.filterIsSpare = spare;
            posts.add(p);
            return p;
        }

        public void build(TestContext ctx) {
            Class<?> monster = net.minecraft.world.entity.monster.Enemy.class;
            // 1. monster filter: the husk in front is shot; the cow (also in the arc) is not
            Post a = post(ctx, "monster filter", 6, 8, true, monster, false);
            a.shoot.add(mob(ctx, a, a.turret.node.coordonate.pos(), a.turret.front, 5, 0, true));
            a.spare.add(mob(ctx, a, a.turret.node.coordonate.pos(), a.turret.front, 4, 4, false));
            // 2. monster filter, spare: the cow is shot, the husk is not
            Post b = post(ctx, "monster filter, spare", 26, 8, true, monster, true);
            b.shoot.add(mob(ctx, b, b.turret.node.coordonate.pos(), b.turret.front, 4, 4, false));
            b.spare.add(mob(ctx, b, b.turret.node.coordonate.pos(), b.turret.front, 5, 0, true));
            // 3. no filter: nothing is shot
            Post c = post(ctx, "no filter", 46, 8, true, null, false);
            c.spare.add(mob(ctx, c, c.turret.node.coordonate.pos(), c.turret.front, 5, 0, true));
            // 4. a stone wall between: not shot
            Post d = post(ctx, "behind a wall", 6, 30, true, monster, false);
            BlockPos dp = d.turret.node.coordonate.pos();
            net.minecraft.core.Direction dfw = d.turret.front.toEnumFacing();
            for (int l = -2; l <= 2; l++)
                for (int y = 0; y < 3; y++) ctx.level().setBlockAndUpdate(dp.relative(dfw, 3).relative(dfw.getClockWise(), l).above(y), Blocks.STONE.defaultBlockState());
            d.spare.add(mob(ctx, d, dp, d.turret.front, 5, 0, true));
            // 5. behind the turret (outside its arc): not shot
            Post e = post(ctx, "behind it", 26, 30, true, monster, false);
            e.spare.add(mob(ctx, e, e.turret.node.coordonate.pos(), e.turret.front, -4, 0, true));
            // 6. no power: not shot
            Post f = post(ctx, "unpowered", 46, 30, false, monster, false);
            f.spare.add(mob(ctx, f, f.turret.node.coordonate.pos(), f.turret.front, 5, 0, true));
        }

        public void measure(TestContext ctx) {
            for (Post p : posts) {
                for (Mob m : p.shoot)
                    ctx.check("turret (" + p.what + ") shoots the " + m.getType().getDescriptionId(), m.getHealth() < m.getMaxHealth() || m.isDeadOrDying(),
                        String.format("health %.1f of %.1f, turret U %.0f V, enabled %s, angle %.0f", m.getHealth(), m.getMaxHealth(),
                            u(p.turret), p.turret.isEnabled(), p.turret.getTurretAngle()));
                for (Mob m : p.spare)
                    ctx.check("turret (" + p.what + ") spares the " + m.getType().getDescriptionId(), m.getHealth() == m.getMaxHealth() && !m.isRemoved(),
                        String.format("health %.1f of %.1f, turret U %.0f V, enabled %s", m.getHealth(), m.getMaxHealth(), u(p.turret), p.turret.isEnabled()));
            }
            Post powered = posts.get(0), off = posts.get(5);
            ctx.check("a powered turret arms (800 V)", powered.turret.isEnabled(), String.format("U %.0f V", u(powered.turret)));
            ctx.check("an unpowered one doesn't", !off.turret.isEnabled(), String.format("U %.0f V", u(off.turret)));
            for (Post p : posts) for (Mob m : p.shoot) m.discard();
            for (Post p : posts) for (Mob m : p.spare) m.discard();
        }
    }
}
