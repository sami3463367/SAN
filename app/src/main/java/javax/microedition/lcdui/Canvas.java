package javax.microedition.lcdui;

import android.graphics.Bitmap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.san.offline.GameRuntime;

public abstract class Canvas extends Displayable {
    public static final int UP=1, LEFT=2, RIGHT=5, DOWN=6, FIRE=8;
    private final Bitmap frame = Bitmap.createBitmap(240,320,Bitmap.Config.ARGB_8888);
    private final Object frameLock = new Object();
    private final ConcurrentLinkedQueue<int[]> events = new ConcurrentLinkedQueue<>();
    private volatile boolean dirty=true;
    public volatile long frames;
    protected abstract void paint(Graphics graphics);
    protected void keyPressed(int code) {}
    protected void keyReleased(int code) {}
    protected void showNotify() {}
    protected void hideNotify() {}
    public final void setFullScreenMode(boolean full) {}
    public final void repaint() { dirty=true; }
    public final void serviceRepaints() {
        // All key callbacks execute on the ORIGINAL game thread, not Android's UI thread.
        int[] event;
        while ((event=events.poll())!=null) {
            if(event[1]==1) keyPressed(event[0]); else keyReleased(event[0]);
        }
        GameRuntime.frameBoundary(this);
        if(!dirty) return;
        dirty=false;
        try {
            synchronized(frameLock) { paint(new Graphics(frame)); frames++; }
            GameRuntime.invalidate();
        } catch (Throwable error) { GameRuntime.fail(error); throw error; }
    }
    public final void enqueueKey(int code,boolean pressed) { events.add(new int[]{code,pressed?1:0}); }
    public final void resumeDisplay() { showNotify(); }
    public final void pauseDisplay() { hideNotify(); }
    public final void drawTo(android.graphics.Canvas target, android.graphics.RectF dest, android.graphics.Paint paint) {
        synchronized(frameLock) { target.drawBitmap(frame,null,dest,paint); }
    }
    public final Bitmap snapshot() { synchronized(frameLock) { return frame.copy(Bitmap.Config.ARGB_8888,false); } }
    public final int getGameAction(int key) {
        switch(key) {
            case -1:case '2':return UP;
            case -2:case '8':return DOWN;
            case -3:case '4':return LEFT;
            case -4:case '6':return RIGHT;
            case -5:case '5':return FIRE;
            default:return 0;
        }
    }
}
