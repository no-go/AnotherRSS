package de.digisocken.anotherrss;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedSourcesActivity extends AppCompatActivity {
    private SharedPreferences _pref;
    private ArrayList<String> _urls;
    private boolean _active[];
    private LinearLayout _linearLayout;
    private ArrayList<EditText> _urlEdit;
    private ArrayList<CheckBox> _urlCheck;
    private Uri datafile = null;

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
        _pref = PreferenceManager.getDefaultSharedPreferences(AnotherRSS.getContextOfApplication());
        loadUrls();

        File file = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            file = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    AnotherRSS.PACKAGE_NAME
            );
        } else {
            file = new File(Environment.getExternalStorageDirectory() + "/Documents/"+AnotherRSS.PACKAGE_NAME);
        }
        String path = file.getPath() + AnotherRSS.OPML_FILENAME;
        try {
            Log.d(AnotherRSS.TAG, "mkdirs()");
            file.mkdirs();
            file = new File(path);
            if (!file.exists()) file.createNewFile();
            datafile = Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            if (!data.toString().startsWith("http")) {
                try {
                    InputStream input = getContentResolver().openInputStream(data);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));

                    String text = "";
                    while (bufferedReader.ready()) {
                        text += bufferedReader.readLine() + "\n";
                    }
                    Pattern feedurl = Pattern.compile("xmlUrl=\"(.*?)\"");
                    Matcher match = feedurl.matcher(text);

                    while (match.find()) {
                        int id = _urlEdit.size();
                        _active = Arrays.copyOf(_active, _active.length + 1);
                        _active[id] = true;
                        CheckBox checkBox = new CheckBox(this);
                        EditText editText = new EditText(this);
                        checkBox.setChecked(true);
                        editText.setText(match.group(1));
                        _urlCheck.add(id, checkBox);
                        _urlEdit.add(id, editText);

                        LinearLayout dummy = new LinearLayout(AnotherRSS.getContextOfApplication());
                        dummy.setOrientation(LinearLayout.HORIZONTAL);
                        dummy.addView(checkBox, 0);
                        dummy.addView(editText, 1);
                        editText.setMinWidth(AnotherRSS.Config.DEFAULT_MAX_IMG_WIDTH);
                        _linearLayout.addView(dummy, id);
                    }
                    storeUrls();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                int id = _urlEdit.size();
                _active = Arrays.copyOf(_active, _active.length + 1);
                _active[id] = true;
                CheckBox checkBox = new CheckBox(this);
                EditText editText = new EditText(this);
                checkBox.setChecked(true);
                editText.setText(data.toString());
                _urlCheck.add(id, checkBox);
                _urlEdit.add(id, editText);

                LinearLayout dummy = new LinearLayout(AnotherRSS.getContextOfApplication());
                dummy.setOrientation(LinearLayout.HORIZONTAL);
                dummy.addView(checkBox, 0);
                dummy.addView(editText, 1);
                editText.setMinWidth(AnotherRSS.Config.DEFAULT_MAX_IMG_WIDTH);
                _linearLayout.addView(dummy, id);
                storeUrls();
                editText.requestFocus();
            }
        }
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

    public void addLine(View v) {
        int id = _urlEdit.size();
        _active = Arrays.copyOf(_active, _active.length +1);
        _active[id] = false;
        _urlCheck.add(id, new CheckBox(this));
        _urlEdit.add(id, new EditText(this));

        LinearLayout dummy = new LinearLayout(AnotherRSS.getContextOfApplication());
        dummy.setOrientation(LinearLayout.HORIZONTAL);
        dummy.addView(_urlCheck.get(id), 0);
        dummy.addView(_urlEdit.get(id), 1);
        _urlEdit.get(id).setMinWidth(AnotherRSS.Config.DEFAULT_MAX_IMG_WIDTH);
        _linearLayout.addView(dummy, id);
    }

    private void loadUrls() {
        _linearLayout = (LinearLayout) findViewById(R.id.feedsourceList);
        _linearLayout.removeAllViews();
        String urls[] = _pref.getString("rss_url", AnotherRSS.urls).split(" ");
        _active = PreferencesActivity.loadArray("rss_url_act", AnotherRSS.getContextOfApplication());
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
            LinearLayout dummy = new LinearLayout(AnotherRSS.getContextOfApplication());
            dummy.setOrientation(LinearLayout.HORIZONTAL);
            dummy.addView(_urlCheck.get(i), 0);
            dummy.addView(_urlEdit.get(i), 1);
            _urlEdit.get(i).setMinWidth(AnotherRSS.Config.DEFAULT_MAX_IMG_WIDTH);
            _linearLayout.addView(dummy, i);
        }
    }

    private void storeUrls() {
        String newurls = "";
        String toOpml = "";
        int i=0;
        int ari=0;
        for (i=0; i < _urlEdit.size(); ) {
            String tmp = _urlEdit.get(i).getText().toString().trim().replace(" ", "%20");
            if (tmp != null && !tmp.equals("")) {
                newurls += tmp + " ";
                _active[ari] = _urlCheck.get(i).isChecked();
                if (_active[ari]) toOpml += tmp + " ";
                // only write bool value for existing urls
                ari++;
            }
            i++;
        }
        // trim the bool array to the real array size
        _active = Arrays.copyOf(_active, ari);

        saveNow(toOpml.trim().split(" "));

        newurls = newurls.trim();
        PreferencesActivity.storeArray(_active, "rss_url_act", AnotherRSS.getContextOfApplication());
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

    void saveNow(String toOpml[]) {
        if (datafile != null) {
            String path = datafile.getPath();
            if (path != null) {
                try {
                    Log.d(AnotherRSS.TAG, "saveNow()");
                    path = PathUtil.getPath(getApplicationContext(), datafile);
                    File file = new File(path);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write("<opml version=\"2.0\">\n\t<body>\n\t\t<outline text=\"Subscriptions\" title=\"Subscriptions\">\n".getBytes());
                    for (int i=0; i < toOpml.length; i++) {
                        fos.write(("\t\t\t<outline xmlUrl=\""+toOpml[i]+"\" />\n").getBytes());
                    }
                    fos.write("\t\t</outline>\n\t</body>\n</opml>\n".getBytes());
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
