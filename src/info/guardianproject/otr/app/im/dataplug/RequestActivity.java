package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.otr.app.im.app.ImApp;
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
        if (intent.getAction() == Api.REQUEST_ACTION) {
            String method = intent.getExtras().getString(Api.EXTRA_METHOD);
            String uri = intent.getExtras().getString(Api.EXTRA_URI);
            String friendId = intent.getExtras().getString(Api.EXTRA_FRIEND_ID);
            String accountId = intent.getExtras().getString(Api.EXTRA_ACCOUNT_ID);
            String requestId = intent.getExtras().getString(Api.EXTRA_REQUEST_ID);
            String content = intent.getExtras().getString(Api.EXTRA_CONTENT);
            Log.d(Api.DATAPLUG_TAG, "Got request @" +friendId + ": " + method + " " + uri);

            PluggerRequest request = new PluggerRequest();
            request.setMethod(method);
            request.setUri(uri);
            request.setFriendId(friendId);
            request.setRequestId(requestId);
            request.setContent(content.getBytes());
            request.setAccountId(accountId);
            new DataPlugger(this, (ImApp)getApplication()).sendRequestToRemote(request);
        } else if (intent.getAction() == Api.RESPONSE_FROM_LOCAL_ACTION) {
            String friendId = intent.getExtras().getString(Api.EXTRA_FRIEND_ID);
            String accountId = intent.getExtras().getString(Api.EXTRA_ACCOUNT_ID);
            String requestId = intent.getExtras().getString(Api.EXTRA_REQUEST_ID);
            String content = intent.getExtras().getString(Api.EXTRA_CONTENT);

            PluggerResponse response = new PluggerResponse();
            response.setCode(200);
            response.setStatusString("OK");
            response.setFriendId(friendId);
            response.setRequestId(requestId);
            response.setContent(content.getBytes());
            response.setAccountId(accountId);
            new DataPlugger(this, (ImApp)getApplication()).sendResponseToRemote(response);
        }
        finish();
    }
}
