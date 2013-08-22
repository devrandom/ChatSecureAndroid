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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
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
	protected void doRequestToLocal( String aUri ) throws Exception {
		// look at EXTRA_URI - /gallery/activate
		if( aUri.equals( URI_GALLERY )) {
			MainActivity.startActivity_REQUEST_GALLERY(this);
			return ;
		}
		if( aUri.startsWith( URI_IMAGE )) {
			// repond with : accountid, friendid, requestid, image binary
			String contentUriEncoded = aUri.substring( URI_IMAGE.length() ) ;
			String contentUri = URLDecoder.decode(contentUriEncoded, CHARSET);
			doRequestToLocal_URI_IMAGE( contentUri );
			return ;
		}
		// unknown
		MainActivity.error( this, "doRequestToLocal: Unknown URI: "+ aUri ) ;
	}
	
	private void doRequestToLocal_URI_IMAGE(String contentUri) throws IOException {
		MainActivity.console( "doRequestGalleryImage:" + contentUri ) ;
		byte[] buffer = Utils.MediaStoreHelper.getImageContent(this, contentUri);
		sendResponseFromLocal( buffer );
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
		MainActivity.console( "doResponse: uri=" + URLDecoder.decode(aRequest.getUri(), CHARSET));
		
		if( aRequest.getUri().equals(URI_GALLERY) ) {
			doResponseGallery( aRequest, aContent ) ;
			return ;
		}
		if( aRequest.getUri().startsWith(URI_IMAGE) ) {
			MainActivity.startActivity_SHOW_IMAGE(this, aContent);
			return ;
		}
	}

	public static void startService(Context aContext, byte[] aContent ) {
		Intent intent = new Intent(aContext, SecureGalleryService.class);
		intent.setAction(Api.ACTION_RESPONSE_FROM_LOCAL);
		intent.putExtra(Api.EXTRA_CONTENT, aContent );
		aContext.startService(intent);
	}
}
