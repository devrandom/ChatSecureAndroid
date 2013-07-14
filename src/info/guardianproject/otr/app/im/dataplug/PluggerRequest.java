package info.guardianproject.otr.app.im.dataplug;

public class PluggerRequest extends PluggerMessage {
    private String method;
    
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
}
