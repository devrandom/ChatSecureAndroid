/**
 * 
 */
package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.otr.app.im.provider.Imps;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Parcelable;

/**
 *
 * @author liorsaar
 *
 */
public class AuthorizationActivity extends Activity {

    public static final String EXTRA_LIST = "list";
    private ArrayList<ResolveInfo> mList;

    /**
     * @param mContext 
     * @param list
     */
    public static boolean startActivity(Activity aActivity, List<ResolveInfo> list, int aRequestCode) {
        List<ResolveInfo> unknowns = Lists.newArrayList();
        for (ResolveInfo ri : list) {
            ServiceInfo info = ri.serviceInfo;
            if( ! Imps.Dataplugs.isKnown(aActivity.getContentResolver(), info.packageName)) {
                unknowns.add(ri);
            }
        }
        
        if (unknowns.isEmpty())
            return false;
        
        Intent intent = new Intent(aActivity, AuthorizationActivity.class);
        intent.putExtra(EXTRA_LIST, list.toArray(new Parcelable[0])) ;
        aActivity.startActivityForResult(intent, aRequestCode) ;
        return true;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parcelable[] p = getIntent().getParcelableArrayExtra(EXTRA_LIST);
        mList = new ArrayList<ResolveInfo>();
        for (int i = 0 ; i < p.length ; i++)
            mList.add((ResolveInfo)p[i]);
        doList();
    }

    private void doList() {
        while( ! mList.isEmpty() ) {
            ResolveInfo ri = mList.remove(0);
            ServiceInfo info = ri.serviceInfo;
            if( Imps.Dataplugs.isKnown(getContentResolver(), info.packageName)) {
                continue;
            }
            authorize(info.packageName );
            return ;
        }
        // no more
        setResult(RESULT_OK);
        finish();
    }

    private void authorize(final String packageName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("OK", new OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO add UI for all options
                Imps.Dataplugs.Auth auth = Imps.Dataplugs.Auth.ALLOW;
                Imps.Dataplugs.authorize( getContentResolver(), packageName, auth);
                doList();
            }
        });
        builder.setMessage(packageName);
        builder.create().show();
    }
}
