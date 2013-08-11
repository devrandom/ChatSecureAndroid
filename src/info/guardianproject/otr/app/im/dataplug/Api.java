package info.guardianproject.otr.app.im.dataplug;

public class Api {
    public static final String DISCOVER_ACTION = "info.guardianproject.otr.app.im.dataplug.DISCOVER";
    public static final String REGISTER_ACTION = "info.guardianproject.otr.app.im.dataplug.REGISTER";
    public static final String ACTIVATE_ACTION = "info.guardianproject.otr.app.im.dataplug.ACTIVATE";
    /** Request from plugin to remote*/
    public static final String REQUEST_ACTION = "info.guardianproject.otr.app.im.dataplug.REQUEST";
    /** Response from remote to plugin */
    public static final String RESPONSE_ACTION = "info.guardianproject.otr.app.im.dataplug.RESPONSE";
    /** Request from remote to plugin */
    public static final String REQUEST_TO_LOCAL_ACTION = "info.guardianproject.otr.app.im.dataplug.REQUEST_TO_LOCAL";
    /** Response from plugin to remote */
    public static final String RESPONSE_FROM_LOCAL_ACTION = "info.guardianproject.otr.app.im.dataplug.RESPONSE_FROM_LOCAL";


    public static final String DATAPLUG_TAG = "GB.dataplug";
    public static final String EXTRA_TOKEN = "Token";
    public static final String EXTRA_FRIEND_ID = "FriendId";
    public static final String EXTRA_ACCOUNT_ID = "AccountId";
    public static final String EXTRA_REQUEST_ID = "RequestId";
    public static final String EXTRA_CONTENT = "Content";
    public static final String REGISTRATION_TOKEN = "Registration";
    public static final String EXTRA_METHOD = "Method";
    public static final String EXTRA_HEADERS = "Headers";
    public static final String EXTRA_URI = "Uri";
}