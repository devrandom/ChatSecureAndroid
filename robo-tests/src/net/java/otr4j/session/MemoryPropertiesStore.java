package net.java.otr4j.session;

import java.util.Properties;

import net.java.otr4j.OtrKeyManagerStore;

import org.jivesoftware.smack.util.Base64;

class MemoryPropertiesStore implements OtrKeyManagerStore {
    private Properties properties = new Properties();

    public MemoryPropertiesStore() {
    }

    public void setProperty(String id, boolean value) {
        properties.setProperty(id, "true");
    }

    public void setProperty(String id, byte[] value) {
        properties.setProperty(id, new String(Base64.encodeBytes(value)));
    }

    public void removeProperty(String id) {
        properties.remove(id);

    }

    public byte[] getPropertyBytes(String id) {
        String value = properties.getProperty(id);

        if (value != null)
            return Base64.decode(value);
        return null;
    }

    public boolean getPropertyBoolean(String id, boolean defaultValue) {
        try {
            return Boolean.valueOf(properties.get(id).toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}