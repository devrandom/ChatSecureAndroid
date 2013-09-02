/**
 * 
 */
package info.guardianproject.otr.sample.securegallery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Copyright (C) 2013 guardian.  All rights reserved.
 *
 * @author liorsaar
 *
 */
public class Utils {
	public static class MediaStoreHelper {
	    public static String getPath(Context aContext, Uri uri) {
	        if (uri.getScheme().equals("file")) {
	            return uri.getPath();
	        }
	    
	        Cursor cursor = aContext.getContentResolver().query(uri, null, null, null, null);
	        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	        cursor.moveToFirst();
	        return cursor.getString(column_index);
	    }

		public static long getImageLength(Context aContext, String contentUri)
				throws FileNotFoundException, IOException {
			// reading the binary file
			Uri uri = Uri.parse(contentUri);
			String path = Utils.MediaStoreHelper.getPath(aContext, uri);
			
			File file = new File(path);
			return file.length();
		}

		public static byte[] getImageContent(Context aContext, String contentUri, int aStart, int aEnd)
				throws IOException {
			// reading the binary file
			Uri uri = Uri.parse(contentUri);
			String path = Utils.MediaStoreHelper.getPath(aContext, uri);
			
			File file = new File(path);
			int length = (int)file.length() ;
			if (aStart > length || aEnd < aStart) {
				return new byte[0];
			}

			FileInputStream fis = new FileInputStream(file);
			fis.skip(aStart);
			if (aEnd >= length)
				aEnd = length - 1;
			byte[] buffer = new byte[ aEnd - aStart + 1 ];
					
			fis.read(buffer);
			fis.close();
			return buffer;
		}

		public static String sha1sum(Context aContext, String contentUri) throws IOException {
			Uri uri = Uri.parse(contentUri);
			String path = Utils.MediaStoreHelper.getPath(aContext, uri);
			
			File file = new File(path);
	        FileInputStream fis = new FileInputStream(file);

	        try {
	        	return Utils.sha1sum(fis.getChannel());
	        } finally {
	        	fis.close();
	        }
		}
	}

	private static String hexChr(int b) {
        return Integer.toHexString(b & 0xF);
    }

    private static String toHex(int b) {
        return hexChr((b & 0xF0) >> 4) + hexChr(b & 0x0F);
    }

    public static String sha1sum(byte[] bytes) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        digest.update(bytes, 0, bytes.length);
        byte[] sha1sum = digest.digest();
        String display = "";
        for(byte b : sha1sum)
            display += toHex(b);
        return display;
    }

	public static String sha1sum(FileChannel channel) throws IOException {
		MessageDigest digest;
		try {
		    digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
		    throw new RuntimeException(e);
		}

		ByteBuffer buffer = ByteBuffer.allocate(32768);
		while (channel.read(buffer) > 0) {
			buffer.flip();
		    digest.update(buffer);
		    buffer.clear();
		}
		
		byte[] sha1sum = digest.digest();
		String display = "";
		for(byte b : sha1sum)
		    display += toHex(b);
		return display;
	}

	public static String sanitize(String path) {
        try {
            return URLEncoder.encode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Bitmap getScaledBitmap( String aPath, int destWidth ) throws IOException {
            InputStream is = new FileInputStream(new File( aPath ));
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, bitmapOptions);
            is.close();
            is = null;
            MainActivity.console( "getScaledBitmap: source dim=" + bitmapOptions.outWidth + "/" + bitmapOptions.outHeight );
            
            if(destWidth == 0) destWidth = bitmapOptions.outWidth;
    //        if(destHeight == 0) destHeight = bitmapOptions.outHeight;
            int widthScale = bitmapOptions.outWidth / destWidth;
    //        int heightScale = bitmapOptions.outHeight / destHeight;
    //        int targetScale = widthScale < heightScale ? widthScale : heightScale;
            bitmapOptions.inSampleSize = widthScale;
            bitmapOptions.inJustDecodeBounds = false;
    
            is = new FileInputStream(new File( aPath ));
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, bitmapOptions);
            is.close();
            is = null;
            return bitmap;
        }
}
