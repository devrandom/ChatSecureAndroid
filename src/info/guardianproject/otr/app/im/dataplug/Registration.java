package info.guardianproject.otr.app.im.dataplug;

import android.content.ComponentName;

/** The registration of a local DataPlug */
public class Registration {
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

    public Descriptor getDescriptor() {
        return descriptor;
    }
}