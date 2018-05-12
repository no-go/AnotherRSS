package de.digisocken.anotherrss;

import android.app.NotificationManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Diese Activity stellt die Liste der Feeds dar. Die Liste selbst
 * ist in {@link FeedListFragment} zu finden.
 */
public class MainActivity extends AppCompatActivity {

    private static final String PROJECT_LINK = "https://no-go.github.io/AnotherRSS/";
    private static final String FLATTR_ID = "o6wo7q";
    private String FLATTR_LINK;

    public Context ctx;
    private BroadcastReceiver alarmReceiver;
    private WebView webView;
    private VideoView videoView;
    private ProgressBar progressBar;
    private UiModeManager umm;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        super.onCreateOptionsMenu(menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        MenuItem sizeItem = menu.findItem(R.id.size_info);
        File f = this.getDatabasePath(FeedHelper.DATABASE_NAME);
        long dbSize = f.length();
        sizeItem.setTitle(String.valueOf(dbSize/1024) + getString(R.string.kB_used));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String msg = getString(R.string.searching) + " " + query;
                AnotherRSS.query = query;
                FeedListFragment fr = (FeedListFragment) getFragmentManager().findFragmentById(R.id.feedlist);
                fr.getLoaderManager().restartLoader(0, null, fr);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                AnotherRSS.query = "";
                FeedListFragment fr = (FeedListFragment) getFragmentManager().findFragmentById(R.id.feedlist);
                fr.getLoaderManager().restartLoader(0, null, fr);
                Toast.makeText(getApplicationContext(), R.string.close_search, Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Toast.makeText(getApplicationContext(), R.string.start_search, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DbClear dbClear = new DbClear();
        int size;
        float fontSize;

        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        switch (item.getItemId()) {
            case R.id.action_flattr:
                Intent intentFlattr = new Intent(Intent.ACTION_VIEW, Uri.parse(FLATTR_LINK));
                startActivity(intentFlattr);
                break;
            case R.id.action_project:
                Intent intentProj= new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_LINK));
                startActivity(intentProj);
                break;
            case R.id.action_feedsources:
                Intent intentfs = new Intent(MainActivity.this, FeedSourcesActivity.class);
                intentfs.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intentfs);
                break;
            case R.id.action_regex:
                Intent intentreg = new Intent(MainActivity.this, PrefRegexActivity.class);
                intentreg.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intentreg);
                break;
            case R.id.action_preferences:
                Intent intent = new Intent(MainActivity.this, PreferencesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                break;
            case R.id.action_delNotifies:
                String ns = Context.NOTIFICATION_SERVICE;
                NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
                nMgr.cancelAll();
                break;
            case R.id.action_readedFeeds:
                dbClear.execute(R.id.action_readedFeeds);
                break;
            case R.id.action_delFeeds:
                dbClear.execute(R.id.action_delFeeds);
                break;
            case R.id.action_biggerText:
                fontSize = mPreferences.getFloat("font_size", AnotherRSS.Config.DEFAULT_FONT_SIZE);
                fontSize = fontSize * 1.1f;
                mPreferences.edit().putFloat("font_size", fontSize).apply();
                break;
            case R.id.action_smallerText:
                fontSize = mPreferences.getFloat("font_size", AnotherRSS.Config.DEFAULT_FONT_SIZE);
                fontSize = fontSize * 0.9f;
                if (fontSize < 3.0f) fontSize = 3.0f;
                mPreferences.edit().putFloat("font_size", fontSize).apply();
                break;
            case R.id.action_biggerImageSize:
                size = mPreferences.getInt("image_width", AnotherRSS.Config.DEFAULT_MAX_IMG_WIDTH);
                size = size + 20;
                mPreferences.edit().putInt("image_width", size).apply();
                break;
            case R.id.action_smallerImageSize:
                size = mPreferences.getInt("image_width", AnotherRSS.Config.DEFAULT_MAX_IMG_WIDTH);
                size = size - 10;
                if (size < 0) size = 0;
                mPreferences.edit().putInt("image_width", size).apply();
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * Beinhaltet alle Start-Funktionen der App.
     * Funktionen:
     * <ul>
     *     <li>Alarm (neu) Starten</li>
     *     <li>Datenbank bereinigen (gelöschte Feeds entfernen)</li>
     *     <li>Ein BroadcastReceiver() wird registriert, um nach neuen Feeds durch den Alarm zu horchen</li>
     * </ul>
     * Außerdem wird das Icon in die ActionBar eingefügt.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(AnotherRSS.TAG, "onCreate");
        ctx = this;

        try {
            FLATTR_LINK = "https://flattr.com/submit/auto?fid="+FLATTR_ID+"&url="+
                    java.net.URLEncoder.encode(PROJECT_LINK, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main);
        umm = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        AnotherRSS.alarm.restart(this);;

        try {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayShowHomeEnabled(true);
                ab.setHomeButtonEnabled(true);
                ab.setDisplayUseLogoEnabled(true);
                ab.setLogo(R.drawable.ic_launcher);
                ab.setTitle(" " + getString(R.string.app_name));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        alarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(getString(R.string.serviceHasNews))) {
                    int countNews = intent.getIntExtra("count", 0);
                    Toast.makeText(
                            ctx,
                            getString(R.string.newFeeds) + ": " + countNews,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        };

        videoView = (VideoView) findViewById(R.id.videoView);
        webView = (WebView) findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.serviceHasNews));
        registerReceiver(alarmReceiver, filter);
    }

    public boolean setMediaView(String url) {
        if (videoView == null) {
            if (url.endsWith(".mp4")) {
                Intent vintent = new Intent(MainActivity.this, VideocastActivity.class);
                vintent.setData(Uri.parse(url));
                startActivity(vintent);
                return true;
            }
            return false;
        }
        if (webView == null) return false;
        if (url.endsWith(".mp4")) {
            webView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            Uri uri = Uri.parse(url);
            videoView.setMediaController(AnotherRSS.mediaController);
            videoView.setVideoURI(uri);
            videoView.requestFocus();
            videoView.start();
        } else {
            webView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
            webView.setWebViewClient(new MyWebClient());
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.loadUrl(url);
        }
        return true;
    }

    public class MyWebClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE);
            super.onPageFinished(view, url);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(alarmReceiver);
    }

    @Override
    protected void onPause() {
        Log.d(AnotherRSS.TAG, "onPause");
        AnotherRSS.withGui = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(AnotherRSS.TAG, "onResume");
        AnotherRSS.withGui = true;
        new DbExpunge().execute();

        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean night = mPreferences.getBoolean("nightmode_use", false);
        if (night) {
            int startH = mPreferences.getInt("nightmode_use_start", AnotherRSS.Config.DEFAULT_NIGHT_START);
            int stopH = mPreferences.getInt("nightmode_use_stop", AnotherRSS.Config.DEFAULT_NIGHT_STOP);
            if (AnotherRSS.inTimeSpan(startH, stopH) && umm.getNightMode() != UiModeManager.MODE_NIGHT_YES) {
                umm.setNightMode(UiModeManager.MODE_NIGHT_YES);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            if (!AnotherRSS.inTimeSpan(startH, stopH) && umm.getNightMode() != UiModeManager.MODE_NIGHT_NO) {
                umm.setNightMode(UiModeManager.MODE_NIGHT_NO);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        } else {
            if (umm.getNightMode() == UiModeManager.MODE_NIGHT_YES) {
                umm.setNightMode(UiModeManager.MODE_NIGHT_NO);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }
        super.onResume();
    }

    /**
     * Setzt unterschiedliche Lösch-Operationen in der DB um.
     */
    private class DbClear extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            ContentValues values = new ContentValues();
            String sel = FeedContract.Feeds.COLUMN_Flag + "<> ?";
            String[] selArgs = {Integer.toString(FeedContract.Flag.FAVORITE)};
            switch (params[0]) {
                case R.id.action_delFeeds:
                    values.put(FeedContract.Feeds.COLUMN_Deleted, FeedContract.Flag.DELETED);
                    getContentResolver().update(FeedContentProvider.CONTENT_URI, values, sel, selArgs);
                    break;
                case R.id.action_readedFeeds:
                    values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.READED);
                    getContentResolver().update(FeedContentProvider.CONTENT_URI, values, sel, selArgs);
                    break;
                default:
                    break;
            }
            return null;
        }
    }

    /***
     * Dient zum Beseitigen von gelöschten Feeds. Achtung! Wird nur gemacht,
     * wenn man die App auch öffnet!
     */
    private class DbExpunge extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

            SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String[] urls = mPreferences.getString("rss_url", AnotherRSS.urls).split(" ");

            for (int urli=0; urli < urls.length; urli++) {
                Date date = new Date();
                c.setTime(date);
                c.add(Calendar.DAY_OF_MONTH, -1 * AnotherRSS.Config.DEFAULT_expunge);
                date = c.getTime();
                String dateStr = FeedContract.dbFriendlyDate(date);

                String where = FeedContract.Feeds.COLUMN_Date + "<? and "
                        + FeedContract.Feeds.COLUMN_Deleted + "=? and "
                        + FeedContract.Feeds.COLUMN_Source + "=?";
                getContentResolver().delete(
                        FeedContentProvider.CONTENT_URI,
                        where,
                        new String[]{
                                dateStr, Integer.toString(FeedContract.Flag.DELETED),
                                // Integer.toString(AnotherRSS.Source1.id)
                                Integer.toString(urli)
                        }
                );
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            new AutoDelete().execute();
        }
    }

    private class AutoDelete extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            int autodeleteDays = mPreferences.getInt("autodelete", AnotherRSS.Config.DEFAULT_autodelete);
            if (autodeleteDays < 1) return null;

            Date date = new Date();
            c.setTime(date);
            c.add(Calendar.DAY_OF_MONTH, -1 * autodeleteDays);
            date = c.getTime();
            String dateStr = FeedContract.dbFriendlyDate(date);

            String where = FeedContract.Feeds.COLUMN_Date + "<? and "
                    + FeedContract.Feeds.COLUMN_Flag + "<> ?";

            ContentValues values = new ContentValues();
            values.put(FeedContract.Feeds.COLUMN_Deleted, FeedContract.Flag.DELETED);

            getContentResolver().update(
                    FeedContentProvider.CONTENT_URI,
                    values,
                    where,
                    new String[]{dateStr, Integer.toString(FeedContract.Flag.FAVORITE)}
            );

            return null;
        }
    }
}
