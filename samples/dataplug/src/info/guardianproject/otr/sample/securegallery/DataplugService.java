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
import java.util.Set;
import java.util.UUID;

import org.json.JSONException;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;


/**
 * @author liorsaar
 */
public abstract class DataplugService extends Service {
	public static final int MAX_CHUNK_LENGTH = 32768;
	private  static final int MAX_OUTSTANDING = 5;
    private static final int MAX_TRANSFER_LENGTH = 1024*1024*64;
	
	public static final String TAG = DataplugService.class.getSimpleName() ;

	public static interface RequestCallback {
		void onResponse(Request aRequest, byte [] aContent);
	}

	public static interface TransferCallback {
		void onResponse(Transfer aTransfer, byte [] aContent);
	}

	RequestCache mOutgoingCache = new RequestCache();
	RequestCache mIncomingCache = new RequestCache();
    Cache<String, Transfer> transferCache = CacheBuilder.newBuilder().maximumSize(100).build();
	
	protected static class RequestCache {
		Cache<String, Request> sCache = CacheBuilder.newBuilder().maximumSize(100).build();

		public void put(Request aRequest) {
			sCache.put(aRequest.getId(), aRequest);
		}

		public Request get( String aRequestId ) {
			return sCache.getIfPresent(aRequestId);
		}

	}

    class Transfer {
		private String mAccountId;
		private String mFriendId;
		private String mUri;

		public int chunks = 0;
        public int chunksReceived = 0;
        
        private int length = 0;
        private int current = 0;
        private Set<Request> outstanding; 
        private byte[] buffer;

        private TransferCallback mCallback;
        
        public Transfer(String aUri, int length, String aAccountId, String aFriendId, TransferCallback aCallback) {
			mAccountId = aAccountId ;
			mFriendId = aFriendId ;
			mUri = aUri ;
			mCallback = aCallback;

			this.length = length;
            
            if (length > MAX_TRANSFER_LENGTH || length <= 0) {
                throw new RuntimeException("Invalid transfer size " + length);
            }
            chunks = ((length - 1) / MAX_CHUNK_LENGTH) + 1;
            buffer = new byte[length];
            outstanding = Sets.newHashSet();
        }
        
        public boolean perform() {
            // TODO global throttle rather than this local hack
            while (outstanding.size() < MAX_OUTSTANDING) {
                if (current >= length)
                    return false;
                int end = current + MAX_CHUNK_LENGTH - 1;
                if (end >= length) {
                    end = length - 1;
                }
                String rangeHeader = "Range: bytes=" + current + "-" + end;

                Request request = sendRequest(mAccountId, mFriendId, mUri, rangeHeader, new RequestCallback() {
                	@Override
                	public void onResponse(Request aRequest, byte[] aContent) {
                		chunkReceived(aRequest, aContent);
                		if (isDone()) {
                			mCallback.onResponse(Transfer.this, buffer);
                		} else {
                			perform();
                		}
                	}
                });
                request.setRange(current, end);
                outstanding.add(request);
                current = end + 1;
            }
            return true;
        }
        
        public boolean isDone() {
            return chunksReceived == chunks;
        }
        
        public void chunkReceived(Request request, byte[] bs) {
            chunksReceived++;
            System.arraycopy(bs, 0, buffer, request.getStart(), bs.length);
            outstanding.remove(request);
        }
    }

    public static class Request {
		private String mId;
		private String mAccountId;
		private String mFriendId;
		private String mUri;
		private String[] mHeaders;

		public int mStart;
        public int mEnd;
        
		RequestCallback mCallback;
		
		public Request( String aAccountId, String aFriendId, String aUri, RequestCallback aCallback) {
			this(UUID.randomUUID().toString(), aAccountId, aFriendId, aUri, aCallback);
		}
		
		public int getStart() {
			return mStart;
		}
		
		public int getEnd() {
			return mEnd;
		}

		public Request( String aId, String aAccountId, String aFriendId, String aUri, RequestCallback aCallback) {
			mId = aId;
			mAccountId = aAccountId ;
			mFriendId = aFriendId ;
			mUri = aUri ;
			mCallback = aCallback;
			mStart = -1;
			mEnd = -1;
		}
		
		public void setHeaders(String headers) {
			mHeaders = headers.split("\n");
			parseRange();
		}
		
		public String getUri() { return mUri ; }
		public String getFriendId() { return mFriendId ; }
		public String getAccountId() { return mAccountId ; }
		public String getId() {
			return mId;
		}
		
		public RequestCallback getCallback() {
			return mCallback;
		}
		
		public void setRange(int aStart, int aEnd) {
			this.mStart = aStart;
			this.mEnd = aEnd;
		}
		
		public boolean isRange() {
			return mStart >= 0;
		}
		
		public void parseRange() {
			String header = null;
			if (mHeaders == null) {
				return;
			}
			for (int i = 0 ; i < mHeaders.length ; i++) {
				if (mHeaders[i].startsWith("Range:")) {
					header = mHeaders[i].split(":", 2)[1].trim();
					break;
				}
			}
			if (header == null)
				return;

			String[] spec = header.split("=");
            if (spec.length != 2 || !spec[0].equals("bytes"))
            {
            	// TODO log error
                return;
            }
            String[] startEnd = spec[1].split("-");
            if (startEnd.length != 2)
            {
            	// TODO log error
                return;
            }

            int start = Integer.parseInt(startEnd[0]);
            int end = Integer.parseInt(startEnd[1]);
            if (end - start + 1 > MAX_CHUNK_LENGTH) {
            	// TODO log error
                return;
            }
            if (end < 0 || start < 0 || start > end) {
            	// TODO log error
            	return;
            }
            
            mStart = start;
            mEnd = end;
		}
	}

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
		MainActivity.console( "doDiscover: " + token ) ; // FIXME logging
		if( token == null ) {
			MainActivity.error( this, "doDiscover: token=null" ) ;
			return ;
		}
		MainActivity.console( "doDiscover: token=" + token ) ;
		sendRegistration( token ) ;
	}

	private void sendRegistration(String token) throws JSONException {
		MainActivity.console( "sendRegistration" ) ;
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_REGISTER) ;
		zIntent.putExtra( Api.EXTRA_TOKEN , token ) ;
		zIntent.putExtra( Api.EXTRA_REGISTRATION, getRegistration() ) ;
		startService( zIntent ) ;
	}

	protected static final String CHARSET = "UTF-8";

	abstract protected String getRegistration() throws JSONException;

	private void doActivate(Intent aIntent) {
		String zFriendId = aIntent.getStringExtra(Api.EXTRA_FRIEND_ID);
		String zAccountId = aIntent.getStringExtra(Api.EXTRA_ACCOUNT_ID);
		MainActivity.console( "doActivate: Friend:" + zFriendId ) ;
		doActivate(zAccountId, zFriendId);
	}
	
	protected abstract void doActivate(String aAccountId, String aFriendId) ;

	protected void sendTransferRequest(String aAccountId, String aFriendId, String aUri, int aLength, TransferCallback aCallback) {
        Transfer transfer = new Transfer(aUri, aLength, aAccountId, aFriendId, aCallback);
        transferCache.put(aUri, transfer);
        transfer.perform();
	}
	
	protected Request sendRequest(String aAccountId, String aFriendId, String aUri, RequestCallback aCallback) {
		return sendRequest(aAccountId, aFriendId, aUri, "", aCallback);
	}
	
	protected Request sendRequest(String aAccountId, String aFriendId, String aUri, String headers, RequestCallback aCallback) {
		Request request = new Request( aAccountId, aFriendId, aUri, aCallback );
		mOutgoingCache.put(request) ;
	
		MainActivity.console( "sendRequest: EXTRA_URI:" + aUri ) ;
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_REQUEST) ;
		zIntent.putExtra( Api.EXTRA_METHOD , "GET" ) ;
		zIntent.putExtra( Api.EXTRA_URI, aUri ) ;
		zIntent.putExtra( Api.EXTRA_HEADERS, headers) ;
		zIntent.putExtra( Api.EXTRA_ACCOUNT_ID , aAccountId ) ;
		zIntent.putExtra( Api.EXTRA_FRIEND_ID , aFriendId ) ;
		zIntent.putExtra( Api.EXTRA_REQUEST_ID , request.getId() ) ;
		startService( zIntent ) ;
		return request;
	}

	protected void doResponse(Intent aIntent) throws Exception {
		String zFriendId = aIntent.getStringExtra(Api.EXTRA_FRIEND_ID);
		MainActivity.console( "doResponse: EXTRA_FRIEND_ID:" + zFriendId );
		String zRequestId = aIntent.getStringExtra(Api.EXTRA_REQUEST_ID);
		byte[] zContent = aIntent.getByteArrayExtra(Api.EXTRA_CONTENT);
		
		Request zRequest = mOutgoingCache.get(zRequestId);

		if( zRequest == null ) {
			MainActivity.error( this, "Request not found: " + zRequestId ) ;
			return ;
		}
		
		MainActivity.console( "doResponse: uri=" + URLDecoder.decode(zRequest.getUri(), CHARSET));
		zRequest.getCallback().onResponse(zRequest, zContent);
	}

	protected void sendResponseFromLocal(String aRequestId, byte[] aContent) {
		// respond with : accountid, friendid, requiestid, body(json)
		MainActivity.console( "sendResponseFromLocal: content=" + aContent ) ;
		Intent zIntent = new Intent();
		zIntent.setAction(Api.ACTION_RESPONSE_FROM_LOCAL) ;
		Request request = mIncomingCache.get(aRequestId);
		zIntent.putExtra( Api.EXTRA_ACCOUNT_ID , request.getAccountId() ) ;
		zIntent.putExtra( Api.EXTRA_FRIEND_ID , request.getFriendId() ) ;
		zIntent.putExtra( Api.EXTRA_REQUEST_ID , request.getId() ) ;
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
			Bundle extras = aIntent.getExtras();
			Request request = new Request(
					extras.getString(Api.EXTRA_REQUEST_ID),
					extras.getString(Api.EXTRA_ACCOUNT_ID),
					extras.getString(Api.EXTRA_FRIEND_ID),
					extras.getString(Api.EXTRA_URI),
					null
					);
			request.setHeaders(extras.getString(Api.EXTRA_HEADERS));
			mIncomingCache.put(request) ;
			
			if( request.getUri() == null ) {
				MainActivity.error( this, "RequestToLocal: uri=null" ) ;
				return ; // TODO error
			}
			
			doRequestToLocal( request ) ;
			return ;
		}
		if( zAction.equals(Api.ACTION_RESPONSE_FROM_LOCAL ) ) {
			doResponseFromLocal( aIntent ) ;
			return ;
		}
		MainActivity.error( this, "Unknown action " + zAction ) ;
	}
	
	protected void doResponseFromLocal(Intent aIntent) {
		sendResponseFromLocal(aIntent.getStringExtra(Api.EXTRA_REQUEST_ID), aIntent.getByteArrayExtra(Api.EXTRA_CONTENT));
	}

	abstract protected void doRequestToLocal(Request aRequest) throws Exception ;

}
