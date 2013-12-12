/**
 * 
 */
package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.provider.Imps.DataplugsColumns.Auth;
import info.guardianproject.otr.app.im.provider.Imps.DataplugsColumns.AuthItem;

import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class AuthAdapter extends BaseAdapter {

    private List<AuthItem> mList;
    private Context mContext;

    public AuthAdapter(Context aContext) {
        mContext = aContext;
        mList = Imps.Dataplugs.getAll(aContext.getContentResolver());
        for( AuthItem item : mList ) {
            PackageManager pm = aContext.getApplicationContext().getPackageManager();
            try {
                ApplicationInfo ai = pm.getApplicationInfo( item.packageName, 0);
                item.applicationLabel = (String) pm.getApplicationLabel(ai);              
                item.applicationIcon = pm.getApplicationIcon( item.packageName ); 
            } catch (final NameNotFoundException e) {
                throw new RuntimeException("Invalid package " + item.packageName);
            }
        }
            
    }
    
    public List<AuthItem> getList() {
        return mList;
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getCount()
     */
    @Override
    public int getCount() {
        return mList.size();
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view;
        if( convertView == null ) {
            view = LayoutInflater.from(mContext).inflate(R.layout.dataplug_authorization_list_item, null);
        } else {
            view = convertView;
        }
        AuthItem item = (AuthItem) getItem(position);
        // package name
        ((TextView) view.findViewById(R.id.dataplug_auth_applabel)).setText( item.applicationLabel );
        ((TextView) view.findViewById(R.id.dataplug_auth_packagename)).setText( item.packageName );
        ((ImageView) view.findViewById(R.id.dataplug_auth_icon)).setImageDrawable(item.applicationIcon);
        // spinner
        final AuthSpinner authSpinner = new AuthSpinner( mContext, (Spinner) view.findViewById(R.id.dataplug_auth_options) );
        authSpinner.setSelection( ((AuthItem)mList.get(position)).auth );
        authSpinner.setOnItemSelectedListener( new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                ((AuthItem)mList.get(position)).auth = authSpinner.getSelectedItem();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return view;
    }
}