package info.guardianproject.otr.app.im.engine;

/**
 * Listen to data events.
 * 
 * @author devrandom
 *
 */
public interface DataListener {
    /** File transfer complete */
    void onTransferComplete(Address from, String url, byte[] data);

    /** File transfer failed */
    void onTransferFailed(Address from, String url, String reason);

    /** File transfer in progress */
    void onTransferProgress(Address from, String url, float f);

    /** A request came in */
    boolean onIncomingRequest(String requestMethod, String url, String requestId,
            String headers, byte[] body);
}
