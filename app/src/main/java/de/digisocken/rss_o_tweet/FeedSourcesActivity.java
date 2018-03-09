package de.digisocken.rss_o_tweet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;

public class FeedSourcesActivity extends AppCompatActivity {
    private SharedPreferences _pref;
    private ArrayList<String> _urls;
    private boolean _active[];
    private LinearLayout _linearLayout;
    private ArrayList<EditText> _urlEdit;
    private ArrayList<CheckBox> _urlCheck;

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
        setContentView(R.layout.pref_sources);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.rss_url));
        }
        _pref = PreferenceManager.getDefaultSharedPreferences(RssOTweet.getContextOfApplication());
        loadUrls();
    }

    @Override
    protected void onPause() {
        Log.d(RssOTweet.TAG, "FeedSources Pref onPause");
        RssOTweet.withGui = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(RssOTweet.TAG, "FeedSources Pref onResume");
        RssOTweet.withGui = true;
        super.onResume();
    }

    public void addLine(View v) {
        int id = _urlEdit.size();
        _active = Arrays.copyOf(_active, _active.length +1);
        _active[id] = false;
        _urlCheck.add(id, new CheckBox(this));
        _urlEdit.add(id, new EditText(this));
        _linearLayout.addView(_urlCheck.get(id), id);
        _linearLayout.addView(_urlEdit.get(id), id+1);
    }

    private void loadUrls() {
        _linearLayout = (LinearLayout) findViewById(R.id.feedsourceList);
        _linearLayout.removeAllViews();
        String urls[] = _pref.getString("rss_url", RssOTweet.urls).split(" ");
        _active = PreferencesActivity.loadArray("rss_url_act", RssOTweet.getContextOfApplication());
        _urlCheck = new ArrayList<>();
        _urlEdit  = new ArrayList<>();

        for (int i=0; i < urls.length + 5; i++) {
            _urlCheck.add(i, new CheckBox(this));
            _urlEdit.add(i, new EditText(this));
            if (i < urls.length) {
                _urlCheck.get(i).setChecked(_active[i]);
                _urlEdit.get(i).setText(urls[i]);
            }

            if (i >= _active.length) {
                _active = Arrays.copyOf(_active, _active.length +1);
                _active[i] = false;
            }
            _linearLayout.addView(_urlCheck.get(i), i);
            _linearLayout.addView(_urlEdit.get(i), i+1);
        }
    }

    private void storeUrls() {
        String newurls = "";
        int i=0;
        int ari=0;
        for (i=0; i < _urlEdit.size(); ) {
            String tmp = _urlEdit.get(i).getText().toString().trim().replace(" ", "%20");
            if (tmp != null && !tmp.equals("")) {
                newurls += tmp + " ";
                _active[ari] = _urlCheck.get(i).isChecked();
                // only write bool value for existing urls
                ari++;
            }
            i++;
        }
        // trim the bool array to the real array size
        _active = Arrays.copyOf(_active, ari);

        newurls = newurls.trim();
        PreferencesActivity.storeArray(_active, "rss_url_act", RssOTweet.getContextOfApplication());
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
