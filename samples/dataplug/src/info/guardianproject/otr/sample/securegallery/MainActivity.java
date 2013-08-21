package info.guardianproject.otr.sample.securegallery;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

	private void handleIntent(Intent intent) throws IOException {
		String action = intent.getAction();
		if (action.equals( "info.guardianproject.otr.app.im.dataplug.REQUEST_GALLERY") ) {
			doRequestGallery();
		}
		if (action.equals( "info.guardianproject.otr.app.im.dataplug.REQUEST_GALLERY_IMAGE") ) {
			doRequestGalleryImage(intent.getExtras().getString(Api.EXTRA_URI));
			finish();
		}
	}

	private void init() {
		sConsole = (TextView)findViewById(R.id.consoleTextView);
		sConsoleImageView = (ImageView)findViewById(R.id.consoleImageView );
		
		sBuffer=new StringBuffer() ;
		sHandler = new Handler(Looper.getMainLooper());	
		console( "Ready" ) ;
//		doRequestGallery( this);
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
	 * @param uri
	 * @throws IOException 
	 */
	private byte[] getByteArray(Uri uri) throws IOException {
		String path = Utils.MediaStoreHelper.getPath(this, uri);
		
		File file = new File(path);
		FileInputStream fis = new FileInputStream(file);
		long length = file.length() ;
		byte[] buffer = new byte[ (int) length ];
				
		fis.read(buffer);
		return buffer ;
	}

	private void doRequestGallery() {
		MainActivity.console( "doRequestGallery" ) ;
		Intent zIntent = new Intent(Intent.ACTION_PICK);
		zIntent.setType("image/*");
		startActivityForResult(zIntent, REQUEST_CODE_GALLERY_LISTING );		
	}

	private void doRequestGalleryImage(String contentUri) throws IOException {
		MainActivity.console( "doRequestGalleryImage:" + contentUri ) ;
		byte[] buffer = Utils.MediaStoreHelper.getImageContent(this, contentUri);
		Intent intent = new Intent(this, DataplugService.class);
		intent.setAction(Api.ACTION_RESPONSE_FROM_LOCAL);
		intent.putExtra(Api.EXTRA_CONTENT, buffer);
		startService(intent);
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
			Intent intent = new Intent(this, DataplugService.class);
			intent.setAction(Api.ACTION_RESPONSE_FROM_LOCAL);
			intent.putExtra(Api.EXTRA_CONTENT, content.getBytes());
			startService(intent);
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
}
