/*
 * Copyright (C) 2013 The Android Open Source Project
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
package info.guardianproject.otr.sample.securegallery.loopback;

import info.guardianproject.otr.dataplug.Api;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.DownloadManager.Request;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

/**
 * @author liorsaar
 */
public class LoopbackService extends Service {
	public static final String TAG = LoopbackService.class.getSimpleName();

	public static final String URI_GALLERY = "chatsecure:/gallery/";
	public static final String URI_IMAGE = URI_GALLERY + "image/";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction() == null) {
			return 0;
		}

		if (intent.getAction().equals(Api.ACTION_OUTGOING_REQUEST)) {
			String method = intent.getExtras().getString(Api.EXTRA_METHOD);
			String uri = intent.getExtras().getString(Api.EXTRA_URI);
			String friendId = intent.getExtras().getString(Api.EXTRA_FRIEND_ID);
			String accountId = intent.getExtras().getString(Api.EXTRA_ACCOUNT_ID);
			String requestId = intent.getExtras().getString(Api.EXTRA_REQUEST_ID);
			String headers = intent.getExtras().getString(Api.EXTRA_HEADERS);
			byte[] content = intent.getExtras().getByteArray(Api.EXTRA_CONTENT);
			Log.e( TAG, "uri:" + uri );
			return 0;
		}

		if (intent.getAction().equals(Api.ACTION_OUTGOING_RESPONSE)) {
			try {
				handleOutgoingResponse( intent.getExtras() ) ;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return 0;
		}
		Log.e(Api.DATAPLUG_TAG, "unknown action " + intent.getAction());

		return super.onStartCommand(intent, flags, startId);
	}

	private void handleOutgoingResponse(Bundle extras) throws UnsupportedEncodingException {
		String friendId = extras.getString(Api.EXTRA_FRIEND_ID);
		String accountId = extras.getString(Api.EXTRA_ACCOUNT_ID);
		String requestId = extras.getString(Api.EXTRA_REQUEST_ID);
		String headers = extras.getString(Api.EXTRA_HEADERS);
		byte[] content = extras.getByteArray(Api.EXTRA_CONTENT);
		Log.e( TAG, "content:" + (new String(content,"UTF-8")) );
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
