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

import info.guardianproject.otr.sample.securegallery.DataplugService.RequestCache.Request;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;


/**
 * @author liorsaar
 */
public class SecureGalleryService extends DataplugService {
	public static final String TAG = SecureGalleryService.class.getSimpleName() ;

	static final String URI_GALLERY = "chatsecure:/gallery/";
	static final String URI_IMAGE = URI_GALLERY + "image/";
	
	/*
	 * Alice - initiator - the side that hit the ui first
	 * Bob - received - in doRequestToLocal - responds with json
	 */
	protected void doRequestToLocal(Intent aIntent) throws Exception {
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
			intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
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
			intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
			intent.putExtra(Api.EXTRA_URI, contentUri);
			startActivity(intent);
			return ;
		}
		MainActivity.error( this, "Invalid URI: "+ zUri ) ;
	}
	
	protected void doResponseGallery( RequestCache.Request aRequest, byte[] aContentByteArray) throws UnsupportedEncodingException, JSONException {
		String content = new String(aContentByteArray, CHARSET);
		MainActivity.console( "doResponseGallery: content=" + content );
		JSONObject jsonObject = new JSONObject( content );
		String responseUri = jsonObject.getString("uri");

		String requestUri = URI_IMAGE + URLEncoder.encode(responseUri, CHARSET);

		sendRequest(aRequest.getAccountId(), aRequest.getFriendId(), requestUri);

		return ;
	}
	
	protected void doActivate(String aAccountId, String aFriendId) {
		sendRequest( aAccountId, aFriendId, URI_GALLERY) ;
	}
	
	@Override
	protected void doResponse(Request aRequest, byte[] aContent) throws Exception {
		
		if( aRequest.getUri().equals(URI_GALLERY) ) {
			doResponseGallery( aRequest, aContent ) ;
			return ;
		}
		if( aRequest.getUri().startsWith(URI_IMAGE) ) {
			MainActivity.console( "doResponseGalleryImage: uri=" + URLDecoder.decode(aRequest.getUri(), CHARSET));
			Intent intent = new Intent(this, MainActivity.class);
			intent.setAction("info.guardianproject.otr.app.im.dataplug.SHOW_IMAGE");
			intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED );
			intent.putExtra(Api.EXTRA_CONTENT, aContent);
			startActivity(intent);
			return ;
		}
	}
}
