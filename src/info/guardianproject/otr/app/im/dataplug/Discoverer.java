package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.bouncycastle.util.encoders.Hex;
import info.guardianproject.otr.dataplug.Api;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Handle DataPlug discovery (local and over the OTR channel) and handle registration.
 * 
 * @author devrandom
 *
 */
public class Discoverer {
    private static Discoverer sInstance;
    private Context mContext;
    private Map<String, ComponentName> tokens;
    private Map<String, Registration> registrations;

    private Discoverer(Context context) {
        mContext = context;
        tokens = Maps.newHashMap();
        registrations = Maps.newHashMap();
    }

    /** Get the singleton Discoverer */
    public static Discoverer getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Discoverer(context);
        }

        return sInstance;
    }

    /** Initiate discovery of local DataPlugs */
    public void discoverDataPlugs() {
        Intent intent = new Intent(Api.ACTION_DISCOVER);
        
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentServices(intent, 0);
        Log.i(Api.DATAPLUG_TAG, "dataplugs:");
        for (ResolveInfo ri : list) {
            ServiceInfo info = ri.serviceInfo;
            ComponentName component = new ComponentName(info.packageName, info.name);
            Log.i(Api.DATAPLUG_TAG, component.getClassName());
            String token = makeToken();
            tokens.put(token, component);
            Intent discoverIntent = new Intent(Api.ACTION_DISCOVER);
            discoverIntent.setComponent(component);
            discoverIntent.putExtra(Api.EXTRA_TOKEN, token);
            mContext.startService(discoverIntent);
        }
        // invoke authorization for new plugins
        AuthorizationActivity.startActivity(mContext, list);
        
        Log.i(Api.DATAPLUG_TAG, "dataplugs end.");
    }

    public void activatePlug(String accountId, String friendId, String uri) {
        Registration registration = registrations.get(uri);
        if (registration == null) {
            return;
        }

        Intent activateIntent = new Intent(Api.ACTION_ACTIVATE);
        activateIntent.setComponent(registration.getComponent());
        activateIntent.putExtra(Api.EXTRA_ACCOUNT_ID, accountId);
        activateIntent.putExtra(Api.EXTRA_FRIEND_ID, friendId);
        mContext.startService(activateIntent);
    }

    /**
     * Register a local DataPlug.
     * 
     * @param token
     * @param registration sample: { "descriptor": { "uri":
     *            "chatsecure:/gallery", "name": "Gallery" }, "meta": {
     *            "publish" : true } }
     */
    public void register(String token, String registration_json) {
        if (token == null || !tokens.containsKey(token)) {
            Log.e(Api.DATAPLUG_TAG, "unknown or null token");
            return;
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

    /** Find the DataPlug registration associated with a uri (matching prefix) */
    public Registration findRegistration(String uri) {
        for (String key : registrations.keySet()) {
            if (uri.startsWith(key)) {
                return registrations.get(key);
            }
        }
        return null;
    }

    /** Get the discovery JSON to be transmitted over the OTR channel */
    public String getDiscoveryPayload() {
        try {
            JSONObject payload = new JSONObject();
            payload.put("plugins", new JSONArray());
            for (Map.Entry<String, Registration> reg : registrations.entrySet()) {
                JSONObject entry = new JSONObject();
                entry.put("uri", reg.getKey());
                entry.put("name", reg.getValue().getDescriptor().getName());
                payload.accumulate("plugins", entry);
            }
            return payload.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /** Parse a discovery JSON we received over the OTR channel */
    public static List<Descriptor> parseDiscoveryPayload(String payload) throws JSONException {
        List<Descriptor> descs = Lists.newArrayList();
        JSONObject disco = new JSONObject(payload);
        JSONArray plugins = disco.getJSONArray("plugins");
        for (int i = 0; i < plugins.length(); i++) {
            JSONObject plugin = plugins.getJSONObject(i);
            Descriptor desc = new Descriptor();
            desc.setName(plugin.getString("name"));
            desc.setUri(plugin.getString("uri"));
            descs.add(desc);
        }
        return descs;
    }
}
