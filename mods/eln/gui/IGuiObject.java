package mods.eln.gui;

public interface IGuiObject {
	public void idraw(int x,int y,float f);
	public void idraw2(int x,int y);
	public boolean ikeyTyped(char key, int code);
	public void imouseClicked(int x, int y, int code);
	public void imouseMove(int x,int y);
	public void imouseMovedOrUp(int x, int y, int witch);
	public interface IGuiObjectObserver
	{
		public void guiObjectEvent(IGuiObject object);
	}
}
