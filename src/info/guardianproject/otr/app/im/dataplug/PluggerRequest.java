package info.guardianproject.otr.app.im.dataplug;

/**
 * @see PluggerMessage
 * 
 * @author devrandom
 *
 */
public class PluggerRequest extends PluggerMessage {
    private String method;
    
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
}
