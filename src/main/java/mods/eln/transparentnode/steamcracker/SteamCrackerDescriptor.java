package mods.eln.transparentnode.steamcracker;

import mods.eln.Eln;
import mods.eln.api.Brownout;
import mods.eln.api.MachineSpec;
import mods.eln.api.RelativeFace;
import mods.eln.api.element.Machines;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The Steam Cracker (the rubber chain's oil route, 2026-11-07; Baughn: electric only, the products on separate outputs):
 * an electrically heated furnace coil that cracks gasoline with steam. Gasoline comes in at the blue C8 flange and steam
 * at the blue H2O flange, both on its back; butadiene (for the Polymeriser) leaves by the red C4H6 flange and natural gas
 * by the red CH4 flange, both on its front. Both outlets need pipes (side by side on one face, pushing into the block in
 * front would mix them), and it stops while either product has nowhere to go. 400 V on its side faces. Per 100 mB of
 * gasoline: 1250 mB of steam, 40 kJ, 40 mB of butadiene and 30 mB of natural gas (the gas's fuel value about half the
 * gasoline's, so a gas turbine on it gives back less than the cracker draws). Model mods.eln.cad.models.SteamCracker.
 */
public class SteamCrackerDescriptor extends TransparentNodeDescriptor {
    public final double nominalU, nominalP;
    final Obj3D obj;
    public static final int GASOLINE_TANK_MB = 1000, STEAM_TANK_MB = 4000, BUTADIENE_TANK_MB = 1000, GAS_TANK_MB = 1000;
    /** Gasoline cracked at rated power (mB/s), and per 100 mB of it: steam, butadiene and natural gas (mB). */
    public static final double GASOLINE_MB_PER_S = 10, STEAM_PER_100 = 1250, BUTADIENE_PER_100 = 40, GAS_PER_100 = 30;
    public static final double LIQUID_PORT_MB_PER_S = 200, STEAM_PORT_MB_PER_S = 1000, OUT_HEAD_PA = 6e5;
    /** Its flanges (px, model frame): the two INs on the back, the two OUTs on the front, 4 px up, at z 4 and 12. */
    public static final int FLANGE_Y = 4, FLANGE_Z_A = 4, FLANGE_Z_B = 12;

    public SteamCrackerDescriptor(String name, String model, double nominalU, double nominalP, ElectricalCableDescriptor cable) {
        super(name, SteamCrackerElement.class, SteamCrackerRender.class);
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;
    }

    public MachineSpec spec() {
        return MachineSpec.consumer(nominalU).ratedWatts(nominalP).onBrownout(Brownout.SLOW)
            .burnsOnOvervoltage(Machines.BURNS_OUT).power(RelativeFace.LEFT, RelativeFace.RIGHT).build();
    }

    void draw(boolean powered, boolean running) {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
        mods.eln.transparentnode.resincollector.IndustrialResinCollectorDescriptor.led(obj.getPart("ledpower"), powered);
        mods.eln.transparentnode.resincollector.IndustrialResinCollectorDescriptor.led(obj.getPart("ledrun"), running);
        var g = obj.getPart("glow");   // the peepholes: the coil's orange heat while it cracks, dark glass when not
        if (g != null) {
            if (running) mods.eln.misc.UtilsClient.disableLight();
            if (running) mods.eln.gl.GL11.glColor4f(1f, 0.55f, 0.15f, 1);
            else mods.eln.gl.GL11.glColor4f(0.15f, 0.12f, 0.1f, 1);
            g.draw();
            if (running) mods.eln.misc.UtilsClient.enableLight();
            mods.eln.misc.UtilsClient.glDefaultColor();
        }
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(false, false);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Cracks gasoline with steam.\nOn its back: gasoline into the blue C8\nflange, steam into the blue H2O one.\nOn its front: butadiene out of the red\nC4H6 flange, natural gas out of CH4.\nBoth outlets need pipes.\nNominal power: %1$W",
            mods.eln.misc.Utils.plotValue(nominalP)).split("\n"));
    }
}
