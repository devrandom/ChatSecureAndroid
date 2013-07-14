package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.otr.app.im.dataplug.Discoverer.Registration;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DataPlugger {
    private Context mContext;

    public DataPlugger(Context context) {
        this.mContext = context;
    }
    
    public boolean sendResponseToRemote(PluggerResponse response) {
        return false;
    }
    
    public boolean sendResponseToLocal(PluggerResponse response) {
        Registration registration = Discoverer.getInstance(mContext).findRegistration(response.getUri());
        if (registration == null) {
            Log.e(Api.DATAPLUG_TAG, "Could not find registration for this uri");
            return false;
        }
        Intent responseIntent = new Intent(Api.RESPONSE_ACTION);
        responseIntent.setComponent(registration.getComponent());
        responseIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        responseIntent.putExtra(Api.EXTRA_REQUEST_ID, response.getRequestId());
        responseIntent.putExtra(Api.EXTRA_FRIEND_ID, response.getFriendId());
        responseIntent.putExtra(Api.EXTRA_CONTENT, response.getContent());
        mContext.startActivity(responseIntent);
        return true;
    }

    public boolean sendRequestToRemote(PluggerRequest request) {
        return false;
    }
    
    public boolean sendRequestToLocal(PluggerRequest request) {
        Registration registration = Discoverer.getInstance(mContext).findRegistration(request.getUri());
        if (registration == null) {
            Log.e(Api.DATAPLUG_TAG, "Could not find registration for this uri");
            return false;
        }
        Intent requestIntent = new Intent(Api.REQUEST_TO_LOCAL_ACTION);
        requestIntent.setComponent(registration.getComponent());
        requestIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        requestIntent.putExtra(Api.EXTRA_METHOD, request.getMethod());
        requestIntent.putExtra(Api.EXTRA_URI, request.getUri());
        requestIntent.putExtra(Api.EXTRA_FRIEND_ID, request.getFriendId());
        requestIntent.putExtra(Api.EXTRA_REQUEST_ID, request.getRequestId());
        requestIntent.putExtra(Api.EXTRA_CONTENT, request.getContent());
        mContext.startActivity(requestIntent);
        return true;
    }

    //String content = "{\"albums\": [\"New Year's 2013\", \"Kitties\"]}";
}
