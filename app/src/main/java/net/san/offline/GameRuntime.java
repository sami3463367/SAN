package net.san.offline;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.microedition.lcdui.Canvas;
import javax.microedition.midlet.MIDlet;
import javax.microedition.media.Manager;

/** Owns one original MIDlet per Android process, surviving Activity recreation. */
public final class GameRuntime {
    private static Context context;
    private static WeakReference<GameActivity> activity=new WeakReference<>(null);
    private static final Handler ui=new Handler(Looper.getMainLooper());
    private static final ConcurrentLinkedQueue<Runnable> callbacks=new ConcurrentLinkedQueue<>();
    private static MIDlet midlet;
    private static volatile Canvas canvas;
    private static volatile boolean paused=true;
    private static boolean pauseDelivered;
    public static volatile Throwable failure;
    public static synchronized void attach(GameActivity owner) {
        context=owner.getApplicationContext();activity=new WeakReference<>(owner);
        if(midlet==null) {
            try {
                // No dynamic download or dex loading: these classes are compiled into the APK.
                midlet=(MIDlet)Class.forName("SaintsRow2").getDeclaredConstructor().newInstance();
                midlet.launch();
            }catch(Throwable e){fail(e);}
        }
    }
    public static void detach(GameActivity owner){if(activity.get()==owner)activity.clear();}
    public static Context context(){return context;}
    public static String property(String key) {
        if("MIDlet-Version".equals(key))return "1.0.7";
        if("MIDlet-Name".equals(key))return "GTA5 Mobile";
        if("MIDlet-Vendor".equals(key))return "[ANDREW-LVIV]";
        return null; // Preserve absent original flags. Do not activate demo or promotional links.
    }
    public static Canvas canvas(){return canvas;}
    public static void setCanvas(Canvas next){canvas=next;if(!paused)next.resumeDisplay();}
    public static void enqueue(Runnable runnable){callbacks.add(runnable);}
    public static void setPaused(boolean value) {
        paused=value;
        if(value)Manager.pauseAll();
        else {pauseDelivered=false;Canvas c=canvas;if(c!=null)c.resumeDisplay();}
    }
    public static void frameBoundary(Canvas c) {
        Runnable callback;while((callback=callbacks.poll())!=null)callback.run();
        if(paused && !pauseDelivered){save();c.pauseDisplay();pauseDelivered=true;}
    }
    /** Invoke the game's own serializer at a game-thread frame boundary. */
    public static void save() {
        try {
            Class<?> game=Class.forName("h");
            java.lang.reflect.Field strings=game.getDeclaredField("S");strings.setAccessible(true);
            java.lang.reflect.Field quests=game.getDeclaredField("aI");quests.setAccessible(true);
            if(strings.get(null)==null || quests.get(null)==null)return; // Still loading/language selection.
            Method save=game.getDeclaredMethod("mg");save.setAccessible(true);save.invoke(null);
        }catch(Throwable e){Log.e("SAN.Save","Original serializer failed",e);}
    }
    public static void invalidate(){GameActivity a=activity.get();if(a!=null)a.gameView.postInvalidateOnAnimation();}
    public static void fail(Throwable error){failure=error;Log.e("SAN.Runtime","Original game failed",error);invalidate();}
    public static void exit(){ui.post(()->{GameActivity a=activity.get();if(a!=null)a.finishAndRemoveTask();ui.postDelayed(()->android.os.Process.killProcess(android.os.Process.myPid()),300);});}
}
