package de.digisocken.anotherrss;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * Das ist die Activity, die die Einstellungen zeigt.
 * Sie lädt das Layout, in dem das {@link MyPreferenceFragment} enthalten ist.
 * Aussderm setzt die Activity App.withGui auf true, wenn sie aktiv ist.
 */
public class PreferencesActivity extends AppCompatActivity {

    /**
     * Diese Methode ermöglicht das Verlassen der Activity über den Home-Button der Action Bar
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pref_main);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.preferences));
        }
    }

    @Override
    protected void onPause() {
        Log.d(AnotherRSS.TAG, "Pref onPause");
        AnotherRSS.withGui = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(AnotherRSS.TAG, "Pref onResume");
        AnotherRSS.withGui = true;
        super.onResume();
    }

    static public boolean storeArray(boolean[] array, String arrayName, Context mContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(arrayName +"_size", array.length);
        for(int i=0;i<array.length;i++)
            editor.putBoolean(arrayName + "_" + i, array[i]);
        return editor.commit();
    }

    static public boolean[] loadArray(String arrayName, Context mContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int size = prefs.getInt(arrayName + "_size", 0);
        boolean array[] = new boolean[size];
        for(int i=0;i<size;i++)
            array[i] = prefs.getBoolean(arrayName + "_" + i, false);
        return array;
    }
}

