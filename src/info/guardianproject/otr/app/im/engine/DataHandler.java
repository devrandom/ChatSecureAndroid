package info.guardianproject.otr.app.im.engine;

import java.util.Map;

/**
 * Handle communication with the remote (using an OTR channel)
 * 
 * @author devrandom
 *
 */
public interface DataHandler {
    /**
     * Callback when a request comes in.
     * 
     * @param from this is OUR address
     * @param session the chat session
     * @param value the serialized request
     */
    void onIncomingRequest(Address us, byte[] value);

    /**
     * Callback when a response comes in.
     * 
     * @param from this is OUR address
     * @param session the chat session
     * @param value the serialized response
     */
    void onIncomingResponse(Address us, byte[] value);

    /**
     * Offer data to the remote.
     * 
     * @param us
     * @param localUri
     * @param headers
     */
    void offerData(Address us, String localUri, Map<String, String> headers);

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
}
