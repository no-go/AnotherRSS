package de.digisocken.anotherrss;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class PrefRegexActivity extends AppCompatActivity {
    private SharedPreferences _pref;
    EditText _edAll;
    EditText _edTo;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                storePref();
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pref_regex);
        _edAll = (EditText) findViewById(R.id.editRegexAll);
        _edTo = (EditText) findViewById(R.id.editRegexTo);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.regex));
        }
        _pref = PreferenceManager.getDefaultSharedPreferences(AnotherRSS.getContextOfApplication());
        loadPref();
    }

    @Override
    protected void onPause() {
        AnotherRSS.withGui = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        AnotherRSS.withGui = true;
        super.onResume();
    }

    private void loadPref() {
        _edAll.setText(_pref.getString("regexAll", AnotherRSS.Config.DEFAULT_regexAll));
        _edTo.setText(_pref.getString("regexTo", AnotherRSS.Config.DEFAULT_regexTo));
    }

    private void storePref() {
        SharedPreferences.Editor editor = _pref.edit();
        editor.putString("regexAll", _edAll.getText().toString());
        editor.putString("regexTo", _edTo.getText().toString());
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        storePref();
        super.onBackPressed();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        loadPref();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        storePref();
        super.onSaveInstanceState(outState);
    }
}
