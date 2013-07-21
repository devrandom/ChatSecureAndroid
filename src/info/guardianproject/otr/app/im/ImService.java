package info.guardianproject.otr.app.im;

import android.content.Context;

public interface ImService {
    public void showToast(CharSequence text, int duration);
    public Context getApplicationContext();
    public IImConnection getConnectionForProvider(long provider);
    public IImConnection getConnectionForLocalAddress(String accountId);
}
