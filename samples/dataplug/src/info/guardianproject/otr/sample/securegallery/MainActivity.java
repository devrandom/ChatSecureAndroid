package info.guardianproject.otr.sample.securegallery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	public static final String TAG = "SecureGallery" ;

	private static TextView sConsole;
	private static StringBuffer sBuffer ;
	
	private static Handler sHandler = new Handler(Looper.getMainLooper());	
	private static ImageView sConsoleImageView ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init() ;
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
	
	private void doRequestGallery(Activity aActivity) {
		MainActivity.console( "doRequestGallery" ) ;
		Intent zIntent = new Intent(Intent.ACTION_PICK);
		zIntent.setType("image/*");
		aActivity.startActivityForResult(zIntent, DiscoverActivity.REQUEST_CODE_GALLERY_LISTING );
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case DiscoverActivity.REQUEST_CODE_GALLERY_LISTING:
			if( resultCode != Activity.RESULT_OK) {
				Toast.makeText(this, "ERROR: REQUEST_CODE_GALLERY_LISTING: " + resultCode, Toast.LENGTH_LONG).show(); // TODO doialog
				return ;
			}
			Uri uri = data.getData() ;
			try {
				byte[] byteArray = getByteArray( uri );
				DiscoverActivity.showPng( byteArray ) ;
			} catch (IOException e) {
				e.printStackTrace();
			}
			break ;
		default:
			Toast.makeText(this, "ERROR: requestCode unknown: " + requestCode, Toast.LENGTH_LONG).show();
		}
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
