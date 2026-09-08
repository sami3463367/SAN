package javax.microedition.lcdui;
import javax.microedition.midlet.MIDlet;
import net.san.offline.GameRuntime;
public final class Display {
    private static final Display INSTANCE = new Display();
    public static Display getDisplay(MIDlet midlet) { return INSTANCE; }
    public void setCurrent(Displayable display) {
        if (!(display instanceof Canvas)) throw new IllegalArgumentException("Unsupported original display");
        GameRuntime.setCanvas((Canvas)display);
    }
    public boolean vibrate(int duration) { return false; } // Optional capability; no permission needed.
}
