/**
 * 
 */
package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.otr.dataplug.Api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Debug;
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
    public static void startActivity(Context mContext, List<ResolveInfo> list) {
        Debug.waitForDebugger();
        Intent intent = new Intent(mContext, AuthorizationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_LIST, list.toArray(new Parcelable[0])) ;
        mContext.startActivity(intent);
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
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
            if( isAuthorized(info.packageName)) {
                continue;
            }
            authorize( info.packageName );
            return ;
        }
        // no more
        finish();
    }

    /**
     * @param packageName
     */
    private void authorize(String packageName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("OK", new OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doList();
            }
        });
        builder.setMessage(packageName);
        builder.create().show();
    }

    /**
     * @param packageName
     * @return
     */
    private boolean isAuthorized(String packageName) {
        return false;
    }

}
