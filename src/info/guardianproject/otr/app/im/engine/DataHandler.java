package info.guardianproject.otr.app.im.engine;

import java.util.Map;

public interface DataHandler {
    /**
     * @param from this is OUR address
     * @param session the chat session
     * @param value the serialized request
     */
    void onIncomingRequest(Address us, byte[] value);

    /**
     * @param from this is OUR address
     * @param session the chat session
     * @param value the serialized response
     */
    void onIncomingResponse(Address us, byte[] value);

    void offerData(Address us, String localUri, Map<String, String> headers);

    void sendDataRequest(Address address, String method, String uri, String requestId,
            byte[] content);

    void sendDataResponse(Address address, int code, String statusString, String requestId, byte[] content);
}
