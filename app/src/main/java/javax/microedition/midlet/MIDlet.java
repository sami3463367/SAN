package javax.microedition.midlet;
import net.san.offline.GameRuntime;
public abstract class MIDlet {
    protected abstract void startApp() throws MIDletStateChangeException;
    protected abstract void pauseApp();
    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;
    public final void launch() throws MIDletStateChangeException { startApp(); }
    public final void pause() { pauseApp(); }
    public final String getAppProperty(String key) { return GameRuntime.property(key); }
    public final void notifyDestroyed() { GameRuntime.exit(); }
    /** Never launch browsers, billing, SMS, or any external URI. This port is offline. */
    public final boolean platformRequest(String url) { return false; }
}
