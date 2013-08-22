package info.guardianproject.otr.sample.securegallery;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	public static final String TAG = "SecureGallery" ;

	private static TextView sConsole;
	private static StringBuffer sBuffer ;
	
	private static Handler sHandler = new Handler(Looper.getMainLooper());	
	private static ImageView sConsoleImageView ;

	public static final int REQUEST_CODE_GALLERY_LISTING = 6661;

	private String mRequestId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init() ;
		try {
			handleIntent(getIntent());
		} catch (Throwable t) {
			error(this, t.getMessage());
		}
	}

	private void handleIntent(Intent intent) throws Exception {
		String action = intent.getAction();
		if (action.equals( "info.guardianproject.otr.app.im.dataplug.REQUEST_GALLERY") ) {
			mRequestId = intent.getExtras().getString(Api.EXTRA_REQUEST_ID);
			doRequestGallery();
		}
		if (action.equals( "info.guardianproject.otr.app.im.dataplug.SHOW_IMAGE") ) {
			doResponseGalleryImage(intent.getExtras().getByteArray(Api.EXTRA_CONTENT));
		}
	}

	private void init() {
		sConsole = (TextView)findViewById(R.id.consoleTextView);
		sConsoleImageView = (ImageView)findViewById(R.id.consoleImageView );
		
		sBuffer=new StringBuffer() ;
		sHandler = new Handler(Looper.getMainLooper());	
		console( "Ready" ) ;
	}
	
	/*
	 * UI output
	 */
	public static void console( final String aMessage ) {
		Log.w(TAG, aMessage ) ;
		sHandler.post(new Runnable() {
			@Override
			public void run() {
				sBuffer.append( aMessage + "\n" );
				sConsole.setText( sBuffer.toString() );
				sConsole.invalidate() ;
			}
		}) ;
	}

	/*
	 * Toast and UI output
	 */
	public static void error( Context aContext, String aMessage ) {
		Log.e(TAG, aMessage ) ;
		Toast.makeText( aContext, "Error: " + aMessage, Toast.LENGTH_LONG).show();
		console( "Error: " + aMessage );
	}
	
	/**
	 * @param discoverActivity
	 * @param bitmap
	 */
	public static void showBitmap( Activity aActivity, final Bitmap bitmap) {
		
		sHandler.post(new Runnable() {
			
			@Override
			public void run() {
				if( sConsoleImageView != null ) {
					sConsoleImageView.setImageBitmap(bitmap);
				}
			}
		}) ;
	}

	
	private void doRequestGallery() {
		MainActivity.console( "doRequestGallery" ) ;
		Intent zIntent = new Intent(Intent.ACTION_PICK);
		zIntent.setType("image/*");
		startActivityForResult(zIntent, REQUEST_CODE_GALLERY_LISTING );		
	}
	
	private void doRequestGalleryResult(Uri aUri) {
		String content = getGalleryListing( aUri.toString() ) ;
		SecureGalleryService.startService_RESPONSE_FROM_LOCAL( this, mRequestId, content.getBytes() );
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
			doRequestGalleryResult( uri );
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

	private void doResponseGalleryImage( byte[] aContent ) throws UnsupportedEncodingException, JSONException {
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

	public static void startActivity_REQUEST_GALLERY( String aRequestId, Context aContext ) {
		// repond with : accountid, friendid, requestid, body(json)
		Intent intent = new Intent(aContext, MainActivity.class);
		intent.setAction("info.guardianproject.otr.app.im.dataplug.REQUEST_GALLERY");
		intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
		intent.putExtra(Api.EXTRA_REQUEST_ID, aRequestId);
		aContext.startActivity(intent);
	}
	
	public static void startActivity_SHOW_IMAGE(Context aContext, byte[] aContent) {
		Intent intent = new Intent(aContext, MainActivity.class);
		intent.setAction("info.guardianproject.otr.app.im.dataplug.SHOW_IMAGE");
		intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED );
		intent.putExtra(Api.EXTRA_CONTENT, aContent);
		aContext.startActivity(intent);
	}

}
