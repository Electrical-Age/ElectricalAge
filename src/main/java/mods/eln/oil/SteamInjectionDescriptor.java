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
public class SteamInjectionDescriptor extends TransparentNodeDescriptor {
    final Obj3D obj;

    public SteamInjectionDescriptor(String name, String model) {
        super(name, SteamInjectionElement.class, SteamInjectionRender.class);
        this.obj = Eln.obj.getObj(model);
    }

    @Override
    public boolean mustHaveFloor() {
        return true;
    }

    void draw() {
        var main = obj == null ? null : obj.getPart("main");
        if (main != null) main.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add("On well casing down to a depleted oil lake: steam from a pipe on its top goes down the well.");
        list.add(String.format("Each %.2f kg of steam brings up a bucket of crude at the pumps' full rate.", OilRecovery.steamKgPerBucket));
        list.add("The steam must push against the well's depth: about 0.08 bar a block.");
    }
}
