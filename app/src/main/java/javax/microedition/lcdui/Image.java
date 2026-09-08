package javax.microedition.lcdui;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;
import java.io.InputStream;
public final class Image {
    final Bitmap bitmap;
    public Image(Bitmap bitmap) { this.bitmap = bitmap; }
    public static Image createImage(String name) throws IOException {
        try (InputStream in = Image.class.getResourceAsStream(name)) { return createImage(in); }
    }
    public static Image createImage(InputStream in) throws IOException {
        if (in == null) throw new IOException("Missing original image");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
        if (bitmap == null) throw new IOException("Invalid original PNG");
        bitmap.setDensity(Bitmap.DENSITY_NONE);
        return new Image(bitmap);
    }
    public int getWidth() { return bitmap.getWidth(); }
    public int getHeight() { return bitmap.getHeight(); }
}
