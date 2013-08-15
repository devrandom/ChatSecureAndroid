/**
 * 
 */
package info.guardianproject.otr.sample.securegallery;

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
	}
}
