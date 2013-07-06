package mods.eln.gui;

public class GuiVerticalTrackBarHeat extends GuiVerticalTrackBar{

	public GuiVerticalTrackBarHeat(int xPosition, int yPosition, int width, int height,GuiHelper helper) {
		super(xPosition, yPosition, width, height,helper);
		// TODO Auto-generated constructor stub
	}

	public float temperatureHit = 0;

	@Override
	public void drawBase(float par1, int x, int y) {
		if(visible == false) return;
		
		super.drawBase(par1, x, y);
       // drawRect(xPosition + 5, getCursorPositionForValue(temperatureHit)-2, xPosition + width - 5, getCursorPositionForValue(temperatureHit) + 2, 0xFF400000);
     //   drawRect(xPosition + 6, getCursorPositionForValue(temperatureHit)-1, xPosition + width - 6, getCursorPositionForValue(temperatureHit) + 1, 0xFF600000);

		drawRect(xPosition+2, getCursorPositionForValue(temperatureHit),xPosition + width-2,yPosition + height,0xFFFF2020);

		
	}
}
