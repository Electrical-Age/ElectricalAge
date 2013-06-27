package mods.eln.gui;

public class GuiVerticalTrackBarHeat extends GuiVerticalTrackBar{

	public GuiVerticalTrackBarHeat(int xPosition, int yPosition, int width, int height) {
		super(xPosition, yPosition, width, height);
		// TODO Auto-generated constructor stub
	}

	public float temperatureHit = 0;

	@Override
	public void draw(float par1, int x, int y) {
		super.draw(par1, x, y);
        drawRect(xPosition + 2, getCursorPositionForValue(temperatureHit), xPosition + width - 2, getCursorPositionForValue(temperatureHit) + 1, 0xFF00FFFF);

		
	}
}
