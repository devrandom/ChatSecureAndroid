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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


/**
 * @author liorsaar
 */
public class DataplugService extends Service {
	private static final String CHARSET = "UTF-8";

	public static final String TAG = DataplugService.class.getSimpleName() ;

	private static final String URI_GALLERY = "chatsecure:/gallery/";
	private static final String URI_IMAGE = URI_GALLERY + "image/";
	private Bundle mRequestToLocalExtras; // TODO create a map, keyed by requestID

	@Override
	public int onStartCommand(Intent aIntent, int flags, int startId) {
		try {
			handleIntent( aIntent ) ;
		} catch (Throwable e) {
			MainActivity.error( this, e.getMessage() ) ;
		}
		return START_NOT_STICKY;
	}

	private void handleIntent(Intent aIntent) throws JSONException, IOException {
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
			doRequestToLocal( aIntent ) ;
			return ;
		}
		if( zAction.equals(Api.ACTION_RESPONSE_FROM_LOCAL ) ) {
			doResponseFromLocal( aIntent ) ;
			return ;
		}
		MainActivity.error( this, "Unknown action " + zAction ) ;
	}
	
	private void doResponseFromLocal(Intent aIntent) {
		// FIXME see further refactoring needed in sendResponseFromLocal
		sendResponseFromLocal(aIntent.getByteArrayExtra(Api.EXTRA_CONTENT));
	}

	/*
	 * Alice - initiator - the side that hit the ui first
	 * Bob - received - in doRequestToLocal - responds with json
	 */
	private void doRequestToLocal(Intent aIntent) throws JSONException, IOException {
		// look at EXTRA_URI - /gallery/activate
		String zUri = aIntent.getStringExtra( Api.EXTRA_URI );
		if( zUri == null ) {
			MainActivity.error( this, "RequestToLocal: uri=null" ) ;
			return ; // TODO error
		}
		if( zUri.equals( URI_GALLERY )) {
			// repond with : accountid, friendid, requestid, body(json)
			mRequestToLocalExtras = aIntent.getExtras() ;
			Intent intent = new Intent(this, MainActivity.class);
			intent.setAction("info.guardianproject.otr.app.im.dataplug.REQUEST_GALLERY");
			startActivity(intent);
			return ;
		}
		if( zUri.startsWith( URI_IMAGE )) {
			// repond with : accountid, friendid, requestid, image binary
			mRequestToLocalExtras = aIntent.getExtras() ;
			String contentUriEncoded = zUri.substring( URI_IMAGE.length() ) ;
			String contentUri = URLDecoder.decode(contentUriEncoded, CHARSET);
			Intent intent = new Intent(this, MainActivity.class);
			intent.setAction("info.guardianproject.otr.app.im.dataplug.REQUEST_GALLERY_IMAGE");
			intent.putExtra(Api.EXTRA_URI, contentUri);
			startActivity(intent);
			return ;
		}
		MainActivity.error( this, "Invalid URI: "+ zUri ) ;
	}
	
	private void doDiscover(Intent aIntent) throws JSONException {
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

	private static final String REGISTRATION = "{ 'descriptor': 	{ 'uri': 'chatsecure:/gallery', 'name': 'Gallery' }, 'meta': { 'publish' : true } }" ;

	private String getRegistration() throws JSONException {
		JSONObject json = new JSONObject( REGISTRATION );				
		return json.toString() ;
	}
	
	private void doActivate(Intent aIntent){
		String zFriendId = aIntent.getStringExtra(Api.EXTRA_FRIEND_ID);
		String zAccountId = aIntent.getStringExtra(Api.EXTRA_ACCOUNT_ID);
		MainActivity.console( "doActivate: Friend:" + zFriendId ) ;
		sendRequest(zAccountId, zFriendId, URI_GALLERY);
	}
	
	static class RequestCache {
		static class Request {
			String mAccountId;
			String mFriendId;
			String mUri;
			public Request( String aAccountId, String aFriendId, String aUri) {
				mAccountId = aAccountId ;
				mFriendId = aFriendId ;
				mUri = aUri ;
			}
			public String getUri() { return mUri ; }
			public String getFriendId() { return mFriendId ; }
			public String getAccountId() { return mAccountId ; }
		}
		
		static Cache<String, Request> sCache ;
		
		public static String create(String aAccountId, String aFriendId, String aUri) {
			if( sCache == null ) {
				sCache = CacheBuilder.newBuilder().maximumSize(100).build();
			}
			Request request = new Request( aAccountId, aFriendId, aUri);
			String key = UUID.randomUUID().toString();
			sCache.put(key, request);
			return key ;
		}
		
		public static Request get( String aReqestId ) {
			return sCache.getIfPresent(aReqestId);
		}
		
	}
	
	private void sendRequest(String aAccountId, String aFriendId, String aUri ) {
		
		String requestId = RequestCache.create( aAccountId, aFriendId, aUri ) ;

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
	
	private void doResponse(Intent aIntent) throws IOException, JSONException{
		String zFriendId = aIntent.getStringExtra(Api.EXTRA_FRIEND_ID);
		MainActivity.console( "doResponse: EXTRA_FRIEND_ID:" + zFriendId );
		String zRequestId = aIntent.getStringExtra(Api.EXTRA_REQUEST_ID);
		byte[] zContent = aIntent.getByteArrayExtra(Api.EXTRA_CONTENT);
		
		RequestCache.Request zRequest = RequestCache.get(zRequestId);
		if( zRequest == null ) {
			MainActivity.error( this, "Request not found: " + zRequestId ) ;
			return ;
		}
		if( zRequest.getUri().equals(URI_GALLERY) ) {
			doResponseGallery( zRequest, zContent ) ;
			return ;
		}
		if( zRequest.getUri().startsWith(URI_IMAGE) ) {
			MainActivity.console( "doResponseGalleryImage: uri=" + URLDecoder.decode(zRequest.getUri(), CHARSET));
			Intent intent = new Intent(this, MainActivity.class);
			intent.setAction("info.guardianproject.otr.app.im.dataplug.SHOW_IMAGE");
			intent.putExtra(Api.EXTRA_CONTENT, zContent);
			startActivity(intent);
			return ;
		}
	}
	
	private void doResponseGallery( RequestCache.Request aRequest, byte[] aContentByteArray) throws UnsupportedEncodingException, JSONException {
		String content = new String(aContentByteArray, CHARSET);
		MainActivity.console( "doResponseGallery: content=" + content );
		JSONObject jsonObject = new JSONObject( content );
		String responseUri = jsonObject.getString("uri");

		String requestUri = URI_IMAGE + URLEncoder.encode(responseUri, CHARSET);

		sendRequest(aRequest.getAccountId(), aRequest.getFriendId(), requestUri);

		return ;
	}
	
	public static void showPng( byte[] aByteArray ) {
		InputStream is = new ByteArrayInputStream( aByteArray );
		Bitmap bitmap = BitmapFactory.decodeStream(is);
		int w = bitmap.getWidth() ;
		int h = bitmap.getHeight() ;
	}
	
	/**
	 * @param requestGalleryListing
	 * @param string
	 */
	private void sendResponseFromLocal(byte[] aContent) {
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
}
