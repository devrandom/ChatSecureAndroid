/**
 * 
 */
package info.guardianproject.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;

/**
 * 
 * @author liorsaar
 * 
 */

public class BitmapUtils {
    public static Bitmap getCroppedBitmap(Uri aImageCaptureUri, int aWidth) throws IOException {
        Bitmap finalBitmap;
        Bitmap sourceBitmap = getScaledBitmap(aImageCaptureUri.getPath(), aWidth*2);

        ExifInterface exif = new ExifInterface(aImageCaptureUri.getPath());
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) { // 6
            Bitmap croppedBitmap = ThumbnailUtils.extractThumbnail(sourceBitmap, aWidth, aWidth);
            finalBitmap = bitmapRotate(croppedBitmap, 90);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) { // 8
            Bitmap croppedBitmap = ThumbnailUtils.extractThumbnail(sourceBitmap, aWidth, aWidth);
            finalBitmap = bitmapRotate(croppedBitmap, 270);
        } else {
            finalBitmap = ThumbnailUtils.extractThumbnail(sourceBitmap, aWidth, aWidth);
        }
        return finalBitmap;
    }

    public static Bitmap bitmapRotate(Bitmap aSourcBitmap, int aDegrees) {
        if (aDegrees == 0 || aSourcBitmap == null) {
            return aSourcBitmap;
        }
        Matrix m = new Matrix();
        m.setRotate(aDegrees, (float) aSourcBitmap.getWidth() / 2, (float) aSourcBitmap.getHeight() / 2);
        Bitmap targetBitmap = Bitmap.createBitmap(aSourcBitmap, 0, 0, aSourcBitmap.getWidth(), aSourcBitmap.getHeight(), m, true);
        return targetBitmap;
    }

    public static Bitmap getScaledBitmap(String aPath, int destWidth) throws IOException {
        InputStream is = new FileInputStream(new File(aPath));
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, bitmapOptions);
        is.close();
        is = null;

        if (destWidth == 0)
            destWidth = bitmapOptions.outWidth;
        // if(destHeight == 0) destHeight = bitmapOptions.outHeight;
        int widthScale = bitmapOptions.outWidth / destWidth;
        // int heightScale = bitmapOptions.outHeight / destHeight;
        // int targetScale = widthScale < heightScale ? widthScale : heightScale;
        bitmapOptions.inSampleSize = widthScale;
        bitmapOptions.inJustDecodeBounds = false;

        is = new FileInputStream(new File(aPath));
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, bitmapOptions);
        is.close();
        is = null;
        return bitmap;
    }

}
