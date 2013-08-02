package info.guardianproject.otr.sample.securegallery;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	public static final String TAG = "SecureGallery" ;

	private static TextView sConsole;
	private static StringBuffer sBuffer ;
	
	private static Handler sHandler = new Handler(Looper.getMainLooper());	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init() ;
	}

	private void init() {
		sConsole = (TextView)findViewById(R.id.consoleTextView);
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
}
