package mods.eln.transparentnode.electricalmachine;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaGauge;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical machines: macerator, compressor, plate machine, magnetizer (P4b, 1.12 ElectricalMachineGuiDraw). */
public class ElectricalMachineScreen extends ElnScreen {
    final ElectricalMachineRender render;

    public ElectricalMachineScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalMachineRender) render;
    }

    static final net.minecraft.resources.Identifier ARROW = net.minecraft.resources.Identifier.withDefaultNamespace("container/furnace/burn_progress");

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new EaGauge(leftPos + 8, topPos + 31, 188, 7, () -> render.UFactor,
            () -> Component.empty(), 1.05, 1.2));
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int ax = leftPos + 67, ay = topPos + 12;
        g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ARROW, ax, ay, 24, 16, 0.25f);
        int w = (int) Math.ceil(24 * Math.max(0, Math.min(1, render.processState)));
        if (w > 0) g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, ax, ay, w, 16);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 196 && mouseY >= topPos + 31 && mouseY < topPos + 38)
            g.setTooltipForNextFrame(Component.literal(Utils.plotVolt("", render.UFactor * render.descriptor.nominalU) + " / "
                + Utils.plotVolt("", render.descriptor.nominalU) + "  " + Utils.plotPower("", render.powerFactor * render.descriptor.nominalP)), mouseX, mouseY);
    }
}
