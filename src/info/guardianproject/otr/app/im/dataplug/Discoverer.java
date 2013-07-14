package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.bouncycastle.util.encoders.Hex;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.google.common.collect.Maps;

public class Discoverer {
    private static Discoverer sInstance;
    private Context mContext;
    private Map<String, ComponentName> tokens;
    private Map<String, Registration> registrations;

    public Discoverer(Context context) {
        mContext = context;
        tokens = Maps.newHashMap();
        registrations = Maps.newHashMap();
    }

    public static Discoverer getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Discoverer(context);
        }

        return sInstance;
    }

    public void discoverDataPlugs() {
        Intent intent = new Intent(Api.DISCOVER_ACTION);

        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        Log.i(Api.DATAPLUG_TAG, "dataplugs:");
        for (ResolveInfo ri : list) {
            ActivityInfo info = ri.activityInfo;
            ComponentName component = new ComponentName(info.packageName, info.name);
            Log.i(Api.DATAPLUG_TAG, component.getClassName());
            String token = makeToken();
            tokens.put(token, component);
            Intent discoverIntent = new Intent(Api.DISCOVER_ACTION);
            discoverIntent.setComponent(component);
            discoverIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            discoverIntent.putExtra(Api.EXTRA_TOKEN, token);
            mContext.startActivity(discoverIntent);
        }
        Log.i(Api.DATAPLUG_TAG, "dataplugs end.");
    }
    
    public void activatePlug(String uri) {
        Registration registration = registrations.get(uri);
        if (registration == null) {
            return;
        }
        
        Intent activateIntent = new Intent(Api.ACTIVATE_ACTION);
        activateIntent.setComponent(registration.getComponent());
        activateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activateIntent.putExtra(Api.EXTRA_FRIEND_ID, "friend1");
        mContext.startActivity(activateIntent);
    }

    static class Descriptor {
        private String uri;
        private String name;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class Registration {
        private boolean publish;
        private ComponentName component;
        Descriptor descriptor;

        public Registration() {
            this.descriptor = new Descriptor();
        }

        public void setPublish(boolean publish) {
            this.publish = publish;
        }

        public boolean isPublish() {
            return publish;
        }
        
        public void setComponent(ComponentName component) {
            this.component = component;
        }
        
        public ComponentName getComponent() {
            return component;
        }
    }

    /**
     * 
     * @param token
     * @param registration sample: { "descriptor": { "uri":
     *            "chatsecure:/gallery", "name": "Gallery" }, "meta": {
     *            "publish" : true } }
     */
    public void register(String token, String registration_json) {
        if (token == null || !tokens.containsKey(token)) {
            Log.e(Api.DATAPLUG_TAG, "unknown or null token");
        }

        Registration registration = new Registration();
        registration.setComponent(tokens.get(token));
        
        JSONObject reg;
        
        try {
            reg = new JSONObject(registration_json);
        } catch (NullPointerException e) {
            Log.e(Api.DATAPLUG_TAG, "Could not parse registration json - NPE");
            return;
        } catch (JSONException e) {
            Log.e(Api.DATAPLUG_TAG, "Could not parse registration json");
            return;
        }
        
        try {
            registration.setPublish(reg.getJSONObject("meta").getBoolean("publish"));
        } catch (JSONException e) {
            // Allow no meta
        }

        try {
            JSONObject desc = reg.getJSONObject("descriptor");
            registration.descriptor.setName(desc.getString("name"));
            String uri = desc.getString("uri");
            registration.descriptor.setUri(uri);
            // FIXME Ask user
            registrations.put(uri, registration);
            Log.i(Api.DATAPLUG_TAG, "registered " + uri);
            activatePlug(uri);
        } catch (JSONException e) {
            Log.e(Api.DATAPLUG_TAG, "Could not parse descriptor json");
            return;
        }
    }

    private static String makeToken() {
        byte[] tokenBytes = new byte[16];
        new SecureRandom().nextBytes(tokenBytes);
        String token;
        try {
            token = new String(Hex.encode(tokenBytes), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
        return token;
    }

    public Registration findRegistration(String uri) {
        for (String key: registrations.keySet()) {
            if (uri.startsWith(key)) {
                return registrations.get(key);
            }
        }
        return null;
    }
}
