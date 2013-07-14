package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.otr.app.im.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

public class RegistrationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dataplug_registration);
        Log.i("GB.dataplug", "onCreate RegistrationActivity");
        
        Intent intent = getIntent();
        String token = intent.getExtras().getString(Api.EXTRA_TOKEN);
        String meta = intent.getExtras().getString(Api.REGISTRATION_TOKEN);
        Discoverer.getInstance(this).register(token, meta);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dataplug_registration, menu);
        return true;
    }

}
