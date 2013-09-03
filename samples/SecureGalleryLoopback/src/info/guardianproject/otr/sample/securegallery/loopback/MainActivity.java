package info.guardianproject.otr.sample.securegallery.loopback;

import info.guardianproject.otr.dataplug.Api;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init() ;
	}
	
	private void init() {
		findViewById(R.id.ButtonActivate).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickActivate() ;
			}
		});
		findViewById(R.id.ButtonRequestGallery).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickRequestGallery() ;
			}
		});
	}
	
	protected void onClickRequestGallery() {
		Intent intent = new Intent();
		intent.setAction(Api.ACTION_INCOMING_REQUEST);
		intent.putExtra(Api.EXTRA_REQUEST_ID, "1234");
		intent.putExtra(Api.EXTRA_FRIEND_ID, "Alice");
		intent.putExtra(Api.EXTRA_ACCOUNT_ID, "666");
		intent.putExtra(Api.EXTRA_HEADERS, "");
		intent.putExtra(Api.EXTRA_URI, LoopbackService.URI_GALLERY );
		startService(intent);
	}

	protected void onClickActivate() {
		Intent intent = new Intent();
		intent.setAction(Api.ACTION_ACTIVATE);
		intent.putExtra(Api.EXTRA_FRIEND_ID, "Alice");
		intent.putExtra(Api.EXTRA_ACCOUNT_ID, "666");
		startService(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
