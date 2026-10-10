package mods.eln.sixnode.tutorialsign;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static mods.eln.i18n.I18N.tr;

public class TutorialSignGui extends GuiScreenEln {

    GuiTextFieldEln fileName;
    TutorialSignRender render;

    public TutorialSignGui(TutorialSignRender render) {
        this.render = render;
    }

    @Override
    protected GuiHelper newHelper() {
        return new GuiHelper(this, 150 + 12, 12 + 12);
    }

    @Override
    public void initGui() {
        super.initGui();

        fileName = newGuiTextField(6, 6, 150);
        fileName.setText(render.baliseName);
        fileName.setObserver(this);
        fileName.setComment(new String[]{tr("Set beacon name")});
    }

    @Override
    public void textFieldNewValue(GuiTextFieldEln textField, String value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            render.preparePacketForServer(stream);

            stream.writeByte(TutorialSignElement.setTextFileId);
            stream.writeUTF(fileName.getText());

            render.sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
