package net.san.offline;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;
import javax.microedition.media.Manager;

/** Native landscape controller. The original framebuffer is never cropped or stretched. */
public final class GameView extends View {
    private static final int MUTE=-100,KEYPAD=-101,HELP=-102;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG),pixels=new Paint();
    private final RectF viewport=new RectF(),pad=new RectF();
    private final List<Button> buttons=new ArrayList<>();
    private final Map<Integer,Boolean> padPointers=new HashMap<>();
    private final Map<Integer,Integer> uiPointers=new HashMap<>();
    private final TouchKeys keys=new TouchKeys((code,down)->{javax.microedition.lcdui.Canvas c=GameRuntime.canvas();if(c!=null)c.enqueueKey(code,down);});
    private boolean keypad;
    private float unit=1,safeLeft,safeRight,safeTop,safeBottom;
    private final GameActivity activity;
    private static final class Button {
        final RectF rect;final int code;final String label,detail;
        Button(RectF rect,int code,String label,String detail){this.rect=rect;this.code=code;this.label=label;this.detail=detail;}
    }
    public GameView(GameActivity activity){super(activity);this.activity=activity;setFocusable(true);setFocusableInTouchMode(true);pixels.setFilterBitmap(false);setContentDescription("Original game with multitouch directional pad, fire, vehicle, weapon, target and menu controls");}
    public void setSafeInsets(float l,float t,float r,float b){safeLeft=l;safeTop=t;safeRight=r;safeBottom=b;layoutControls(getWidth(),getHeight());invalidate();}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){releaseAll();layoutControls(w,h);}
    private void add(float x,float y,float w,float h,int code,String label,String detail){buttons.add(new Button(new RectF(x,y,x+w,y+h),code,label,detail));}
    private void layoutControls(int width,int height) {
        buttons.clear();
        float w=width-safeLeft-safeRight,h=height-safeTop-safeBottom;
        unit=Math.min(h/360f,w/760f);
        float frameHeight=Math.min(h-16*unit,(w-300*unit)/.75f),frameWidth=frameHeight*.75f;
        viewport.set(safeLeft+(w-frameWidth)/2,safeTop+(h-frameHeight)/2,safeLeft+(w+frameWidth)/2,safeTop+(h+frameHeight)/2);
        float margin=16*unit,left=safeLeft+margin,right=viewport.right+margin;
        float panelW=Math.min(viewport.left-left-margin,width-safeRight-right-margin);
        float top=safeTop;
        add(left,top+18*unit,52*unit,30*unit,HELP,"?","");
        add(right+panelW-112*unit,top+18*unit,52*unit,30*unit,MUTE,"SOUND","");
        add(right+panelW-52*unit,top+18*unit,52*unit,30*unit,KEYPAD,keypad?"PLAY":"KEYS","");
        float size=Math.min(panelW,180*unit),cx=left+panelW/2,cy=top+h*.53f;
        pad.set(cx-size/2,cy-size/2,cx+size/2,cy+size/2);
        float bw=(panelW-12*unit)/3;
        if(keypad) {
            String[] labels={"1","2","3","4","5","6","7","8","9","*","0","#"};
            float bh=(h*.62f-18*unit)/4;
            for(int i=0;i<12;i++)add(right+(i%3)*(bw+6*unit),top+h*.21f+(i/3)*(bh+6*unit),bw,bh,labels[i].charAt(0),labels[i],"");
        }else {
            add(right,top+h*.25f,bw,54*unit,'1',"1","VEHICLE");
            add(right+bw+6*unit,top+h*.25f,bw,54*unit,'*',"*","WEAPON");
            add(right+2*(bw+6*unit),top+h*.25f,bw,54*unit,'#',"#","TARGET");
            float fw=Math.min(panelW,148*unit);
            add(right+(panelW-fw)/2,top+h*.47f,fw,100*unit,-5,"5","FIRE / OK");
        }
        add(left,top+h-58*unit,panelW,42*unit,-6,"SELECT","LEFT SOFT KEY");
        add(right,top+h-58*unit,panelW,42*unit,-7,"MENU / BACK","RIGHT SOFT KEY");
    }
    @Override protected void onDraw(Canvas c) {
        c.drawColor(Color.rgb(12,16,23));
        paint.setColor(0xff141b26);c.drawRoundRect(viewport.left-3*unit,viewport.top-3*unit,viewport.right+3*unit,viewport.bottom+3*unit,4*unit,4*unit,paint);
        javax.microedition.lcdui.Canvas original=GameRuntime.canvas();
        if(original!=null && original.frames>0)original.drawTo(c,viewport,pixels);
        else label(c,"Loading original game…",viewport.centerX(),viewport.centerY(),12,0xffd6dcec);
        label(c,"ORIGINAL  /  OFFLINE",safeLeft+16*unit,safeTop+77*unit,10,0xff768399,Paint.Align.LEFT);
        drawPad(c);
        for(Button b:buttons) {
            boolean active=keys.isDown(b.code)||(b.code==MUTE&&Manager.isMuted());
            paint.setColor(active?0xff6e4caf:0xff1c2533);paint.setStyle(Paint.Style.FILL);c.drawRoundRect(b.rect,10*unit,10*unit,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(unit);paint.setColor(active?0xffc6a6ff:0xff334056);c.drawRoundRect(b.rect,10*unit,10*unit,paint);paint.setStyle(Paint.Style.FILL);
            String label=b.code==MUTE?(Manager.isMuted()?"MUTED":"SOUND"):b.label;
            float y=b.rect.centerY()-(b.detail.isEmpty()?0:7*unit);
            label(c,label,b.rect.centerX(),y,b.code==-5&&!keypad?32:b.code<=-100?9:14,0xfff0f2f8);
            if(!b.detail.isEmpty())label(c,b.detail,b.rect.centerX(),y+21*unit,8,0xffaebbd0);
        }
        if(GameRuntime.failure!=null) {
            paint.setColor(0xf51c1015);c.drawRect(viewport,paint);
            label(c,"Game could not continue",viewport.centerX(),viewport.centerY()-14*unit,12,0xffffc2cd);
            label(c,"Please report this build's error.",viewport.centerX(),viewport.centerY()+10*unit,9,0xffe9d4db);
        }
    }
    private void drawPad(Canvas c) {
        float cx=pad.centerX(),cy=pad.centerY(),r=pad.width()/2,cell=pad.width()/3;
        paint.setColor(0xff101722);c.drawCircle(cx,cy,r+7*unit,paint);
        int[] codes={-1,-3,-4,-2};float[][] places={{cx,cy-cell},{cx-cell,cy},{cx+cell,cy},{cx,cy+cell}};
        String[] marks={"▲","◀","▶","▼"};
        for(int i=0;i<4;i++) {
            float x=places[i][0],y=places[i][1];
            paint.setColor(keys.isDown(codes[i])?0xff6e4caf:0xff233044);
            c.drawRoundRect(x-cell*.45f,y-cell*.45f,x+cell*.45f,y+cell*.45f,8*unit,8*unit,paint);
            label(c,marks[i],x,y,17,0xffe2e7f1);
        }
        paint.setColor(0xff192233);c.drawCircle(cx,cy,cell*.28f,paint);
    }
    private void label(Canvas c,String text,float x,float y,float size,int color){label(c,text,x,y,size,color,Paint.Align.CENTER);}
    private void label(Canvas c,String text,float x,float y,float size,int color,Paint.Align align){paint.setColor(color);paint.setTextSize(size*unit);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextAlign(align);c.drawText(text,x,y-(paint.ascent()+paint.descent())/2,paint);}
    private Button hit(float x,float y){for(Button b:buttons)if(b.rect.contains(x,y))return b;return null;}
    private void updatePointer(int id,float x,float y) {
        if(Boolean.TRUE.equals(padPointers.get(id))) {
            float dx=(x-pad.centerX())/(pad.width()/2),dy=(y-pad.centerY())/(pad.height()/2);
            if(Math.hypot(dx,dy)<.22){keys.release(id);return;}
            int horizontal=dx<-.28?-3:dx>.28?-4:0,vertical=dy<-.28?-1:dy>.28?-2:0;
            if(horizontal!=0&&vertical!=0)keys.update(id,horizontal,vertical);
            else if(horizontal!=0)keys.update(id,horizontal);else if(vertical!=0)keys.update(id,vertical);else keys.release(id);
        }else {
            Button b=hit(x,y);if(b!=null&&b.code> -100)keys.update(id,b.code);else keys.release(id);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action=event.getActionMasked(),index=event.getActionIndex(),id=event.getPointerId(index);
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN) {
            float x=event.getX(index),y=event.getY(index);Button b=hit(x,y);
            if(b!=null&&b.code<=-100)uiPointers.put(id,b.code);
            else {padPointers.put(id,pad.contains(x,y));updatePointer(id,x,y);}
        }else if(action==MotionEvent.ACTION_MOVE) {
            for(int i=0;i<event.getPointerCount();i++){int p=event.getPointerId(i);if(!uiPointers.containsKey(p))updatePointer(p,event.getX(i),event.getY(i));}
        }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP) {
            Integer ui=uiPointers.remove(id);Button b=hit(event.getX(index),event.getY(index));
            keys.release(id);padPointers.remove(id);
            if(ui!=null&&b!=null&&b.code==ui) {
                releaseAll();
                if(ui==KEYPAD){keypad=!keypad;layoutControls(getWidth(),getHeight());}
                if(ui==MUTE){Manager.setMuted(!Manager.isMuted());activity.getPreferences(Context.MODE_PRIVATE).edit().putBoolean("muted",Manager.isMuted()).apply();}
                if(ui==HELP)activity.showHelp();
                performClick();
            }
        }else if(action==MotionEvent.ACTION_CANCEL)releaseAll();
        invalidate();return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    public void releaseAll(){keys.releaseAll();padPointers.clear();uiPointers.clear();invalidate();}
    public void hardware(int androidCode,int gameCode,boolean pressed){if(pressed)keys.update(-1000-androidCode,gameCode);else keys.release(-1000-androidCode);invalidate();}
    public void back(){javax.microedition.lcdui.Canvas c=GameRuntime.canvas();if(c!=null){c.enqueueKey(-7,true);postDelayed(()->c.enqueueKey(-7,false),100);}}
    RectF controlBounds(int code){for(Button button:buttons)if(button.code==code)return new RectF(button.rect);throw new IllegalArgumentException("No control "+code);}
    RectF padBounds(){return new RectF(pad);}
    public RectF viewport(){return new RectF(viewport);}
}
