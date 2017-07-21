package de.digisocken.anotherrss;

import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.support.v7.app.AppCompatDelegate;

/**
 * Wird zum Laden der in XML abgelegten Preferences genutzt.
 * Es wird im Layout der {@link PreferencesActivity} genutzt.
 */
public class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private UiModeManager umm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        umm = (UiModeManager) getActivity().getSystemService(Context.UI_MODE_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals("nightmode_use")) {
            boolean night = sharedPreferences.getBoolean("nightmode_use", false);
            int startH = sharedPreferences.getInt("nightmode_use_start", AnotherRSS.Config.DEFAULT_NIGHT_START);
            int stopH = sharedPreferences.getInt("nightmode_use_stop", AnotherRSS.Config.DEFAULT_NIGHT_STOP);
            if (night && AnotherRSS.inTimeSpan(startH, stopH)) {
                umm.setNightMode(UiModeManager.MODE_NIGHT_YES);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                umm.setNightMode(UiModeManager.MODE_NIGHT_NO);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                getActivity().recreate();
            }
        }
    }
}
