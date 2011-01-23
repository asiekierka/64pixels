public abstract class CraftrSoundShim
{
	public abstract void setPos(int posX, int posY);
	public abstract void playSampleByNumber(int x, int y, int smp, double vol);
	public abstract void playNote(int x, int y, int note, double vol);
}
