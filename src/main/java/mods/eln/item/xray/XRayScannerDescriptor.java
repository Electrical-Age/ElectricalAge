package mods.eln.item.xray;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.gl.GL11;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;
import mods.eln.wiki.Data;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * X-Ray Scanner, recreated (Baughn 2026-10-11; the 1.12 one was broken): a held device with a small screen showing a
 * ray-marched picture of the ores ahead, chosen by 4 coloured filter slots (ore blocks or raw chunks; sneak + use
 * opens them). Use switches it on (boot 4 s) and off (1 s); it draws 300 W while on (10 kJ, charged at 400 W like
 * other portable items). The picture is computed on the client of the player holding it (client XRayScan) and
 * looked up here by the stack's id; other players' scanners show only the LED and status text.
 */
public class XRayScannerDescriptor extends GenericItemUsingDamageDescriptor implements IItemEnergyBattery {
    public enum State {Idle, Boot, Run, Stop}

    static final short BOOT_TIME = 4 * 20, STOP_TIME = 20;
    public static final int RES_W = 48, RES_H = 30;

    final Obj3D obj;
    final Obj3D.Obj3DPart base, led, buttons, textRun, textInit;
    final Obj3D.Obj3DPart[] textBat;
    final double energyStorage, chargePower, dischargePower;
    public final float range;
    public final float fov;

    public XRayScannerDescriptor(String name, Obj3D obj, double energyStorage, double chargePower, double dischargePower,
                                 float range, float fov) {
        super(name);
        this.obj = obj;
        this.energyStorage = energyStorage;
        this.chargePower = chargePower;
        this.dischargePower = dischargePower;
        this.range = range;
        this.fov = fov;
        base = part("Base");
        led = part("Led");
        buttons = part("Buttons");
        textRun = part("TextRun");
        textInit = part("TextInit");
        textBat = new Obj3D.Obj3DPart[4];
        for (int i = 0; i < 4; i++) textBat[i] = part("TextBat" + i);
    }

    Obj3D.Obj3DPart part(String n) {
        return obj == null ? null : obj.getPart(n);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addPortable(newItemStack());
    }

    @Override
    public CompoundTag getDefaultNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble("e", energyStorage * 0.2);
        nbt.putByte("s", (byte) State.Idle.ordinal());
        nbt.putShort("c", (short) 0);
        nbt.putLong("id", newId());
        return nbt;
    }

    static long newId() {
        return java.util.concurrent.ThreadLocalRandom.current().nextLong();
    }

    public State getState(ItemStack s) {
        int i = getNbt(s).getByteOr("s", (byte) 0);
        return i >= 0 && i < State.values().length ? State.values()[i] : State.Idle;
    }

    void setState(ItemStack s, State st, short counter) {
        editNbt(s, n -> {
            n.putByte("s", (byte) st.ordinal());
            n.putShort("c", counter);
        });
    }

    /** The stack's identity for the client's picture (0: none yet; the server assigns one on the first tick). */
    public static long id(ItemStack s) {
        CompoundTag n = mods.eln.generic.ItemNbt.get(s);
        return n == null ? 0 : n.getLongOr("id", 0L);
    }

    @Override
    public void addInformation(ItemStack stack, Player player, List list, boolean par4) {
        super.addInformation(stack, player, list, par4);
        list.add(tr("Shows the ores you choose through rock, %1$ blocks ahead.", (int) range));
        list.add(tr("Use: on/off. Sneak + use: choose the ores (4 slots: ore blocks or raw chunks)."));
        list.add(tr("Power: %1$W", (int) dischargePower));
    }

    @Override
    public void onUpdate(ItemStack stack, Level world, Entity entity, int slot, boolean held) {
        if (world.isClientSide()) return;
        if (id(stack) == 0) editNbt(stack, n -> n.putLong("id", newId()));   // a new stack gets its default tag here: 20 % charge
        State st = getState(stack);
        if (st != State.Boot && st != State.Stop) return;
        short c = getNbt(stack).getShortOr("c", (short) 0);
        if (--c > 0) editNbt(stack, n -> n.putShort("c", (short) (n.getShortOr("c", (short) 1) - 1)));
        else setState(stack, st == State.Boot ? State.Run : State.Idle, (short) 0);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack s, Level w, Player p) {
        if (w.isClientSide()) return s;
        if (p.isShiftKeyDown()) {
            if (p instanceof ServerPlayer sp) {
                int slot = p.getMainHandItem() == s ? p.getInventory().getSelectedSlot() : XRayScannerMenu.OFFHAND;
                sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new XRayScannerMenu(id, inv, slot),
                    Component.literal(name)), buf -> buf.writeInt(slot));
            }
            return s;
        }
        switch (getState(s)) {
            case Idle -> {
                if (getEnergy(s) > dischargePower) editNbt(s, n -> {
                    n.putByte("s", (byte) State.Boot.ordinal());
                    n.putShort("c", BOOT_TIME);
                    n.putInt("boots", n.getIntOr("boots", 0) + 1);   // a new look for the boot bar each time
                });
            }
            case Run -> setState(s, State.Stop, STOP_TIME);
            default -> {
            }
        }
        return s;
    }

    // ---- energy (1.12)

    @Override
    public double getEnergy(ItemStack stack) {
        return getNbt(stack).getDoubleOr("e", 0.0);
    }

    @Override
    public void setEnergy(ItemStack stack, double value) {
        editNbt(stack, n -> n.putDouble("e", value));
    }

    @Override
    public double getEnergyMax(ItemStack stack) {
        return energyStorage;
    }

    @Override
    public double getChargePower(ItemStack stack) {
        return chargePower;
    }

    @Override
    public double getDischagePower(ItemStack stack) {
        return 0;
    }

    @Override
    public int getPriority(ItemStack stack) {
        return 0;
    }

    @Override
    public void electricalItemUpdate(ItemStack stack, double time) {
        if (getState(stack) == State.Idle) return;
        double e = getEnergy(stack) - dischargePower * time;
        if (e <= 0) {
            setState(stack, State.Idle, (short) 0);
            setEnergy(stack, 0);
        } else {
            setEnergy(stack, e);
        }
    }

    // ---- drawing

    /** Client: the latest picture for a scanner id (RES_W * RES_H * 3 floats), or null. Set by the client scan. */
    public static java.util.function.LongFunction<float[]> pictures = id -> null;

    @Override
    public boolean hasItemDraw() {
        return obj != null;
    }

    @Override
    public String itemDrawParent() {
        return "eln:item/device_obj";
    }

    @Override
    public void drawItem(ItemStack stack, boolean entity) {
        State st = getState(stack);
        GL11.glTranslatef(-0.5f, 0f, 0f); // the model spans x 0..1 around the item centre
        base.draw();
        float[] pic = st == State.Run ? pictures.apply(id(stack)) : null;
        if (pic != null) drawPicture(pic);
        UtilsClient.disableLight();
        float r = 0.5f, g = 0.5f, b = 0.5f;
        switch (st) {
            case Boot -> {
                r = 0.9f;
                g = 0.4f;
                b = 0f;
            }
            case Run -> {
                r = 0f;
                g = 1f;
                b = 0f;
            }
            case Stop -> {
                r = 1f;
                g = 0f;
                b = 0f;
            }
            default -> {
            }
        }
        GL11.glColor4f(r, g, b, 1f);
        if (led != null) led.draw();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        if (buttons != null) buttons.draw();
        UtilsClient.enableBlend();
        GL11.glColor4f(1f, 1f, 1f, 0.6f);
        if (st == State.Boot && textInit != null) textInit.draw();
        if (st == State.Boot) drawBootBar(bootFill(stack));
        if (st == State.Run && textRun != null) {
            if (pic == null) textRun.draw();
            int bat = (int) Math.min(textBat.length - 1, getEnergy(stack) / energyStorage * textBat.length + 0.5);
            if (textBat[bat] != null) textBat[bat].draw();
        }
        UtilsClient.disableBlend();
        UtilsClient.enableLight();
        UtilsClient.glDefaultColor();
    }

    /** The boot progress bar's fill (0..1): see XRayBoot. */
    public double bootFill(ItemStack stack) {
        CompoundTag n = getNbt(stack);
        double t = XRayBoot.time(n.getShortOr("c", BOOT_TIME), BOOT_TIME);
        return XRayBoot.fill(t, id(stack) * 31 + n.getIntOr("boots", 0));
    }

    /** The boot progress bar on the screen, above the boot text (screen top is x 0.9051, the text row x 0.625..0.656). */
    static void drawBootBar(double fill) {
        float xa = 0.735f, xb = 0.700f, za = -0.21875f, zb = 0.21875f, y = 0.1652f, m = 0.006f;
        float zf = (float) (za + m + (zb - za - 2 * m) * Math.max(0, Math.min(1, fill)));
        UtilsClient.disableTexture();
        UtilsClient.disableCulling();
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glNormal3f(0, 1, 0);
        GL11.glColor4f(0.15f, 0.15f, 0.15f, 0.8f);   // the empty bar
        GL11.glVertex3f(xa, y, za);
        GL11.glVertex3f(xa, y, zb);
        GL11.glVertex3f(xb, y, zb);
        GL11.glVertex3f(xb, y, za);
        if (fill > 0) {
            GL11.glColor4f(0.95f, 0.55f, 0.1f, 1f);   // the boot LED's orange
            GL11.glVertex3f(xa - m, y + 0.0004f, za + m);
            GL11.glVertex3f(xa - m, y + 0.0004f, zf);
            GL11.glVertex3f(xb + m, y + 0.0004f, zf);
            GL11.glVertex3f(xb + m, y + 0.0004f, za + m);
        }
        GL11.glEnd();
        UtilsClient.enableCulling();
        UtilsClient.enableTexture();
        GL11.glColor4f(1f, 1f, 1f, 0.6f);
    }

    /** The picture on the model's screen (the plane y = 0.163, x 0.5955..0.9051 top to bottom, z -0.2475..0.2483). */
    static void drawPicture(float[] pic) {
        float x0 = 0.9051f, x1 = 0.5955f, z0 = -0.2475f, z1 = 0.2483f, y = 0.1645f;
        float dx = (x1 - x0) / RES_H, dz = (z1 - z0) / RES_W;
        UtilsClient.disableLight();
        UtilsClient.disableTexture();
        UtilsClient.disableCulling();
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glNormal3f(0, 1, 0);
        for (int py = 0; py < RES_H; py++)
            for (int px = 0; px < RES_W; px++) {
                int o = (py * RES_W + px) * 3;
                GL11.glColor4f(pic[o], pic[o + 1], pic[o + 2], 1f);
                float xa = x0 + dx * py, xb = xa + dx, za = z0 + dz * px, zb = za + dz;
                GL11.glVertex3f(xa, y, za);
                GL11.glVertex3f(xa, y, zb);
                GL11.glVertex3f(xb, y, zb);
                GL11.glVertex3f(xb, y, za);
            }
        GL11.glEnd();
        UtilsClient.enableCulling();
        UtilsClient.enableTexture();
        UtilsClient.enableLight();
    }
}
