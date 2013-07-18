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
import android.widget.Toast;

/**
 * @author liorsaar
 */
public class DiscoverActivity extends Activity {

	private static final int REQUEST_CODE_GALLERY_LISTING = 6661;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			Intent zIntent = getIntent() ;
			handleIntent( zIntent ) ;
		} catch (Throwable e) {
			Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
			Intent zResultIntent = new Intent();
			setResult(Activity.RESULT_CANCELED, zResultIntent);
		}
	}

	private void handleIntent(Intent aIntent) throws JSONException, IOException {
		String zAction = aIntent.getAction();
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
		Toast.makeText(this, "Error: unknown action " + zAction, Toast.LENGTH_LONG).show();
	}
	private void doRequestToLocal(Intent aIntent) throws JSONException {
		String zRequest = aIntent.getStringExtra( Api.EXTRA_REQUEST );
		if( zRequest == null ) {
			Toast.makeText(this, "Error: RequestToLocal: request=null", Toast.LENGTH_LONG).show();
		}
		if( Api.REQUEST_GALLERY_LISTING.equals( zRequest ) ) {
			doRequestGalleryListing( this ) ;
		}
	}
	
	/**
	 * @param aActivity 
	 * 
	 */
	private void doRequestGalleryListing(Activity aActivity) {
		Intent zIntent = new Intent(Intent.ACTION_PICK);
		zIntent.setType("image/*");
		aActivity.startActivityForResult(zIntent, REQUEST_CODE_GALLERY_LISTING );		
	}

	private void doDiscover(Intent aIntent) throws JSONException {
		String token = aIntent.getStringExtra( Api.EXTRA_TOKEN );
		if( token != null ) {
			Toast.makeText(this, "Discover: token="+token, Toast.LENGTH_LONG).show();
			sendRegistration( token ) ;
			return ;
		} else {
			Toast.makeText(this, "Discover: NO TOKE", Toast.LENGTH_LONG).show();
			token = "Discover: No value for 'token'";
		}
	}

	private void sendRegistration(String token) throws JSONException {
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_REGISTER) ;
		zIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		zIntent.putExtra( Api.EXTRA_TOKEN , token ) ;
		zIntent.putExtra( Api.EXTRA_REGISTRATION, getRegistration() ) ;
		startActivity( zIntent ) ;
	}

	private static final String REGISTRATION = "{ 'descriptor': 	{ 'uri': 'chatsecure:/gallery', 'name': 'Gallery' }, 'meta': { 'publish' : true } }" ;

	private String getRegistration() throws JSONException {
		JSONObject json = new JSONObject( REGISTRATION );				
		return json.toString() ;
	}
	
	private static String sRequestId ;
	
	private void doActivate(Intent aIntent){
		String zFriendId = aIntent.getStringExtra(Api.EXTRA_FRIEND_ID);
		Toast.makeText(this, "Friend id:" + zFriendId, Toast.LENGTH_LONG).show();
		sRequestId = "123456798" ;
		sendRequest(zFriendId, sRequestId);
	}
	
	private void sendRequest(String aFriendId, String aRequestId) {
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_REQUEST) ;
		zIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		zIntent.putExtra( Api.EXTRA_METHOD , "GET" ) ;
		zIntent.putExtra( Api.EXTRA_URI , "chatsecure:/gallery/activate" ) ;
		zIntent.putExtra( Api.EXTRA_FRIEND_ID , aFriendId ) ;
		zIntent.putExtra( Api.EXTRA_REQUEST_ID , aRequestId ) ;
		startActivity( zIntent ) ;
	}
	
	private void doResponse(Intent aIntent) throws IOException{
		String zFriendId = aIntent.getStringExtra(Api.EXTRA_FRIEND_ID);
		String zRequestId = aIntent.getStringExtra(Api.EXTRA_REQUEST_ID);
		String zContent = aIntent.getStringExtra(Api.EXTRA_CONTENT);
		if( ! sRequestId.equals(zRequestId) ) {
			Toast.makeText(this, "Request id mismatch: " + zRequestId, Toast.LENGTH_LONG).show();
			return ;
		}
		// launch target
		launch( zFriendId, zContent ) ;
	}
	
	private void launch(String aFriendId, String aContent) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return json.toString();
	}

	/**
	 * @param requestGalleryListing
	 * @param string
	 */
	private void sendResponseFromLocal(String aRequest, String aContent) {
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_RESPONSE_FROM_LOCAL) ;
		zIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		zIntent.putExtra( Api.EXTRA_REQUEST , aRequest ) ;
		zIntent.putExtra( Api.EXTRA_CONTENT, aContent ) ;
		startActivity( zIntent ) ;
		finish() ;
	}
	
}
