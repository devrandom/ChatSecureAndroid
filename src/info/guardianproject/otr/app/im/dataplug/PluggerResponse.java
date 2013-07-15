package info.guardianproject.otr.app.im.dataplug;

public class PluggerResponse extends PluggerMessage {
    private int code;
    private String statusString;
    
    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }
    public String getStatusString() {
        return statusString;
    }
    public void setStatusString(String statusString) {
        this.statusString = statusString;
    }
}
