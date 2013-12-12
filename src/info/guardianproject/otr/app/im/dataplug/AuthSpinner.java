/**
 * 
 */
package info.guardianproject.otr.app.im.dataplug;

import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.provider.Imps;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

class AuthSpinner {
    private Spinner mSpinner;
    // in order of display
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
        // copy the presets strings into the spinner
        for( Imps.Dataplugs.Auth auth : mPresets ) {
            list.add( strings[ auth.getValue()] );
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(aContext, android.R.layout.simple_spinner_item, list);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(arrayAdapter);     
    }
    
    public Imps.Dataplugs.Auth getSelectedItem() {
        int selected = mSpinner.getSelectedItemPosition();
        return mPresets[selected];
    }
    
    public void setSelection( Imps.Dataplugs.Auth aSelectionAuth ) {
        for( int i=0; i < mPresets.length ;i++ ) {
            if( aSelectionAuth.getValue() == mPresets[i].getValue() ) {
                mSpinner.setSelection(i);
                return;
            }
        }
        throw new RuntimeException("Invalid auth value:" + aSelectionAuth);
    }
    
    public void setOnItemSelectedListener( OnItemSelectedListener aListener ) {
        mSpinner.setOnItemSelectedListener(aListener);
    }
}