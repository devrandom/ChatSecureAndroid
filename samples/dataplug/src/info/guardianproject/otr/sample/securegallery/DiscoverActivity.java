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

import info.guardianproject.otr.sample.securegallery.DiscoverActivity.RequestCache.Request;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.UUID;

import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/**
 * @author liorsaar
 */
public class DiscoverActivity extends Activity {
	
	/**
	 * 
	 */
	private static final String CHARSET = "UTF-8";

	public static final String TAG = DiscoverActivity.class.getSimpleName() ;

	/**
	 * 
	 */
	private static final String URI_GALLERY = "chatsecure:/gallery/";
	private static final String URI_IMAGE = URI_GALLERY + "image/";
	public static final int REQUEST_CODE_GALLERY_LISTING = 6661;

	private Bundle mRequestToLocalExtras; // TODO create a map, keyed by requestID

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			Intent zIntent = getIntent() ;
			handleIntent( zIntent ) ;
		} catch (Throwable e) {
			MainActivity.error( this, e.getMessage() ) ;
			Intent zResultIntent = new Intent();
			setResult(Activity.RESULT_CANCELED, zResultIntent);
		}
	}

	private void handleIntent(Intent aIntent) throws JSONException, IOException {
		String zAction = aIntent.getAction();
		MainActivity.console( "handleIntent: "+zAction ) ;
		if( zAction == null ) {
			return ;
		}
		if( zAction.equals(Api.ACTION_DISCOVER ) ) {
			doDiscover( aIntent ) ;
			finish();
			return ;
		}
		if( zAction.equals(Api.ACTION_ACTIVATE ) ) {
			doActivate( aIntent ) ;
			finish();
			return ;
		}
		if( zAction.equals(Api.ACTION_RESPONSE ) ) {
			doResponse( aIntent ) ;
			finish();
			return ;
		}
		if( zAction.equals(Api.ACTION_REQUEST_TO_LOCAL ) ) {
			doRequestToLocal( aIntent ) ;
			return ;
		}
		MainActivity.error( this, "Unknown action " + zAction ) ;
	}
	
	/*
	 * Alice - initiator - the side that hit the ui first
	 * Bob - recieved - in doRequestToLocal - responds with json
	 */
	private void doRequestToLocal(Intent aIntent) throws JSONException, IOException {
		// look at EXTRA_URI - /gallery/activate
		String zUri = aIntent.getStringExtra( Api.EXTRA_URI );
		if( zUri == null ) {
			MainActivity.error( this, "RequestToLocal: uri=null" ) ;
			return ; // TODO error
		}
		if( zUri.equals( URI_GALLERY )) {
			// repond with : accountid, friendid, requiestid, body(json)
			mRequestToLocalExtras = aIntent.getExtras() ;
			doRequestGallery( this ) ;
			return ;
		}
		if( zUri.startsWith( URI_IMAGE )) {
			// repond with : accountid, friendid, requiestid, image binary
			mRequestToLocalExtras = aIntent.getExtras() ;
			doRequestGalleryImage( this, zUri ) ;
			return ;
		}
		MainActivity.error( this, "Invalid URI: "+ zUri ) ;
	}
	
	private void doRequestGallery(Activity aActivity) {
		MainActivity.console( "doRequestGallery" ) ;
		Intent zIntent = new Intent(Intent.ACTION_PICK);
		zIntent.setType("image/*");
		aActivity.startActivityForResult(zIntent, REQUEST_CODE_GALLERY_LISTING );		
	}
	
	private void doRequestGalleryImage(Activity aActivity, String aUri) throws IOException {
		String contentUriEncoded = aUri.substring( URI_IMAGE.length() ) ;
		String contentUri = URLDecoder.decode(contentUriEncoded, CHARSET);
		MainActivity.console( "doRequestGalleryImage:" + contentUri ) ;
		// reading the binary file
		Uri uri = Uri.parse(contentUri);
		String path = Utils.MediaStoreHelper.getPath(aActivity, uri);
		
		File file = new File(path);
		FileInputStream fis = new FileInputStream(file);
		long length = file.length() ;
		byte[] buffer = new byte[ (int) length ];
				
		fis.read(buffer);
		MainActivity.console( "doRequestGalleryImage:" + buffer.length ) ;
		fis.close();
		sendResponseFromLocal(buffer);
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
			doResponseGalleryImage( zRequest, zContent ) ;
			return ;
		}
	}
	
	private void doResponseGallery( Request aRequest, byte[] aContentByteArray) throws UnsupportedEncodingException, JSONException {
		String content = new String(aContentByteArray, CHARSET);
		MainActivity.console( "doResponseGallery: content=" + content );
		JSONObject jsonObject = new JSONObject( content );
		String responseUri = jsonObject.getString("uri");

		String requestUri = URI_IMAGE + URLEncoder.encode(responseUri, CHARSET);

		sendRequest(aRequest.getAccountId(), aRequest.getFriendId(), requestUri);

		return ;
	}
	
	private void doResponseGalleryImage( Request aRequest, byte[] aContent ) throws UnsupportedEncodingException, JSONException {
		MainActivity.console( "doResponseGalleryImage: uri=" + URLDecoder.decode(aRequest.getUri(), CHARSET));
		MainActivity.console( "doResponseGalleryImage: length=" + aContent.length );

		InputStream is = new ByteArrayInputStream(aContent);
		Bitmap bitmap = BitmapFactory.decodeStream(is);
		if( bitmap == null ) {
			MainActivity.error(this,"Bitmap NULL");
			return ;
		}
		int w = bitmap.getWidth() ;
		int h = bitmap.getHeight() ;
		MainActivity.console( "doResponseGalleryImage: bitmap dim=" + w + "/" + h );
		MainActivity.showBitmap( this, bitmap ) ;
		return ;
	}
	
	public static void showPng( byte[] aByteArray ) {
		InputStream is = new ByteArrayInputStream( aByteArray );
		Bitmap bitmap = BitmapFactory.decodeStream(is);
		int w = bitmap.getWidth() ;
		int h = bitmap.getHeight() ;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_CODE_GALLERY_LISTING:
			if( resultCode != Activity.RESULT_OK) {
				Toast.makeText(this, "ERROR: REQUEST_CODE_GALLERY_LISTING: " + resultCode, Toast.LENGTH_LONG).show(); // TODO doialog
				return ;
			}
			Uri uri = data.getData() ;
			String content = getGalleryListing( uri.toString() ) ;
			sendResponseFromLocal( content.getBytes() ) ;
			break ;
		default:
			Toast.makeText(this, "ERROR: requestCode unknown: " + requestCode, Toast.LENGTH_LONG).show();
		}
	}

	private String getGalleryListing(String aUri) {
		JSONObject json = new JSONObject();
		try {
			json.put( "uri", aUri );
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		return json.toString();
	}

	/**
	 * @param requestGalleryListing
	 * @param string
	 */
	private void sendResponseFromLocal(byte[] aContent) {
		// repond with : accountid, friendid, requiestid, body(json)
		MainActivity.console( "sendResponseFromLocal: content=" + aContent ) ;
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_RESPONSE_FROM_LOCAL) ;
		zIntent.putExtra( Api.EXTRA_ACCOUNT_ID , mRequestToLocalExtras.getString(Api.EXTRA_ACCOUNT_ID) ) ;
		zIntent.putExtra( Api.EXTRA_FRIEND_ID , mRequestToLocalExtras.getString(Api.EXTRA_FRIEND_ID) ) ;
		zIntent.putExtra( Api.EXTRA_REQUEST_ID , mRequestToLocalExtras.getString(Api.EXTRA_REQUEST_ID) ) ;
		zIntent.putExtra( Api.EXTRA_CONTENT, aContent ) ;
		startService( zIntent ) ;
		finish() ;
	}
	
}
