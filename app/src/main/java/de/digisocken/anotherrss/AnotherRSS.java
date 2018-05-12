package de.digisocken.anotherrss;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.media.MediaPlayer;
import android.widget.MediaController;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Der Einstiegspunkt des Launchers.
 * In der Applications-Klasse befinden sich auch die initalen
 * Konfigurationen der SharedPreferences sowie einige Konstanten.
 *
 * @author Jochen Peters
 */
public class AnotherRSS extends Application {
    public static boolean showAdditionalFeed = true;
    public static String query = "";
    public static MediaPlayer mediaPlayer;
    public static MediaController mediaController;

    public static final String urls =
            "http://www.tagesschau.de/xml/rss2 " +
                    "http://www.taz.de/!p4608;rss/ " +
                    "http://www.deutschlandfunk.de/die-nachrichten.353.de.rss " +
                    "http://digisocken.de/_p/wdrWetter/?rss=true " +
                    "http://feeds.bbci.co.uk/news/world/europe/rss.xml " +
                    "http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml " +
                    "http://feeds.t-online.de/rss/nachrichten " +
                    "http://www.wz.de/cmlink/wz-rss-uebersicht-1.516698 " +
                    "https://www.heise.de/security/news/news-atom.xml " +
                    "https://www.amnesty.de/rss/news " +
                    "https://www.umwelt.nrw.de/rss.xml " +
                    "http://feeds.reuters.com/Reuters/UKWorldNews " +
                    "http://feeds.reuters.com/reuters/scienceNews?format=xml " +
                    "https://www1.wdr.de/mediathek/audio/wdr5/wdr5-alles-in-butter/alles-in-butter106.podcast " +
                    "https://www1.wdr.de/mediathek/audio/wdr5/polit-wg/polit-wg-104.podcast " +
                    "https://www.tagesschau.de/export/video-podcast/webm/tagesschau-in-100-sekunden_https " +
                    "https://thebugcast.org/feed/ogg " +
                    "http://feeds.feedburner.com/daily_tech_news_show?format=xml " +
                    "http://www.wetterleitstelle.de/nordrhein-westfalen.xml";

    public static final boolean feedActive[] = {
            true,
            true,
            false,
            true,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            false,
            true,
            false,
            true,
            true,
            false,
            false
    };

    public static class Config {
        /**
         * really delete old database entries (marked as deleted)
         * older than Config.DEFAULT_expunge days
         */
        public static final int DEFAULT_autodelete = 14;
        public static final int DEFAULT_expunge = 5;
        public static final String DEFAULT_rsssec = "10800";
        public static final String DEFAULT_notifySound = "0";
        public static final String DEFAULT_notifyColor = "#FF00FFFF";
        public static final String DEFAULT_notifyType = "2";
        public static final int DEFAULT_NIGHT_START = 18;
        public static final int DEFAULT_NIGHT_STOP = 6;
        public static final String SEARCH_HINT_COLOR = "#FFAA00";
        public static final float DEFAULT_FONT_SIZE = 14.0f;
        public static final int DEFAULT_MAX_IMG_WIDTH = 120;
        public static final float IMG_ROUND = 15f;

        public static final String DEFAULT_regexAll = "";
        public static final String DEFAULT_regexTo = "";
        public static final boolean DEFAULT_OFFLINEHINT = false;

        /**
         * sollte eine Verbindung nicht zu sande kommen, wird ein neuer
         * Alarm in {@value #RETRYSEC_AFTER_OFFLINE} sec ausgelöst
         */
        public static final long RETRYSEC_AFTER_OFFLINE = 75L;
    }

    public static Alarm alarm = null;

    /**
     * So kann der {@link Refresher} erkennen, ob er nur im Hintergrund läuft.
     * Wäre withGui auf true, wird nur eine HeadUp Notifikation gezeigt.
     * An dieser Stelle wird klar, dass der Alarm <i>doch</i> auf this zugreifen kann (?)
     */
    public static boolean withGui = false;
    public static final String TAG = AnotherRSS.class.getSimpleName();
    private static Context contextOfApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        contextOfApplication = getApplicationContext();
        mediaPlayer = new MediaPlayer();

        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!mPreferences.contains("rss_url")) {
            mPreferences.edit().putString("rss_url", AnotherRSS.urls).commit();
            PreferencesActivity.storeArray(feedActive, "rss_url_act", getApplicationContext());
        } else {
            if (!mPreferences.contains("rss_url_act_0")) {
                String[] urls = mPreferences.getString("rss_url", AnotherRSS.urls).split(" ");
                boolean act[] = new boolean[urls.length];
                Arrays.fill(act, true);
                PreferencesActivity.storeArray(act, "rss_url_act", getApplicationContext());
            }
        }
        if (!mPreferences.contains("regexAll")) {
            mPreferences.edit().putString("regexAll", Config.DEFAULT_regexAll).commit();
        }
        if (!mPreferences.contains("regexTo")) {
            mPreferences.edit().putString("regexTo", Config.DEFAULT_regexTo).commit();
        }
        if (!mPreferences.contains("nightmode_use_start")) {
            mPreferences.edit().putInt("nightmode_use_start", Config.DEFAULT_NIGHT_START).commit();
        }
        if (!mPreferences.contains("nightmode_use_stop")) {
            mPreferences.edit().putInt("nightmode_use_stop", Config.DEFAULT_NIGHT_STOP).commit();
        }
        if (!mPreferences.contains("offline_hint")) {
            mPreferences.edit().putBoolean("offline_hint", Config.DEFAULT_OFFLINEHINT).commit();
        }
        if (!mPreferences.contains("autodelete")) {
            mPreferences.edit().putInt("autodelete", Config.DEFAULT_autodelete).commit();
        }

        if (alarm == null) alarm = new Alarm();
    }

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    public static boolean inTimeSpan(int startH, int stopH) {
        int nowH = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (startH == stopH && startH == nowH) return true;
        if (startH > stopH && (nowH <= stopH || nowH >= startH)) return true;
        if (startH < stopH && nowH >= startH && nowH <= stopH) return true;
        return false;
    }
}
