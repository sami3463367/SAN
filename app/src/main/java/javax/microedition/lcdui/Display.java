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
    public boolean vibrate(int duration) {
        if(duration<0)throw new IllegalArgumentException("Negative vibration duration");
        android.os.Vibrator vibrator=(android.os.Vibrator)GameRuntime.context().getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if(vibrator==null || !vibrator.hasVibrator())return false;
        if(duration==0)vibrator.cancel();
        else vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration,android.os.VibrationEffect.DEFAULT_AMPLITUDE));
        return true;
    }
}
