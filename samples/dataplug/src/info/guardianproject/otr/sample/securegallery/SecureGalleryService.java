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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

/**
 * @author liorsaar
 */
public class SecureGalleryService extends DataplugService {
	public static final String TAG = SecureGalleryService.class.getSimpleName() ;

	static final String URI_GALLERY = "chatsecure:/gallery/";
	static final String URI_IMAGE = URI_GALLERY + "image/";

	protected RandomAccessFile mStream;
	
	/*
	 * Alice - initiator - the side that hit the ui first
	 * Bob - received - in doRequestToLocal - responds with json
	 */
	protected void doRequestToLocal( Request aRequest ) throws Exception {
		// look at EXTRA_URI - /gallery/activate
		String aUri = aRequest.getUri();
		if( aUri.equals( URI_GALLERY )) {
			MainActivity.startActivity_REQUEST_GALLERY(aRequest.getId(), this);
			return ;
		}
		if( aUri.startsWith( URI_IMAGE )) {
			// respond with : accountid, friendid, requestid, image binary
			String contentUriEncoded = aUri.substring( URI_IMAGE.length() ) ;
			String contentUri = URLDecoder.decode(contentUriEncoded, CHARSET);
			doRequestToLocal_URI_IMAGE( aRequest.getId(), contentUri, aRequest.getStart(), aRequest.getEnd() );
			return ;
		}
		// unknown
		MainActivity.error( this, "doRequestToLocal: Unknown URI: "+ aUri ) ;
	}
	
	private void doRequestToLocal_URI_IMAGE(String aRequestId, String contentUri, int aStart, int aEnd) throws IOException {
		MainActivity.console( "doRequestGalleryImage:" + contentUri + " @" + aStart ) ;
		if (aEnd - aStart > DataplugService.MAX_CHUNK_LENGTH) {
			MainActivity.error( this, "doRequestToLocal: request range too large" ) ;
			return;
		}
		byte[] buffer = Utils.MediaStoreHelper.getImageContent(this, contentUri, aStart, aEnd);
		String headers = "";
		if (aStart == 0) {
	        headers = "File-Hash-SHA1: " + Utils.MediaStoreHelper.sha1sum(this, contentUri);
	        MainActivity.console(headers);
		}

		sendResponseFromLocal(aRequestId, buffer,  headers);
	}

	protected void doResponseGallery( final Request aRequest, byte[] aContentByteArray) throws UnsupportedEncodingException, JSONException {
		String content = new String(aContentByteArray, CHARSET);
		MainActivity.console( "doResponseGallery: content=" + content );
		JSONObject jsonObject = new JSONObject( content );
		JSONArray images = jsonObject.getJSONArray("images");
		for (int i = 0 ; i < images.length() ; i++) {
			JSONObject image = images.getJSONObject(i);
			final String responseUri = image.getString("uri");
			int length = (int)image.getLong("length");

			String requestUri = URI_IMAGE + URLEncoder.encode(responseUri, CHARSET);

			sendTransferRequest(aRequest.getAccountId(), aRequest.getFriendId(), requestUri, length, new TransferCallback() {
				@Override
				public void onResponse(Transfer aTransfer) {
					byte[] content = new byte[aTransfer.getLength()];
					try {
						mStream.getChannel().position(0);
						mStream.readFully(content);
						mStream.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					mStream = null;
					MainActivity.startActivity_SHOW_IMAGE(SecureGalleryService.this, content);
				}

				@Override
				public RandomAccessFile openFile(Transfer aTransfer) {
		            String[] path = responseUri.split("/"); 
		            String sanitizedPath = Utils.sanitize(path[path.length - 1]);
		            File sdCard = Environment.getExternalStorageDirectory();
		            File dir = new File (sdCard.getAbsolutePath() + "/SecureGallery/peerdata/" + aRequest.getAccountId() + "-" + aRequest.getFriendId());
		            dir.mkdirs();
		            File file = new File(dir, sanitizedPath);
					try {
						if (mStream != null) {
							mStream.close();
						}
						mStream = new RandomAccessFile(file, "rw");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					return mStream;
				}
			});
		}

		return ;
	}
	
	protected void doActivate(String aAccountId, String aFriendId) {
		sendRequest( aAccountId, aFriendId, URI_GALLERY, new RequestCallback() {
			@Override
			public void onResponse(Request aRequest, byte[] aContent, String headersString) {
				try {
					doResponseGallery( aRequest, aContent ) ;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}) ;
	}

	public static void startService_RESPONSE_FROM_LOCAL(Context aContext, String aRequestId, byte[] aContent ) {
		Intent intent = new Intent(aContext, SecureGalleryService.class);
		intent.setAction(Api.ACTION_RESPONSE_FROM_LOCAL);
		intent.putExtra(Api.EXTRA_CONTENT, aContent );
		intent.putExtra(Api.EXTRA_REQUEST_ID, aRequestId);
		aContext.startService(intent);
	}

	private static final String REGISTRATION = "{ 'descriptor': 	{ 'uri': 'chatsecure:/gallery', 'name': 'Gallery' }, 'meta': { 'publish' : true } }";

	protected String getRegistration() throws JSONException {
		JSONObject json = new JSONObject( REGISTRATION );				
		return json.toString() ;
	}
}
