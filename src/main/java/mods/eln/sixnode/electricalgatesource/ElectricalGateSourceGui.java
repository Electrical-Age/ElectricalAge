package mods.eln.sixnode.electricalgatesource;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.IGuiObject;
import net.minecraft.entity.player.EntityPlayer;

import static mods.eln.i18n.I18N.tr;

public class ElectricalGateSourceGui extends GuiScreenEln {

    ElectricalGateSourceRender render;
    GuiVerticalTrackBar voltage;

	public ElectricalGateSourceGui(EntityPlayer player, ElectricalGateSourceRender render) {
		this.render = render;
	}

	@Override
	public void initGui() {
		super.initGui();

		voltage = newGuiVerticalTrackBar(6, 6 + 2, 20, 50);
		voltage.setStepIdMax((int)100);
		voltage.setEnable(true);
    	voltage.setRange(0f, 50f);

    	syncVoltage();
	}
	
    public void syncVoltage() {
    	voltage.setValue(render.voltageSyncValue);
    	render.voltageSyncNew = false;
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
    	if (object == voltage) {
    		render.clientSetFloat(ElectricalGateSourceElement.setVoltagerId, voltage.getValue());
    	}
    }

    @Override
    protected void preDraw(float f, int x, int y) {
    	super.preDraw(f, x, y);
    	if (render.voltageSyncNew) syncVoltage();
		voltage.setComment(0, tr("Output at %1$%", ((int) voltage.getValue() * 2)));
	}

	@Override
	protected GuiHelper newHelper() {
		return new GuiHelper(this, 12 + 20, 12 + 50 + 4);
	}
}
