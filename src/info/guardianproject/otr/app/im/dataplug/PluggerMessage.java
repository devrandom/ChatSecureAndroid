package info.guardianproject.otr.app.im.dataplug;

/** Base class for DataPlug requests and responses */
public class PluggerMessage {
    private String uri;
    private String accountId;
    private String friendId;
    private String requestId;
    private byte[] content;
    private String headers;
 
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    /** The JID of the friend */
    public String getFriendId() {
        return friendId;
    }
    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }
    
    /** The JID of the local user */
    public String getAccountId() {
        return accountId;
    }
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    /** A unique request ID */
    public String getRequestId() {
        return requestId;
    }
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    public byte[] getContent() {
        return content;
    }
    public void setContent(byte[] content) {
        this.content = content;
    }
    public String getHeaders() {
        return headers;
    }
    public void setHeaders(String headers) {
        this.headers = headers;
    }
}
