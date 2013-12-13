/*
 * Copyright (C) 2007-2008 Esmertec AG. Copyright (C) 2007-2008 The Android Open
 * Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package info.guardianproject.otr.app.im.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.dataplug.AuthAdapter;
import info.guardianproject.otr.app.im.dataplug.AuthorizationActivity;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.Imps.ProviderSettings;
import info.guardianproject.otr.app.im.provider.Imps.DataplugsColumns.AuthItem;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class SettingActivity extends SherlockPreferenceActivity implements
        OnSharedPreferenceChangeListener {
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 1;
    private static final int REQUEST_CODE_TAKE_PHOTO = 1001;
    private static final int REQUEST_CODE_SELECT_EXISTING = 1002;
    ListPreference mOtrMode;
    CheckBoxPreference mHideOfflineContacts;
    CheckBoxPreference mEnableNotification;
    CheckBoxPreference mNotificationVibrate;
    CheckBoxPreference mNotificationSound;
    CheckBoxPreference mForegroundService;
    EditTextPreference mHeartbeatInterval;
    
    EditTextPreference mThemeBackground;

    private void setInitialValues() {
        ContentResolver cr = getContentResolver();
        Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(cr,
                false /* keep updated */, null /* no handler */);
        mOtrMode.setValue(settings.getOtrMode());
        mHideOfflineContacts.setChecked(settings.getHideOfflineContacts());
        mEnableNotification.setChecked(settings.getEnableNotification());
        mNotificationVibrate.setChecked(settings.getVibrate());
        mNotificationSound.setChecked(settings.getRingtoneURI() != null);
        
        mForegroundService.setChecked(settings.getUseForegroundPriority());
        
        long heartbeatInterval = settings.getHeartbeatInterval();
        if (heartbeatInterval == 0) heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
        mHeartbeatInterval.setText(String.valueOf(heartbeatInterval));

        settings.close();
    }

    /* save the preferences in Imps so they are accessible everywhere */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                getContentResolver(), false /* don't keep updated */, null /* no handler */);

        if (key.equals("pref_security_otr_mode")) {
            settings.setOtrMode(prefs.getString(key, "auto"));
        } else if (key.equals("pref_hide_offline_contacts")) {
            settings.setHideOfflineContacts(prefs.getBoolean(key, false));
        } else if (key.equals("pref_enable_notification")) {
            settings.setEnableNotification(prefs.getBoolean(key, true));
        } else if (key.equals("pref_notification_vibrate")) {
            settings.setVibrate(prefs.getBoolean(key, true));
        } else if (key.equals("pref_notification_sound")) {
            // TODO sort out notification sound pref
            if (prefs.getBoolean(key, true)) {
                settings.setRingtoneURI("android.resource://" + getPackageName() + "/" + R.raw.notify);
            } else {
                settings.setRingtoneURI(null);
            }
        } else if (key.equals("pref_enable_custom_notification")) {
            if (prefs.getBoolean(key, false)) {
                settings.setRingtoneURI("android.resource://" + getPackageName() + "/" + R.raw.notify);
            } else {
                settings.setRingtoneURI(ProviderSettings.RINGTONE_DEFAULT);
            }
        }
        else if (key.equals("pref_foreground_service")) {
            settings.setUseForegroundPriority(prefs.getBoolean(key, false));
        } else if (key.equals("pref_heartbeat_interval")) {
            try
            {
                settings.setHeartbeatInterval(Integer.valueOf(prefs.getString(key, String.valueOf(DEFAULT_HEARTBEAT_INTERVAL))));
            }
            catch (NumberFormatException nfe)
            {
                settings.setHeartbeatInterval((DEFAULT_HEARTBEAT_INTERVAL));
            }
        }
        else if (key.equals("pref_default_locale"))
        {
           ((ImApp)getApplication()).setNewLocale(this, prefs.getString(key, ""));
           setResult(2);
           
        }
        else if (key.equals("themeDark"))
        {
         
            setResult(2);
        }
        
        settings.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        Preference avatar = findPreference("pref_avatar_image");
        avatar.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            
            @Override
            public boolean onPreferenceClick(Preference preference) {
                doAvatarImage(SettingActivity.this);
                return true;
            }
        });

        Preference plugin = findPreference("pref_plugin_authorization");
        plugin.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AuthorizationActivity.doSettings(SettingActivity.this);
                return true;
            }
        });

        mHideOfflineContacts = (CheckBoxPreference) findPreference("pref_hide_offline_contacts");
        mOtrMode = (ListPreference) findPreference("pref_security_otr_mode");
        mEnableNotification = (CheckBoxPreference) findPreference("pref_enable_notification");
        mNotificationVibrate = (CheckBoxPreference) findPreference("pref_notification_vibrate");
        mNotificationSound = (CheckBoxPreference) findPreference("pref_notification_sound");
        // TODO re-enable Ringtone preference
        //mNotificationRingtone = (CheckBoxPreference) findPreference("pref_notification_ringtone");
        mForegroundService = (CheckBoxPreference) findPreference("pref_foreground_service");
        mHeartbeatInterval = (EditTextPreference) findPreference("pref_heartbeat_interval");
        
        mThemeBackground = (EditTextPreference) findPreference("pref_background");
        
        mThemeBackground.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
              
                showThemeChooserDialog ();
                return true;
            }
            
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 888 && data != null && data.getData() != null){
            Uri _uri = data.getData();

            if (_uri != null) {
                //User had pick an image.
                Cursor cursor = getContentResolver().query(_uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
              
                if (cursor != null)
                {
                    cursor.moveToFirst();
    
                    //Link to the image
                    final String imageFilePath = cursor.getString(0);
                    mThemeBackground.setText(imageFilePath);                
                    mThemeBackground.getDialog().cancel();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
        
        if(requestCode == REQUEST_CODE_TAKE_PHOTO) {
            if( resultCode == RESULT_OK ) {
                onActivityResultTakePhoto();
            }
            return;
        }
        
        if(requestCode == REQUEST_CODE_SELECT_EXISTING) {
            if( resultCode == RESULT_OK ) {
                onActivityResultSelectExisting(data);
            }
            return;
        }
    }

    private void showThemeChooserDialog ()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Choose Background");
        builder.setMessage("Do you want to select a background image from the Gallery?");

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 888);

                dialog.dismiss();
            }

        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // I do not need any action here you might
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setInitialValues();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
    }
    
    private ImageView mAvatarImageView;
    
    protected void doAvatarImage( final Context aContext) {
        AlertDialog.Builder builder = new AlertDialog.Builder(aContext);
        View view = LayoutInflater.from(aContext).inflate(R.layout.settings_avatar_image, null);
        mAvatarImageView = (ImageView) view.findViewById(R.id.settings_avatar_image);
        view.findViewById(R.id.settings_avatar_select_existing).setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onClickSelectExisting( aContext );
            }
        });
        
        view.findViewById(R.id.settings_avatar_take_photo).setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onClickTakePhoto( aContext );
            }
        });
        
        builder.setTitle( R.string.settings_set_avatar_image);
        builder.setView(view);
        builder.setNegativeButton(aContext.getString(R.string.cancel), null);
        builder.setPositiveButton(aContext.getString(R.string.ok), new OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
    }

    protected void onClickSelectExisting(Context aContext) {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent,REQUEST_CODE_SELECT_EXISTING);
    }
    
    Uri mImageCaptureUri;
    
    protected void onClickTakePhoto(Context aContext) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mImageCaptureUri = getImageCaptureUri();
        intent.putExtra(MediaStore.EXTRA_OUTPUT,mImageCaptureUri);
        startActivityForResult(intent,REQUEST_CODE_TAKE_PHOTO);
    }
    
    private Uri getImageCaptureUri() {
        String filename = "IMG_" + System.currentTimeMillis() + ".jpg"; // TODO IMG_date_time.jpg
        File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),filename);
        boolean exists = imageFile.exists();
        Uri uri = Uri.fromFile(imageFile);
        return Uri.fromFile(imageFile);
    }
    
    private void onActivityResultSelectExisting(Intent data) {
        if( data == null ) {
            return;
        }
        Uri uri = data.getData();
        mImageCaptureUri = Uri.fromFile( new File(getPath(uri)) );
        try {
            Bitmap bitmap = getCroppedBitmap() ;
            mAvatarImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void onActivityResultTakePhoto() {
        getContentResolver().notifyChange(mImageCaptureUri, null);
        
        try {
            Bitmap bitmap = getCroppedBitmap() ;
            mAvatarImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private Bitmap getCroppedBitmap() throws IOException {
        Bitmap finalBitmap;
        Bitmap sourceBitmap = getScaledBitmap(mImageCaptureUri.getPath(), 256);
        
        ExifInterface exif = new ExifInterface( mImageCaptureUri.getPath() );
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        if( orientation == ExifInterface.ORIENTATION_ROTATE_90 ) { // 6
            Bitmap croppedBitmap = ThumbnailUtils.extractThumbnail(sourceBitmap, 128, 128);
            finalBitmap = bitmapRotate( croppedBitmap, 90);
        } else if( orientation == ExifInterface.ORIENTATION_ROTATE_270 ) { // 8
            Bitmap croppedBitmap = ThumbnailUtils.extractThumbnail(sourceBitmap, 128, 128);
            finalBitmap = bitmapRotate( croppedBitmap, 270);
        } else {
            finalBitmap = ThumbnailUtils.extractThumbnail(sourceBitmap, 128, 128);
        }
        return finalBitmap;
    }

    public static Bitmap bitmapRotate(Bitmap aSourcBitmap, int aDegrees) {
        if( aDegrees == 0  ||  aSourcBitmap == null ) {
            return aSourcBitmap ;
        }
        Matrix m = new Matrix();

        m.setRotate(aDegrees, (float) aSourcBitmap.getWidth() / 2, (float) aSourcBitmap.getHeight() / 2);
        Bitmap targetBitmap = Bitmap.createBitmap(aSourcBitmap, 0, 0, aSourcBitmap.getWidth(), aSourcBitmap.getHeight(), m, true);
        return targetBitmap;
    }
    

    public static Bitmap getScaledBitmap( String aPath, int destWidth ) throws IOException {
        InputStream is = new FileInputStream(new File( aPath ));
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, bitmapOptions);
        is.close();
        is = null;
        
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
    
    public String getPath(Uri uri) { 
        String filename = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if( cursor == null) {
            throw new RuntimeException("Error getting filename for " + uri);
        }
        try {
            if( !cursor.moveToFirst()) {
                throw new RuntimeException("Error getting filename for " + uri);
            }
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            filename = cursor.getString(index);
        } finally {
            cursor.close() ;
        }
        return filename ;
    }     
    
}
