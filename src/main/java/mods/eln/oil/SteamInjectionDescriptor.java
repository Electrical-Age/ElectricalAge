package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Steam Injection Wellhead (todo #138; Baughn 2026-10-30, after Argon's a3: enhanced recovery): sits on a second
 * column of well casing down to an oil lake and pushes steam from an EA pipe on its top down into it, against the
 * casing's head of crude. Steam it puts into a lake in its seepage tail pays for crude (OilRecovery), and the lake's
 * pumps run at their full rate on it. Model: the 50V oil pump's until its own.
 */
public class SteamInjectionDescriptor extends TransparentNodeDescriptor implements mods.eln.node.transparent.FrontDrawnRender.Drawn {
    final Obj3D obj;

    public SteamInjectionDescriptor(String name, String model) {
        super(name, SteamInjectionElement.class, mods.eln.node.transparent.FrontDrawnRender.class);
        this.obj = Eln.obj.getObj(model);
    }

    @Override
    public boolean mustHaveFloor() {
        return true;
    }

    @Override
    public void draw() {
        var main = obj == null ? null : obj.getPart("main");
        if (main != null) main.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add("Injects steam down well casing to boost depleted oil lakes.");
        mods.eln.client.ItemTooltips.detail(list,
            String.format("%.2f kg steam per bucket of crude; requires ~0.08 bar per block of depth.", OilRecovery.steamKgPerBucket));
    }
}
