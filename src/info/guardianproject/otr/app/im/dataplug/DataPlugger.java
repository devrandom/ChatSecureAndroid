package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.otr.app.im.IChatSession;
import info.guardianproject.otr.app.im.IChatSessionManager;
import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.ImService;
import info.guardianproject.otr.app.im.dataplug.Discoverer.Registration;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

public class DataPlugger {
    private Context mContext;
    private ImService mService;

    public DataPlugger(ImService service, Context context) {
        this.mContext = context;
        this.mService = service;
    }

    public boolean sendResponseToRemote(PluggerResponse response) {
        IChatSession chatSession = getChatSession(response.getAccountId(), response.getFriendId());
        try {
            chatSession.sendDataResponse(response.getCode(), response.getStatusString(), response.getRequestId(), response.getContent());
        } catch (RemoteException e) {
            Log.e(Api.DATAPLUG_TAG, "Could not send response");
            return false;
        }
        return true;
    }

    public boolean sendResponseToLocal(PluggerResponse response) {
        Registration registration = Discoverer.getInstance(mContext).findRegistration(
                response.getUri());
        if (registration == null) {
            Log.e(Api.DATAPLUG_TAG, "Could not find registration for this uri");
            return false;
        }
        Intent responseIntent = new Intent(Api.RESPONSE_ACTION);
        responseIntent.setComponent(registration.getComponent());
        responseIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        responseIntent.putExtra(Api.EXTRA_REQUEST_ID, response.getRequestId());
        responseIntent.putExtra(Api.EXTRA_FRIEND_ID, response.getFriendId());
        responseIntent.putExtra(Api.EXTRA_HEADERS, response.getHeaders());
        responseIntent.putExtra(Api.EXTRA_CONTENT, response.getContent());
        mContext.startActivity(responseIntent);
        return true;
    }

    public boolean sendRequestToRemote(PluggerRequest request) {
        IChatSession chatSession = getChatSession(request.getAccountId(), request.getFriendId());
        try {
            chatSession.sendDataRequest(request.getMethod(), request.getUri(),
                    request.getRequestId(), request.getHeaders(), request.getContent());
        } catch (RemoteException e) {
            Log.e(Api.DATAPLUG_TAG, "Could not send request");
            return false;
        }
        return true;
    }

    public boolean sendRequestToLocal(PluggerRequest request) {
        Registration registration = Discoverer.getInstance(mContext).findRegistration(
                request.getUri());
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
        requestIntent.putExtra(Api.EXTRA_HEADERS, request.getHeaders());
        requestIntent.putExtra(Api.EXTRA_CONTENT, request.getContent());
        mContext.startActivity(requestIntent);
        return true;
    }

    private IChatSession getChatSession(String accountId, String friendId) {
        try {
            return getChatSessionManager(Integer.parseInt(accountId)).getChatSession(friendId);
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            Log.e(Api.DATAPLUG_TAG, "could not get chat session", e);
        }
        return null;
    }

    private IChatSessionManager getChatSessionManager(long providerId) {
        IImConnection conn = mService.getConnectionForProvider(providerId);

        IChatSessionManager chatSessionManager = null;
        if (conn != null) {
            try {
                chatSessionManager = conn.getChatSessionManager();
            } catch (RemoteException e) {
                Log.e(Api.DATAPLUG_TAG, "could not get manager", e);
            }
        }

        return chatSessionManager;
    }

    //String content = "{\"albums\": [\"New Year's 2013\", \"Kitties\"]}";
}
