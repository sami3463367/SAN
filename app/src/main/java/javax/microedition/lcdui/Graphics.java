package javax.microedition.lcdui;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/** Software rasterizer for the original MIDP pixel grid. No sprite resampling. */
public final class Graphics {
    public static final int HCENTER=1, VCENTER=2, LEFT=4, RIGHT=8, TOP=16, BOTTOM=32;
    private final android.graphics.Canvas canvas;
    private final Paint paint = new Paint();
    private final Matrix transform = new Matrix();
    private final Rect clip = new Rect();
    private final RectF oval = new RectF();
    private final int width, height;
    private int tx, ty;
    public Graphics(Bitmap target) {
        canvas = new android.graphics.Canvas(target);
        width = target.getWidth(); height = target.getHeight();
        paint.setAntiAlias(false); paint.setFilterBitmap(false); paint.setDither(false);
        setColor(0); canvas.save();
    }
    public void translate(int x, int y) { tx += x; ty += y; canvas.translate(x,y); }
    public int getTranslateX() { return tx; }
    public int getTranslateY() { return ty; }
    public void setClip(int x, int y, int w, int h) {
        // MIDP setClip replaces the old user clip; Android clipRect only intersects it.
        canvas.restoreToCount(1); canvas.save(); canvas.translate(tx,ty);
        canvas.clipRect(x, y, x+Math.max(0,w), y+Math.max(0,h));
    }
    public int getClipX() { canvas.getClipBounds(clip); return clip.left; }
    public int getClipY() { canvas.getClipBounds(clip); return clip.top; }
    public int getClipWidth() { canvas.getClipBounds(clip); return clip.width(); }
    public int getClipHeight() { canvas.getClipBounds(clip); return clip.height(); }
    public void setColor(int rgb) { paint.setColor(0xff000000 | rgb); }
    public void fillRect(int x, int y, int w, int h) {
        if (w<=0 || h<=0) return;
        paint.setStyle(Paint.Style.FILL); canvas.drawRect(x,y,x+w,y+h,paint);
    }
    public void drawRect(int x,int y,int w,int h) {
        if (w<0 || h<0) return;
        fillRect(x,y,w+1,1); fillRect(x,y+h,w+1,1);
        fillRect(x,y,1,h+1); fillRect(x+w,y,1,h+1);
    }
    public void drawLine(int x1,int y1,int x2,int y2) {
        // MIDP includes both endpoints. Bresenham is deterministic on every Android GPU.
        int dx=Math.abs(x2-x1), sx=x1<x2?1:-1, dy=-Math.abs(y2-y1), sy=y1<y2?1:-1, err=dx+dy;
        for (;;) {
            fillRect(x1,y1,1,1);
            if (x1==x2 && y1==y2) break;
            int e=2*err; if(e>=dy){err+=dy;x1+=sx;} if(e<=dx){err+=dx;y1+=sy;}
        }
    }
    public void drawArc(int x,int y,int w,int h,int start,int sweep) { arc(x,y,w,h,start,sweep,false); }
    public void fillArc(int x,int y,int w,int h,int start,int sweep) { arc(x,y,w,h,start,sweep,true); }
    private void arc(int x,int y,int w,int h,int start,int sweep,boolean fill) {
        if(w<=0 || h<=0) return;
        paint.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE); paint.setStrokeWidth(1);
        oval.set(x+(fill?0:0.5f),y+(fill?0:0.5f),x+w+(fill?0:0.5f),y+h+(fill?0:0.5f));
        canvas.drawArc(oval,-start,-sweep,fill,paint); paint.setStyle(Paint.Style.FILL);
    }
    public void drawRegion(Image image,int sx,int sy,int w,int h,int type,int x,int y,int anchor) {
        if (image==null) throw new NullPointerException("image");
        if(w<0 || h<0 || sx<0 || sy<0 || sx+w>image.getWidth() || sy+h>image.getHeight() || type<0 || type>7)
            throw new IllegalArgumentException("Invalid sprite region: "+sx+","+sy+","+w+","+h);
        if(w==0 || h==0) return;
        int dw=type>=4?h:w, dh=type>=4?w:h;
        if((anchor&HCENTER)!=0)x-=dw/2; else if((anchor&RIGHT)!=0)x-=dw;
        if((anchor&VCENTER)!=0)y-=dh/2; else if((anchor&BOTTOM)!=0)y-=dh;
        float a=1,b=0,c=0,d=1,ox=0,oy=0;
        switch(type) {
            case 1:d=-1;oy=h;break;
            case 2:a=-1;ox=w;break;
            case 3:a=-1;d=-1;ox=w;oy=h;break;
            case 4:a=0;b=1;c=1;d=0;break;
            case 5:a=0;b=-1;c=1;d=0;ox=h;break;
            case 6:a=0;b=1;c=-1;d=0;oy=w;break;
            case 7:a=0;b=-1;c=-1;d=0;ox=h;oy=w;break;
        }
        transform.setValues(new float[]{a,b,ox,c,d,oy,0,0,1});
        int save=canvas.save();
        canvas.translate(x,y); canvas.clipRect(0,0,dw,dh); canvas.concat(transform);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawBitmap(image.bitmap,-sx,-sy,paint); canvas.restoreToCount(save);
    }
}
