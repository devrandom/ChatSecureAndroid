package info.guardianproject.otr.app.im.dataplug;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Part of the registration of a local DataPlug.
 * 
 * @author devrandom
 *
 */
public class Descriptor implements Parcelable {
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

    public static final Parcelable.Creator<Descriptor> CREATOR = new Parcelable.Creator<Descriptor>() {
        public Descriptor createFromParcel(Parcel in) {
            Descriptor desc = new Descriptor();
            desc.setName(in.readString());
            desc.setUri(in.readString());
            return desc;
        }

        public Descriptor[] newArray(int size) {
            return new Descriptor[size];
        }
    };

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(uri);
    }
}