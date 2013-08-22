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
package info.guardianproject.otr.sample.securegallery;

import java.net.URLDecoder;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


/**
 * @author liorsaar
 */
public abstract class DataplugService extends Service {
	public static interface RequestCallback {
		void onResponse(Request aRequest, byte [] aContent);
	}

	protected static class RequestCache {
		static Cache<String, Request> sCache ;

		public static String create(String aAccountId, String aFriendId, String aUri, RequestCallback aCallback) {
			if( sCache == null ) {
				sCache = CacheBuilder.newBuilder().maximumSize(100).build();
			}
			Request request = new Request( aAccountId, aFriendId, aUri, aCallback);
			String key = UUID.randomUUID().toString();
			sCache.put(key, request);
			return key ;
		}

		public static Request get( String aReqestId ) {
			return sCache.getIfPresent(aReqestId);
		}

	}

	public static class Request {
		String mAccountId;
		String mFriendId;
		String mUri;
		RequestCallback mCallback;
		
		public Request( String aAccountId, String aFriendId, String aUri, RequestCallback aCallback) {
			mAccountId = aAccountId ;
			mFriendId = aFriendId ;
			mUri = aUri ;
			mCallback = aCallback;
		}
		public String getUri() { return mUri ; }
		public String getFriendId() { return mFriendId ; }
		public String getAccountId() { return mAccountId ; }
		
		public RequestCallback getCallback() {
			return mCallback;
		}
	}

	public static final String TAG = DataplugService.class.getSimpleName() ;

	protected Bundle mRequestToLocalExtras;

	@Override
	public int onStartCommand(Intent aIntent, int flags, int startId) {
		try {
			handleIntent( aIntent ) ;
		} catch (Throwable e) {
			MainActivity.error( this, e.getMessage() ) ;
		}
		return START_NOT_STICKY;
	}

	protected void doDiscover(Intent aIntent) throws JSONException {
		String token = aIntent.getStringExtra( Api.EXTRA_TOKEN );
		MainActivity.console( "doDiscover: " + token ) ;
		if( token == null ) {
			MainActivity.error( this, "doDiscover: token=null" ) ;
			return ;
		}
		MainActivity.console( "doDiscover: token=" + token ) ;
		sendRegistration( token ) ;
	}

	private void sendRegistration(String token) throws JSONException {
		MainActivity.console( "sendRegistration: " + REGISTRATION ) ;
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_REGISTER) ;
		zIntent.putExtra( Api.EXTRA_TOKEN , token ) ;
		zIntent.putExtra( Api.EXTRA_REGISTRATION, getRegistration() ) ;
		startService( zIntent ) ;
	}

	private static final String REGISTRATION = "{ 'descriptor': 	{ 'uri': 'chatsecure:/gallery', 'name': 'Gallery' }, 'meta': { 'publish' : true } }";

	private String getRegistration() throws JSONException {
		JSONObject json = new JSONObject( REGISTRATION );				
		return json.toString() ;
	}

	private void doActivate(Intent aIntent) {
		String zFriendId = aIntent.getStringExtra(Api.EXTRA_FRIEND_ID);
		String zAccountId = aIntent.getStringExtra(Api.EXTRA_ACCOUNT_ID);
		MainActivity.console( "doActivate: Friend:" + zFriendId ) ;
		doActivate(zAccountId, zFriendId);
	}
	
	protected abstract void doActivate(String aAccountId, String aFriendId) ;

	protected static final String CHARSET = "UTF-8";

	protected void sendRequest(String aAccountId, String aFriendId, String aUri, RequestCallback aCallback) {
		
		String requestId = RequestCache.create( aAccountId, aFriendId, aUri, aCallback ) ;
	
		MainActivity.console( "sendRequest: EXTRA_URI:" + aUri ) ;
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_REQUEST) ;
		zIntent.putExtra( Api.EXTRA_METHOD , "GET" ) ;
		zIntent.putExtra( Api.EXTRA_URI, aUri ) ;
		zIntent.putExtra( Api.EXTRA_ACCOUNT_ID , aAccountId ) ;
		zIntent.putExtra( Api.EXTRA_FRIEND_ID , aFriendId ) ;
		zIntent.putExtra( Api.EXTRA_REQUEST_ID , requestId ) ;
		startService( zIntent ) ;
	}

	protected void doResponse(Intent aIntent) throws Exception {
		String zFriendId = aIntent.getStringExtra(Api.EXTRA_FRIEND_ID);
		MainActivity.console( "doResponse: EXTRA_FRIEND_ID:" + zFriendId );
		String zRequestId = aIntent.getStringExtra(Api.EXTRA_REQUEST_ID);
		byte[] zContent = aIntent.getByteArrayExtra(Api.EXTRA_CONTENT);
		
		Request zRequest = RequestCache.get(zRequestId);

		if( zRequest == null ) {
			MainActivity.error( this, "Request not found: " + zRequestId ) ;
			return ;
		}
		
		MainActivity.console( "doResponse: uri=" + URLDecoder.decode(zRequest.getUri(), CHARSET));
		zRequest.getCallback().onResponse(zRequest, zContent);
	}

	/**
	 * @param requestGalleryListing
	 * @param string
	 */
	protected void sendResponseFromLocal(byte[] aContent) {
		// respond with : accountid, friendid, requiestid, body(json)
		MainActivity.console( "sendResponseFromLocal: content=" + aContent ) ;
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_RESPONSE_FROM_LOCAL) ;
		// FIXME mRequestToLocalExtras needs to be something that causes race conditions
		zIntent.putExtra( Api.EXTRA_ACCOUNT_ID , mRequestToLocalExtras.getString(Api.EXTRA_ACCOUNT_ID) ) ;
		zIntent.putExtra( Api.EXTRA_FRIEND_ID , mRequestToLocalExtras.getString(Api.EXTRA_FRIEND_ID) ) ;
		zIntent.putExtra( Api.EXTRA_REQUEST_ID , mRequestToLocalExtras.getString(Api.EXTRA_REQUEST_ID) ) ;
		zIntent.putExtra( Api.EXTRA_CONTENT, aContent ) ;
		startService( zIntent ) ;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	void handleIntent(Intent aIntent) throws Exception {
		String zAction = aIntent.getAction();
		MainActivity.console( "handleIntent: "+zAction ) ;
		if( zAction == null ) {
			return ;
		}
		if( zAction.equals(Api.ACTION_DISCOVER ) ) {
			doDiscover( aIntent ) ;
			return ;
		}
		if( zAction.equals(Api.ACTION_ACTIVATE ) ) {
			doActivate( aIntent ) ;
			return ;
		}
		if( zAction.equals(Api.ACTION_RESPONSE ) ) {
			doResponse( aIntent ) ;
			return ;
		}
		if( zAction.equals(Api.ACTION_REQUEST_TO_LOCAL ) ) {
			mRequestToLocalExtras = aIntent.getExtras() ;
			
			String zUri = aIntent.getStringExtra( Api.EXTRA_URI );
			if( zUri == null ) {
				MainActivity.error( this, "RequestToLocal: uri=null" ) ;
				return ; // TODO error
			}
			
			doRequestToLocal( zUri ) ;
			return ;
		}
		if( zAction.equals(Api.ACTION_RESPONSE_FROM_LOCAL ) ) {
			doResponseFromLocal( aIntent ) ;
			return ;
		}
		MainActivity.error( this, "Unknown action " + zAction ) ;
	}
	
	protected void doResponseFromLocal(Intent aIntent) {
		// FIXME see further refactoring needed in sendResponseFromLocal
		// dispatch based on request id
		sendResponseFromLocal(aIntent.getByteArrayExtra(Api.EXTRA_CONTENT));
	}

	abstract protected void doRequestToLocal(String zUri) throws Exception ;

}
