package javax.microedition.media;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;
import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.microedition.media.control.VolumeControl;
import net.san.offline.GameRuntime;

public final class Manager {
    private static final CopyOnWriteArrayList<AndroidPlayer> players=new CopyOnWriteArrayList<>();
    private static volatile boolean muted;
    public static Player createPlayer(InputStream input,String mime) throws IOException {
        if(input==null)throw new IOException("Missing original audio: "+mime);
        File file=File.createTempFile("original-",mime.toLowerCase().contains("midi")?".mid":".amr",GameRuntime.context().getCacheDir());
        try(InputStream in=input;OutputStream out=new FileOutputStream(file)) {
            byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
        }
        AndroidPlayer player=new AndroidPlayer(file);players.add(player);return player;
    }
    public static void setMuted(boolean value) {muted=value;for(AndroidPlayer p:players)p.applyVolume();}
    public static boolean isMuted() {return muted;}
    public static void pauseAll() {for(AndroidPlayer p:players)try{p.stop();}catch(MediaException e){Log.w("SAN.Audio","pause",e);}}
    private static final class AndroidPlayer implements Player,VolumeControl {
        private final File file;
        private final CopyOnWriteArrayList<PlayerListener> listeners=new CopyOnWriteArrayList<>();
        private MediaPlayer media;
        private int state=UNREALIZED,level=100,loopCount=1,remaining=1;
        AndroidPlayer(File file){this.file=file;}
        public void addPlayerListener(PlayerListener listener){listeners.addIfAbsent(listener);}
        public synchronized int getState(){return state;}
        public synchronized void realize() throws MediaException {
            if(state==CLOSED)throw new MediaException("Player closed");
            if(state>=REALIZED)return;
            try {
                media=new MediaPlayer();
                media.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                media.setDataSource(file.getAbsolutePath());media.prepare();
                media.setOnCompletionListener(ignored -> {
                    synchronized(AndroidPlayer.this) {
                        if(state==CLOSED)return;
                        if(remaining>1){remaining--;media.seekTo(0);media.start();return;}
                        state=PREFETCHED;
                    }
                    emit("endOfMedia");
                });
                media.setOnErrorListener((p,what,extra)->{Log.e("SAN.Audio","Decoder error "+what+"/"+extra);synchronized(AndroidPlayer.this){if(state!=CLOSED)state=REALIZED;}return true;});
                state=REALIZED;applyVolume();
            } catch(Exception e) {if(media!=null){media.release();media=null;}throw new MediaException(e);}
        }
        public synchronized void prefetch() throws MediaException {realize();if(state<STARTED)state=PREFETCHED;}
        public synchronized void start() throws MediaException {
            prefetch();try{remaining=loopCount;media.setLooping(loopCount==-1);media.start();state=STARTED;}catch(Exception e){throw new MediaException(e);}
        }
        public synchronized void stop() throws MediaException {
            if(state!=STARTED)return;
            try{media.pause();state=PREFETCHED;emit("stopped");}catch(Exception e){throw new MediaException(e);}
        }
        public synchronized void deallocate(){if(state==STARTED)try{stop();}catch(MediaException ignored){}if(state>=REALIZED)state=REALIZED;}
        public synchronized void close(){if(media!=null){media.release();media=null;}state=CLOSED;file.delete();players.remove(this);}
        public synchronized void setLoopCount(int count){if(count==0||count< -1)throw new IllegalArgumentException("Loop count");loopCount=count;}
        public synchronized long setMediaTime(long time) throws MediaException {
            realize();try{media.seekTo(Math.max(0,time/1000),MediaPlayer.SEEK_CLOSEST);return Math.max(0,time);}catch(Exception e){throw new MediaException(e);}
        }
        public Control getControl(String name){return name.endsWith("VolumeControl")?this:null;}
        public synchronized int setLevel(int value){level=Math.max(0,Math.min(100,value));applyVolume();return level;}
        synchronized void applyVolume(){if(media!=null && state!=CLOSED){float v=muted?0:level/100f;media.setVolume(v,v);}}
        private void emit(String event){GameRuntime.enqueue(()->{for(PlayerListener l:listeners)l.playerUpdate(this,event,null);});}
    }
}
