package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.otr.app.im.dataplug.Discoverer.Registration;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class RequestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("GB.dataplug", "onCreate RegistrationActivity");
        
        Intent intent = getIntent();
        String method = intent.getExtras().getString(Api.EXTRA_METHOD);
        String uri = intent.getExtras().getString(Api.EXTRA_URI);
        String friendId = intent.getExtras().getString(Api.EXTRA_FRIEND_ID);
        String requestId = intent.getExtras().getString(Api.EXTRA_REQUEST_ID);
        Log.d(Api.DATAPLUG_TAG, "Got request @" +friendId + ": " + method + " " + uri);
        
        Registration registration = Discoverer.getInstance(this).findRegistration(uri);
        if (registration == null) {
            Log.e(Api.DATAPLUG_TAG, "Could not find registration for this uri");
            finish();
            return;
        }
        Intent responseIntent = new Intent(Api.RESPONSE_ACTION);
        responseIntent.setComponent(registration.getComponent());
        responseIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        responseIntent.putExtra(Api.EXTRA_REQUEST_ID, requestId);
        responseIntent.putExtra(Api.EXTRA_FRIEND_ID, friendId);
        responseIntent.putExtra(Api.EXTRA_CONTENT, 
                "{\"albums\": [\"New Year's 2013\", \"Kitties\"]}");
        startActivity(responseIntent);
        finish();
    }
}
