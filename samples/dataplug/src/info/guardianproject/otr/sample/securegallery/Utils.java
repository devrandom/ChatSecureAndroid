/**
 * 
 */
package info.guardianproject.otr.sample.securegallery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
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
				throws FileNotFoundException, IOException {
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
	}
}
