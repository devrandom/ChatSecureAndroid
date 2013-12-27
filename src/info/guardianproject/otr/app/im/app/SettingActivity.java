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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.dataplug.AuthorizationActivity;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.Imps.ProviderSettings;
import info.guardianproject.util.BitmapUtils;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class SettingActivity extends SherlockPreferenceActivity implements
        OnSharedPreferenceChangeListener {
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 1;
    private static final int REQUEST_CODE_TAKE_PHOTO = 1001;
    private static final int REQUEST_CODE_SELECT_EXISTING = 1002;
    private static final int AVATAR_IMAGE_SIZE = 128;
    private static final String AVATAR_FILENAME = "avatar.jpg";
    ListPreference mOtrMode;
    CheckBoxPreference mHideOfflineContacts;
    CheckBoxPreference mEnableNotification;
    CheckBoxPreference mNotificationVibrate;
    CheckBoxPreference mNotificationSound;
    CheckBoxPreference mForegroundService;
    EditTextPreference mHeartbeatInterval;
    
    EditTextPreference mThemeBackground;
    private ImageView mAvatarImageView;
    private Uri mImageCaptureUri;

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
                showAvatarImageDialog(SettingActivity.this);
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
            } else {
                // Cancelled
            }
            return;
        }
        
        if(requestCode == REQUEST_CODE_SELECT_EXISTING) {
            if( resultCode == RESULT_OK ) {
                onActivityResultSelectExisting(data);
            } else {
                // Cancelled
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
    
    protected void showAvatarImageDialog( final Context aContext) {
        AlertDialog.Builder builder = new AlertDialog.Builder(aContext);
        View view = LayoutInflater.from(aContext).inflate(R.layout.settings_avatar_image, null);
        mAvatarImageView = (ImageView) view.findViewById(R.id.settings_avatar_image);
        try {
            getAvatarImage();
        } catch ( IOException e) {
        }
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
                onClickSetAvatarImage();
            }
        });
        builder.create().show();
    }


    protected void onClickSetAvatarImage() {
        // TODO upload image to ???
        try {
            FileInputStream fis = openFileInput(AVATAR_FILENAME);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void onClickSelectExisting(Context aContext) {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent,REQUEST_CODE_SELECT_EXISTING);
    }
    
    protected void onClickTakePhoto(Context aContext) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mImageCaptureUri = getImageCaptureUri();
        intent.putExtra(MediaStore.EXTRA_OUTPUT,mImageCaptureUri);
        startActivityForResult(intent,REQUEST_CODE_TAKE_PHOTO);
    }
    
    private void onActivityResultSelectExisting(Intent data) {
        if( data == null  ||  data.getData() == null ) {
            return;
        }
        try {
            Uri selectedUri = Uri.fromFile( new File(getPath( data.getData() )) );
            setAvatarImage(selectedUri);
        } catch (Throwable t) {
            // OOM caught here
        }
    }

    private void onActivityResultTakePhoto() {
        try {
            setAvatarImage(mImageCaptureUri);
        } catch (Throwable t) {
            // OOM caught here
        }
    }
    
    private void setAvatarImage( Uri aUri ) throws IOException {
        Bitmap avatarBitmap = BitmapUtils.getCroppedBitmap( aUri, AVATAR_IMAGE_SIZE ) ;
        mAvatarImageView.setImageBitmap(avatarBitmap);
        
        // save image
        FileOutputStream fos = openFileOutput(AVATAR_FILENAME, Context.MODE_PRIVATE);
        avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
    }
    
    private void getAvatarImage() throws IOException {
        // read image
        Bitmap avatarBitmap = BitmapFactory.decodeStream(openFileInput(AVATAR_FILENAME));
        mAvatarImageView.setImageBitmap(avatarBitmap);
    }
    
    private Uri getImageCaptureUri() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_hhmm", Locale.US); // TODO get locale 
        String formattedDate = formatter.format( new Date(System.currentTimeMillis()));
        String filename = "IMG_" + formattedDate + ".jpg";
        File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),filename);
        return Uri.fromFile(imageFile);
    }
    
    private String getPath(Uri uri) { 
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
