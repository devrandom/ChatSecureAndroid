/**
 * 
 */
package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

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
        View view = LayoutInflater.from(this).inflate(R.layout.dataplug_authorization_single, null);
        TextView packagenameTextView = (TextView) view.findViewById(R.id.dataplug_auth_packagename);
        packagenameTextView.setText( packageName );
        
        final AuthSpinner authSpinner = new AuthSpinner( this, (Spinner) view.findViewById(R.id.dataplug_auth_options) );
        
        builder.setTitle( R.string.dataplug_unknown_plugin_found);
        builder.setView(view);
        builder.setPositiveButton(getString(R.string.ok), new OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Imps.Dataplugs.Auth auth = authSpinner.getSelected();
                Imps.Dataplugs.authorize( getContentResolver(), packageName, auth);
                doList();
            }
        });
        builder.create().show();
    }
    
    class AuthSpinner {
        private Spinner mSpinner;
        private Imps.Dataplugs.Auth[] mPresets = {
            Imps.Dataplugs.Auth.ALLOW,
            Imps.Dataplugs.Auth.BLOCK,
            Imps.Dataplugs.Auth.ASK,
        };

        public AuthSpinner( Context aContext, Spinner aSpinner ) {
            mSpinner = aSpinner;
            init( aContext );
        }
        
        private void init(Context aContext) {
            String[] strings = aContext.getResources().getStringArray(R.array.dataplug_authorization_options);
            List<String> list = new ArrayList<String>();
            for( Imps.Dataplugs.Auth auth : mPresets ) {
                list.add( strings[ auth.getValue()] );
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(aContext, android.R.layout.simple_spinner_item, list);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(arrayAdapter);        
        }
        
        public Imps.Dataplugs.Auth getSelected() {
            int selected = mSpinner.getSelectedItemPosition();
            return mPresets[selected];
        }
        
    }
}
