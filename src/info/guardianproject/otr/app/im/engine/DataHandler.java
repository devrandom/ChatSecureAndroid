package info.guardianproject.otr.app.im.engine;

import info.guardianproject.otr.app.im.IDataListener;

import java.util.Map;

import net.java.otr4j.session.SessionStatus;

public interface DataHandler {
    /**
     * @param from this is OUR address
     * @param session the chat session
     * @param value the serialized request
     */
    void onIncomingRequest(Address from, Address to, byte[] value);

    /**
     * Send an arbitrary request to the remote.
     * 
     * @param us
     * @param method
     * @param uri
     * @param requestId
     * @param headersString
     * @param content
     */
    void sendDataRequest(Address us, String method, String uri, String requestId,
            String headersString, byte[] content);

    /**
     * Send a response to the remote.
     * @param us
     * @param code
     * @param statusString
     * @param requestId
     * @param content
     */
    void sendDataResponse(Address us, int code, String statusString, String requestId, byte[] content, String headers);

    void onIncomingResponse(Address from, Address to, byte[] value);

    void offerData(Address us, String localUri, Map<String, String> headers);
    
    void setDataListener(IDataListener dataListener);

    void onOtrStatusChanged(SessionStatus status);
}
