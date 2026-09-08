package net.san.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.media.*;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import javax.microedition.media.Manager;

public final class GameActivity extends Activity {
    public GameView gameView;
    private AudioManager audio;
    private AudioFocusRequest focus;
    private boolean resumed,dialogOpen;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        gameView=new GameView(this);setContentView(gameView);
        gameView.setOnApplyWindowInsetsListener((view,insets)->{
            DisplayCutout cutout=insets.getDisplayCutout();
            if(cutout!=null)gameView.setSafeInsets(cutout.getSafeInsetLeft(),cutout.getSafeInsetTop(),cutout.getSafeInsetRight(),cutout.getSafeInsetBottom());
            else gameView.setSafeInsets(0,0,0,0);
            return insets;
        });
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener(change->{if(change<0){gameView.releaseAll();GameRuntime.setPaused(true);}else if(resumed&&!dialogOpen)GameRuntime.setPaused(false);}).build();
        Manager.setMuted(getPreferences(MODE_PRIVATE).getBoolean("muted",false));
        GameRuntime.attach(this);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->gameView.back());
    }
    @Override protected void onResume(){super.onResume();resumed=true;immersive();audio.requestAudioFocus(focus);if(!dialogOpen)GameRuntime.setPaused(false);}
    @Override protected void onPause(){resumed=false;gameView.releaseAll();GameRuntime.setPaused(true);audio.abandonAudioFocusRequest(focus);super.onPause();}
    @Override protected void onDestroy(){GameRuntime.detach(this);super.onDestroy();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();else if(gameView!=null)gameView.releaseAll();}
    @SuppressWarnings("deprecation") private void immersive(){
        if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController controller=getWindow().getInsetsController();if(controller!=null){controller.hide(WindowInsets.Type.systemBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    @Override public void onBackPressed(){gameView.back();}
    public void showHelp(){
        dialogOpen=true;gameView.releaseAll();GameRuntime.setPaused(true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Original game · touch controls")
            .setMessage("Move with the left pad. Hold a direction and FIRE together to move and attack.\n\n5 / FIRE: attack or confirm\n1 / VEHICLE: enter or leave a car\n* / WEAPON: switch weapon\n# / TARGET: switch target\nSELECT: original left soft key\nMENU / BACK: original right soft key\nKEYS: full number pad for names and number prompts\n\nUse the original Settings menu to choose steering or directional driving.\n\nThe 240 × 320 game is shown without stretching or cropping. Original missions, sprites and text are bundled locally. Saves stay on this device; uninstalling clears them.")
            .setPositiveButton("Resume",null).create();
        dialog.setOnDismissListener(ignored->{dialogOpen=false;if(resumed)GameRuntime.setPaused(false);immersive();});dialog.show();
    }
    private int mapKey(int key){
        if(key>=KeyEvent.KEYCODE_0&&key<=KeyEvent.KEYCODE_9)return '0'+key-KeyEvent.KEYCODE_0;
        switch(key){
            case KeyEvent.KEYCODE_DPAD_UP:case KeyEvent.KEYCODE_W:return -1;
            case KeyEvent.KEYCODE_DPAD_DOWN:case KeyEvent.KEYCODE_S:return -2;
            case KeyEvent.KEYCODE_DPAD_LEFT:case KeyEvent.KEYCODE_A:return -3;
            case KeyEvent.KEYCODE_DPAD_RIGHT:case KeyEvent.KEYCODE_D:return -4;
            case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_ENTER:case KeyEvent.KEYCODE_SPACE:case KeyEvent.KEYCODE_BUTTON_A:return -5;
            case KeyEvent.KEYCODE_Q:case KeyEvent.KEYCODE_STAR:case KeyEvent.KEYCODE_BUTTON_L1:return '*';
            case KeyEvent.KEYCODE_E:case KeyEvent.KEYCODE_POUND:case KeyEvent.KEYCODE_BUTTON_R1:return '#';
            case KeyEvent.KEYCODE_F:case KeyEvent.KEYCODE_BUTTON_X:return '1';
            case KeyEvent.KEYCODE_TAB:case KeyEvent.KEYCODE_BUTTON_Y:return -6;
            case KeyEvent.KEYCODE_ESCAPE:case KeyEvent.KEYCODE_BUTTON_START:case KeyEvent.KEYCODE_BUTTON_B:return -7;
            default:return 0;
        }
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){int key=mapKey(code);if(key!=0){if(event.getRepeatCount()==0)gameView.hardware(code,key,true);return true;}return super.onKeyDown(code,event);}
    @Override public boolean onKeyUp(int code,KeyEvent event){int key=mapKey(code);if(key!=0){gameView.hardware(code,key,false);return true;}return super.onKeyUp(code,event);}
}
