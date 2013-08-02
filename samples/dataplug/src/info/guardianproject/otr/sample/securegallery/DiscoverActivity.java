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

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * @author liorsaar
 */
public class DiscoverActivity extends Activity {
	
	public static final String TAG = DiscoverActivity.class.getSimpleName() ;

	/**
	 * 
	 */
	private static final String URI_GALLERY = "chatsecure:/gallery/";
	private static final int REQUEST_CODE_GALLERY_LISTING = 6661;

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
	private void doRequestToLocal(Intent aIntent) throws JSONException {
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
		MainActivity.error( this, "Invalid URI: "+ zUri ) ;
	}
	
	private void doRequestGallery(Activity aActivity) {
		MainActivity.console( "doRequestGallery" ) ;
		Intent zIntent = new Intent(Intent.ACTION_PICK);
		zIntent.setType("image/*");
		aActivity.startActivityForResult(zIntent, REQUEST_CODE_GALLERY_LISTING );		
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
	
	private static String sRequestId ;
	
	private void doActivate(Intent aIntent){
		String zFriendId = aIntent.getStringExtra(Api.EXTRA_FRIEND_ID);
		String zAccountId = aIntent.getStringExtra(Api.EXTRA_ACCOUNT_ID);
		MainActivity.console( "doActivate: Friend:" + zFriendId ) ;
		sRequestId = "123456798" ;
		sendRequest(zAccountId, zFriendId, sRequestId);
	}
	
	private void sendRequest(String aAccountId, String aFriendId, String aRequestId) {
		MainActivity.console( "sendRequest: EXTRA_URI:" + URI_GALLERY ) ;
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_REQUEST) ;
		zIntent.putExtra( Api.EXTRA_METHOD , "GET" ) ;
		zIntent.putExtra( Api.EXTRA_URI , URI_GALLERY ) ;
		zIntent.putExtra( Api.EXTRA_ACCOUNT_ID , aAccountId ) ;
		zIntent.putExtra( Api.EXTRA_FRIEND_ID , aFriendId ) ;
		zIntent.putExtra( Api.EXTRA_REQUEST_ID , aRequestId ) ;
		startService( zIntent ) ;
	}
	
	private void doResponse(Intent aIntent) throws IOException{
		String zFriendId = aIntent.getStringExtra(Api.EXTRA_FRIEND_ID);
		String zRequestId = aIntent.getStringExtra(Api.EXTRA_REQUEST_ID);
		String zContent = aIntent.getStringExtra(Api.EXTRA_CONTENT);
		if( ! sRequestId.equals(zRequestId) ) {
			MainActivity.error( this, "Request id mismatch: " + zRequestId ) ;
			return ;
		}
		// launch target
		launch( zFriendId, zContent ) ;
	}
	
	private void launch(String aFriendId, String aContent) {
		MainActivity.console( "launch: content=" + aContent ) ;
		// TODO your code here !!!
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
			sendResponseFromLocal( Api.REQUEST_GALLERY_LISTING, content ) ;
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
	private void sendResponseFromLocal(String aRequest, String aContent) {
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
