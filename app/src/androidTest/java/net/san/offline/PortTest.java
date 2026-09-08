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
