package mods.eln.transparentnode.electricalfurnace;

import mods.eln.client.gui.EaGauge;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.item.HeatingCorpElement;
import mods.eln.misc.Utils;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import static mods.eln.i18n.I18N.tr;

/**
 * Electrical furnace (P4b, 1.12 ElectricalFurnaceGuiDraw restyled): input -> progress -> output, heater and regulator
 * slots, the target temperature as a slider with the actual temperature, the heater's supply as a gauge, power and
 * auto-shutdown buttons. Same EA packets as 1.12.
 */
public class ElectricalFurnaceScreen extends ElnScreen {
    static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    final ElectricalFurnaceRender render;
    EaSlider target;
    Button power, autoShutdown;

    public ElectricalFurnaceScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, 176, menu.spec.inventoryY() + 82);
        this.render = (ElectricalFurnaceRender) render;
    }

    static String celsius(double relative) {
        return Utils.plotValue(relative + PhysicalConstant.Tamb) + "°C";
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        target = addRenderableWidget(new EaSlider(x + 8, y + 50, 160, 0, 800, 10,
            render.temperatureTargetSyncValue < -1000 ? 0 : render.temperatureTargetSyncValue,
            v -> Component.literal(tr("Target %1$  (now %2$)", celsius(v), celsius(render.temperature))),
            v -> render.clientSetTemperatureTarget((float) v)));
        target.setTooltip(Tooltip.create(Component.literal(tr("Temperature the regulator keeps"))));
        addRenderableWidget(new EaGauge(x + 8, y + 74, 160, 12, this::supplyFraction, this::supplyText, 1.0, 1.2));
        power = addRenderableWidget(Button.builder(Component.empty(), b -> render.clientSetPowerOn(!render.getPowerOn()))
            .bounds(x + 8, y + 90, 78, 16).build());
        autoShutdown = addRenderableWidget(Button.builder(Component.empty(),
                b -> render.clientSendId(ElectricalFurnaceElement.unserializeAutoShutDownId))
            .bounds(x + 90, y + 90, 78, 16)
            .tooltip(Tooltip.create(Component.literal(tr("Auto: the heater stops when nothing is left to smelt")))).build());
        update();
    }

    ItemStack heater() {
        for (var s : menu.slots)
            if (s.container != null && s.getContainerSlot() == ElectricalFurnaceElement.heatingCorpSlotId && s instanceof mods.eln.gui.ElnSlot)
                return s.getItem();
        return ItemStack.EMPTY;
    }

    double nominalU() {
        ItemStack h = heater();
        if (Utils.isEmpty(h)) return 0;
        Object d = HeatingCorpElement.getDescriptor(h);
        return d instanceof HeatingCorpElement hc ? hc.electricalNominalU : 0;
    }

    double supplyFraction() {
        double u = nominalU();
        return u <= 0 ? 0 : render.voltage / u;
    }

    Component supplyText() {
        double u = nominalU();
        if (u <= 0) return Component.literal(tr("No heating element"));
        return Component.literal(Utils.plotVolt("", render.voltage) + " / " + Utils.plotVolt("", u) + "  " + Utils.plotPower("", render.heatingCorpResistorP));
    }

    @Override
    protected void update() {
        if (render.temperatureTargetSyncNew) {
            target.setFromDevice(render.temperatureTargetSyncValue);
            render.temperatureTargetSyncNew = false;
        }
        target.refreshLabel();
        power.setMessage(Component.literal(render.getPowerOn() ? tr("Heater: on") : tr("Heater: off")));
        power.active = !render.autoShutDown;
        autoShutdown.setMessage(Component.literal(render.autoShutDown ? tr("Auto stop: on") : tr("Auto stop: off")));
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int ax = leftPos + 52, ay = topPos + 25;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, ax, ay, 24, 16, 0.25f);
        int w = (int) Math.ceil(24 * Math.max(0, Math.min(1, render.processState)));
        if (w > 0) g.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, ax, ay, w, 16);
    }
}
