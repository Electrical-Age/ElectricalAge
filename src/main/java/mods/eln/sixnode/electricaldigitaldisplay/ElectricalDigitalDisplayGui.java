package mods.eln.sixnode.electricaldigitaldisplay;

import mods.eln.Eln;
import mods.eln.debug.DebugType;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
import mods.eln.i18n.I18N;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiButton;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

public class ElectricalDigitalDisplayGui extends GuiScreenEln {
    GuiTextFieldEln minValue, maxValue;
    GuiButton validate;
    ElectricalDigitalDisplayRender render;

    public ElectricalDigitalDisplayGui(ElectricalDigitalDisplayRender render) {
        super();
        this.render = render;
    }

    @Override
    protected GuiHelper newHelper() {
        return new GuiHelper(this, 169, 44);
    }

    @Override
    public void initGui() {
        super.initGui();
        minValue = newGuiTextField(8, 24, 70);
        minValue.setComment(new String[]{"Display at minimum signal input"});
        minValue.setText(String.format("%.2f", render.min));
        maxValue = newGuiTextField(8, 8, 70);
        maxValue.setComment(new String[]{"Display at maximum signal input"});
        maxValue.setText(String.format("%.2f", render.max));
        validate = newGuiButton(82, 12, 80, I18N.tr("Validate"));
        validate.enabled = true;
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        super.guiObjectEvent(object);
        if(object == validate) {
            try {
                NumberFormat fmt = NumberFormat.getInstance();
                float newMin = fmt.parse(minValue.getText()).floatValue();
                float newMax = fmt.parse(maxValue.getText()).floatValue();
                Eln.dp.println(DebugType.SIX_NODE, String.format("EDDG sending %f - %f", newMin, newMax));

                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    DataOutputStream stream = new DataOutputStream(bos);

                    render.preparePacketForServer(stream);

                    stream.writeByte(ElectricalDigitalDisplayDescriptor.netSetRange);
                    stream.writeFloat(newMin);
                    stream.writeFloat(newMax);

                    render.sendPacketToServer(bos);
                } catch(IOException e) {
                    e.printStackTrace();
                }
            } catch(ParseException e) {}
        }
    }
}
