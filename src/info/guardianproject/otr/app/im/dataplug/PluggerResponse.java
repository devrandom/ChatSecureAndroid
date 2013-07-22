package info.guardianproject.otr.app.im.dataplug;

/**
 * @see PluggerMessage
 * 
 * @author devrandom
 *
 */
public class PluggerResponse extends PluggerMessage {
    private int code;
    private String statusString;

    /** HTTP response code */
    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }
    
    /** HTTP status string */
    public String getStatusString() {
        return statusString;
    }
    public void setStatusString(String statusString) {
        this.statusString = statusString;
    }
}
