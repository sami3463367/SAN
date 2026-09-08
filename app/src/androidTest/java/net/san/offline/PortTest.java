package net.san.offline;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.lang.reflect.Field;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PortTest {
    private static int field(String name)throws Exception{Field f=Class.forName("h").getDeclaredField(name);f.setAccessible(true);return f.getInt(null);}
    private static void key(int code){GameRuntime.canvas().enqueueKey(code,true);SystemClock.sleep(180);GameRuntime.canvas().enqueueKey(code,false);SystemClock.sleep(300);}
    private static void waitState(int state,long timeout)throws Exception{
        long until=SystemClock.uptimeMillis()+timeout;
        while(SystemClock.uptimeMillis()<until){if(GameRuntime.failure!=null)throw new AssertionError(GameRuntime.failure);if(GameRuntime.canvas()!=null&&field("db")==state)return;SystemClock.sleep(100);}
        fail("Expected game state "+state+"; actual "+field("db"));
    }
    private static void shot(String name)throws Exception{
        File dir=new File(GameRuntime.context().getExternalFilesDir(null),"evidence");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){GameRuntime.canvas().snapshot().compress(Bitmap.CompressFormat.PNG,100,out);}
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+"-landscape.png"))){
            InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,out);
        }
        shell("mkdir -p /data/local/tmp/san-evidence");
        shell("cp "+new File(dir,name+".png").getAbsolutePath()+" /data/local/tmp/san-evidence/"+name+".png");
        shell("cp "+new File(dir,name+"-landscape.png").getAbsolutePath()+" /data/local/tmp/san-evidence/"+name+"-landscape.png");
    }
    private static void shell(String command)throws Exception {
        try(android.os.ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
            InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            byte[] buffer=new byte[1024];while(in.read(buffer)!=-1){}
        }
    }
    private static boolean booleanField(String name)throws Exception{Field f=Class.forName("h").getDeclaredField(name);f.setAccessible(true);return f.getBoolean(null);}
    private static boolean held(int index)throws Exception{Field f=Class.forName("h").getDeclaredField("k");f.setAccessible(true);return ((boolean[])f.get(null))[index];}
    private static void touch(GameActivity activity,long down,int action,int[] ids,float[][] xy) {
        android.view.MotionEvent.PointerProperties[] props=new android.view.MotionEvent.PointerProperties[ids.length];
        android.view.MotionEvent.PointerCoords[] coords=new android.view.MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++){
            props[i]=new android.view.MotionEvent.PointerProperties();props[i].id=ids[i];props[i].toolType=android.view.MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new android.view.MotionEvent.PointerCoords();coords[i].x=xy[i][0];coords[i].y=xy[i][1];coords[i].pressure=1;coords[i].size=1;
        }
        android.view.MotionEvent event=android.view.MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,ids.length,props,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->activity.gameView.dispatchTouchEvent(event));event.recycle();SystemClock.sleep(250);
    }
    private static boolean dialogue()throws Exception {
        java.lang.reflect.Method method=Class.forName("h").getDeclaredMethod("kY");method.setAccessible(true);return (Boolean)method.invoke(null);
    }
    private static void finishIntro()throws Exception {
        // The original deliberately clears ALL held keys when a dialogue is dismissed.
        // Test movement/fire in live gameplay, not while FIRE is confirming Johnny Gat's dialogue.
        long until=SystemClock.uptimeMillis()+30000,stableSince=0;
        while(SystemClock.uptimeMillis()<until) {
            if(dialogue() || booleanField("dd") || booleanField("de")) {key(-5);stableSince=0;}
            else {
                if(stableSince==0)stableSince=SystemClock.uptimeMillis();
                if(SystemClock.uptimeMillis()-stableSince>=1000)return;
                SystemClock.sleep(100);
            }
        }
        fail("Intro did not reach unobstructed gameplay; db="+field("db"));
    }
    private static void nativeMultiTouch(GameActivity activity)throws Exception {
        finishIntro();
        android.graphics.RectF pad=activity.gameView.padBounds(),fire=activity.gameView.controlBounds(-5);
        float[] move={pad.centerX()+pad.width()*.36f,pad.centerY()},attack={fire.centerX(),fire.centerY()};
        long down=SystemClock.uptimeMillis();
        touch(activity,down,android.view.MotionEvent.ACTION_DOWN,new int[]{11},new float[][]{move});
        touch(activity,down,android.view.MotionEvent.ACTION_POINTER_DOWN|(1<<8),new int[]{11,19},new float[][]{move,attack});
        assertTrue("Native right direction held (dialogue="+dialogue()+", db="+field("db")+")",held(1));
        assertTrue("Native fire held simultaneously",held(4));
        shot("08-native-multitouch");
        touch(activity,down,android.view.MotionEvent.ACTION_POINTER_UP,new int[]{11,19},new float[][]{move,attack});
        assertFalse("Released direction",held(1));assertTrue("Other finger still fires",held(4));
        touch(activity,down,android.view.MotionEvent.ACTION_UP,new int[]{19},new float[][]{attack});assertFalse("Released fire",held(4));
        touch(activity,down,android.view.MotionEvent.ACTION_DOWN,new int[]{23},new float[][]{move});
        touch(activity,down,android.view.MotionEvent.ACTION_CANCEL,new int[]{23},new float[][]{move});assertFalse("Cancel releases direction",held(1));
    }
    @Test public void originalGameBootsAndLoadsNewGameOffline() throws Exception {
        android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        Intent intent=new Intent(ins.getTargetContext(),GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        GameActivity activity=(GameActivity)ins.startActivitySync(intent);
        waitState(2,20000);SystemClock.sleep(500);shot("01-language");
        key('5');waitState(8,30000);shot("02-sound");key('5');
        waitState(4,20000);shot("03-original-menu");
        key('5');waitState(7,10000);shot("04-create-player");
        // Character creation can have several original steps; log and capture before advancing.
        for(int i=0;i<8 && field("db")==7;i++){key(-6);SystemClock.sleep(500);shot("05-create-"+i);}
        if(field("db")==8){shot("06-tutorial-prompt");key(-7);}
        waitState(9,30000);SystemClock.sleep(2500);shot("07-gameplay");
        key(-6);key('5');key('5');
        GameRuntime.canvas().enqueueKey('6',true);GameRuntime.canvas().enqueueKey('5',true);SystemClock.sleep(900);
        GameRuntime.canvas().enqueueKey('6',false);GameRuntime.canvas().enqueueKey('5',false);SystemClock.sleep(500);shot("08-movement-fire");
        try {nativeMultiTouch(activity);} catch(AssertionError error){shot("08-native-failure");throw error;}
        assertNull(GameRuntime.failure);assertTrue(GameRuntime.canvas().frames>30);
        assertTrue(activity.gameView.getWidth()>activity.gameView.getHeight());
        assertEquals(.75f,activity.gameView.viewport().width()/activity.gameView.viewport().height(),.001f);
        java.util.concurrent.CountDownLatch saved=new java.util.concurrent.CountDownLatch(1);
        GameRuntime.enqueue(()->{GameRuntime.save();saved.countDown();});assertTrue(saved.await(10,java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(new File(GameRuntime.context().getFilesDir(),"rms/saintsrow2.rms").length()>20);
        // Real Android decoders, not a silent stub.
        for(String file:new String[]{"MainMenu.mid","pistol.amr","explosion.amr"}) {
            Player p=Manager.createPlayer(getClass().getResourceAsStream("/"+file),file.endsWith("mid")?"audio/midi":"audio/amr");
            p.realize();p.prefetch();p.start();assertEquals(Player.STARTED,p.getState());SystemClock.sleep(100);p.stop();p.close();
        }
        key(-7);shot("09-pause-menu");
        // Actual background/resume lifecycle, including automatic save and released keys.
        ins.getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
        long until=SystemClock.uptimeMillis()+5000;while(!booleanField("f")&&SystemClock.uptimeMillis()<until)SystemClock.sleep(100);
        assertTrue("Original loop pauses in background",booleanField("f"));
        long pausedFrames=GameRuntime.canvas().frames;SystemClock.sleep(500);assertTrue(GameRuntime.canvas().frames<=pausedFrames+1);
        ins.getTargetContext().startActivity(intent);SystemClock.sleep(1500);assertFalse("Resumes on return",booleanField("f"));
        key(-5);shot("10-resumed");assertNull(GameRuntime.failure);
    }
    @Test public void spriteTransformsAndClipReplacementAreCorrect() {
        int[] colors={0xffff0000,0xff00ff00,0xff0000ff,0xffffff00,0xffff00ff,0xff00ffff};
        Image image=new Image(Bitmap.createBitmap(colors,3,2,Bitmap.Config.ARGB_8888));
        int[][] expected={{0,1,2,3,4,5},{3,4,5,0,1,2},{2,1,0,5,4,3},{5,4,3,2,1,0},{0,3,1,4,2,5},{3,0,4,1,5,2},{2,5,1,4,0,3},{5,2,4,1,3,0}};
        for(int t=0;t<8;t++) {
            int w=t>=4?2:3,h=t>=4?3:2;
            Bitmap target=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Graphics g=new Graphics(target);
            g.drawRegion(image,0,0,3,2,t,0,0,20);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++)assertEquals("transform "+t,colors[expected[t][y*w+x]],target.getPixel(x,y));
        }
        Bitmap target=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888);Graphics g=new Graphics(target);
        g.setClip(0,0,1,1);g.setClip(0,0,4,4);g.setColor(0xff123456);g.fillRect(0,0,4,4);assertEquals(0xff123456,target.getPixel(3,3));
        g.translate(1,2);g.setClip(0,0,2,2);assertEquals(0,g.getClipX());assertEquals(2,g.getTranslateY());
    }
}
