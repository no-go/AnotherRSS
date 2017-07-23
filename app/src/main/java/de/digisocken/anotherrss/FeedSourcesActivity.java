package de.digisocken.anotherrss;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;

public class FeedSourcesActivity extends AppCompatActivity {
    private SharedPreferences _pref;
    private String[] _urls;
    private LinearLayout _linearLayout;
    private EditText[] _urlEdit;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                storeUrls();
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedsources);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.rss_url));
        }
        _pref = PreferenceManager.getDefaultSharedPreferences(AnotherRSS.getContextOfApplication());
        loadUrls();
    }

    @Override
    protected void onPause() {
        Log.d(AnotherRSS.TAG, "FeedSources Pref onPause");
        AnotherRSS.withGui = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(AnotherRSS.TAG, "FeedSources Pref onResume");
        AnotherRSS.withGui = true;
        super.onResume();
    }

    private void loadUrls() {
        _linearLayout = (LinearLayout) findViewById(R.id.feedsourceList);
        _linearLayout.removeAllViews();
        _urls = _pref.getString("rss_url", AnotherRSS.urls).split(" ");
        _urlEdit = new EditText[_urls.length +5];

        for (int i=0; i < _urlEdit.length; i++) {
            _urlEdit[i] = new EditText(this);
            if (i < _urls.length) _urlEdit[i].setText(_urls[i]);
            _linearLayout.addView(_urlEdit[i], i);
        }
    }

    private void storeUrls() {
        String newurls = "";
        for (int i=0; i < _urlEdit.length; i++) {
            String tmp = _urlEdit[i].getText().toString().trim().replace(" ", "%20");
            if (tmp != null && !tmp.equals("")) {
                newurls += tmp + " ";
            }
        }
        newurls = newurls.trim();
        _pref.edit().putString("rss_url", newurls).commit();
    }

    @Override
    public void onBackPressed() {
        storeUrls();
        super.onBackPressed();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        loadUrls();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        storeUrls();
        super.onSaveInstanceState(outState);
    }
}
