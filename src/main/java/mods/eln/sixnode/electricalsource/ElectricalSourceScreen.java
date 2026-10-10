package mods.eln.sixnode.electricalsource;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;
import java.text.ParseException;

import static mods.eln.i18n.I18N.tr;

/** Electrical source (P4b, 1.12 ElectricalSourceGui restyled): the output voltage, typed in, and what it is now. */
public class ElectricalSourceScreen extends ElnScreen {
    final ElectricalSourceRender render;
    EditBox voltage;

    public ElectricalSourceScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, 176, 64);
        this.render = (ElectricalSourceRender) render;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        voltage = addRenderableWidget(new EditBox(font, x + 8, y + 32, 90, 18, Component.literal(tr("Output voltage"))));
        voltage.setValue(Utils.plotValue(render.voltage));
        voltage.setTooltip(Tooltip.create(Component.literal(tr("Output voltage"))));
        addRenderableWidget(Button.builder(Component.literal(tr("Set")), b -> commit()).bounds(x + 120, y + 31, 48, 20).build());
        setInitialFocus(voltage);
    }

    void commit() {
        try {
            float v = NumberFormat.getInstance().parse(voltage.getValue().replace("V", "").trim()).floatValue();
            render.clientSetFloat(ElectricalSourceElement.setVoltageId, v);
        } catch (ParseException e) {
            voltage.setTextColor(0xFFFF5555);
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (voltage.isFocused() && (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN
            || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER)) {
            commit();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.text(font, tr("Output voltage") + "  (" + tr("now %1$", Utils.plotVolt("", render.voltage)) + ")", leftPos + 8, topPos + 20, LABEL, false);
        g.text(font, "V", leftPos + 102, topPos + 37, LABEL, false);
    }
}
